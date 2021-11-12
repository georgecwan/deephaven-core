package io.deephaven.engine.tuplesource.generated;

import io.deephaven.datastructures.util.SmartKey;
import io.deephaven.engine.chunk.Attributes;
import io.deephaven.engine.chunk.Chunk;
import io.deephaven.engine.chunk.WritableChunk;
import io.deephaven.engine.chunk.WritableObjectChunk;
import io.deephaven.engine.tuple.generated.ShortLongShortTuple;
import io.deephaven.engine.tuplesource.AbstractTupleSource;
import io.deephaven.engine.tuplesource.ThreeColumnTupleSourceFactory;
import io.deephaven.engine.table.TupleSource;
import io.deephaven.engine.v2.sources.ColumnSource;
import io.deephaven.engine.v2.sources.WritableSource;
import io.deephaven.util.type.TypeUtils;
import org.jetbrains.annotations.NotNull;


/**
 * <p>{@link TupleSource} that produces key column values from {@link ColumnSource} types Short, Long, and Short.
 * <p>Generated by io.deephaven.replicators.TupleSourceCodeGenerator.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class ShortLongShortColumnTupleSource extends AbstractTupleSource<ShortLongShortTuple> {

    /** {@link ThreeColumnTupleSourceFactory} instance to create instances of {@link ShortLongShortColumnTupleSource}. **/
    public static final ThreeColumnTupleSourceFactory<ShortLongShortTuple, Short, Long, Short> FACTORY = new Factory();

    private final ColumnSource<Short> columnSource1;
    private final ColumnSource<Long> columnSource2;
    private final ColumnSource<Short> columnSource3;

    public ShortLongShortColumnTupleSource(
            @NotNull final ColumnSource<Short> columnSource1,
            @NotNull final ColumnSource<Long> columnSource2,
            @NotNull final ColumnSource<Short> columnSource3
    ) {
        super(columnSource1, columnSource2, columnSource3);
        this.columnSource1 = columnSource1;
        this.columnSource2 = columnSource2;
        this.columnSource3 = columnSource3;
    }

    @Override
    public final ShortLongShortTuple createTuple(final long indexKey) {
        return new ShortLongShortTuple(
                columnSource1.getShort(indexKey),
                columnSource2.getLong(indexKey),
                columnSource3.getShort(indexKey)
        );
    }

    @Override
    public final ShortLongShortTuple createPreviousTuple(final long indexKey) {
        return new ShortLongShortTuple(
                columnSource1.getPrevShort(indexKey),
                columnSource2.getPrevLong(indexKey),
                columnSource3.getPrevShort(indexKey)
        );
    }

    @Override
    public final ShortLongShortTuple createTupleFromValues(@NotNull final Object... values) {
        return new ShortLongShortTuple(
                TypeUtils.unbox((Short)values[0]),
                TypeUtils.unbox((Long)values[1]),
                TypeUtils.unbox((Short)values[2])
        );
    }

    @Override
    public final ShortLongShortTuple createTupleFromReinterpretedValues(@NotNull final Object... values) {
        return new ShortLongShortTuple(
                TypeUtils.unbox((Short)values[0]),
                TypeUtils.unbox((Long)values[1]),
                TypeUtils.unbox((Short)values[2])
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <ELEMENT_TYPE> void exportElement(@NotNull final ShortLongShortTuple tuple, final int elementIndex, @NotNull final WritableSource<ELEMENT_TYPE> writableSource, final long destinationIndexKey) {
        if (elementIndex == 0) {
            writableSource.set(destinationIndexKey, tuple.getFirstElement());
            return;
        }
        if (elementIndex == 1) {
            writableSource.set(destinationIndexKey, tuple.getSecondElement());
            return;
        }
        if (elementIndex == 2) {
            writableSource.set(destinationIndexKey, tuple.getThirdElement());
            return;
        }
        throw new IndexOutOfBoundsException("Invalid element index " + elementIndex + " for export");
    }

    @Override
    public final Object exportToExternalKey(@NotNull final ShortLongShortTuple tuple) {
        return new SmartKey(
                TypeUtils.box(tuple.getFirstElement()),
                TypeUtils.box(tuple.getSecondElement()),
                TypeUtils.box(tuple.getThirdElement())
        );
    }

    @Override
    public final Object exportElement(@NotNull final ShortLongShortTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return TypeUtils.box(tuple.getFirstElement());
        }
        if (elementIndex == 1) {
            return TypeUtils.box(tuple.getSecondElement());
        }
        if (elementIndex == 2) {
            return TypeUtils.box(tuple.getThirdElement());
        }
        throw new IllegalArgumentException("Bad elementIndex for 3 element tuple: " + elementIndex);
    }

    @Override
    public final Object exportElementReinterpreted(@NotNull final ShortLongShortTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return TypeUtils.box(tuple.getFirstElement());
        }
        if (elementIndex == 1) {
            return TypeUtils.box(tuple.getSecondElement());
        }
        if (elementIndex == 2) {
            return TypeUtils.box(tuple.getThirdElement());
        }
        throw new IllegalArgumentException("Bad elementIndex for 3 element tuple: " + elementIndex);
    }

    @Override
    protected void convertChunks(@NotNull WritableChunk<? super Attributes.Values> destination, int chunkSize, Chunk<Attributes.Values> [] chunks) {
        WritableObjectChunk<ShortLongShortTuple, ? super Attributes.Values> destinationObjectChunk = destination.asWritableObjectChunk();
        ShortChunk<Attributes.Values> chunk1 = chunks[0].asShortChunk();
        LongChunk<Attributes.Values> chunk2 = chunks[1].asLongChunk();
        ShortChunk<Attributes.Values> chunk3 = chunks[2].asShortChunk();
        for (int ii = 0; ii < chunkSize; ++ii) {
            destinationObjectChunk.set(ii, new ShortLongShortTuple(chunk1.get(ii), chunk2.get(ii), chunk3.get(ii)));
        }
        destinationObjectChunk.setSize(chunkSize);
    }

    /** {@link ThreeColumnTupleSourceFactory} for instances of {@link ShortLongShortColumnTupleSource}. **/
    private static final class Factory implements ThreeColumnTupleSourceFactory<ShortLongShortTuple, Short, Long, Short> {

        private Factory() {
        }

        @Override
        public TupleSource<ShortLongShortTuple> create(
                @NotNull final ColumnSource<Short> columnSource1,
                @NotNull final ColumnSource<Long> columnSource2,
                @NotNull final ColumnSource<Short> columnSource3
        ) {
            return new ShortLongShortColumnTupleSource(
                    columnSource1,
                    columnSource2,
                    columnSource3
            );
        }
    }
}
