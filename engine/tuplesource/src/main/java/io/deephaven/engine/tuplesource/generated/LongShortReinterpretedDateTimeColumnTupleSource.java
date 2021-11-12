package io.deephaven.engine.tuplesource.generated;

import io.deephaven.datastructures.util.SmartKey;
import io.deephaven.engine.chunk.Attributes;
import io.deephaven.engine.chunk.Chunk;
import io.deephaven.engine.chunk.WritableChunk;
import io.deephaven.engine.chunk.WritableObjectChunk;
import io.deephaven.engine.time.DateTime;
import io.deephaven.engine.time.DateTimeUtils;
import io.deephaven.engine.tuple.generated.LongShortLongTuple;
import io.deephaven.engine.tuplesource.AbstractTupleSource;
import io.deephaven.engine.tuplesource.ThreeColumnTupleSourceFactory;
import io.deephaven.engine.table.TupleSource;
import io.deephaven.engine.v2.sources.ColumnSource;
import io.deephaven.engine.v2.sources.WritableSource;
import io.deephaven.util.type.TypeUtils;
import org.jetbrains.annotations.NotNull;


/**
 * <p>{@link TupleSource} that produces key column values from {@link ColumnSource} types Long, Short, and Long.
 * <p>Generated by io.deephaven.replicators.TupleSourceCodeGenerator.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class LongShortReinterpretedDateTimeColumnTupleSource extends AbstractTupleSource<LongShortLongTuple> {

    /** {@link ThreeColumnTupleSourceFactory} instance to create instances of {@link LongShortReinterpretedDateTimeColumnTupleSource}. **/
    public static final ThreeColumnTupleSourceFactory<LongShortLongTuple, Long, Short, Long> FACTORY = new Factory();

    private final ColumnSource<Long> columnSource1;
    private final ColumnSource<Short> columnSource2;
    private final ColumnSource<Long> columnSource3;

    public LongShortReinterpretedDateTimeColumnTupleSource(
            @NotNull final ColumnSource<Long> columnSource1,
            @NotNull final ColumnSource<Short> columnSource2,
            @NotNull final ColumnSource<Long> columnSource3
    ) {
        super(columnSource1, columnSource2, columnSource3);
        this.columnSource1 = columnSource1;
        this.columnSource2 = columnSource2;
        this.columnSource3 = columnSource3;
    }

    @Override
    public final LongShortLongTuple createTuple(final long indexKey) {
        return new LongShortLongTuple(
                columnSource1.getLong(indexKey),
                columnSource2.getShort(indexKey),
                columnSource3.getLong(indexKey)
        );
    }

    @Override
    public final LongShortLongTuple createPreviousTuple(final long indexKey) {
        return new LongShortLongTuple(
                columnSource1.getPrevLong(indexKey),
                columnSource2.getPrevShort(indexKey),
                columnSource3.getPrevLong(indexKey)
        );
    }

    @Override
    public final LongShortLongTuple createTupleFromValues(@NotNull final Object... values) {
        return new LongShortLongTuple(
                TypeUtils.unbox((Long)values[0]),
                TypeUtils.unbox((Short)values[1]),
                DateTimeUtils.nanos((DateTime)values[2])
        );
    }

    @Override
    public final LongShortLongTuple createTupleFromReinterpretedValues(@NotNull final Object... values) {
        return new LongShortLongTuple(
                TypeUtils.unbox((Long)values[0]),
                TypeUtils.unbox((Short)values[1]),
                TypeUtils.unbox((Long)values[2])
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <ELEMENT_TYPE> void exportElement(@NotNull final LongShortLongTuple tuple, final int elementIndex, @NotNull final WritableSource<ELEMENT_TYPE> writableSource, final long destinationIndexKey) {
        if (elementIndex == 0) {
            writableSource.set(destinationIndexKey, tuple.getFirstElement());
            return;
        }
        if (elementIndex == 1) {
            writableSource.set(destinationIndexKey, tuple.getSecondElement());
            return;
        }
        if (elementIndex == 2) {
            writableSource.set(destinationIndexKey, (ELEMENT_TYPE) DateTimeUtils.nanosToTime(tuple.getThirdElement()));
            return;
        }
        throw new IndexOutOfBoundsException("Invalid element index " + elementIndex + " for export");
    }

    @Override
    public final Object exportToExternalKey(@NotNull final LongShortLongTuple tuple) {
        return new SmartKey(
                TypeUtils.box(tuple.getFirstElement()),
                TypeUtils.box(tuple.getSecondElement()),
                DateTimeUtils.nanosToTime(tuple.getThirdElement())
        );
    }

    @Override
    public final Object exportElement(@NotNull final LongShortLongTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return TypeUtils.box(tuple.getFirstElement());
        }
        if (elementIndex == 1) {
            return TypeUtils.box(tuple.getSecondElement());
        }
        if (elementIndex == 2) {
            return DateTimeUtils.nanosToTime(tuple.getThirdElement());
        }
        throw new IllegalArgumentException("Bad elementIndex for 3 element tuple: " + elementIndex);
    }

    @Override
    public final Object exportElementReinterpreted(@NotNull final LongShortLongTuple tuple, int elementIndex) {
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
        WritableObjectChunk<LongShortLongTuple, ? super Attributes.Values> destinationObjectChunk = destination.asWritableObjectChunk();
        LongChunk<Attributes.Values> chunk1 = chunks[0].asLongChunk();
        ShortChunk<Attributes.Values> chunk2 = chunks[1].asShortChunk();
        LongChunk<Attributes.Values> chunk3 = chunks[2].asLongChunk();
        for (int ii = 0; ii < chunkSize; ++ii) {
            destinationObjectChunk.set(ii, new LongShortLongTuple(chunk1.get(ii), chunk2.get(ii), chunk3.get(ii)));
        }
        destinationObjectChunk.setSize(chunkSize);
    }

    /** {@link ThreeColumnTupleSourceFactory} for instances of {@link LongShortReinterpretedDateTimeColumnTupleSource}. **/
    private static final class Factory implements ThreeColumnTupleSourceFactory<LongShortLongTuple, Long, Short, Long> {

        private Factory() {
        }

        @Override
        public TupleSource<LongShortLongTuple> create(
                @NotNull final ColumnSource<Long> columnSource1,
                @NotNull final ColumnSource<Short> columnSource2,
                @NotNull final ColumnSource<Long> columnSource3
        ) {
            return new LongShortReinterpretedDateTimeColumnTupleSource(
                    columnSource1,
                    columnSource2,
                    columnSource3
            );
        }
    }
}
