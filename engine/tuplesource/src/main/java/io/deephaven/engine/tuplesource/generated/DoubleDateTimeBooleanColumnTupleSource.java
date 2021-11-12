package io.deephaven.engine.tuplesource.generated;

import io.deephaven.datastructures.util.SmartKey;
import io.deephaven.engine.chunk.Attributes;
import io.deephaven.engine.chunk.Chunk;
import io.deephaven.engine.chunk.ObjectChunk;
import io.deephaven.engine.chunk.WritableChunk;
import io.deephaven.engine.chunk.WritableObjectChunk;
import io.deephaven.engine.time.DateTime;
import io.deephaven.engine.time.DateTimeUtils;
import io.deephaven.engine.tuple.generated.DoubleLongByteTuple;
import io.deephaven.engine.tuplesource.AbstractTupleSource;
import io.deephaven.engine.tuplesource.ThreeColumnTupleSourceFactory;
import io.deephaven.engine.table.TupleSource;
import io.deephaven.engine.v2.sources.ColumnSource;
import io.deephaven.engine.v2.sources.WritableSource;
import io.deephaven.util.BooleanUtils;
import io.deephaven.util.type.TypeUtils;
import org.jetbrains.annotations.NotNull;


/**
 * <p>{@link TupleSource} that produces key column values from {@link ColumnSource} types Double, DateTime, and Boolean.
 * <p>Generated by io.deephaven.replicators.TupleSourceCodeGenerator.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class DoubleDateTimeBooleanColumnTupleSource extends AbstractTupleSource<DoubleLongByteTuple> {

    /** {@link ThreeColumnTupleSourceFactory} instance to create instances of {@link DoubleDateTimeBooleanColumnTupleSource}. **/
    public static final ThreeColumnTupleSourceFactory<DoubleLongByteTuple, Double, DateTime, Boolean> FACTORY = new Factory();

    private final ColumnSource<Double> columnSource1;
    private final ColumnSource<DateTime> columnSource2;
    private final ColumnSource<Boolean> columnSource3;

    public DoubleDateTimeBooleanColumnTupleSource(
            @NotNull final ColumnSource<Double> columnSource1,
            @NotNull final ColumnSource<DateTime> columnSource2,
            @NotNull final ColumnSource<Boolean> columnSource3
    ) {
        super(columnSource1, columnSource2, columnSource3);
        this.columnSource1 = columnSource1;
        this.columnSource2 = columnSource2;
        this.columnSource3 = columnSource3;
    }

    @Override
    public final DoubleLongByteTuple createTuple(final long indexKey) {
        return new DoubleLongByteTuple(
                columnSource1.getDouble(indexKey),
                DateTimeUtils.nanos(columnSource2.get(indexKey)),
                BooleanUtils.booleanAsByte(columnSource3.getBoolean(indexKey))
        );
    }

    @Override
    public final DoubleLongByteTuple createPreviousTuple(final long indexKey) {
        return new DoubleLongByteTuple(
                columnSource1.getPrevDouble(indexKey),
                DateTimeUtils.nanos(columnSource2.getPrev(indexKey)),
                BooleanUtils.booleanAsByte(columnSource3.getPrevBoolean(indexKey))
        );
    }

    @Override
    public final DoubleLongByteTuple createTupleFromValues(@NotNull final Object... values) {
        return new DoubleLongByteTuple(
                TypeUtils.unbox((Double)values[0]),
                DateTimeUtils.nanos((DateTime)values[1]),
                BooleanUtils.booleanAsByte((Boolean)values[2])
        );
    }

    @Override
    public final DoubleLongByteTuple createTupleFromReinterpretedValues(@NotNull final Object... values) {
        return new DoubleLongByteTuple(
                TypeUtils.unbox((Double)values[0]),
                DateTimeUtils.nanos((DateTime)values[1]),
                BooleanUtils.booleanAsByte((Boolean)values[2])
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <ELEMENT_TYPE> void exportElement(@NotNull final DoubleLongByteTuple tuple, final int elementIndex, @NotNull final WritableSource<ELEMENT_TYPE> writableSource, final long destinationIndexKey) {
        if (elementIndex == 0) {
            writableSource.set(destinationIndexKey, tuple.getFirstElement());
            return;
        }
        if (elementIndex == 1) {
            writableSource.set(destinationIndexKey, (ELEMENT_TYPE) DateTimeUtils.nanosToTime(tuple.getSecondElement()));
            return;
        }
        if (elementIndex == 2) {
            writableSource.set(destinationIndexKey, (ELEMENT_TYPE) BooleanUtils.byteAsBoolean(tuple.getThirdElement()));
            return;
        }
        throw new IndexOutOfBoundsException("Invalid element index " + elementIndex + " for export");
    }

    @Override
    public final Object exportToExternalKey(@NotNull final DoubleLongByteTuple tuple) {
        return new SmartKey(
                TypeUtils.box(tuple.getFirstElement()),
                DateTimeUtils.nanosToTime(tuple.getSecondElement()),
                BooleanUtils.byteAsBoolean(tuple.getThirdElement())
        );
    }

    @Override
    public final Object exportElement(@NotNull final DoubleLongByteTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return TypeUtils.box(tuple.getFirstElement());
        }
        if (elementIndex == 1) {
            return DateTimeUtils.nanosToTime(tuple.getSecondElement());
        }
        if (elementIndex == 2) {
            return BooleanUtils.byteAsBoolean(tuple.getThirdElement());
        }
        throw new IllegalArgumentException("Bad elementIndex for 3 element tuple: " + elementIndex);
    }

    @Override
    public final Object exportElementReinterpreted(@NotNull final DoubleLongByteTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return TypeUtils.box(tuple.getFirstElement());
        }
        if (elementIndex == 1) {
            return DateTimeUtils.nanosToTime(tuple.getSecondElement());
        }
        if (elementIndex == 2) {
            return BooleanUtils.byteAsBoolean(tuple.getThirdElement());
        }
        throw new IllegalArgumentException("Bad elementIndex for 3 element tuple: " + elementIndex);
    }

    @Override
    protected void convertChunks(@NotNull WritableChunk<? super Attributes.Values> destination, int chunkSize, Chunk<Attributes.Values> [] chunks) {
        WritableObjectChunk<DoubleLongByteTuple, ? super Attributes.Values> destinationObjectChunk = destination.asWritableObjectChunk();
        DoubleChunk<Attributes.Values> chunk1 = chunks[0].asDoubleChunk();
        ObjectChunk<DateTime, Attributes.Values> chunk2 = chunks[1].asObjectChunk();
        ObjectChunk<Boolean, Attributes.Values> chunk3 = chunks[2].asObjectChunk();
        for (int ii = 0; ii < chunkSize; ++ii) {
            destinationObjectChunk.set(ii, new DoubleLongByteTuple(chunk1.get(ii), DateTimeUtils.nanos(chunk2.get(ii)), BooleanUtils.booleanAsByte(chunk3.get(ii))));
        }
        destinationObjectChunk.setSize(chunkSize);
    }

    /** {@link ThreeColumnTupleSourceFactory} for instances of {@link DoubleDateTimeBooleanColumnTupleSource}. **/
    private static final class Factory implements ThreeColumnTupleSourceFactory<DoubleLongByteTuple, Double, DateTime, Boolean> {

        private Factory() {
        }

        @Override
        public TupleSource<DoubleLongByteTuple> create(
                @NotNull final ColumnSource<Double> columnSource1,
                @NotNull final ColumnSource<DateTime> columnSource2,
                @NotNull final ColumnSource<Boolean> columnSource3
        ) {
            return new DoubleDateTimeBooleanColumnTupleSource(
                    columnSource1,
                    columnSource2,
                    columnSource3
            );
        }
    }
}
