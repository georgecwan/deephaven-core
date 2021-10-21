package io.deephaven.engine.v2.tuples.generated;

import io.deephaven.datastructures.util.SmartKey;
import io.deephaven.engine.util.tuples.generated.ShortByteDoubleTuple;
import io.deephaven.engine.v2.sources.ColumnSource;
import io.deephaven.engine.v2.sources.WritableSource;
import io.deephaven.engine.v2.sources.chunk.Attributes;
import io.deephaven.engine.v2.sources.chunk.ByteChunk;
import io.deephaven.engine.v2.sources.chunk.Chunk;
import io.deephaven.engine.v2.sources.chunk.DoubleChunk;
import io.deephaven.engine.v2.sources.chunk.ObjectChunk;
import io.deephaven.engine.v2.sources.chunk.ShortChunk;
import io.deephaven.engine.v2.sources.chunk.WritableChunk;
import io.deephaven.engine.v2.sources.chunk.WritableObjectChunk;
import io.deephaven.engine.v2.tuples.AbstractTupleSource;
import io.deephaven.engine.v2.tuples.ThreeColumnTupleSourceFactory;
import io.deephaven.engine.v2.tuples.TupleSource;
import io.deephaven.util.type.TypeUtils;
import org.jetbrains.annotations.NotNull;


/**
 * <p>{@link TupleSource} that produces key column values from {@link ColumnSource} types Short, Byte, and Double.
 * <p>Generated by {@link io.deephaven.engine.v2.tuples.TupleSourceCodeGenerator}.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class ShortByteDoubleColumnTupleSource extends AbstractTupleSource<ShortByteDoubleTuple> {

    /** {@link ThreeColumnTupleSourceFactory} instance to create instances of {@link ShortByteDoubleColumnTupleSource}. **/
    public static final ThreeColumnTupleSourceFactory<ShortByteDoubleTuple, Short, Byte, Double> FACTORY = new Factory();

    private final ColumnSource<Short> columnSource1;
    private final ColumnSource<Byte> columnSource2;
    private final ColumnSource<Double> columnSource3;

    public ShortByteDoubleColumnTupleSource(
            @NotNull final ColumnSource<Short> columnSource1,
            @NotNull final ColumnSource<Byte> columnSource2,
            @NotNull final ColumnSource<Double> columnSource3
    ) {
        super(columnSource1, columnSource2, columnSource3);
        this.columnSource1 = columnSource1;
        this.columnSource2 = columnSource2;
        this.columnSource3 = columnSource3;
    }

    @Override
    public final ShortByteDoubleTuple createTuple(final long indexKey) {
        return new ShortByteDoubleTuple(
                columnSource1.getShort(indexKey),
                columnSource2.getByte(indexKey),
                columnSource3.getDouble(indexKey)
        );
    }

    @Override
    public final ShortByteDoubleTuple createPreviousTuple(final long indexKey) {
        return new ShortByteDoubleTuple(
                columnSource1.getPrevShort(indexKey),
                columnSource2.getPrevByte(indexKey),
                columnSource3.getPrevDouble(indexKey)
        );
    }

    @Override
    public final ShortByteDoubleTuple createTupleFromValues(@NotNull final Object... values) {
        return new ShortByteDoubleTuple(
                TypeUtils.unbox((Short)values[0]),
                TypeUtils.unbox((Byte)values[1]),
                TypeUtils.unbox((Double)values[2])
        );
    }

    @Override
    public final ShortByteDoubleTuple createTupleFromReinterpretedValues(@NotNull final Object... values) {
        return new ShortByteDoubleTuple(
                TypeUtils.unbox((Short)values[0]),
                TypeUtils.unbox((Byte)values[1]),
                TypeUtils.unbox((Double)values[2])
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <ELEMENT_TYPE> void exportElement(@NotNull final ShortByteDoubleTuple tuple, final int elementIndex, @NotNull final WritableSource<ELEMENT_TYPE> writableSource, final long destinationIndexKey) {
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
    public final Object exportToExternalKey(@NotNull final ShortByteDoubleTuple tuple) {
        return new SmartKey(
                TypeUtils.box(tuple.getFirstElement()),
                TypeUtils.box(tuple.getSecondElement()),
                TypeUtils.box(tuple.getThirdElement())
        );
    }

    @Override
    public final Object exportElement(@NotNull final ShortByteDoubleTuple tuple, int elementIndex) {
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
    public final Object exportElementReinterpreted(@NotNull final ShortByteDoubleTuple tuple, int elementIndex) {
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
        WritableObjectChunk<ShortByteDoubleTuple, ? super Attributes.Values> destinationObjectChunk = destination.asWritableObjectChunk();
        ShortChunk<Attributes.Values> chunk1 = chunks[0].asShortChunk();
        ByteChunk<Attributes.Values> chunk2 = chunks[1].asByteChunk();
        DoubleChunk<Attributes.Values> chunk3 = chunks[2].asDoubleChunk();
        for (int ii = 0; ii < chunkSize; ++ii) {
            destinationObjectChunk.set(ii, new ShortByteDoubleTuple(chunk1.get(ii), chunk2.get(ii), chunk3.get(ii)));
        }
        destinationObjectChunk.setSize(chunkSize);
    }

    /** {@link ThreeColumnTupleSourceFactory} for instances of {@link ShortByteDoubleColumnTupleSource}. **/
    private static final class Factory implements ThreeColumnTupleSourceFactory<ShortByteDoubleTuple, Short, Byte, Double> {

        private Factory() {
        }

        @Override
        public TupleSource<ShortByteDoubleTuple> create(
                @NotNull final ColumnSource<Short> columnSource1,
                @NotNull final ColumnSource<Byte> columnSource2,
                @NotNull final ColumnSource<Double> columnSource3
        ) {
            return new ShortByteDoubleColumnTupleSource(
                    columnSource1,
                    columnSource2,
                    columnSource3
            );
        }
    }
}
