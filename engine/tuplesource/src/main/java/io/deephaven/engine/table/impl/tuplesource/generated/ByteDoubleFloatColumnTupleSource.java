package io.deephaven.engine.table.impl.tuplesource.generated;

import io.deephaven.datastructures.util.SmartKey;
import io.deephaven.engine.chunk.Attributes.Values;
import io.deephaven.engine.chunk.ByteChunk;
import io.deephaven.engine.chunk.Chunk;
import io.deephaven.engine.chunk.DoubleChunk;
import io.deephaven.engine.chunk.FloatChunk;
import io.deephaven.engine.chunk.WritableChunk;
import io.deephaven.engine.chunk.WritableObjectChunk;
import io.deephaven.engine.table.ColumnSource;
import io.deephaven.engine.table.TupleSource;
import io.deephaven.engine.table.WritableColumnSource;
import io.deephaven.engine.table.impl.tuplesource.AbstractTupleSource;
import io.deephaven.engine.table.impl.tuplesource.ThreeColumnTupleSourceFactory;
import io.deephaven.engine.tuple.generated.ByteDoubleFloatTuple;
import io.deephaven.util.type.TypeUtils;
import org.jetbrains.annotations.NotNull;


/**
 * <p>{@link TupleSource} that produces key column values from {@link ColumnSource} types Byte, Double, and Float.
 * <p>Generated by io.deephaven.replicators.TupleSourceCodeGenerator.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class ByteDoubleFloatColumnTupleSource extends AbstractTupleSource<ByteDoubleFloatTuple> {

    /** {@link ThreeColumnTupleSourceFactory} instance to create instances of {@link ByteDoubleFloatColumnTupleSource}. **/
    public static final ThreeColumnTupleSourceFactory<ByteDoubleFloatTuple, Byte, Double, Float> FACTORY = new Factory();

    private final ColumnSource<Byte> columnSource1;
    private final ColumnSource<Double> columnSource2;
    private final ColumnSource<Float> columnSource3;

    public ByteDoubleFloatColumnTupleSource(
            @NotNull final ColumnSource<Byte> columnSource1,
            @NotNull final ColumnSource<Double> columnSource2,
            @NotNull final ColumnSource<Float> columnSource3
    ) {
        super(columnSource1, columnSource2, columnSource3);
        this.columnSource1 = columnSource1;
        this.columnSource2 = columnSource2;
        this.columnSource3 = columnSource3;
    }

    @Override
    public final ByteDoubleFloatTuple createTuple(final long indexKey) {
        return new ByteDoubleFloatTuple(
                columnSource1.getByte(indexKey),
                columnSource2.getDouble(indexKey),
                columnSource3.getFloat(indexKey)
        );
    }

    @Override
    public final ByteDoubleFloatTuple createPreviousTuple(final long indexKey) {
        return new ByteDoubleFloatTuple(
                columnSource1.getPrevByte(indexKey),
                columnSource2.getPrevDouble(indexKey),
                columnSource3.getPrevFloat(indexKey)
        );
    }

    @Override
    public final ByteDoubleFloatTuple createTupleFromValues(@NotNull final Object... values) {
        return new ByteDoubleFloatTuple(
                TypeUtils.unbox((Byte)values[0]),
                TypeUtils.unbox((Double)values[1]),
                TypeUtils.unbox((Float)values[2])
        );
    }

    @Override
    public final ByteDoubleFloatTuple createTupleFromReinterpretedValues(@NotNull final Object... values) {
        return new ByteDoubleFloatTuple(
                TypeUtils.unbox((Byte)values[0]),
                TypeUtils.unbox((Double)values[1]),
                TypeUtils.unbox((Float)values[2])
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <ELEMENT_TYPE> void exportElement(@NotNull final ByteDoubleFloatTuple tuple, final int elementIndex, @NotNull final WritableColumnSource<ELEMENT_TYPE> writableSource, final long destinationIndexKey) {
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
    public final Object exportToExternalKey(@NotNull final ByteDoubleFloatTuple tuple) {
        return new SmartKey(
                TypeUtils.box(tuple.getFirstElement()),
                TypeUtils.box(tuple.getSecondElement()),
                TypeUtils.box(tuple.getThirdElement())
        );
    }

    @Override
    public final Object exportElement(@NotNull final ByteDoubleFloatTuple tuple, int elementIndex) {
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
    public final Object exportElementReinterpreted(@NotNull final ByteDoubleFloatTuple tuple, int elementIndex) {
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
    protected void convertChunks(@NotNull WritableChunk<? super Values> destination, int chunkSize, Chunk<Values> [] chunks) {
        WritableObjectChunk<ByteDoubleFloatTuple, ? super Values> destinationObjectChunk = destination.asWritableObjectChunk();
        ByteChunk<Values> chunk1 = chunks[0].asByteChunk();
        DoubleChunk<Values> chunk2 = chunks[1].asDoubleChunk();
        FloatChunk<Values> chunk3 = chunks[2].asFloatChunk();
        for (int ii = 0; ii < chunkSize; ++ii) {
            destinationObjectChunk.set(ii, new ByteDoubleFloatTuple(chunk1.get(ii), chunk2.get(ii), chunk3.get(ii)));
        }
        destinationObjectChunk.setSize(chunkSize);
    }

    /** {@link ThreeColumnTupleSourceFactory} for instances of {@link ByteDoubleFloatColumnTupleSource}. **/
    private static final class Factory implements ThreeColumnTupleSourceFactory<ByteDoubleFloatTuple, Byte, Double, Float> {

        private Factory() {
        }

        @Override
        public TupleSource<ByteDoubleFloatTuple> create(
                @NotNull final ColumnSource<Byte> columnSource1,
                @NotNull final ColumnSource<Double> columnSource2,
                @NotNull final ColumnSource<Float> columnSource3
        ) {
            return new ByteDoubleFloatColumnTupleSource(
                    columnSource1,
                    columnSource2,
                    columnSource3
            );
        }
    }
}