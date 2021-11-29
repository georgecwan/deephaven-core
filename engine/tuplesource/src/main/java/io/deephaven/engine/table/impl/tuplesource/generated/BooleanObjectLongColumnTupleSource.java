package io.deephaven.engine.table.impl.tuplesource.generated;

import io.deephaven.datastructures.util.SmartKey;
import io.deephaven.chunk.attributes.Values;
import io.deephaven.chunk.Chunk;
import io.deephaven.chunk.LongChunk;
import io.deephaven.chunk.ObjectChunk;
import io.deephaven.chunk.WritableChunk;
import io.deephaven.chunk.WritableObjectChunk;
import io.deephaven.engine.table.ColumnSource;
import io.deephaven.engine.table.TupleSource;
import io.deephaven.engine.table.WritableColumnSource;
import io.deephaven.engine.table.impl.tuplesource.AbstractTupleSource;
import io.deephaven.engine.table.impl.tuplesource.ThreeColumnTupleSourceFactory;
import io.deephaven.tuple.generated.ByteObjectLongTuple;
import io.deephaven.util.BooleanUtils;
import io.deephaven.util.type.TypeUtils;
import org.jetbrains.annotations.NotNull;


/**
 * <p>{@link TupleSource} that produces key column values from {@link ColumnSource} types Boolean, Object, and Long.
 * <p>Generated by io.deephaven.replicators.TupleSourceCodeGenerator.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class BooleanObjectLongColumnTupleSource extends AbstractTupleSource<ByteObjectLongTuple> {

    /** {@link ThreeColumnTupleSourceFactory} instance to create instances of {@link BooleanObjectLongColumnTupleSource}. **/
    public static final ThreeColumnTupleSourceFactory<ByteObjectLongTuple, Boolean, Object, Long> FACTORY = new Factory();

    private final ColumnSource<Boolean> columnSource1;
    private final ColumnSource<Object> columnSource2;
    private final ColumnSource<Long> columnSource3;

    public BooleanObjectLongColumnTupleSource(
            @NotNull final ColumnSource<Boolean> columnSource1,
            @NotNull final ColumnSource<Object> columnSource2,
            @NotNull final ColumnSource<Long> columnSource3
    ) {
        super(columnSource1, columnSource2, columnSource3);
        this.columnSource1 = columnSource1;
        this.columnSource2 = columnSource2;
        this.columnSource3 = columnSource3;
    }

    @Override
    public final ByteObjectLongTuple createTuple(final long rowKey) {
        return new ByteObjectLongTuple(
                BooleanUtils.booleanAsByte(columnSource1.getBoolean(rowKey)),
                columnSource2.get(rowKey),
                columnSource3.getLong(rowKey)
        );
    }

    @Override
    public final ByteObjectLongTuple createPreviousTuple(final long rowKey) {
        return new ByteObjectLongTuple(
                BooleanUtils.booleanAsByte(columnSource1.getPrevBoolean(rowKey)),
                columnSource2.getPrev(rowKey),
                columnSource3.getPrevLong(rowKey)
        );
    }

    @Override
    public final ByteObjectLongTuple createTupleFromValues(@NotNull final Object... values) {
        return new ByteObjectLongTuple(
                BooleanUtils.booleanAsByte((Boolean)values[0]),
                values[1],
                TypeUtils.unbox((Long)values[2])
        );
    }

    @Override
    public final ByteObjectLongTuple createTupleFromReinterpretedValues(@NotNull final Object... values) {
        return new ByteObjectLongTuple(
                BooleanUtils.booleanAsByte((Boolean)values[0]),
                values[1],
                TypeUtils.unbox((Long)values[2])
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <ELEMENT_TYPE> void exportElement(@NotNull final ByteObjectLongTuple tuple, final int elementIndex, @NotNull final WritableColumnSource<ELEMENT_TYPE> writableSource, final long destinationRowKey) {
        if (elementIndex == 0) {
            writableSource.set(destinationRowKey, (ELEMENT_TYPE) BooleanUtils.byteAsBoolean(tuple.getFirstElement()));
            return;
        }
        if (elementIndex == 1) {
            writableSource.set(destinationRowKey, (ELEMENT_TYPE) tuple.getSecondElement());
            return;
        }
        if (elementIndex == 2) {
            writableSource.set(destinationRowKey, tuple.getThirdElement());
            return;
        }
        throw new IndexOutOfBoundsException("Invalid element index " + elementIndex + " for export");
    }

    @Override
    public final Object exportToExternalKey(@NotNull final ByteObjectLongTuple tuple) {
        return new SmartKey(
                BooleanUtils.byteAsBoolean(tuple.getFirstElement()),
                tuple.getSecondElement(),
                TypeUtils.box(tuple.getThirdElement())
        );
    }

    @Override
    public final Object exportElement(@NotNull final ByteObjectLongTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return BooleanUtils.byteAsBoolean(tuple.getFirstElement());
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
    public final Object exportElementReinterpreted(@NotNull final ByteObjectLongTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return BooleanUtils.byteAsBoolean(tuple.getFirstElement());
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
    protected void convertChunks(@NotNull WritableChunk<? super Values> destination, int chunkSize, Chunk<Values> [] chunks) {
        WritableObjectChunk<ByteObjectLongTuple, ? super Values> destinationObjectChunk = destination.asWritableObjectChunk();
        ObjectChunk<Boolean, Values> chunk1 = chunks[0].asObjectChunk();
        ObjectChunk<Object, Values> chunk2 = chunks[1].asObjectChunk();
        LongChunk<Values> chunk3 = chunks[2].asLongChunk();
        for (int ii = 0; ii < chunkSize; ++ii) {
            destinationObjectChunk.set(ii, new ByteObjectLongTuple(BooleanUtils.booleanAsByte(chunk1.get(ii)), chunk2.get(ii), chunk3.get(ii)));
        }
        destinationObjectChunk.setSize(chunkSize);
    }

    /** {@link ThreeColumnTupleSourceFactory} for instances of {@link BooleanObjectLongColumnTupleSource}. **/
    private static final class Factory implements ThreeColumnTupleSourceFactory<ByteObjectLongTuple, Boolean, Object, Long> {

        private Factory() {
        }

        @Override
        public TupleSource<ByteObjectLongTuple> create(
                @NotNull final ColumnSource<Boolean> columnSource1,
                @NotNull final ColumnSource<Object> columnSource2,
                @NotNull final ColumnSource<Long> columnSource3
        ) {
            return new BooleanObjectLongColumnTupleSource(
                    columnSource1,
                    columnSource2,
                    columnSource3
            );
        }
    }
}
