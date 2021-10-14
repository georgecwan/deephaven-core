package io.deephaven.engine.v2.tuples.generated;

import io.deephaven.datastructures.util.SmartKey;
import io.deephaven.engine.util.tuples.generated.FloatFloatObjectTuple;
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
 * <p>{@link TupleSource} that produces key column values from {@link ColumnSource} types Float, Float, and Object.
 * <p>Generated by {@link io.deephaven.engine.v2.tuples.TupleSourceCodeGenerator}.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class FloatFloatObjectColumnTupleSource extends AbstractTupleSource<FloatFloatObjectTuple> {

    /** {@link ThreeColumnTupleSourceFactory} instance to create instances of {@link FloatFloatObjectColumnTupleSource}. **/
    public static final ThreeColumnTupleSourceFactory<FloatFloatObjectTuple, Float, Float, Object> FACTORY = new Factory();

    private final ColumnSource<Float> columnSource1;
    private final ColumnSource<Float> columnSource2;
    private final ColumnSource<Object> columnSource3;

    public FloatFloatObjectColumnTupleSource(
            @NotNull final ColumnSource<Float> columnSource1,
            @NotNull final ColumnSource<Float> columnSource2,
            @NotNull final ColumnSource<Object> columnSource3
    ) {
        super(columnSource1, columnSource2, columnSource3);
        this.columnSource1 = columnSource1;
        this.columnSource2 = columnSource2;
        this.columnSource3 = columnSource3;
    }

    @Override
    public final FloatFloatObjectTuple createTuple(final long indexKey) {
        return new FloatFloatObjectTuple(
                columnSource1.getFloat(indexKey),
                columnSource2.getFloat(indexKey),
                columnSource3.get(indexKey)
        );
    }

    @Override
    public final FloatFloatObjectTuple createPreviousTuple(final long indexKey) {
        return new FloatFloatObjectTuple(
                columnSource1.getPrevFloat(indexKey),
                columnSource2.getPrevFloat(indexKey),
                columnSource3.getPrev(indexKey)
        );
    }

    @Override
    public final FloatFloatObjectTuple createTupleFromValues(@NotNull final Object... values) {
        return new FloatFloatObjectTuple(
                TypeUtils.unbox((Float)values[0]),
                TypeUtils.unbox((Float)values[1]),
                values[2]
        );
    }

    @Override
    public final FloatFloatObjectTuple createTupleFromReinterpretedValues(@NotNull final Object... values) {
        return new FloatFloatObjectTuple(
                TypeUtils.unbox((Float)values[0]),
                TypeUtils.unbox((Float)values[1]),
                values[2]
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <ELEMENT_TYPE> void exportElement(@NotNull final FloatFloatObjectTuple tuple, final int elementIndex, @NotNull final WritableSource<ELEMENT_TYPE> writableSource, final long destinationIndexKey) {
        if (elementIndex == 0) {
            writableSource.set(destinationIndexKey, tuple.getFirstElement());
            return;
        }
        if (elementIndex == 1) {
            writableSource.set(destinationIndexKey, tuple.getSecondElement());
            return;
        }
        if (elementIndex == 2) {
            writableSource.set(destinationIndexKey, (ELEMENT_TYPE) tuple.getThirdElement());
            return;
        }
        throw new IndexOutOfBoundsException("Invalid element index " + elementIndex + " for export");
    }

    @Override
    public final Object exportToExternalKey(@NotNull final FloatFloatObjectTuple tuple) {
        return new SmartKey(
                TypeUtils.box(tuple.getFirstElement()),
                TypeUtils.box(tuple.getSecondElement()),
                tuple.getThirdElement()
        );
    }

    @Override
    public final Object exportElement(@NotNull final FloatFloatObjectTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return TypeUtils.box(tuple.getFirstElement());
        }
        if (elementIndex == 1) {
            return TypeUtils.box(tuple.getSecondElement());
        }
        if (elementIndex == 2) {
            return tuple.getThirdElement();
        }
        throw new IllegalArgumentException("Bad elementIndex for 3 element tuple: " + elementIndex);
    }

    @Override
    public final Object exportElementReinterpreted(@NotNull final FloatFloatObjectTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return TypeUtils.box(tuple.getFirstElement());
        }
        if (elementIndex == 1) {
            return TypeUtils.box(tuple.getSecondElement());
        }
        if (elementIndex == 2) {
            return tuple.getThirdElement();
        }
        throw new IllegalArgumentException("Bad elementIndex for 3 element tuple: " + elementIndex);
    }

    @Override
    protected void convertChunks(@NotNull WritableChunk<? super Attributes.Values> destination, int chunkSize, Chunk<Attributes.Values> [] chunks) {
        WritableObjectChunk<FloatFloatObjectTuple, ? super Attributes.Values> destinationObjectChunk = destination.asWritableObjectChunk();
        FloatChunk<Attributes.Values> chunk1 = chunks[0].asFloatChunk();
        FloatChunk<Attributes.Values> chunk2 = chunks[1].asFloatChunk();
        ObjectChunk<Object, Attributes.Values> chunk3 = chunks[2].asObjectChunk();
        for (int ii = 0; ii < chunkSize; ++ii) {
            destinationObjectChunk.set(ii, new FloatFloatObjectTuple(chunk1.get(ii), chunk2.get(ii), chunk3.get(ii)));
        }
        destinationObjectChunk.setSize(chunkSize);
    }

    /** {@link ThreeColumnTupleSourceFactory} for instances of {@link FloatFloatObjectColumnTupleSource}. **/
    private static final class Factory implements ThreeColumnTupleSourceFactory<FloatFloatObjectTuple, Float, Float, Object> {

        private Factory() {
        }

        @Override
        public TupleSource<FloatFloatObjectTuple> create(
                @NotNull final ColumnSource<Float> columnSource1,
                @NotNull final ColumnSource<Float> columnSource2,
                @NotNull final ColumnSource<Object> columnSource3
        ) {
            return new FloatFloatObjectColumnTupleSource(
                    columnSource1,
                    columnSource2,
                    columnSource3
            );
        }
    }
}