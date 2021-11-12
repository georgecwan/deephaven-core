/* ---------------------------------------------------------------------------------------------------------------------
 * AUTO-GENERATED CLASS - DO NOT EDIT MANUALLY - for any changes edit CharChunkSsaStamp and regenerate
 * ------------------------------------------------------------------------------------------------------------------ */
package io.deephaven.engine.v2.ssa;

import io.deephaven.engine.chunk.*;
import io.deephaven.engine.chunk.Attributes.RowKeys;
import io.deephaven.engine.chunk.Attributes.Values;
import io.deephaven.engine.rowset.RowSequence;
import io.deephaven.engine.v2.utils.RowRedirection;
import io.deephaven.engine.rowset.RowSetBuilderRandom;
import io.deephaven.engine.v2.utils.MutableRowRedirection;

/**
 * Stamp kernel for when the left hand side is a sorted chunk and the right hand side is a ticking SegmentedSortedArray.
 */
public class ShortReverseChunkSsaStamp implements ChunkSsaStamp {
    static ShortReverseChunkSsaStamp INSTANCE = new ShortReverseChunkSsaStamp();

    private ShortReverseChunkSsaStamp() {} // use the instance

    @Override
    public void processEntry(Chunk<Values> leftStampValues, Chunk<Attributes.RowKeys> leftStampKeys, SegmentedSortedArray ssa, WritableLongChunk<Attributes.RowKeys> rightKeysForLeft, boolean disallowExactMatch) {
        processEntry(leftStampValues.asShortChunk(), leftStampKeys, (ShortReverseSegmentedSortedArray)ssa, rightKeysForLeft, disallowExactMatch);
    }

    private static void processEntry(ShortChunk<Values> leftStampValues, Chunk<Attributes.RowKeys> leftStampKeys, ShortReverseSegmentedSortedArray ssa, WritableLongChunk<Attributes.RowKeys> rightKeysForLeft, boolean disallowExactMatch) {
        final int leftSize = leftStampKeys.size();
        final long rightSize = ssa.size();
        if (rightSize == 0) {
            rightKeysForLeft.fillWithValue(0, leftSize, RowSequence.NULL_ROW_KEY);
            rightKeysForLeft.setSize(leftSize);
            return;
        }

        final ShortReverseSegmentedSortedArray.Iterator ssaIt = ssa.iterator(disallowExactMatch, true);

        for (int li = 0; li < leftSize; ) {
            final short leftValue = leftStampValues.get(li);
            final int comparison = doComparison(leftValue, ssaIt.getValue());
            if (disallowExactMatch ? comparison <= 0 : comparison < 0) {
                rightKeysForLeft.set(li++, RowSequence.NULL_ROW_KEY);
                continue;
            }
            else if (comparison == 0) {
                rightKeysForLeft.set(li++, ssaIt.getKey());
                continue;
            }

            ssaIt.advanceToLast(leftValue);

            final long redirectionKey = ssaIt.getKey();
            if (!ssaIt.hasNext()) {
                rightKeysForLeft.fillWithValue(li, leftSize - li, redirectionKey);
                return;
            } else {
                rightKeysForLeft.set(li++, redirectionKey);
                final short nextRightValue = ssaIt.nextValue();
                while (li < leftSize && (disallowExactMatch ? leq(leftStampValues.get(li), nextRightValue) :  lt(leftStampValues.get(li), nextRightValue))) {
                    rightKeysForLeft.set(li++, redirectionKey);
                }
            }
        }
    }

    @Override
    public void processRemovals(Chunk<Values> leftStampValues, LongChunk<Attributes.RowKeys> leftStampKeys, Chunk<? extends Values> rightStampChunk, LongChunk<Attributes.RowKeys> rightKeys, WritableLongChunk<Attributes.RowKeys> priorRedirections, MutableRowRedirection rowRedirection, RowSetBuilderRandom modifiedBuilder, boolean disallowExactMatch) {
        processRemovals(leftStampValues.asShortChunk(), leftStampKeys, rightStampChunk.asShortChunk(), rightKeys, priorRedirections, rowRedirection, modifiedBuilder, disallowExactMatch);
    }

