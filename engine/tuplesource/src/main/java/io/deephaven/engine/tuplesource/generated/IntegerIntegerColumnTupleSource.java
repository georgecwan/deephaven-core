package io.deephaven.engine.tuplesource.generated;

import io.deephaven.datastructures.util.SmartKey;
import io.deephaven.engine.chunk.Attributes;
import io.deephaven.engine.chunk.Chunk;
import io.deephaven.engine.chunk.WritableChunk;
import io.deephaven.engine.chunk.WritableObjectChunk;
import io.deephaven.engine.tuple.generated.IntIntTuple;
import io.deephaven.engine.tuplesource.AbstractTupleSource;
import io.deephaven.engine.table.TupleSource;
import io.deephaven.engine.tuplesource.TwoColumnTupleSourceFactory;
import io.deephaven.engine.v2.sources.ColumnSource;
import io.deephaven.engine.v2.sources.WritableSource;
import io.deephaven.util.type.TypeUtils;
import org.jetbrains.annotations.NotNull;


/**
 * <p>{@link TupleSource} that produces key column values from {@link ColumnSource} types Int and Int.
 * <p>Generated by io.deephaven.replicators.TupleSourceCodeGenerator.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class IntegerIntegerColumnTupleSource extends AbstractTupleSource<IntIntTuple> {

    /** {@link TwoColumnTupleSourceFactory} instance to create instances of {@link IntegerIntegerColumnTupleSource}. **/
    public static final TwoColumnTupleSourceFactory<IntIntTuple, Int, Int> FACTORY = new Factory();

    private final ColumnSource<Int> columnSource1;
    private final ColumnSource<Int> columnSource2;

    public IntegerIntegerColumnTupleSource(
            @NotNull final ColumnSource<Int> columnSource1,
            @NotNull final ColumnSource<Int> columnSource2
    ) {
        super(columnSource1, columnSource2);
        this.columnSource1 = columnSource1;
        this.columnSource2 = columnSource2;
    }

    @Override
    public final IntIntTuple createTuple(final long indexKey) {
        return new IntIntTuple(
                columnSource1.getInt(indexKey),
                columnSource2.getInt(indexKey)
        );
    }

    @Override
    public final IntIntTuple createPreviousTuple(final long indexKey) {
        return new IntIntTuple(
                columnSource1.getPrevInt(indexKey),
                columnSource2.getPrevInt(indexKey)
        );
    }

    @Override
    public final IntIntTuple createTupleFromValues(@NotNull final Object... values) {
        return new IntIntTuple(
                TypeUtils.unbox((Integer)values[0]),
                TypeUtils.unbox((Integer)values[1])
        );
    }

    @Override
    public final IntIntTuple createTupleFromReinterpretedValues(@NotNull final Object... values) {
        return new IntIntTuple(
                TypeUtils.unbox((Integer)values[0]),
                TypeUtils.unbox((Integer)values[1])
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <ELEMENT_TYPE> void exportElement(@NotNull final IntIntTuple tuple, final int elementIndex, @NotNull final WritableSource<ELEMENT_TYPE> writableSource, final long destinationIndexKey) {
        if (elementIndex == 0) {
            writableSource.set(destinationIndexKey, tuple.getFirstElement());
            return;
        }
        if (elementIndex == 1) {
            writableSource.set(destinationIndexKey, tuple.getSecondElement());
            return;
        }
        throw new IndexOutOfBoundsException("Invalid element index " + elementIndex + " for export");
    }

    @Override
    public final Object exportToExternalKey(@NotNull final IntIntTuple tuple) {
        return new SmartKey(
                TypeUtils.box(tuple.getFirstElement()),
                TypeUtils.box(tuple.getSecondElement())
        );
    }

    @Override
    public final Object exportElement(@NotNull final IntIntTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return TypeUtils.box(tuple.getFirstElement());
        }
        if (elementIndex == 1) {
            return TypeUtils.box(tuple.getSecondElement());
        }
        throw new IllegalArgumentException("Bad elementIndex for 2 element tuple: " + elementIndex);
    }

    @Override
    public final Object exportElementReinterpreted(@NotNull final IntIntTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return TypeUtils.box(tuple.getFirstElement());
        }
        if (elementIndex == 1) {
            return TypeUtils.box(tuple.getSecondElement());
        }
        throw new IllegalArgumentException("Bad elementIndex for 2 element tuple: " + elementIndex);
    }

    protected void convertChunks(@NotNull WritableChunk<? super Attributes.Values> destination, int chunkSize, Chunk<Attributes.Values> [] chunks) {
        WritableObjectChunk<IntIntTuple, ? super Attributes.Values> destinationObjectChunk = destination.asWritableObjectChunk();
        IntChunk<Attributes.Values> chunk1 = chunks[0].asIntChunk();
        IntChunk<Attributes.Values> chunk2 = chunks[1].asIntChunk();
        for (int ii = 0; ii < chunkSize; ++ii) {
            destinationObjectChunk.set(ii, new IntIntTuple(chunk1.get(ii), chunk2.get(ii)));
        }
        destination.setSize(chunkSize);
    }

    /** {@link TwoColumnTupleSourceFactory} for instances of {@link IntegerIntegerColumnTupleSource}. **/
    private static final class Factory implements TwoColumnTupleSourceFactory<IntIntTuple, Int, Int> {

        private Factory() {
        }

        @Override
        public TupleSource<IntIntTuple> create(
                @NotNull final ColumnSource<Int> columnSource1,
                @NotNull final ColumnSource<Int> columnSource2
        ) {
            return new IntegerIntegerColumnTupleSource(
                    columnSource1,
                    columnSource2
            );
        }
    }
}
