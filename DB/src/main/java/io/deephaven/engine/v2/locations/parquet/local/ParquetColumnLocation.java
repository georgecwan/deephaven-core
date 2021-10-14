/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */

package io.deephaven.engine.v2.locations.parquet.local;

import io.deephaven.base.verify.Assert;
import io.deephaven.base.verify.Require;
import io.deephaven.configuration.Configuration;
import io.deephaven.engine.tables.CodecLookup;
import io.deephaven.engine.tables.ColumnDefinition;
import io.deephaven.engine.tables.dbarrays.DbArrayBase;
import io.deephaven.engine.v2.locations.TableDataException;
import io.deephaven.engine.v2.locations.impl.AbstractColumnLocation;
import io.deephaven.engine.v2.locations.parquet.ColumnChunkPageStore;
import io.deephaven.engine.v2.locations.parquet.PageCache;
import io.deephaven.engine.v2.locations.parquet.topage.*;
import io.deephaven.engine.v2.parquet.ParquetInstructions;
import io.deephaven.engine.v2.parquet.ParquetSchemaReader;
import io.deephaven.engine.v2.parquet.ParquetTableWriter;
import io.deephaven.engine.v2.parquet.metadata.CodecInfo;
import io.deephaven.engine.v2.parquet.metadata.ColumnTypeInfo;
import io.deephaven.engine.v2.parquet.metadata.GroupingColumnInfo;
import io.deephaven.engine.v2.parquet.metadata.TableInfo;
import io.deephaven.engine.v2.sources.chunk.Attributes.Any;
import io.deephaven.engine.v2.sources.chunk.Attributes.DictionaryKeys;
import io.deephaven.engine.v2.sources.chunk.Attributes.UnorderedKeyIndices;
import io.deephaven.engine.v2.sources.chunk.Attributes.Values;
import io.deephaven.engine.v2.sources.chunk.*;
import io.deephaven.engine.v2.sources.regioned.*;
import io.deephaven.engine.v2.utils.ChunkBoxer;
import io.deephaven.engine.v2.utils.OrderedKeys;
import io.deephaven.internal.log.LoggerFactory;
import io.deephaven.io.logger.Logger;
import io.deephaven.parquet.ColumnChunkReader;
import io.deephaven.parquet.ParquetFileReader;
import io.deephaven.parquet.RowGroupReader;
import io.deephaven.parquet.tempfix.ParquetMetadataConverter;
import io.deephaven.util.codec.CodecCache;
import io.deephaven.util.codec.ObjectCodec;
import io.deephaven.util.codec.SimpleByteArrayCodec;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.PrimitiveType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.deephaven.engine.v2.parquet.ParquetTableWriter.*;
import static io.deephaven.engine.v2.sources.regioned.RegionedColumnSource.ELEMENT_INDEX_TO_SUB_REGION_ELEMENT_INDEX_MASK;

final class ParquetColumnLocation<ATTR extends Values> extends AbstractColumnLocation {

    private static final String IMPLEMENTATION_NAME = ParquetColumnLocation.class.getSimpleName();

    private static final int CHUNK_SIZE = Configuration.getInstance()
            .getIntegerForClassWithDefault(ParquetColumnLocation.class, "chunkSize", 4096);
    private static final int PAGE_CACHE_SIZE =
            Configuration.getInstance().getIntegerForClassWithDefault(ParquetColumnLocation.class, "pageCacheSize",
                    1 << 13);

    private static final Logger log = LoggerFactory.getLogger(ParquetColumnLocation.class);

    private final String parquetColumnName;
    /**
     * Factory object needed for deferred initialization of the remaining fields. Reference serves as a barrier to
     * ensure visibility of the derived fields.
     */
    private volatile ColumnChunkReader[] columnChunkReaders;
    private final boolean hasGroupingTable;

    // We should consider moving this to column level if needed. Column-location level likely allows more parallelism.
    private final PageCache<ATTR> pageCache = new PageCache<>(PAGE_CACHE_SIZE);

