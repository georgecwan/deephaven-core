package io.deephaven.engine.table.impl.tuplesource.generated;

import io.deephaven.datastructures.util.SmartKey;
import io.deephaven.chunk.attributes.Values;
import io.deephaven.chunk.Chunk;
import io.deephaven.chunk.DoubleChunk;
import io.deephaven.chunk.ObjectChunk;
import io.deephaven.chunk.WritableChunk;
import io.deephaven.chunk.WritableObjectChunk;
import io.deephaven.engine.table.ColumnSource;
import io.deephaven.engine.table.TupleSource;
import io.deephaven.engine.table.WritableColumnSource;
import io.deephaven.engine.table.impl.tuplesource.AbstractTupleSource;
import io.deephaven.engine.table.impl.tuplesource.TwoColumnTupleSourceFactory;
import io.deephaven.tuple.generated.DoubleByteTuple;
import io.deephaven.util.BooleanUtils;
import io.deephaven.util.type.TypeUtils;
import org.jetbrains.annotations.NotNull;


/**
 * <p>{@link TupleSource} that produces key column values from {@link ColumnSource} types Double and Boolean.
 * <p>Generated by io.deephaven.replicators.TupleSourceCodeGenerator.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class DoubleBooleanColumnTupleSource extends AbstractTupleSource<DoubleByteTuple> {

    /** {@link TwoColumnTupleSourceFactory} instance to create instances of {@link DoubleBooleanColumnTupleSource}. **/
    public static final TwoColumnTupleSourceFactory<DoubleByteTuple, Double, Boolean> FACTORY = new Factory();

    private final ColumnSource<Double> columnSource1;
    private final ColumnSource<Boolean> columnSource2;

    public DoubleBooleanColumnTupleSource(
            @NotNull final ColumnSource<Double> columnSource1,
            @NotNull final ColumnSource<Boolean> columnSource2
    ) {
        super(columnSource1, columnSource2);
        this.columnSource1 = columnSource1;
        this.columnSource2 = columnSource2;
    }

    @Override
    public final DoubleByteTuple createTuple(final long rowKey) {
        return new DoubleByteTuple(
                columnSource1.getDouble(rowKey),
                BooleanUtils.booleanAsByte(columnSource2.getBoolean(rowKey))
        );
    }

    @Override
    public final DoubleByteTuple createPreviousTuple(final long rowKey) {
        return new DoubleByteTuple(
                columnSource1.getPrevDouble(rowKey),
                BooleanUtils.booleanAsByte(columnSource2.getPrevBoolean(rowKey))
        );
    }

    @Override
    public final DoubleByteTuple createTupleFromValues(@NotNull final Object... values) {
        return new DoubleByteTuple(
                TypeUtils.unbox((Double)values[0]),
                BooleanUtils.booleanAsByte((Boolean)values[1])
        );
    }

    @Override
    public final DoubleByteTuple createTupleFromReinterpretedValues(@NotNull final Object... values) {
        return new DoubleByteTuple(
                TypeUtils.unbox((Double)values[0]),
                BooleanUtils.booleanAsByte((Boolean)values[1])
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <ELEMENT_TYPE> void exportElement(@NotNull final DoubleByteTuple tuple, final int elementIndex, @NotNull final WritableColumnSource<ELEMENT_TYPE> writableSource, final long destinationRowKey) {
        if (elementIndex == 0) {
            writableSource.set(destinationRowKey, tuple.getFirstElement());
            return;
        }
        if (elementIndex == 1) {
            writableSource.set(destinationRowKey, (ELEMENT_TYPE) BooleanUtils.byteAsBoolean(tuple.getSecondElement()));
            return;
        }
        throw new IndexOutOfBoundsException("Invalid element index " + elementIndex + " for export");
    }

    @Override
    public final Object exportToExternalKey(@NotNull final DoubleByteTuple tuple) {
        return new SmartKey(
                TypeUtils.box(tuple.getFirstElement()),
                BooleanUtils.byteAsBoolean(tuple.getSecondElement())
        );
    }

    @Override
    public final Object exportElement(@NotNull final DoubleByteTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return TypeUtils.box(tuple.getFirstElement());
        }
        if (elementIndex == 1) {
            return BooleanUtils.byteAsBoolean(tuple.getSecondElement());
        }
        throw new IllegalArgumentException("Bad elementIndex for 2 element tuple: " + elementIndex);
    }

    @Override
    public final Object exportElementReinterpreted(@NotNull final DoubleByteTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return TypeUtils.box(tuple.getFirstElement());
        }
        if (elementIndex == 1) {
            return BooleanUtils.byteAsBoolean(tuple.getSecondElement());
        }
        throw new IllegalArgumentException("Bad elementIndex for 2 element tuple: " + elementIndex);
    }

    protected void convertChunks(@NotNull WritableChunk<? super Values> destination, int chunkSize, Chunk<Values> [] chunks) {
        WritableObjectChunk<DoubleByteTuple, ? super Values> destinationObjectChunk = destination.asWritableObjectChunk();
        DoubleChunk<Values> chunk1 = chunks[0].asDoubleChunk();
        ObjectChunk<Boolean, Values> chunk2 = chunks[1].asObjectChunk();
        for (int ii = 0; ii < chunkSize; ++ii) {
            destinationObjectChunk.set(ii, new DoubleByteTuple(chunk1.get(ii), BooleanUtils.booleanAsByte(chunk2.get(ii))));
        }
        destination.setSize(chunkSize);
    }

    /** {@link TwoColumnTupleSourceFactory} for instances of {@link DoubleBooleanColumnTupleSource}. **/
    private static final class Factory implements TwoColumnTupleSourceFactory<DoubleByteTuple, Double, Boolean> {

        private Factory() {
        }

        @Override
        public TupleSource<DoubleByteTuple> create(
                @NotNull final ColumnSource<Double> columnSource1,
                @NotNull final ColumnSource<Boolean> columnSource2
        ) {
            return new DoubleBooleanColumnTupleSource(
                    columnSource1,
                    columnSource2
            );
        }
    }
}