    private static void processRemovals(ShortChunk<Values> leftStampValues, LongChunk<Attributes.RowKeys> leftStampKeys, ShortChunk<? extends Values> rightStampChunk, LongChunk<RowKeys> rightKeys, WritableLongChunk<Attributes.RowKeys> nextRedirections, MutableRowRedirection rowRedirection, RowSetBuilderRandom modifiedBuilder, boolean disallowExactMatch) {
        // When removing a row, record the stamp, redirection key, and prior redirection key.  Binary search
        // in the left for the removed key to find the smallest value geq the removed right.  Update all rows
        // with the removed redirection to the previous key.

        int leftLowIdx = 0;

        for (int ii = 0; ii < rightStampChunk.size(); ++ii) {
            final short rightStampValue = rightStampChunk.get(ii);
            final long rightStampKey = rightKeys.get(ii);
            final long newRightStampKey = nextRedirections.get(ii);

            leftLowIdx = findFirstResponsiveLeft(leftLowIdx, leftStampValues, disallowExactMatch, rightStampValue);

            while (leftLowIdx < leftStampKeys.size()) {
                final long leftKey = leftStampKeys.get(leftLowIdx);
                final long leftRedirectionKey = rowRedirection.get(leftKey);
                if (leftRedirectionKey == rightStampKey) {
                    modifiedBuilder.addKey(leftKey);
                    if (newRightStampKey == RowSequence.NULL_ROW_KEY) {
                        rowRedirection.removeVoid(leftKey);
                    } else {
                        rowRedirection.putVoid(leftKey, newRightStampKey);
                    }
                    leftLowIdx++;
                } else {
                    break;
                }
            }
        }
    }

    @Override
    public void processInsertion(Chunk<Values> leftStampValues, LongChunk<Attributes.RowKeys> leftStampKeys, Chunk<? extends Values> rightStampChunk, LongChunk<Attributes.RowKeys> rightKeys, Chunk<Values> nextRightValue, MutableRowRedirection rowRedirection, RowSetBuilderRandom modifiedBuilder, boolean endsWithLastValue, boolean disallowExactMatch) {
        processInsertion(leftStampValues.asShortChunk(), leftStampKeys, rightStampChunk.asShortChunk(), rightKeys, nextRightValue.asShortChunk(), rowRedirection, modifiedBuilder, endsWithLastValue, disallowExactMatch);
    }

    private static void processInsertion(ShortChunk<Values> leftStampValues, LongChunk<Attributes.RowKeys> leftStampKeys, ShortChunk<? extends Values> rightStampChunk, LongChunk<Attributes.RowKeys> rightKeys, ShortChunk<Values> nextRightValue, MutableRowRedirection rowRedirection, RowSetBuilderRandom modifiedBuilder, boolean endsWithLastValue, boolean disallowExactMatch) {
        // We've already filtered out duplicate right stamps by the time we get here, which means that the rightStampChunk
        // contains only values that are the last in any given run; and thus are possible matches.

        // We binary search in the left for the first value >=, everything up until the next extant right value (contained
        // in the nextRightValue chunk) should be re-stamped with our value

        int leftLowIdx = 0;

        for (int ii = 0; ii < rightStampChunk.size(); ++ii) {
            final short rightStampValue = rightStampChunk.get(ii);

            leftLowIdx = findFirstResponsiveLeft(leftLowIdx, leftStampValues, disallowExactMatch, rightStampValue);

            final long rightStampKey = rightKeys.get(ii);

            if (ii == rightStampChunk.size() - 1 && endsWithLastValue) {
                while (leftLowIdx < leftStampKeys.size()) {
                    final long leftKey = leftStampKeys.get(leftLowIdx);
                    rowRedirection.putVoid(leftKey, rightStampKey);
                    modifiedBuilder.addKey(leftKey);
                    leftLowIdx++;
                }
            } else {
                final short nextRight = nextRightValue.get(ii);
                while (leftLowIdx < leftStampKeys.size()) {
                    final short leftValue = leftStampValues.get(leftLowIdx);
                    if (disallowExactMatch ? leq(leftValue, nextRight) : lt(leftValue, nextRight)) {
                        final long leftKey = leftStampKeys.get(leftLowIdx);
                        rowRedirection.putVoid(leftKey, rightStampKey);
                        modifiedBuilder.addKey(leftKey);
                        leftLowIdx++;
                    } else {
                        break;
                    }
                }
            }
        }
    }

