/* ---------------------------------------------------------------------------------------------------------------------
 * AUTO-GENERATED CLASS - DO NOT EDIT MANUALLY - for any changes edit CharSsaSsaStamp and regenerate
 * ------------------------------------------------------------------------------------------------------------------ */
package io.deephaven.engine.v2.ssa;

import io.deephaven.engine.util.DhDoubleComparisons;

import io.deephaven.engine.v2.sources.chunk.*;
import io.deephaven.engine.v2.sources.chunk.Attributes.KeyIndices;
import io.deephaven.engine.v2.sources.chunk.Attributes.Values;
import io.deephaven.engine.v2.sources.chunk.sized.SizedLongChunk;
import io.deephaven.engine.v2.utils.Index;
import io.deephaven.engine.v2.utils.RedirectionIndex;

/**
 * Stamp kernel for when the left hand side is a sorted chunk and the right hand side is a ticking SegmentedSortedArray.
 */
public class DoubleSsaSsaStamp implements SsaSsaStamp {
    static DoubleSsaSsaStamp INSTANCE = new DoubleSsaSsaStamp();

    private DoubleSsaSsaStamp() {} // use the instance

    @Override
    public void processEntry(SegmentedSortedArray leftSsa, SegmentedSortedArray rightSsa, RedirectionIndex redirectionIndex, boolean disallowExactMatch) {
        processEntry((DoubleSegmentedSortedArray)leftSsa, (DoubleSegmentedSortedArray)rightSsa, redirectionIndex, disallowExactMatch);
    }

    private static void processEntry(DoubleSegmentedSortedArray leftSsa, DoubleSegmentedSortedArray rightSsa, RedirectionIndex redirectionIndex, boolean disallowExactMatch) {
        final long rightSize = rightSsa.size();
        if (rightSize == 0) {
            fillWithNull(redirectionIndex, leftSsa.iterator(disallowExactMatch, false));
            return;
        }

        final DoubleSegmentedSortedArray.Iterator rightIt = rightSsa.iterator(disallowExactMatch, true);
        final DoubleSegmentedSortedArray.Iterator leftIt = leftSsa.iterator(disallowExactMatch, false);

        while (leftIt.hasNext()) {
            leftIt.next();
            final double leftValue = leftIt.getValue();
            final int comparison = doComparison(leftValue, rightIt.getValue());
            if (disallowExactMatch ? comparison <= 0 : comparison < 0) {
                redirectionIndex.removeVoid(leftIt.getKey());
                continue;
            }
            else if (comparison == 0) {
                redirectionIndex.putVoid(leftIt.getKey(), rightIt.getKey());
                continue;
            }

            rightIt.advanceToLast(leftValue);

            final long redirectionKey = rightIt.getKey();
            if (!rightIt.hasNext()) {
                redirectionIndex.put(leftIt.getKey(), redirectionKey);
                fillWithValue(redirectionIndex, leftIt, redirectionKey);
                return;
            } else {
                redirectionIndex.putVoid(leftIt.getKey(), redirectionKey);
                final double nextRightValue = rightIt.nextValue();
                while (leftIt.hasNext() && (disallowExactMatch ? leq(leftIt.nextValue(), nextRightValue) :  lt(leftIt.nextValue(), nextRightValue))) {
                    leftIt.next();
                    redirectionIndex.put(leftIt.getKey(), redirectionKey);
                }
            }
        }
    }

    private static void fillWithNull(RedirectionIndex redirectionIndex, DoubleSegmentedSortedArray.Iterator leftIt) {
        while (leftIt.hasNext()) {
            leftIt.next();
            redirectionIndex.removeVoid(leftIt.getKey());
        }
    }

    private static void fillWithValue(RedirectionIndex redirectionIndex, DoubleSegmentedSortedArray.Iterator leftIt, long rightKey) {
        while (leftIt.hasNext()) {
            leftIt.next();
            redirectionIndex.putVoid(leftIt.getKey(), rightKey);
        }
    }

    @Override
    public void processRemovals(SegmentedSortedArray leftSsa, Chunk<? extends Values> rightStampChunk, LongChunk<KeyIndices> rightKeys, WritableLongChunk<KeyIndices> priorRedirections, RedirectionIndex redirectionIndex, Index.RandomBuilder modifiedBuilder, boolean disallowExactMatch) {
        processRemovals((DoubleSegmentedSortedArray)leftSsa, rightStampChunk.asDoubleChunk(), rightKeys, priorRedirections, redirectionIndex, modifiedBuilder, disallowExactMatch);
    }

