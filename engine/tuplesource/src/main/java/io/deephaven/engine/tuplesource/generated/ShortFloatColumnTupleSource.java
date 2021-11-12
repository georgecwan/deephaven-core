package io.deephaven.engine.tuplesource.generated;

import io.deephaven.datastructures.util.SmartKey;
import io.deephaven.engine.chunk.Attributes;
import io.deephaven.engine.chunk.Chunk;
import io.deephaven.engine.chunk.WritableChunk;
import io.deephaven.engine.chunk.WritableObjectChunk;
import io.deephaven.engine.tuple.generated.ShortFloatTuple;
import io.deephaven.engine.tuplesource.AbstractTupleSource;
import io.deephaven.engine.table.TupleSource;
import io.deephaven.engine.tuplesource.TwoColumnTupleSourceFactory;
import io.deephaven.engine.v2.sources.ColumnSource;
import io.deephaven.engine.v2.sources.WritableSource;
import io.deephaven.util.type.TypeUtils;
import org.jetbrains.annotations.NotNull;


/**
 * <p>{@link TupleSource} that produces key column values from {@link ColumnSource} types Short and Float.
 * <p>Generated by io.deephaven.replicators.TupleSourceCodeGenerator.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class ShortFloatColumnTupleSource extends AbstractTupleSource<ShortFloatTuple> {

    /** {@link TwoColumnTupleSourceFactory} instance to create instances of {@link ShortFloatColumnTupleSource}. **/
    public static final TwoColumnTupleSourceFactory<ShortFloatTuple, Short, Float> FACTORY = new Factory();

    private final ColumnSource<Short> columnSource1;
    private final ColumnSource<Float> columnSource2;

    public ShortFloatColumnTupleSource(
            @NotNull final ColumnSource<Short> columnSource1,
            @NotNull final ColumnSource<Float> columnSource2
    ) {
        super(columnSource1, columnSource2);
        this.columnSource1 = columnSource1;
        this.columnSource2 = columnSource2;
    }

    @Override
    public final ShortFloatTuple createTuple(final long indexKey) {
        return new ShortFloatTuple(
                columnSource1.getShort(indexKey),
                columnSource2.getFloat(indexKey)
        );
    }

    @Override
    public final ShortFloatTuple createPreviousTuple(final long indexKey) {
        return new ShortFloatTuple(
                columnSource1.getPrevShort(indexKey),
                columnSource2.getPrevFloat(indexKey)
        );
    }

    @Override
    public final ShortFloatTuple createTupleFromValues(@NotNull final Object... values) {
        return new ShortFloatTuple(
                TypeUtils.unbox((Short)values[0]),
                TypeUtils.unbox((Float)values[1])
        );
    }

    @Override
    public final ShortFloatTuple createTupleFromReinterpretedValues(@NotNull final Object... values) {
        return new ShortFloatTuple(
                TypeUtils.unbox((Short)values[0]),
                TypeUtils.unbox((Float)values[1])
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <ELEMENT_TYPE> void exportElement(@NotNull final ShortFloatTuple tuple, final int elementIndex, @NotNull final WritableSource<ELEMENT_TYPE> writableSource, final long destinationIndexKey) {
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
    public final Object exportToExternalKey(@NotNull final ShortFloatTuple tuple) {
        return new SmartKey(
                TypeUtils.box(tuple.getFirstElement()),
                TypeUtils.box(tuple.getSecondElement())
        );
    }

    @Override
    public final Object exportElement(@NotNull final ShortFloatTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return TypeUtils.box(tuple.getFirstElement());
        }
        if (elementIndex == 1) {
            return TypeUtils.box(tuple.getSecondElement());
        }
        throw new IllegalArgumentException("Bad elementIndex for 2 element tuple: " + elementIndex);
    }

    @Override
    public final Object exportElementReinterpreted(@NotNull final ShortFloatTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return TypeUtils.box(tuple.getFirstElement());
        }
        if (elementIndex == 1) {
            return TypeUtils.box(tuple.getSecondElement());
        }
        throw new IllegalArgumentException("Bad elementIndex for 2 element tuple: " + elementIndex);
    }

    protected void convertChunks(@NotNull WritableChunk<? super Attributes.Values> destination, int chunkSize, Chunk<Attributes.Values> [] chunks) {
        WritableObjectChunk<ShortFloatTuple, ? super Attributes.Values> destinationObjectChunk = destination.asWritableObjectChunk();
        ShortChunk<Attributes.Values> chunk1 = chunks[0].asShortChunk();
        FloatChunk<Attributes.Values> chunk2 = chunks[1].asFloatChunk();
        for (int ii = 0; ii < chunkSize; ++ii) {
            destinationObjectChunk.set(ii, new ShortFloatTuple(chunk1.get(ii), chunk2.get(ii)));
        }
        destination.setSize(chunkSize);
    }

    /** {@link TwoColumnTupleSourceFactory} for instances of {@link ShortFloatColumnTupleSource}. **/
    private static final class Factory implements TwoColumnTupleSourceFactory<ShortFloatTuple, Short, Float> {

        private Factory() {
        }

        @Override
        public TupleSource<ShortFloatTuple> create(
                @NotNull final ColumnSource<Short> columnSource1,
                @NotNull final ColumnSource<Float> columnSource2
        ) {
            return new ShortFloatColumnTupleSource(
                    columnSource1,
                    columnSource2
            );
        }
    }
}
