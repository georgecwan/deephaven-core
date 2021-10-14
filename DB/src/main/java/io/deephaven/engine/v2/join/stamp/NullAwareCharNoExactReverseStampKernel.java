/* ---------------------------------------------------------------------------------------------------------------------
 * AUTO-GENERATED CLASS - DO NOT EDIT MANUALLY - for any changes edit CharNoExactStampKernel and regenerate
 * ------------------------------------------------------------------------------------------------------------------ */
package io.deephaven.engine.v2.join.stamp;

import io.deephaven.engine.util.DhCharComparisons;

import io.deephaven.engine.v2.sources.chunk.Attributes.KeyIndices;
import io.deephaven.engine.v2.sources.chunk.Attributes.Values;
import io.deephaven.engine.v2.sources.chunk.CharChunk;
import io.deephaven.engine.v2.sources.chunk.Chunk;
import io.deephaven.engine.v2.sources.chunk.LongChunk;
import io.deephaven.engine.v2.sources.chunk.WritableLongChunk;
import io.deephaven.engine.v2.utils.Index;


public class NullAwareCharNoExactReverseStampKernel implements StampKernel {
    static final NullAwareCharNoExactReverseStampKernel INSTANCE = new NullAwareCharNoExactReverseStampKernel();
    private NullAwareCharNoExactReverseStampKernel() {} // static use only

    @Override
    public void computeRedirections(Chunk<Values> leftStamps, Chunk<Values> rightStamps, LongChunk<KeyIndices> rightKeyIndices, WritableLongChunk<KeyIndices> leftRedirections) {
        computeRedirections(leftStamps.asCharChunk(), rightStamps.asCharChunk(), rightKeyIndices, leftRedirections);
    }

    static private void computeRedirections(CharChunk<Values> leftStamps, CharChunk<Values> rightStamps, LongChunk<KeyIndices> rightKeyIndices, WritableLongChunk<KeyIndices> leftRedirections) {
        final int leftSize = leftStamps.size();
        final int rightSize = rightStamps.size();
        if (rightSize == 0) {
            leftRedirections.fillWithValue(0, leftSize, Index.NULL_KEY);
            leftRedirections.setSize(leftSize);
            return;
        }

        int rightLowIdx = 0;
        char rightLowValue = rightStamps.get(0);

        final int maxRightIdx = rightSize - 1;

        for (int li = 0; li < leftSize; ) {
            final char leftValue = leftStamps.get(li);
            if (leq(leftValue, rightLowValue)) {
                leftRedirections.set(li++, Index.NULL_KEY);
                continue;
            }

            int rightHighIdx = rightSize;

            while (rightLowIdx < rightHighIdx) {
                final int rightMidIdx = ((rightHighIdx - rightLowIdx) / 2) + rightLowIdx;
                final char rightMidValue = rightStamps.get(rightMidIdx);
                if (lt(rightMidValue, leftValue)) {
                    rightLowIdx = rightMidIdx;
                    rightLowValue = rightMidValue;
                    if (rightLowIdx == rightHighIdx - 1) {
                        break;
                    }
                } else {
                    rightHighIdx = rightMidIdx;
                }
            }

            final long redirectionKey = rightKeyIndices.get(rightLowIdx);
            if (rightLowIdx == maxRightIdx) {
                leftRedirections.fillWithValue(li, leftSize - li, redirectionKey);
                return;
            } else {
                leftRedirections.set(li++, redirectionKey);
                final char nextRightValue = rightStamps.get(rightLowIdx + 1);
                while (li < leftSize && lt(leftStamps.get(li), nextRightValue)) {
                    leftRedirections.set(li++, redirectionKey);
                }
            }
        }
    }
    // region comparison functions
    // note that this is a descending kernel, thus the comparisons here are backwards (e.g., the lt function is in terms of the sort direction, so is implemented by gt)
    private static int doComparison(char lhs, char rhs) {
        return -1 * DhCharComparisons.compare(lhs, rhs);
    }
    // endregion comparison functions

    private static boolean lt(char lhs, char rhs) {
        return doComparison(lhs, rhs) < 0;
    }

    private static boolean leq(char lhs, char rhs) {
        return doComparison(lhs, rhs) <= 0;
    }

    private static boolean eq(char lhs, char rhs) {
        // region equality function
        return lhs == rhs;
        // endregion equality function
    }
}