    static private void processRemovals(DoubleSegmentedSortedArray leftSsa, DoubleChunk<? extends Values> rightStampChunk, LongChunk<KeyIndices> rightKeys, WritableLongChunk<KeyIndices> nextRedirections, RedirectionIndex redirectionIndex, Index.RandomBuilder modifiedBuilder, boolean disallowExactMatch) {
        // When removing a row, record the stamp, redirection key, and prior redirection key.  Binary search
        // in the left for the removed key to find the smallest value geq the removed right.  Update all rows
        // with the removed redirection to the previous key.

        final DoubleSegmentedSortedArray.Iterator leftIt = leftSsa.iterator(disallowExactMatch, false);

        try (final SizedLongChunk<KeyIndices> modifiedKeys = new SizedLongChunk<>()) {
            int capacity = rightStampChunk.size();
            modifiedKeys.ensureCapacity(capacity).setSize(capacity);
            int mks = 0;

            for (int ii = 0; ii < rightStampChunk.size(); ++ii) {
                final double rightStampValue = rightStampChunk.get(ii);
                final long rightStampKey = rightKeys.get(ii);
                final long newRightStampKey = nextRedirections.get(ii);

                leftIt.advanceToBeforeFirst(rightStampValue);

                while (leftIt.hasNext()) {
                    final long leftKey = leftIt.nextKey();
                    final long leftRedirectionKey = redirectionIndex.get(leftKey);
                    if (leftRedirectionKey == rightStampKey) {
                        if (mks == capacity) {
                            capacity *= 2;
                            modifiedKeys.ensureCapacityPreserve(capacity).setSize(capacity);
                        }
                        modifiedKeys.get().set(mks++, leftKey);
                        if (newRightStampKey == Index.NULL_KEY) {
                            redirectionIndex.removeVoid(leftKey);
                        } else {
                            redirectionIndex.putVoid(leftKey, newRightStampKey);
                        }
                        leftIt.next();
                    } else {
                        break;
                    }
                }
            }

            if (mks > 0) {
                modifiedKeys.get().setSize(mks);
                modifiedKeys.get().sort();
                modifiedBuilder.addOrderedKeyIndicesChunk(WritableLongChunk.downcast(modifiedKeys.get()));
            }
        }
    }

    @Override
    public void processInsertion(SegmentedSortedArray leftSsa, Chunk<? extends Values> rightStampChunk, LongChunk<KeyIndices> rightKeys, Chunk<Values> nextRightValue, RedirectionIndex redirectionIndex, Index.RandomBuilder modifiedBuilder, boolean endsWithLastValue, boolean disallowExactMatch) {
        processInsertion((DoubleSegmentedSortedArray)leftSsa, rightStampChunk.asDoubleChunk(), rightKeys, nextRightValue.asDoubleChunk(), redirectionIndex, modifiedBuilder, endsWithLastValue, disallowExactMatch);
    }

    static private void processInsertion(DoubleSegmentedSortedArray leftSsa, DoubleChunk<? extends Values> rightStampChunk, LongChunk<KeyIndices> rightKeys, DoubleChunk<Values> nextRightValue, RedirectionIndex redirectionIndex, Index.RandomBuilder modifiedBuilder, boolean endsWithLastValue, boolean disallowExactMatch) {
        // We've already filtered out duplicate right stamps by the time we get here, which means that the rightStampChunk
        // contains only values that are the last in any given run; and thus are possible matches.

        // We binary search in the left for the first value >=, everything up until the next extant right value (contained
        // in the nextRightValue chunk) should be re-stamped with our value

        final DoubleSegmentedSortedArray.Iterator leftIt = leftSsa.iterator(disallowExactMatch, false);

        try (final SizedLongChunk<KeyIndices> modifiedKeys = new SizedLongChunk<>()) {
            int capacity = rightStampChunk.size();
            modifiedKeys.ensureCapacity(capacity).setSize(capacity);
            int mks = 0;

            for (int ii = 0; ii < rightStampChunk.size(); ++ii) {
                final double rightStampValue = rightStampChunk.get(ii);

                leftIt.advanceToBeforeFirst(rightStampValue);

                final long rightStampKey = rightKeys.get(ii);

                if (ii == rightStampChunk.size() - 1 && endsWithLastValue) {
                    while (leftIt.hasNext()) {
                        leftIt.next();
                        final long leftKey = leftIt.getKey();
                        redirectionIndex.putVoid(leftKey, rightStampKey);
                        if (mks == capacity) {
                            capacity *= 2;
                            modifiedKeys.ensureCapacityPreserve(capacity).setSize(capacity);
                        }
                        modifiedKeys.get().set(mks++, leftKey);
                    }
                } else {
                    final double nextRight = nextRightValue.get(ii);
                    while (leftIt.hasNext()) {
                        final double leftValue = leftIt.nextValue();
                        if (disallowExactMatch ? leq(leftValue, nextRight) : lt(leftValue, nextRight)) {
                            final long leftKey = leftIt.nextKey();
                            redirectionIndex.putVoid(leftKey, rightStampKey);
                            if (mks == capacity) {
                                capacity *= 2;
                                modifiedKeys.ensureCapacityPreserve(capacity).setSize(capacity);
                            }
                            modifiedKeys.get().set(mks++, leftKey);
                            leftIt.next();
                        } else {
                            break;
                        }
                    }
                }
            }
            if (mks > 0) {
                modifiedKeys.get().setSize(mks);
                modifiedKeys.get().sort();
                modifiedBuilder.addOrderedKeyIndicesChunk(WritableLongChunk.downcast(modifiedKeys.get()));
            }
        }
    }

