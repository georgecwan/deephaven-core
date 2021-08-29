package io.deephaven.engine.structures.chunk.page;

import io.deephaven.base.verify.Require;
import io.deephaven.engine.structures.chunk.*;
import io.deephaven.engine.structures.rowsequence.OrderedKeys;
import org.jetbrains.annotations.NotNull;

public class CharChunkPage<ATTR extends Attributes.Any> extends CharChunk<ATTR> implements ChunkPage<ATTR> {

    private final long mask;
    private final long firstRow;

    public static <ATTR extends Attributes.Any> CharChunkPage<ATTR> pageWrap(long beginRow, char[] data, int offset, int capacity, long mask) {
        return new CharChunkPage<>(beginRow, data, offset, capacity, mask);
    }

    public static <ATTR extends Attributes.Any> CharChunkPage<ATTR> pageWrap(long beginRow, char[] data, long mask) {
        return new CharChunkPage<>(beginRow, data, 0, data.length, mask);
    }

    private CharChunkPage(long firstRow, char[] data, int offset, int capacity, long mask) {
        super(data, offset, Require.lt(capacity, "capacity", Integer.MAX_VALUE, "INT_MAX"));
        this.mask = mask;
        this.firstRow = Require.inRange(firstRow, "firstRow", mask, "mask");
    }

    @Override
    public final void fillChunkAppend(@NotNull FillContext context, @NotNull WritableChunk<? super ATTR> destination, @NotNull OrderedKeys orderedKeys) {
        WritableCharChunk<? super ATTR> to = destination.asWritableCharChunk();

        if (orderedKeys.getAverageRunLengthEstimate() >= Chunk.SYSTEM_ARRAYCOPY_THRESHOLD) {
            orderedKeys.forAllLongRanges((final long rangeStartKey, final long rangeEndKey) ->
                    to.appendTypedChunk(this, getChunkOffset(rangeStartKey), (int) (rangeEndKey - rangeStartKey + 1)));
        } else {
            orderedKeys.forEachLong((final long key) -> {
                to.add(get(getChunkOffset(key)));
                return true;
            });
        }
    }

    @Override
    public final long firstRowOffset() {
        return firstRow;
    }

    @Override
    public final long mask() {
        return mask;
    }
}
