/* ---------------------------------------------------------------------------------------------------------------------
 * AUTO-GENERATED CLASS - DO NOT EDIT MANUALLY - for any changes edit TestCharCopyKernel and regenerate
 * ------------------------------------------------------------------------------------------------------------------ */
package io.deephaven.engine.v2.utils.copy;

import io.deephaven.engine.v2.sources.chunk.Attributes.Values;
import io.deephaven.engine.v2.sources.chunk.WritableBooleanChunk;
import org.junit.Test;

public class TestBooleanCopyKernel {
    /**
     * There was an edge case (IDS-5504) leading to an array out of bounds exception caused by an inconsistency between
     * closed and half-open intervals.
     */
    @Test
    public void confirmEdgecaseFixed() {
        final WritableBooleanChunk<Values> output = WritableBooleanChunk.makeWritableChunk(64);
        final boolean[] baseInput = new boolean[64];
        final boolean[] overInput = new boolean[64];
        final long[] useOverInput = new long[1];
        useOverInput[0] = 1L << 63;  // This is the edge case
        BooleanCopyKernel.conditionalCopy(output, baseInput, overInput, useOverInput, 0, 0, 64);
    }
}