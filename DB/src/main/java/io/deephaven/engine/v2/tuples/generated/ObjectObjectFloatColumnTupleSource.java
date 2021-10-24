package io.deephaven.engine.v2.tuples.generated;

import io.deephaven.datastructures.util.SmartKey;
import io.deephaven.engine.util.tuples.generated.ObjectObjectFloatTuple;
import io.deephaven.engine.v2.sources.ColumnSource;
import io.deephaven.engine.v2.sources.WritableSource;
import io.deephaven.engine.v2.sources.chunk.Attributes;
import io.deephaven.engine.v2.sources.chunk.Chunk;
import io.deephaven.engine.v2.sources.chunk.FloatChunk;
import io.deephaven.engine.v2.sources.chunk.ObjectChunk;
import io.deephaven.engine.v2.sources.chunk.WritableChunk;
import io.deephaven.engine.v2.sources.chunk.WritableObjectChunk;
import io.deephaven.engine.v2.tuples.AbstractTupleSource;
import io.deephaven.engine.v2.tuples.ThreeColumnTupleSourceFactory;
import io.deephaven.engine.v2.tuples.TupleSource;
import io.deephaven.util.type.TypeUtils;
import org.jetbrains.annotations.NotNull;


/**
 * <p>{@link TupleSource} that produces key column values from {@link ColumnSource} types Object, Object, and Float.
 * <p>Generated by {@link io.deephaven.engine.v2.tuples.TupleSourceCodeGenerator}.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class ObjectObjectFloatColumnTupleSource extends AbstractTupleSource<ObjectObjectFloatTuple> {

    /** {@link ThreeColumnTupleSourceFactory} instance to create instances of {@link ObjectObjectFloatColumnTupleSource}. **/
    public static final ThreeColumnTupleSourceFactory<ObjectObjectFloatTuple, Object, Object, Float> FACTORY = new Factory();

    private final ColumnSource<Object> columnSource1;
    private final ColumnSource<Object> columnSource2;
    private final ColumnSource<Float> columnSource3;

    public ObjectObjectFloatColumnTupleSource(
            @NotNull final ColumnSource<Object> columnSource1,
            @NotNull final ColumnSource<Object> columnSource2,
            @NotNull final ColumnSource<Float> columnSource3
    ) {
        super(columnSource1, columnSource2, columnSource3);
        this.columnSource1 = columnSource1;
        this.columnSource2 = columnSource2;
        this.columnSource3 = columnSource3;
    }

    @Override
    public final ObjectObjectFloatTuple createTuple(final long indexKey) {
        return new ObjectObjectFloatTuple(
                columnSource1.get(indexKey),
                columnSource2.get(indexKey),
                columnSource3.getFloat(indexKey)
        );
    }

    @Override
    public final ObjectObjectFloatTuple createPreviousTuple(final long indexKey) {
        return new ObjectObjectFloatTuple(
                columnSource1.getPrev(indexKey),
                columnSource2.getPrev(indexKey),
                columnSource3.getPrevFloat(indexKey)
        );
    }

    @Override
    public final ObjectObjectFloatTuple createTupleFromValues(@NotNull final Object... values) {
        return new ObjectObjectFloatTuple(
                values[0],
                values[1],
                TypeUtils.unbox((Float)values[2])
        );
    }

    @Override
    public final ObjectObjectFloatTuple createTupleFromReinterpretedValues(@NotNull final Object... values) {
        return new ObjectObjectFloatTuple(
                values[0],
                values[1],
                TypeUtils.unbox((Float)values[2])
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <ELEMENT_TYPE> void exportElement(@NotNull final ObjectObjectFloatTuple tuple, final int elementIndex, @NotNull final WritableSource<ELEMENT_TYPE> writableSource, final long destinationIndexKey) {
        if (elementIndex == 0) {
            writableSource.set(destinationIndexKey, (ELEMENT_TYPE) tuple.getFirstElement());
            return;
        }
        if (elementIndex == 1) {
            writableSource.set(destinationIndexKey, (ELEMENT_TYPE) tuple.getSecondElement());
            return;
        }
        if (elementIndex == 2) {
            writableSource.set(destinationIndexKey, tuple.getThirdElement());
            return;
        }
        throw new IndexOutOfBoundsException("Invalid element rowSet " + elementIndex + " for export");
    }

    @Override
    public final Object exportToExternalKey(@NotNull final ObjectObjectFloatTuple tuple) {
        return new SmartKey(
                tuple.getFirstElement(),
                tuple.getSecondElement(),
                TypeUtils.box(tuple.getThirdElement())
        );
    }

    @Override
    public final Object exportElement(@NotNull final ObjectObjectFloatTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return tuple.getFirstElement();
        }
        if (elementIndex == 1) {
            return tuple.getSecondElement();
        }
        if (elementIndex == 2) {
            return TypeUtils.box(tuple.getThirdElement());
        }
        throw new IllegalArgumentException("Bad elementIndex for 3 element tuple: " + elementIndex);
    }

    @Override
    public final Object exportElementReinterpreted(@NotNull final ObjectObjectFloatTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return tuple.getFirstElement();
        }
        if (elementIndex == 1) {
            return tuple.getSecondElement();
        }
        if (elementIndex == 2) {
            return TypeUtils.box(tuple.getThirdElement());
        }
        throw new IllegalArgumentException("Bad elementIndex for 3 element tuple: " + elementIndex);
    }

    @Override
    protected void convertChunks(@NotNull WritableChunk<? super Attributes.Values> destination, int chunkSize, Chunk<Attributes.Values> [] chunks) {
        WritableObjectChunk<ObjectObjectFloatTuple, ? super Attributes.Values> destinationObjectChunk = destination.asWritableObjectChunk();
        ObjectChunk<Object, Attributes.Values> chunk1 = chunks[0].asObjectChunk();
        ObjectChunk<Object, Attributes.Values> chunk2 = chunks[1].asObjectChunk();
        FloatChunk<Attributes.Values> chunk3 = chunks[2].asFloatChunk();
        for (int ii = 0; ii < chunkSize; ++ii) {
            destinationObjectChunk.set(ii, new ObjectObjectFloatTuple(chunk1.get(ii), chunk2.get(ii), chunk3.get(ii)));
        }
        destinationObjectChunk.setSize(chunkSize);
    }

    /** {@link ThreeColumnTupleSourceFactory} for instances of {@link ObjectObjectFloatColumnTupleSource}. **/
    private static final class Factory implements ThreeColumnTupleSourceFactory<ObjectObjectFloatTuple, Object, Object, Float> {

        private Factory() {
        }

        @Override
        public TupleSource<ObjectObjectFloatTuple> create(
                @NotNull final ColumnSource<Object> columnSource1,
                @NotNull final ColumnSource<Object> columnSource2,
                @NotNull final ColumnSource<Float> columnSource3
        ) {
            return new ObjectObjectFloatColumnTupleSource(
                    columnSource1,
                    columnSource2,
                    columnSource3
            );
        }
    }
}
