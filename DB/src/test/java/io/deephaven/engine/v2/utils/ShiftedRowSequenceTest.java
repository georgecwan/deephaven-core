package io.deephaven.engine.v2.utils;

import io.deephaven.engine.structures.RowSequence;
import io.deephaven.engine.structures.rowsequence.RowSequenceUtil;
import io.deephaven.engine.v2.sources.chunk.LongChunk;
import org.junit.Test;

public class ShiftedRowSequenceTest extends RowSequenceTestBase {

    private final long SHIFT = 1 << 20;

    @Override
    protected RowSequence create(long... values) {
        final long[] shifted = new long[values.length];
        for (int i = 0; i < values.length; ++i) {
            shifted[i] = values[i] + SHIFT;
        }
        final RowSequence other = RowSequenceUtil.wrapRowKeysChunkAsRowSequence(LongChunk.chunkWrap(shifted));
        return ShiftedRowSequence.wrap(closeOnTearDownCase(other), -SHIFT);
    }

    @Test
    @Override
    // The original test uses some large keys that overflow when shifted.
    public void testCanConstructRowSequence() {
        final long[] indices = indicesFromRanges(0, 4, Long.MAX_VALUE - 4 - SHIFT, Long.MAX_VALUE - SHIFT);
        try (final RowSequence OK = create(indices)) {
            assertContentsByIndices(indices, OK);
        }
    }
}