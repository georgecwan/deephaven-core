package io.deephaven.engine.v2.tuples.generated;

import io.deephaven.datastructures.util.SmartKey;
import io.deephaven.engine.util.BooleanUtils;
import io.deephaven.engine.util.tuples.generated.ShortByteShortTuple;
import io.deephaven.engine.v2.sources.ColumnSource;
import io.deephaven.engine.v2.sources.WritableSource;
import io.deephaven.engine.v2.sources.chunk.Attributes;
import io.deephaven.engine.v2.sources.chunk.ByteChunk;
import io.deephaven.engine.v2.sources.chunk.Chunk;
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
 * <p>{@link TupleSource} that produces key column values from {@link ColumnSource} types Short, Byte, and Short.
 * <p>Generated by {@link io.deephaven.engine.v2.tuples.TupleSourceCodeGenerator}.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class ShortReinterpretedBooleanShortColumnTupleSource extends AbstractTupleSource<ShortByteShortTuple> {

    /** {@link ThreeColumnTupleSourceFactory} instance to create instances of {@link ShortReinterpretedBooleanShortColumnTupleSource}. **/
    public static final ThreeColumnTupleSourceFactory<ShortByteShortTuple, Short, Byte, Short> FACTORY = new Factory();

    private final ColumnSource<Short> columnSource1;
    private final ColumnSource<Byte> columnSource2;
    private final ColumnSource<Short> columnSource3;

    public ShortReinterpretedBooleanShortColumnTupleSource(
            @NotNull final ColumnSource<Short> columnSource1,
            @NotNull final ColumnSource<Byte> columnSource2,
            @NotNull final ColumnSource<Short> columnSource3
    ) {
        super(columnSource1, columnSource2, columnSource3);
        this.columnSource1 = columnSource1;
        this.columnSource2 = columnSource2;
        this.columnSource3 = columnSource3;
    }

    @Override
    public final ShortByteShortTuple createTuple(final long indexKey) {
        return new ShortByteShortTuple(
                columnSource1.getShort(indexKey),
                columnSource2.getByte(indexKey),
                columnSource3.getShort(indexKey)
        );
    }

    @Override
    public final ShortByteShortTuple createPreviousTuple(final long indexKey) {
        return new ShortByteShortTuple(
                columnSource1.getPrevShort(indexKey),
                columnSource2.getPrevByte(indexKey),
                columnSource3.getPrevShort(indexKey)
        );
    }

    @Override
    public final ShortByteShortTuple createTupleFromValues(@NotNull final Object... values) {
        return new ShortByteShortTuple(
                TypeUtils.unbox((Short)values[0]),
                BooleanUtils.booleanAsByte((Boolean)values[1]),
                TypeUtils.unbox((Short)values[2])
        );
    }

    @Override
    public final ShortByteShortTuple createTupleFromReinterpretedValues(@NotNull final Object... values) {
        return new ShortByteShortTuple(
                TypeUtils.unbox((Short)values[0]),
                TypeUtils.unbox((Byte)values[1]),
                TypeUtils.unbox((Short)values[2])
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <ELEMENT_TYPE> void exportElement(@NotNull final ShortByteShortTuple tuple, final int elementIndex, @NotNull final WritableSource<ELEMENT_TYPE> writableSource, final long destinationIndexKey) {
        if (elementIndex == 0) {
            writableSource.set(destinationIndexKey, tuple.getFirstElement());
            return;
        }
        if (elementIndex == 1) {
            writableSource.set(destinationIndexKey, (ELEMENT_TYPE) BooleanUtils.byteAsBoolean(tuple.getSecondElement()));
            return;
        }
        if (elementIndex == 2) {
            writableSource.set(destinationIndexKey, tuple.getThirdElement());
            return;
        }
        throw new IndexOutOfBoundsException("Invalid element index " + elementIndex + " for export");
    }

    @Override
    public final Object exportToExternalKey(@NotNull final ShortByteShortTuple tuple) {
        return new SmartKey(
                TypeUtils.box(tuple.getFirstElement()),
                BooleanUtils.byteAsBoolean(tuple.getSecondElement()),
                TypeUtils.box(tuple.getThirdElement())
        );
    }

    @Override
    public final Object exportElement(@NotNull final ShortByteShortTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return TypeUtils.box(tuple.getFirstElement());
        }
        if (elementIndex == 1) {
            return BooleanUtils.byteAsBoolean(tuple.getSecondElement());
        }
        if (elementIndex == 2) {
            return TypeUtils.box(tuple.getThirdElement());
        }
        throw new IllegalArgumentException("Bad elementIndex for 3 element tuple: " + elementIndex);
    }

    @Override
    public final Object exportElementReinterpreted(@NotNull final ShortByteShortTuple tuple, int elementIndex) {
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
        WritableObjectChunk<ShortByteShortTuple, ? super Attributes.Values> destinationObjectChunk = destination.asWritableObjectChunk();
        ShortChunk<Attributes.Values> chunk1 = chunks[0].asShortChunk();
        ByteChunk<Attributes.Values> chunk2 = chunks[1].asByteChunk();
        ShortChunk<Attributes.Values> chunk3 = chunks[2].asShortChunk();
        for (int ii = 0; ii < chunkSize; ++ii) {
            destinationObjectChunk.set(ii, new ShortByteShortTuple(chunk1.get(ii), chunk2.get(ii), chunk3.get(ii)));
        }
        destinationObjectChunk.setSize(chunkSize);
    }

    /** {@link ThreeColumnTupleSourceFactory} for instances of {@link ShortReinterpretedBooleanShortColumnTupleSource}. **/
    private static final class Factory implements ThreeColumnTupleSourceFactory<ShortByteShortTuple, Short, Byte, Short> {

        private Factory() {
        }

        @Override
        public TupleSource<ShortByteShortTuple> create(
                @NotNull final ColumnSource<Short> columnSource1,
                @NotNull final ColumnSource<Byte> columnSource2,
                @NotNull final ColumnSource<Short> columnSource3
        ) {
            return new ShortReinterpretedBooleanShortColumnTupleSource(
                    columnSource1,
                    columnSource2,
                    columnSource3
            );
        }
    }
}