    private ColumnChunkPageStore<ATTR>[] pageStores;
    private Supplier<Chunk<ATTR>>[] dictionaryChunkSuppliers;
    private ColumnChunkPageStore<DictionaryKeys>[] dictionaryKeysPageStores;

    /**
     * Construct a new {@link ParquetColumnLocation} for the specified {@link ParquetTableLocation} and column name.
     *
     * @param tableLocation The table location enclosing this column location
     * @param parquetColumnName The Parquet file column name
     * @param columnChunkReaders The {@link ColumnChunkReader column chunk readers} for this location
     * @param hasGroupingTable Whether this column has an associated grouping table file
     */
    ParquetColumnLocation(@NotNull final ParquetTableLocation tableLocation,
            @NotNull final String columnName,
            @NotNull final String parquetColumnName,
            @Nullable final ColumnChunkReader[] columnChunkReaders,
            final boolean hasGroupingTable) {
        super(tableLocation, columnName);
        this.parquetColumnName = parquetColumnName;
        this.columnChunkReaders = columnChunkReaders;
        this.hasGroupingTable = hasGroupingTable;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // AbstractColumnLocation implementation
    // -----------------------------------------------------------------------------------------------------------------

    @Override
    public String getImplementationName() {
        return IMPLEMENTATION_NAME;
    }

    @Override
    public boolean exists() {
        // If we see a null columnChunkReaders array, either we don't exist or we are guaranteed to
        // see a non-null
        // pageStores array
        return columnChunkReaders != null || pageStores != null;
    }

    private ParquetTableLocation tl() {
        return (ParquetTableLocation) getTableLocation();
    }

    private static final ColumnDefinition<Long> FIRST_KEY_COL_DEF =
            ColumnDefinition.ofLong("__firstKey__");
    private static final ColumnDefinition<Long> LAST_KEY_COL_DEF =
            ColumnDefinition.ofLong("__lastKey__");

    @Override
    @Nullable
    public <METADATA_TYPE> METADATA_TYPE getMetadata(
            @NotNull final ColumnDefinition<?> columnDefinition) {
        if (!hasGroupingTable) {
            return null;
        }

        final Function<String, String> defaultGroupingFilenameByColumnName =
                ParquetTableWriter.defaultGroupingFileName(tl().getParquetFile().getAbsolutePath());
        try {
            final GroupingColumnInfo groupingColumnInfo =
                    tl().getGroupingColumns().get(parquetColumnName);
            final ParquetFileReader parquetFileReader;
            final String groupingFileName = groupingColumnInfo == null
                    ? defaultGroupingFilenameByColumnName.apply(parquetColumnName)
                    : tl().getParquetFile().toPath().getParent()
                            .resolve(groupingColumnInfo.groupingTablePath()).toString();
            try {
                parquetFileReader =
                        new ParquetFileReader(groupingFileName, tl().getChannelProvider(), -1);
            } catch (Exception e) {
                log.warn().append("Failed to read expected grouping file ").append(groupingFileName)
                        .append(" for table location ").append(tl()).append(", column ")
                        .append(getName());
                return null;
            }
            final Map<String, ColumnTypeInfo> columnTypes = ParquetSchemaReader.parseMetadata(
                    new ParquetMetadataConverter().fromParquetMetadata(parquetFileReader.fileMetaData)
                            .getFileMetaData().getKeyValueMetaData())
                    .map(TableInfo::columnTypeMap).orElse(Collections.emptyMap());

            final RowGroupReader rowGroupReader = parquetFileReader.getRowGroup(0);
            final ColumnChunkReader groupingKeyReader =
                    rowGroupReader.getColumnChunk(Collections.singletonList(GROUPING_KEY));
            final ColumnChunkReader beginPosReader =
                    rowGroupReader.getColumnChunk(Collections.singletonList(BEGIN_POS));
            final ColumnChunkReader endPosReader =
                    rowGroupReader.getColumnChunk(Collections.singletonList(END_POS));
            if (groupingKeyReader == null || beginPosReader == null || endPosReader == null) {
                log.warn().append("Grouping file ").append(groupingFileName)
                        .append(" is missing one or more expected columns for table location ")
                        .append(tl()).append(", column ").append(getName());
                return null;
            }

            // noinspection unchecked
            return (METADATA_TYPE) new MetaDataTableFactory(
                    ColumnChunkPageStore.<Values>create(
                            pageCache.castAttr(), groupingKeyReader,
                            ELEMENT_INDEX_TO_SUB_REGION_ELEMENT_INDEX_MASK,
                            makeToPage(columnTypes.get(GROUPING_KEY), ParquetInstructions.EMPTY,
                                    GROUPING_KEY, groupingKeyReader, columnDefinition)).pageStore,
                    ColumnChunkPageStore.<UnorderedKeyIndices>create(
                            pageCache.castAttr(), beginPosReader,
                            ELEMENT_INDEX_TO_SUB_REGION_ELEMENT_INDEX_MASK,
                            makeToPage(columnTypes.get(BEGIN_POS), ParquetInstructions.EMPTY, BEGIN_POS,
                                    beginPosReader, FIRST_KEY_COL_DEF)).pageStore,
                    ColumnChunkPageStore.<UnorderedKeyIndices>create(pageCache.castAttr(), endPosReader,
                            ELEMENT_INDEX_TO_SUB_REGION_ELEMENT_INDEX_MASK,
                            makeToPage(columnTypes.get(END_POS), ParquetInstructions.EMPTY, END_POS,
                                    beginPosReader, LAST_KEY_COL_DEF)).pageStore).get();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private <SOURCE, REGION_TYPE> REGION_TYPE makeColumnRegion(
            @NotNull final Function<ColumnDefinition<?>, SOURCE[]> sourceArrayFactory,
            @NotNull final ColumnDefinition<?> columnDefinition,
            @NotNull final LongFunction<REGION_TYPE> nullRegionFactory,
            @NotNull final Function<SOURCE, REGION_TYPE> singleRegionFactory,
            @NotNull final Function<Stream<REGION_TYPE>, REGION_TYPE> multiRegionFactory) {
        final SOURCE[] sources = sourceArrayFactory.apply(columnDefinition);
        return sources.length == 1
                ? makeSingleColumnRegion(sources[0], nullRegionFactory, singleRegionFactory)
                : multiRegionFactory.apply(Arrays.stream(sources).map(
                        source -> makeSingleColumnRegion(source, nullRegionFactory, singleRegionFactory)));
    }

    private <SOURCE, REGION_TYPE> REGION_TYPE makeSingleColumnRegion(final SOURCE source,
            @NotNull final LongFunction<REGION_TYPE> nullRegionFactory,
            @NotNull final Function<SOURCE, REGION_TYPE> singleRegionFactory) {
        return source == null ? nullRegionFactory.apply(tl().getRegionParameters().regionMask)
                : singleRegionFactory.apply(source);
    }

    @Override
    public ColumnRegionChar<Values> makeColumnRegionChar(
            @NotNull final ColumnDefinition<?> columnDefinition) {
        // noinspection unchecked
        return (ColumnRegionChar<Values>) makeColumnRegion(this::getPageStores, columnDefinition,
                ColumnRegionChar::createNull, ParquetColumnRegionChar::new,
                rs -> new ColumnRegionChar.StaticPageStore<>(tl().getRegionParameters(),
                        rs.toArray(ColumnRegionChar[]::new)));
    }

    @Override
    public ColumnRegionByte<Values> makeColumnRegionByte(
            @NotNull final ColumnDefinition<?> columnDefinition) {
        // noinspection unchecked
        return (ColumnRegionByte<Values>) makeColumnRegion(this::getPageStores, columnDefinition,
                ColumnRegionByte::createNull, ParquetColumnRegionByte::new,
                rs -> new ColumnRegionByte.StaticPageStore<>(tl().getRegionParameters(),
                        rs.toArray(ColumnRegionByte[]::new)));
    }

    @Override
    public ColumnRegionShort<Values> makeColumnRegionShort(
            @NotNull final ColumnDefinition<?> columnDefinition) {
        // noinspection unchecked
        return (ColumnRegionShort<Values>) makeColumnRegion(this::getPageStores, columnDefinition,
                ColumnRegionShort::createNull, ParquetColumnRegionShort::new,
                rs -> new ColumnRegionShort.StaticPageStore<>(tl().getRegionParameters(),
                        rs.toArray(ColumnRegionShort[]::new)));
    }

    @Override
    public ColumnRegionInt<Values> makeColumnRegionInt(
            @NotNull final ColumnDefinition<?> columnDefinition) {
        // noinspection unchecked
        return (ColumnRegionInt<Values>) makeColumnRegion(this::getPageStores, columnDefinition,
                ColumnRegionInt::createNull, ParquetColumnRegionInt::new,
                rs -> new ColumnRegionInt.StaticPageStore<>(tl().getRegionParameters(),
                        rs.toArray(ColumnRegionInt[]::new)));
    }

    @Override
    public ColumnRegionLong<Values> makeColumnRegionLong(
            @NotNull final ColumnDefinition<?> columnDefinition) {
        // noinspection unchecked
        return (ColumnRegionLong<Values>) makeColumnRegion(this::getPageStores, columnDefinition,
                ColumnRegionLong::createNull, ParquetColumnRegionLong::new,
                rs -> new ColumnRegionLong.StaticPageStore<>(tl().getRegionParameters(),
                        rs.toArray(ColumnRegionLong[]::new)));
    }

    @Override
    public ColumnRegionFloat<Values> makeColumnRegionFloat(
            @NotNull final ColumnDefinition<?> columnDefinition) {
        // noinspection unchecked
        return (ColumnRegionFloat<Values>) makeColumnRegion(this::getPageStores, columnDefinition,
                ColumnRegionFloat::createNull, ParquetColumnRegionFloat::new,
                rs -> new ColumnRegionFloat.StaticPageStore<>(tl().getRegionParameters(),
                        rs.toArray(ColumnRegionFloat[]::new)));
    }

    @Override
    public ColumnRegionDouble<Values> makeColumnRegionDouble(
            @NotNull final ColumnDefinition<?> columnDefinition) {
        // noinspection unchecked
        return (ColumnRegionDouble<Values>) makeColumnRegion(this::getPageStores, columnDefinition,
                ColumnRegionDouble::createNull, ParquetColumnRegionDouble::new,
                rs -> new ColumnRegionDouble.StaticPageStore<>(tl().getRegionParameters(),
                        rs.toArray(ColumnRegionDouble[]::new)));
    }

    @Override
    public <TYPE> ColumnRegionObject<TYPE, Values> makeColumnRegionObject(
            @NotNull final ColumnDefinition<TYPE> columnDefinition) {
        final Class<TYPE> dataType = columnDefinition.getDataType();
        final ColumnChunkPageStore<ATTR>[] sources = getPageStores(columnDefinition);
        final ColumnChunkPageStore<DictionaryKeys>[] dictKeySources =
                getDictionaryKeysPageStores(columnDefinition);
        final Supplier<Chunk<ATTR>>[] dictionaryChunkSuppliers =
                getDictionaryChunkSuppliers(columnDefinition);
        if (sources.length == 1) {
            // noinspection unchecked
            return (ColumnRegionObject<TYPE, Values>) makeSingleColumnRegionObject(dataType,
                    sources[0], dictKeySources[0], dictionaryChunkSuppliers[0]);
        }
        // noinspection unchecked
        return (ColumnRegionObject<TYPE, Values>) new ColumnRegionObject.StaticPageStore<TYPE, ATTR>(
                tl().getRegionParameters(),
                IntStream.range(0, sources.length)
                        .mapToObj(ri -> makeSingleColumnRegionObject(dataType, sources[ri],
                                dictKeySources[ri], dictionaryChunkSuppliers[ri]))
                        .toArray(ColumnRegionObject[]::new));
    }

    private <TYPE> ColumnRegionObject<TYPE, ATTR> makeSingleColumnRegionObject(
            @NotNull final Class<TYPE> dataType,
            @Nullable final ColumnChunkPageStore<ATTR> source,
            @Nullable final ColumnChunkPageStore<DictionaryKeys> dictKeySource,
            @Nullable final Supplier<Chunk<ATTR>> dictValuesSupplier) {
        if (source == null) {
            return ColumnRegionObject.createNull(tl().getRegionParameters().regionMask);
        }
        return new ParquetColumnRegionObject<>(source,
                () -> new ParquetColumnRegionLong<>(Require.neqNull(dictKeySource, "dictKeySource")),
                () -> ColumnRegionChunkDictionary.create(tl().getRegionParameters().regionMask,
                        dataType, Require.neqNull(dictValuesSupplier, "dictValuesSupplier")));
    }

    /**
     * Get the {@link ColumnChunkPageStore page stores} backing this column location.
     *
     * @param columnDefinition The {@link ColumnDefinition} used to lookup type information
     * @return The page stores
     */
    @NotNull
    public ColumnChunkPageStore<ATTR>[] getPageStores(
            @NotNull final ColumnDefinition<?> columnDefinition) {
        fetchValues(columnDefinition);
        return pageStores;
    }

    /**
     * Get suppliers to access the {@link Chunk dictionary chunks} backing this column location.
     *
     * @param columnDefinition The {@link ColumnDefinition} used to lookup type information
     * @return The dictionary values chunk suppliers, or null if none exist
     */
    public Supplier<Chunk<ATTR>>[] getDictionaryChunkSuppliers(
            @NotNull final ColumnDefinition<?> columnDefinition) {
        fetchValues(columnDefinition);
        return dictionaryChunkSuppliers;
    }

    /**
     * Get the {@link ColumnChunkPageStore page stores} backing the indices for this column location. Only usable when
     * there are dictionaries.
     *
     * @param columnDefinition The {@link ColumnDefinition} used to lookup type information
     * @return The page stores
     */
    private ColumnChunkPageStore<DictionaryKeys>[] getDictionaryKeysPageStores(
            @NotNull final ColumnDefinition<?> columnDefinition) {
        fetchValues(columnDefinition);
        return dictionaryKeysPageStores;
    }

    @SuppressWarnings("unchecked")
    private void fetchValues(@NotNull final ColumnDefinition<?> columnDefinition) {
        if (columnChunkReaders == null) {
            return;
        }
        synchronized (this) {
            if (columnChunkReaders == null) {
                return;
            }

            final int pageStoreCount = columnChunkReaders.length;
            pageStores = new ColumnChunkPageStore[pageStoreCount];
            dictionaryChunkSuppliers = new Supplier[pageStoreCount];
            dictionaryKeysPageStores = new ColumnChunkPageStore[pageStoreCount];
            for (int psi = 0; psi < pageStoreCount; ++psi) {
                final ColumnChunkReader columnChunkReader = columnChunkReaders[psi];
                try {
                    final ColumnChunkPageStore.CreatorResult<ATTR> creatorResult =
                            ColumnChunkPageStore.create(
                                    pageCache,
                                    columnChunkReader,
                                    tl().getRegionParameters().regionMask,
                                    makeToPage(tl().getColumnTypes().get(parquetColumnName),
                                            tl().getReadInstructions(), parquetColumnName, columnChunkReader,
                                            columnDefinition));
                    pageStores[psi] = creatorResult.pageStore;
                    dictionaryChunkSuppliers[psi] = creatorResult.dictionaryChunkSupplier;
                    dictionaryKeysPageStores[psi] = creatorResult.dictionaryKeysPageStore;
                } catch (IOException e) {
                    throw new TableDataException(
                            "Failed to read parquet file for " + this + ", row group " + psi, e);
                }
            }

            columnChunkReaders = null;
        }
    }

    private static final class MetaDataTableFactory {

        private final ColumnChunkPageStore<Values> keyColumn;
        private final ColumnChunkPageStore<UnorderedKeyIndices> firstColumn;
        private final ColumnChunkPageStore<UnorderedKeyIndices> lastColumn;

        private volatile Object metaData;

        private MetaDataTableFactory(@NotNull final ColumnChunkPageStore<Values> keyColumn,
                @NotNull final ColumnChunkPageStore<UnorderedKeyIndices> firstColumn,
                @NotNull final ColumnChunkPageStore<UnorderedKeyIndices> lastColumn) {
            this.keyColumn = Require.neqNull(keyColumn, "keyColumn");
            this.firstColumn = Require.neqNull(firstColumn, "firstColumn");
            this.lastColumn = Require.neqNull(lastColumn, "lastColumn");
        }

        public Object get() {
            if (metaData != null) {
                return metaData;
            }
            synchronized (this) {
                if (metaData != null) {
                    return metaData;
                }
                final int numRows = (int) keyColumn.size();

                try (
                        final ChunkBoxer.BoxerKernel boxerKernel =
                                ChunkBoxer.getBoxer(keyColumn.getChunkType(), CHUNK_SIZE);
                        final BuildGrouping buildGrouping =
                                BuildGrouping.builder(firstColumn.getChunkType(), numRows);
                        final ChunkSource.GetContext keyContext = keyColumn.makeGetContext(CHUNK_SIZE);
                        final ChunkSource.GetContext firstContext =
                                firstColumn.makeGetContext(CHUNK_SIZE);
                        final ChunkSource.GetContext lastContext =
                                lastColumn.makeGetContext(CHUNK_SIZE);
                        final OrderedKeys rows = OrderedKeys.forRange(0, numRows - 1);
                        final OrderedKeys.Iterator rowsIterator = rows.getOrderedKeysIterator()) {

                    while (rowsIterator.hasMore()) {
                        final OrderedKeys chunkRows =
                                rowsIterator.getNextOrderedKeysWithLength(CHUNK_SIZE);

                        buildGrouping.build(
                                boxerKernel.box(keyColumn.getChunk(keyContext, chunkRows)),
                                firstColumn.getChunk(firstContext, chunkRows),
                                lastColumn.getChunk(lastContext, chunkRows));
                    }

                    metaData = buildGrouping.getGrouping();
                }
            }
            return metaData;
        }

        private interface BuildGrouping extends Context {
            void build(@NotNull ObjectChunk<?, ? extends Values> keyChunk,
                    @NotNull Chunk<? extends UnorderedKeyIndices> firstChunk,
                    @NotNull Chunk<? extends UnorderedKeyIndices> lastChunk);

            Object getGrouping();

            static BuildGrouping builder(@NotNull final ChunkType chunkType, final int numRows) {
                switch (chunkType) {
                    case Int:
                        return new IntBuildGrouping(numRows);
                    case Long:
                        return new LongBuildGrouping(numRows);
                    default:
                        throw new IllegalArgumentException(
                                "Unknown type for an index: " + chunkType);
                }
            }

            final class IntBuildGrouping implements BuildGrouping {

                private final Map<Object, int[]> grouping;

                IntBuildGrouping(final int numRows) {
                    grouping = new LinkedHashMap<>(numRows);
                }

                @Override
                public void build(@NotNull final ObjectChunk<?, ? extends Values> keyChunk,
                        @NotNull final Chunk<? extends UnorderedKeyIndices> firstChunk,
                        @NotNull final Chunk<? extends UnorderedKeyIndices> lastChunk) {
                    final IntChunk<? extends UnorderedKeyIndices> firstIntChunk =
                            firstChunk.asIntChunk();
                    final IntChunk<? extends UnorderedKeyIndices> lastIntChunk =
                            lastChunk.asIntChunk();

                    for (int ki = 0; ki < keyChunk.size(); ++ki) {
                        final int[] range = new int[2];

                        range[0] = firstIntChunk.get(ki);
                        range[1] = lastIntChunk.get(ki);

                        grouping.put(keyChunk.get(ki), range);
                    }
                }

                @Override
                public Object getGrouping() {
                    return grouping;
                }
            }

            final class LongBuildGrouping implements BuildGrouping {

                private final Map<Object, long[]> grouping;

                LongBuildGrouping(final int numRows) {
                    grouping = new LinkedHashMap<>(numRows);
                }

                @Override
                public void build(@NotNull final ObjectChunk<?, ? extends Values> keyChunk,
                        @NotNull final Chunk<? extends UnorderedKeyIndices> firstChunk,
                        @NotNull final Chunk<? extends UnorderedKeyIndices> lastChunk) {
                    final LongChunk<? extends UnorderedKeyIndices> firstLongChunk =
                            firstChunk.asLongChunk();
                    final LongChunk<? extends UnorderedKeyIndices> lastLongChunk =
                            lastChunk.asLongChunk();

                    for (int ki = 0; ki < keyChunk.size(); ++ki) {
                        final long[] range = new long[2];

                        range[0] = firstLongChunk.get(ki);
                        range[1] = lastLongChunk.get(ki);

                        grouping.put(keyChunk.get(ki), range);
                    }
                }

                @Override
                public Object getGrouping() {
                    return grouping;
                }
            }
        }
    }

    private static <ATTR extends Any, RESULT> ToPage<ATTR, RESULT> makeToPage(
            @Nullable final ColumnTypeInfo columnTypeInfo,
            @NotNull final ParquetInstructions readInstructions,
            @NotNull final String parquetColumnName,
            @NotNull final ColumnChunkReader columnChunkReader,
            @NotNull final ColumnDefinition<?> columnDefinition) {
        final PrimitiveType type = columnChunkReader.getType();
        final LogicalTypeAnnotation logicalTypeAnnotation = type.getLogicalTypeAnnotation();
        final String codecFromInstructions =
                readInstructions.getCodecName(columnDefinition.getName());
        final String codecName = (codecFromInstructions != null)
                ? codecFromInstructions
                : columnTypeInfo == null ? null
                        : columnTypeInfo.codec().map(CodecInfo::codecName).orElse(null);
        final ColumnTypeInfo.SpecialType specialTypeName =
                columnTypeInfo == null ? null : columnTypeInfo.specialType().orElse(null);

        final boolean isArray = columnChunkReader.getMaxRl() > 0;
        final boolean isCodec = CodecLookup.explicitCodecPresent(codecName);

        if (isArray && columnChunkReader.getMaxRl() > 1) {
            throw new TableDataException("No support for nested repeated parquet columns.");
        }

        try {
            // Note that componentType is null for a StringSet. ToStringSetPage.create specifically
            // doesn't take this parameter.
            final Class<?> dataType = columnDefinition.getDataType();
            final Class<?> componentType = columnDefinition.getComponentType();
            final Class<?> pageType = isArray ? componentType : dataType;

            ToPage<ATTR, ?> toPage = null;

            if (logicalTypeAnnotation != null) {
                toPage = logicalTypeAnnotation.accept(
                        new LogicalTypeVisitor<ATTR>(parquetColumnName, columnChunkReader, pageType))
                        .orElse(null);
            }

            if (toPage == null) {
                final PrimitiveType.PrimitiveTypeName typeName = type.getPrimitiveTypeName();
                switch (typeName) {
                    case BOOLEAN:
                        toPage = ToBooleanAsBytePage.create(pageType);
                        break;
                    case INT32:
                        toPage = ToIntPage.create(pageType);
                        break;
                    case INT64:
                        toPage = ToLongPage.create(pageType);
                        break;
                    case INT96:
                        toPage = ToDBDateTimePageFromInt96.create(pageType);
                        break;
                    case DOUBLE:
                        toPage = ToDoublePage.create(pageType);
                        break;
                    case FLOAT:
                        toPage = ToFloatPage.create(pageType);
                        break;
                    case BINARY:
                    case FIXED_LEN_BYTE_ARRAY:
                        // noinspection rawtypes
                        final ObjectCodec codec;
                        if (isCodec) {
                            final String codecArgs = codecFromInstructions != null
                                    ? readInstructions.getCodecArgs(columnDefinition.getName())
                                    : columnTypeInfo.codec().flatMap(CodecInfo::codecArg).orElse(null);
                            codec = CodecCache.DEFAULT.getCodec(codecName, codecArgs);
                        } else {
                            final String codecArgs =
                                    (typeName == PrimitiveType.PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY)
                                            ? Integer.toString(type.getTypeLength())
                                            : null;
                            codec = CodecCache.DEFAULT
                                    .getCodec(SimpleByteArrayCodec.class.getName(), codecArgs);
                        }
                        // noinspection unchecked
                        toPage = ToObjectPage.create(dataType, codec,
                                columnChunkReader.getDictionarySupplier());
                        break;
                    default:
                }
            }

            if (toPage == null) {
                throw new TableDataException(
                        "Unsupported parquet column type " + type.getPrimitiveTypeName() +
                                " with logical type " + logicalTypeAnnotation);
            }

            if (specialTypeName == ColumnTypeInfo.SpecialType.StringSet) {
                Assert.assertion(isArray, "isArray");
                toPage = ToStringSetPage.create(dataType, toPage);
            } else if (isArray) {
                Assert.assertion(!isCodec, "!isCodec");
                if (DbArrayBase.class.isAssignableFrom(dataType)) {
                    toPage = ToDbArrayPage.create(dataType, componentType, toPage);
                } else if (dataType.isArray()) {
                    toPage = ToArrayPage.create(dataType, componentType, toPage);
                }
            }

            // noinspection unchecked
            return (ToPage<ATTR, RESULT>) toPage;

        } catch (RuntimeException except) {
            throw new TableDataException(
                    "Unexpected exception accessing column " + parquetColumnName, except);
        }
    }

    private static class LogicalTypeVisitor<ATTR extends Any>
            implements LogicalTypeAnnotation.LogicalTypeAnnotationVisitor<ToPage<ATTR, ?>> {

        private final String name;
        private final ColumnChunkReader columnChunkReader;
        private final Class<?> componentType;

        LogicalTypeVisitor(@NotNull String name, @NotNull ColumnChunkReader columnChunkReader,
                Class<?> componentType) {
            this.name = name;
            this.columnChunkReader = columnChunkReader;
            this.componentType = componentType;
        }

        @Override
        public Optional<ToPage<ATTR, ?>> visit(
                LogicalTypeAnnotation.StringLogicalTypeAnnotation stringLogicalType) {
            return Optional
                    .of(ToStringPage.create(componentType, columnChunkReader.getDictionarySupplier()));
        }

        @Override
        public Optional<ToPage<ATTR, ?>> visit(
                LogicalTypeAnnotation.TimestampLogicalTypeAnnotation timestampLogicalType) {
            if (timestampLogicalType.isAdjustedToUTC()) {
                return Optional
                        .of(ToDBDateTimePage.create(componentType, timestampLogicalType.getUnit()));
            }

            throw new TableDataException(
                    "Timestamp column is not UTC or is not nanoseconds " + name);
        }

        @Override
        public Optional<ToPage<ATTR, ?>> visit(
                LogicalTypeAnnotation.IntLogicalTypeAnnotation intLogicalType) {

            if (intLogicalType.isSigned()) {
                switch (intLogicalType.getBitWidth()) {
                    case 8:
                        return Optional.of(ToBytePageFromInt.create(componentType));
                    case 16:
                        return Optional.of(ToShortPageFromInt.create(componentType));
                    case 32:
                        return Optional.of(ToIntPage.create(componentType));
                    case 64:
                        return Optional.of(ToLongPage.create(componentType));
                }
            } else {
                switch (intLogicalType.getBitWidth()) {
                    case 8:
                    case 16:
                        return Optional.of(ToCharPageFromInt.create(componentType));
                    case 32:
                        return Optional.of(ToLongPage.create(componentType));
                }
            }

            return Optional.empty();
        }

        @Override
        public Optional<ToPage<ATTR, ?>> visit(
                LogicalTypeAnnotation.DateLogicalTypeAnnotation dateLogicalType) {
            return Optional.of(ToIntPage.create(componentType));
        }

        @Override
        public Optional<ToPage<ATTR, ?>> visit(
                LogicalTypeAnnotation.TimeLogicalTypeAnnotation timeLogicalType) {
            if (timeLogicalType.getUnit() == LogicalTypeAnnotation.TimeUnit.MILLIS) {
                return Optional.of(ToIntPage.create(componentType));
            }
            return Optional.of(ToLongPage.create(componentType));
        }
    }
}