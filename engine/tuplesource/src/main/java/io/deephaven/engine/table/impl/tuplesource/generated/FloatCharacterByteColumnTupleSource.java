package io.deephaven.engine.table.impl.tuplesource.generated;

import io.deephaven.datastructures.util.SmartKey;
import io.deephaven.engine.chunk.Attributes.Values;
import io.deephaven.engine.chunk.ByteChunk;
import io.deephaven.engine.chunk.CharChunk;
import io.deephaven.engine.chunk.Chunk;
import io.deephaven.engine.chunk.FloatChunk;
import io.deephaven.engine.chunk.WritableChunk;
import io.deephaven.engine.chunk.WritableObjectChunk;
import io.deephaven.engine.table.ColumnSource;
import io.deephaven.engine.table.TupleSource;
import io.deephaven.engine.table.WritableColumnSource;
import io.deephaven.engine.table.impl.tuplesource.AbstractTupleSource;
import io.deephaven.engine.table.impl.tuplesource.ThreeColumnTupleSourceFactory;
import io.deephaven.engine.tuple.generated.FloatCharByteTuple;
import io.deephaven.util.type.TypeUtils;
import org.jetbrains.annotations.NotNull;


/**
 * <p>{@link TupleSource} that produces key column values from {@link ColumnSource} types Float, Character, and Byte.
 * <p>Generated by io.deephaven.replicators.TupleSourceCodeGenerator.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class FloatCharacterByteColumnTupleSource extends AbstractTupleSource<FloatCharByteTuple> {

    /** {@link ThreeColumnTupleSourceFactory} instance to create instances of {@link FloatCharacterByteColumnTupleSource}. **/
    public static final ThreeColumnTupleSourceFactory<FloatCharByteTuple, Float, Character, Byte> FACTORY = new Factory();

    private final ColumnSource<Float> columnSource1;
    private final ColumnSource<Character> columnSource2;
    private final ColumnSource<Byte> columnSource3;

    public FloatCharacterByteColumnTupleSource(
            @NotNull final ColumnSource<Float> columnSource1,
            @NotNull final ColumnSource<Character> columnSource2,
            @NotNull final ColumnSource<Byte> columnSource3
    ) {
        super(columnSource1, columnSource2, columnSource3);
        this.columnSource1 = columnSource1;
        this.columnSource2 = columnSource2;
        this.columnSource3 = columnSource3;
    }

    @Override
    public final FloatCharByteTuple createTuple(final long indexKey) {
        return new FloatCharByteTuple(
                columnSource1.getFloat(indexKey),
                columnSource2.getChar(indexKey),
                columnSource3.getByte(indexKey)
        );
    }

    @Override
    public final FloatCharByteTuple createPreviousTuple(final long indexKey) {
        return new FloatCharByteTuple(
                columnSource1.getPrevFloat(indexKey),
                columnSource2.getPrevChar(indexKey),
                columnSource3.getPrevByte(indexKey)
        );
    }

    @Override
    public final FloatCharByteTuple createTupleFromValues(@NotNull final Object... values) {
        return new FloatCharByteTuple(
                TypeUtils.unbox((Float)values[0]),
                TypeUtils.unbox((Character)values[1]),
                TypeUtils.unbox((Byte)values[2])
        );
    }

    @Override
    public final FloatCharByteTuple createTupleFromReinterpretedValues(@NotNull final Object... values) {
        return new FloatCharByteTuple(
                TypeUtils.unbox((Float)values[0]),
                TypeUtils.unbox((Character)values[1]),
                TypeUtils.unbox((Byte)values[2])
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <ELEMENT_TYPE> void exportElement(@NotNull final FloatCharByteTuple tuple, final int elementIndex, @NotNull final WritableColumnSource<ELEMENT_TYPE> writableSource, final long destinationIndexKey) {
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
    public final Object exportToExternalKey(@NotNull final FloatCharByteTuple tuple) {
        return new SmartKey(
                TypeUtils.box(tuple.getFirstElement()),
                TypeUtils.box(tuple.getSecondElement()),
                TypeUtils.box(tuple.getThirdElement())
        );
    }

    @Override
    public final Object exportElement(@NotNull final FloatCharByteTuple tuple, int elementIndex) {
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
    public final Object exportElementReinterpreted(@NotNull final FloatCharByteTuple tuple, int elementIndex) {
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
        WritableObjectChunk<FloatCharByteTuple, ? super Values> destinationObjectChunk = destination.asWritableObjectChunk();
        FloatChunk<Values> chunk1 = chunks[0].asFloatChunk();
        CharChunk<Values> chunk2 = chunks[1].asCharChunk();
        ByteChunk<Values> chunk3 = chunks[2].asByteChunk();
        for (int ii = 0; ii < chunkSize; ++ii) {
            destinationObjectChunk.set(ii, new FloatCharByteTuple(chunk1.get(ii), chunk2.get(ii), chunk3.get(ii)));
        }
        destinationObjectChunk.setSize(chunkSize);
    }

    /** {@link ThreeColumnTupleSourceFactory} for instances of {@link FloatCharacterByteColumnTupleSource}. **/
    private static final class Factory implements ThreeColumnTupleSourceFactory<FloatCharByteTuple, Float, Character, Byte> {

        private Factory() {
        }

        @Override
        public TupleSource<FloatCharByteTuple> create(
                @NotNull final ColumnSource<Float> columnSource1,
                @NotNull final ColumnSource<Character> columnSource2,
                @NotNull final ColumnSource<Byte> columnSource3
        ) {
            return new FloatCharacterByteColumnTupleSource(
                    columnSource1,
                    columnSource2,
                    columnSource3
            );
        }
    }
}