    @Override
    public void findModified(SegmentedSortedArray leftSsa, RedirectionIndex redirectionIndex, Chunk<? extends Values> rightStampChunk, LongChunk<KeyIndices> rightStampIndices, Index.RandomBuilder modifiedBuilder, boolean disallowExactMatch) {
        findModified((DoubleSegmentedSortedArray)leftSsa, redirectionIndex, rightStampChunk.asDoubleChunk(), rightStampIndices, modifiedBuilder, disallowExactMatch);
    }

    private static void findModified(DoubleSegmentedSortedArray leftSsa, RedirectionIndex redirectionIndex, DoubleChunk<? extends Values> rightStampChunk, LongChunk<KeyIndices> rightStampIndices, Index.RandomBuilder modifiedBuilder, boolean disallowExactMatch) {
        final DoubleSegmentedSortedArray.Iterator leftIt = leftSsa.iterator(disallowExactMatch, false);

        try (final SizedLongChunk<KeyIndices> modifiedKeys = new SizedLongChunk<>()) {
            int capacity = rightStampChunk.size();
            modifiedKeys.ensureCapacity(capacity).setSize(capacity);
            int mks = 0;

            for (int ii = 0; ii < rightStampChunk.size(); ++ii) {
                final double rightStampValue = rightStampChunk.get(ii);

                // now find the lowest left value leq (lt) than rightStampValue
                leftIt.advanceToBeforeFirst(rightStampValue);

                final long rightStampKey = rightStampIndices.get(ii);
                while (leftIt.hasNext() && redirectionIndex.get(leftIt.nextKey()) == rightStampKey) {
                    leftIt.next();

                    if (mks == capacity) {
                        capacity *= 2;
                        modifiedKeys.ensureCapacityPreserve(capacity).setSize(capacity);
                    }
                    modifiedKeys.get().set(mks++, leftIt.getKey());
                }
            }

            if (mks > 0) {
                modifiedKeys.get().setSize(mks);
                modifiedKeys.get().sort();
                modifiedBuilder.addOrderedKeyIndicesChunk(WritableLongChunk.downcast(modifiedKeys.get()));
            }
        }
    }

    @Override
    public void applyShift(SegmentedSortedArray leftSsa, Chunk<? extends Values> rightStampChunk, LongChunk<KeyIndices> rightStampKeys, long shiftDelta, RedirectionIndex redirectionIndex, boolean disallowExactMatch) {
        applyShift((DoubleSegmentedSortedArray)leftSsa, rightStampChunk.asDoubleChunk(), rightStampKeys, shiftDelta, redirectionIndex, disallowExactMatch);
    }

    private void applyShift(DoubleSegmentedSortedArray leftSsa, DoubleChunk<? extends Values> rightStampChunk, LongChunk<KeyIndices> rightStampKeys, long shiftDelta, RedirectionIndex redirectionIndex, boolean disallowExactMatch) {
        final DoubleSegmentedSortedArray.Iterator leftIt = leftSsa.iterator(disallowExactMatch, false);

        for (int ii = 0; ii < rightStampChunk.size(); ++ii) {
            final double rightStampValue = rightStampChunk.get(ii);

            leftIt.advanceToBeforeFirst(rightStampValue);

            final long rightStampKey = rightStampKeys.get(ii);
            while (leftIt.hasNext() && redirectionIndex.get(leftIt.nextKey()) == rightStampKey) {
                leftIt.next();
                redirectionIndex.putVoid(leftIt.getKey(), rightStampKey + shiftDelta);
            }
        }
    }

    // region comparison functions
    private static int doComparison(double lhs, double rhs) {
        return DhDoubleComparisons.compare(lhs, rhs);
    }
    // endregion comparison functions

    private static boolean lt(double lhs, double rhs) {
        return doComparison(lhs, rhs) < 0;
    }

    private static boolean leq(double lhs, double rhs) {
        return doComparison(lhs, rhs) <= 0;
    }
}