    @Override
    public int findModified(int first, Chunk<Values> leftStampValues, LongChunk<Attributes.RowKeys> leftStampKeys, RowRedirection rowRedirection, Chunk<? extends Values> rightStampChunk, LongChunk<Attributes.RowKeys> rightStampIndices, RowSetBuilderRandom modifiedBuilder, boolean disallowExactMatch) {
        return findModified(first, leftStampValues.asShortChunk(), leftStampKeys, rowRedirection, rightStampChunk.asShortChunk(), rightStampIndices, modifiedBuilder, disallowExactMatch);
    }

    private static int findModified(int leftLowIdx, ShortChunk<Values> leftStampValues, LongChunk<Attributes.RowKeys> leftStampKeys, RowRedirection rowRedirection, ShortChunk<? extends Values> rightStampChunk, LongChunk<Attributes.RowKeys> rightStampIndices, RowSetBuilderRandom modifiedBuilder, boolean disallowExactMatch) {
        for (int ii = 0; ii < rightStampChunk.size(); ++ii) {
            final short rightStampValue = rightStampChunk.get(ii);

            // now find the lowest left value leq (lt) than rightStampValue
            leftLowIdx = findFirstResponsiveLeft(leftLowIdx, leftStampValues, disallowExactMatch, rightStampValue);

            final long rightStampKey = rightStampIndices.get(ii);
            int checkIdx = leftLowIdx;
            while (checkIdx < leftStampValues.size() && rowRedirection.get(leftStampKeys.get(checkIdx)) == rightStampKey) {
                modifiedBuilder.addKey(leftStampKeys.get(checkIdx));
                checkIdx++;
            }
        }

        return leftLowIdx;
    }

    @Override
    public void applyShift(Chunk<Values> leftStampValues, LongChunk<Attributes.RowKeys> leftStampKeys, Chunk<? extends Values> rightStampChunk, LongChunk<Attributes.RowKeys> rightStampKeys, long shiftDelta, MutableRowRedirection rowRedirection, boolean disallowExactMatch) {
        applyShift(leftStampValues.asShortChunk(), leftStampKeys, rightStampChunk.asShortChunk(), rightStampKeys, shiftDelta, rowRedirection, disallowExactMatch);
    }

    private void applyShift(ShortChunk<Values> leftStampValues, LongChunk<Attributes.RowKeys> leftStampKeys, ShortChunk<? extends Values> rightStampChunk, LongChunk<Attributes.RowKeys> rightStampKeys, long shiftDelta, MutableRowRedirection rowRedirection, boolean disallowExactMatch) {
        int leftLowIdx = 0;
        for (int ii = 0; ii < rightStampChunk.size(); ++ii) {
            final short rightStampValue = rightStampChunk.get(ii);

            // now find the lowest left value leq (lt) than rightStampValue
            leftLowIdx = findFirstResponsiveLeft(leftLowIdx, leftStampValues, disallowExactMatch, rightStampValue);

            final long rightStampKey = rightStampKeys.get(ii);
            int checkIdx = leftLowIdx;
            while (checkIdx < leftStampValues.size() && rowRedirection.get(leftStampKeys.get(checkIdx)) == rightStampKey) {
                rowRedirection.putVoid(leftStampKeys.get(checkIdx), rightStampKey + shiftDelta);
                checkIdx++;
            }
        }
    }

    private static int findFirstResponsiveLeft(int leftLowIdx, ShortChunk<Values> leftStampValues, boolean disallowExactMatch, short rightStampValue) {
        int leftHighIdx = leftStampValues.size();

        while (leftLowIdx < leftHighIdx) {
            final int leftMidIdx = (leftHighIdx + leftLowIdx) >>> 1;
            final short leftMidValue = leftStampValues.get(leftMidIdx);

            final int comparison = doComparison(leftMidValue, rightStampValue);
            final boolean moveLow = disallowExactMatch ? comparison <= 0 : comparison < 0;
            if (moveLow) {
                leftLowIdx = leftMidIdx + 1;
            } else {
                leftHighIdx = leftMidIdx;
            }
        }
        return leftLowIdx;
    }

    // region comparison functions
    // note that this is a descending kernel, thus the comparisons here are backwards (e.g., the lt function is in terms of the sort direction, so is implemented by gt)
    private static int doComparison(short lhs, short rhs) {
        return -1 * Short.compare(lhs, rhs);
    }
    // endregion comparison functions

    private static boolean lt(short lhs, short rhs) {
        return doComparison(lhs, rhs) < 0;
    }

    private static boolean leq(short lhs, short rhs) {
        return doComparison(lhs, rhs) <= 0;
    }
}

