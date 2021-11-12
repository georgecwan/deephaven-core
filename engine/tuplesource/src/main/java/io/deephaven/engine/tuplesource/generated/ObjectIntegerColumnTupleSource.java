package io.deephaven.engine.tuplesource.generated;

import io.deephaven.datastructures.util.SmartKey;
import io.deephaven.engine.chunk.Attributes;
import io.deephaven.engine.chunk.Chunk;
import io.deephaven.engine.chunk.ObjectChunk;
import io.deephaven.engine.chunk.WritableChunk;
import io.deephaven.engine.chunk.WritableObjectChunk;
import io.deephaven.engine.tuple.generated.ObjectIntTuple;
import io.deephaven.engine.tuplesource.AbstractTupleSource;
import io.deephaven.engine.table.TupleSource;
import io.deephaven.engine.tuplesource.TwoColumnTupleSourceFactory;
import io.deephaven.engine.v2.sources.ColumnSource;
import io.deephaven.engine.v2.sources.WritableSource;
import io.deephaven.util.type.TypeUtils;
import org.jetbrains.annotations.NotNull;


/**
 * <p>{@link TupleSource} that produces key column values from {@link ColumnSource} types Object and Int.
 * <p>Generated by io.deephaven.replicators.TupleSourceCodeGenerator.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class ObjectIntegerColumnTupleSource extends AbstractTupleSource<ObjectIntTuple> {

    /** {@link TwoColumnTupleSourceFactory} instance to create instances of {@link ObjectIntegerColumnTupleSource}. **/
    public static final TwoColumnTupleSourceFactory<ObjectIntTuple, Object, Int> FACTORY = new Factory();

    private final ColumnSource<Object> columnSource1;
    private final ColumnSource<Int> columnSource2;

    public ObjectIntegerColumnTupleSource(
            @NotNull final ColumnSource<Object> columnSource1,
            @NotNull final ColumnSource<Int> columnSource2
    ) {
        super(columnSource1, columnSource2);
        this.columnSource1 = columnSource1;
        this.columnSource2 = columnSource2;
    }

    @Override
    public final ObjectIntTuple createTuple(final long indexKey) {
        return new ObjectIntTuple(
                columnSource1.get(indexKey),
                columnSource2.getInt(indexKey)
        );
    }

    @Override
    public final ObjectIntTuple createPreviousTuple(final long indexKey) {
        return new ObjectIntTuple(
                columnSource1.getPrev(indexKey),
                columnSource2.getPrevInt(indexKey)
        );
    }

    @Override
    public final ObjectIntTuple createTupleFromValues(@NotNull final Object... values) {
        return new ObjectIntTuple(
                values[0],
                TypeUtils.unbox((Integer)values[1])
        );
    }

    @Override
    public final ObjectIntTuple createTupleFromReinterpretedValues(@NotNull final Object... values) {
        return new ObjectIntTuple(
                values[0],
                TypeUtils.unbox((Integer)values[1])
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <ELEMENT_TYPE> void exportElement(@NotNull final ObjectIntTuple tuple, final int elementIndex, @NotNull final WritableSource<ELEMENT_TYPE> writableSource, final long destinationIndexKey) {
        if (elementIndex == 0) {
            writableSource.set(destinationIndexKey, (ELEMENT_TYPE) tuple.getFirstElement());
            return;
        }
        if (elementIndex == 1) {
            writableSource.set(destinationIndexKey, tuple.getSecondElement());
            return;
        }
        throw new IndexOutOfBoundsException("Invalid element index " + elementIndex + " for export");
    }

    @Override
    public final Object exportToExternalKey(@NotNull final ObjectIntTuple tuple) {
        return new SmartKey(
                tuple.getFirstElement(),
                TypeUtils.box(tuple.getSecondElement())
        );
    }

    @Override
    public final Object exportElement(@NotNull final ObjectIntTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return tuple.getFirstElement();
        }
        if (elementIndex == 1) {
            return TypeUtils.box(tuple.getSecondElement());
        }
        throw new IllegalArgumentException("Bad elementIndex for 2 element tuple: " + elementIndex);
    }

    @Override
    public final Object exportElementReinterpreted(@NotNull final ObjectIntTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return tuple.getFirstElement();
        }
        if (elementIndex == 1) {
            return TypeUtils.box(tuple.getSecondElement());
        }
        throw new IllegalArgumentException("Bad elementIndex for 2 element tuple: " + elementIndex);
    }

    protected void convertChunks(@NotNull WritableChunk<? super Attributes.Values> destination, int chunkSize, Chunk<Attributes.Values> [] chunks) {
        WritableObjectChunk<ObjectIntTuple, ? super Attributes.Values> destinationObjectChunk = destination.asWritableObjectChunk();
        ObjectChunk<Object, Attributes.Values> chunk1 = chunks[0].asObjectChunk();
        IntChunk<Attributes.Values> chunk2 = chunks[1].asIntChunk();
        for (int ii = 0; ii < chunkSize; ++ii) {
            destinationObjectChunk.set(ii, new ObjectIntTuple(chunk1.get(ii), chunk2.get(ii)));
        }
        destination.setSize(chunkSize);
    }

    /** {@link TwoColumnTupleSourceFactory} for instances of {@link ObjectIntegerColumnTupleSource}. **/
    private static final class Factory implements TwoColumnTupleSourceFactory<ObjectIntTuple, Object, Int> {

        private Factory() {
        }

        @Override
        public TupleSource<ObjectIntTuple> create(
                @NotNull final ColumnSource<Object> columnSource1,
                @NotNull final ColumnSource<Int> columnSource2
        ) {
            return new ObjectIntegerColumnTupleSource(
                    columnSource1,
                    columnSource2
            );
        }
    }
}
