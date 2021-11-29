package io.deephaven.engine.table.impl.tuplesource.generated;

import io.deephaven.datastructures.util.SmartKey;
import io.deephaven.chunk.attributes.Values;
import io.deephaven.chunk.CharChunk;
import io.deephaven.chunk.Chunk;
import io.deephaven.chunk.ObjectChunk;
import io.deephaven.chunk.WritableChunk;
import io.deephaven.chunk.WritableObjectChunk;
import io.deephaven.engine.table.ColumnSource;
import io.deephaven.engine.table.TupleSource;
import io.deephaven.engine.table.WritableColumnSource;
import io.deephaven.engine.table.impl.tuplesource.AbstractTupleSource;
import io.deephaven.engine.table.impl.tuplesource.ThreeColumnTupleSourceFactory;
import io.deephaven.time.DateTime;
import io.deephaven.time.DateTimeUtils;
import io.deephaven.tuple.generated.CharCharLongTuple;
import io.deephaven.util.type.TypeUtils;
import org.jetbrains.annotations.NotNull;


/**
 * <p>{@link TupleSource} that produces key column values from {@link ColumnSource} types Character, Character, and DateTime.
 * <p>Generated by io.deephaven.replicators.TupleSourceCodeGenerator.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class CharacterCharacterDateTimeColumnTupleSource extends AbstractTupleSource<CharCharLongTuple> {

    /** {@link ThreeColumnTupleSourceFactory} instance to create instances of {@link CharacterCharacterDateTimeColumnTupleSource}. **/
    public static final ThreeColumnTupleSourceFactory<CharCharLongTuple, Character, Character, DateTime> FACTORY = new Factory();

    private final ColumnSource<Character> columnSource1;
    private final ColumnSource<Character> columnSource2;
    private final ColumnSource<DateTime> columnSource3;

    public CharacterCharacterDateTimeColumnTupleSource(
            @NotNull final ColumnSource<Character> columnSource1,
            @NotNull final ColumnSource<Character> columnSource2,
            @NotNull final ColumnSource<DateTime> columnSource3
    ) {
        super(columnSource1, columnSource2, columnSource3);
        this.columnSource1 = columnSource1;
        this.columnSource2 = columnSource2;
        this.columnSource3 = columnSource3;
    }

    @Override
    public final CharCharLongTuple createTuple(final long rowKey) {
        return new CharCharLongTuple(
                columnSource1.getChar(rowKey),
                columnSource2.getChar(rowKey),
                DateTimeUtils.nanos(columnSource3.get(rowKey))
        );
    }

    @Override
    public final CharCharLongTuple createPreviousTuple(final long rowKey) {
        return new CharCharLongTuple(
                columnSource1.getPrevChar(rowKey),
                columnSource2.getPrevChar(rowKey),
                DateTimeUtils.nanos(columnSource3.getPrev(rowKey))
        );
    }

    @Override
    public final CharCharLongTuple createTupleFromValues(@NotNull final Object... values) {
        return new CharCharLongTuple(
                TypeUtils.unbox((Character)values[0]),
                TypeUtils.unbox((Character)values[1]),
                DateTimeUtils.nanos((DateTime)values[2])
        );
    }

    @Override
    public final CharCharLongTuple createTupleFromReinterpretedValues(@NotNull final Object... values) {
        return new CharCharLongTuple(
                TypeUtils.unbox((Character)values[0]),
                TypeUtils.unbox((Character)values[1]),
                DateTimeUtils.nanos((DateTime)values[2])
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <ELEMENT_TYPE> void exportElement(@NotNull final CharCharLongTuple tuple, final int elementIndex, @NotNull final WritableColumnSource<ELEMENT_TYPE> writableSource, final long destinationRowKey) {
        if (elementIndex == 0) {
            writableSource.set(destinationRowKey, tuple.getFirstElement());
            return;
        }
        if (elementIndex == 1) {
            writableSource.set(destinationRowKey, tuple.getSecondElement());
            return;
        }
        if (elementIndex == 2) {
            writableSource.set(destinationRowKey, (ELEMENT_TYPE) DateTimeUtils.nanosToTime(tuple.getThirdElement()));
            return;
        }
        throw new IndexOutOfBoundsException("Invalid element index " + elementIndex + " for export");
    }

    @Override
    public final Object exportToExternalKey(@NotNull final CharCharLongTuple tuple) {
        return new SmartKey(
                TypeUtils.box(tuple.getFirstElement()),
                TypeUtils.box(tuple.getSecondElement()),
                DateTimeUtils.nanosToTime(tuple.getThirdElement())
        );
    }

    @Override
    public final Object exportElement(@NotNull final CharCharLongTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return TypeUtils.box(tuple.getFirstElement());
        }
        if (elementIndex == 1) {
            return TypeUtils.box(tuple.getSecondElement());
        }
        if (elementIndex == 2) {
            return DateTimeUtils.nanosToTime(tuple.getThirdElement());
        }
        throw new IllegalArgumentException("Bad elementIndex for 3 element tuple: " + elementIndex);
    }

    @Override
    public final Object exportElementReinterpreted(@NotNull final CharCharLongTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return TypeUtils.box(tuple.getFirstElement());
        }
        if (elementIndex == 1) {
            return TypeUtils.box(tuple.getSecondElement());
        }
        if (elementIndex == 2) {
            return DateTimeUtils.nanosToTime(tuple.getThirdElement());
        }
        throw new IllegalArgumentException("Bad elementIndex for 3 element tuple: " + elementIndex);
    }

    @Override
    protected void convertChunks(@NotNull WritableChunk<? super Values> destination, int chunkSize, Chunk<Values> [] chunks) {
        WritableObjectChunk<CharCharLongTuple, ? super Values> destinationObjectChunk = destination.asWritableObjectChunk();
        CharChunk<Values> chunk1 = chunks[0].asCharChunk();
        CharChunk<Values> chunk2 = chunks[1].asCharChunk();
        ObjectChunk<DateTime, Values> chunk3 = chunks[2].asObjectChunk();
        for (int ii = 0; ii < chunkSize; ++ii) {
            destinationObjectChunk.set(ii, new CharCharLongTuple(chunk1.get(ii), chunk2.get(ii), DateTimeUtils.nanos(chunk3.get(ii))));
        }
        destinationObjectChunk.setSize(chunkSize);
    }

    /** {@link ThreeColumnTupleSourceFactory} for instances of {@link CharacterCharacterDateTimeColumnTupleSource}. **/
    private static final class Factory implements ThreeColumnTupleSourceFactory<CharCharLongTuple, Character, Character, DateTime> {

        private Factory() {
        }

        @Override
        public TupleSource<CharCharLongTuple> create(
                @NotNull final ColumnSource<Character> columnSource1,
                @NotNull final ColumnSource<Character> columnSource2,
                @NotNull final ColumnSource<DateTime> columnSource3
        ) {
            return new CharacterCharacterDateTimeColumnTupleSource(
                    columnSource1,
                    columnSource2,
                    columnSource3
            );
        }
    }
}
