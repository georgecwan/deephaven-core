package io.deephaven.engine.v2;

import io.deephaven.base.verify.Assert;
import io.deephaven.configuration.Configuration;
import io.deephaven.datastructures.util.CollectionUtil;
import io.deephaven.engine.tables.Table;
import io.deephaven.engine.tables.select.MatchPair;
import io.deephaven.engine.v2.join.JoinListenerRecorder;
import io.deephaven.engine.v2.sources.BitMaskingColumnSource;
import io.deephaven.engine.v2.sources.BitShiftingColumnSource;
import io.deephaven.engine.v2.sources.ColumnSource;
import io.deephaven.engine.v2.sources.CrossJoinRightColumnSource;
import io.deephaven.engine.v2.utils.*;
import io.deephaven.engine.structures.RowSequence;
import io.deephaven.util.SafeCloseableList;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableLong;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.LongConsumer;

/**
 * Implementation for chunk-oriented aggregation operations, including {@link Table#join}.
 */
public class CrossJoinHelper {
    // Note: This would be >= 16 to get efficient performance from TrackingMutableRowSet#insert and TrackingMutableRowSet#shiftInPlace. However, it is
    // very costly for joins of many small groups for the default to be so high.
    public static final int DEFAULT_NUM_RIGHT_BITS_TO_RESERVE = Configuration.getInstance()
            .getIntegerForClassWithDefault(CrossJoinHelper.class, "numRightBitsToReserve", 10);

    /**
     * Static-use only.
     */
    private CrossJoinHelper() {}

    static Table join(final QueryTable leftTable, final QueryTable rightTable, final MatchPair[] columnsToMatch,
            final MatchPair[] columnsToAdd, final int numReserveRightBits) {
        return join(leftTable, rightTable, columnsToMatch, columnsToAdd, numReserveRightBits, new JoinControl());
    }

    static Table join(final QueryTable leftTable, final QueryTable rightTable, final MatchPair[] columnsToMatch,
            final MatchPair[] columnsToAdd, final int numReserveRightBits, final JoinControl control) {
        final Table result =
                internalJoin(leftTable, rightTable, columnsToMatch, columnsToAdd, numReserveRightBits, control);
        leftTable.maybeCopyColumnDescriptions(result, rightTable, columnsToMatch, columnsToAdd);
        return result;
    }

    private static Table internalJoin(final QueryTable leftTable, final QueryTable rightTable,
            final MatchPair[] columnsToMatch, final MatchPair[] columnsToAdd, int numRightBitsToReserve,
            final JoinControl control) {
        QueryTable.checkInitiateOperation(leftTable);
        QueryTable.checkInitiateOperation(rightTable);

        try (final BucketingContext bucketingContext =
                new BucketingContext("join", leftTable, rightTable, columnsToMatch, columnsToAdd, control)) {
            // TODO: if we have a single column of unique values, and the range is small, we can use a simplified table
            // if (!rightTable.isLive() && control.useUniqueTable(uniqueValues, maximumUniqueValue, minumumUniqueValue))
            // { (etc)
            if (bucketingContext.keyColumnCount == 0) {
                if (!leftTable.isLive() && !rightTable.isLive()) {
                    numRightBitsToReserve = 1; // tight computation of this is efficient and appropriate
                }
                return zeroKeyColumnsJoin(leftTable, rightTable, columnsToAdd, numRightBitsToReserve,
                        bucketingContext.listenerDescription);
            }

            final ModifiedColumnSet rightKeyColumns =
                    rightTable.newModifiedColumnSet(MatchPair.getRightColumns(columnsToMatch));
            final ModifiedColumnSet leftKeyColumns =
                    leftTable.newModifiedColumnSet(MatchPair.getLeftColumns(columnsToMatch));

            if (!rightTable.isLive()) {
                // TODO: use grouping
                if (!leftTable.isLive()) {
                    final StaticChunkedCrossJoinStateManager jsm = new StaticChunkedCrossJoinStateManager(
                            bucketingContext.leftSources, control.initialBuildSize(), control, leftTable);
                    jsm.setMaximumLoadFactor(control.getMaximumLoadFactor());
                    jsm.setTargetLoadFactor(control.getTargetLoadFactor());

                    // We can only build from right, because the left hand side does not permit us to nicely rehash as
                    // we only have the redirection rowSet when building left and no way to reverse the lookup.
                    final TrackingMutableRowSet resultRowSet = jsm.buildFromRight(leftTable, bucketingContext.leftSources, rightTable,
                            bucketingContext.rightSources);

                    return makeResult(leftTable, rightTable, columnsToAdd, jsm, resultRowSet,
                            cs -> new CrossJoinRightColumnSource<>(jsm, cs, rightTable.isLive()));
                }

                final LeftOnlyIncrementalChunkedCrossJoinStateManager jsm =
                        new LeftOnlyIncrementalChunkedCrossJoinStateManager(
                                bucketingContext.leftSources, control.initialBuildSize(), leftTable,
                                numRightBitsToReserve);
                jsm.setMaximumLoadFactor(control.getMaximumLoadFactor());
                jsm.setTargetLoadFactor(control.getTargetLoadFactor());

                final TrackingMutableRowSet resultRowSet = jsm.buildLeftTicking(leftTable, rightTable, bucketingContext.rightSources);
                final QueryTable resultTable = makeResult(leftTable, rightTable, columnsToAdd, jsm, resultRowSet,
                        cs -> new CrossJoinRightColumnSource<>(jsm, cs, rightTable.isLive()));

                jsm.startTrackingPrevValues();
                final ModifiedColumnSet.Transformer leftTransformer = leftTable.newModifiedColumnSetTransformer(
                        resultTable,
                        leftTable.getColumnSourceMap().keySet().toArray(CollectionUtil.ZERO_LENGTH_STRING_ARRAY));

                leftTable.listenForUpdates(new BaseTable.ShiftAwareListenerImpl(bucketingContext.listenerDescription,
                        leftTable, resultTable) {
                    @Override
                    public void onUpdate(final Update upstream) {
                        jsm.validateKeySpaceSize();

                        final Update downstream = new Update();
                        downstream.added = TrackingMutableRowSet.FACTORY.getEmptyRowSet();

                        final RowSetBuilder rmBuilder = TrackingMutableRowSet.FACTORY.getRandomBuilder();
                        jsm.removeLeft(upstream.removed, (stateSlot, leftKey) -> {
                            final TrackingMutableRowSet rightRowSet = jsm.getRightIndex(stateSlot);
                            if (rightRowSet.nonempty()) {
                                final long prevLeftOffset = leftKey << jsm.getPrevNumShiftBits();
                                final long lastRightIndex = prevLeftOffset + rightRowSet.size() - 1;
                                rmBuilder.addRange(prevLeftOffset, lastRightIndex);
                            }
                        });
                        downstream.removed = rmBuilder.build();
                        resultRowSet.remove(downstream.removed);

                        try (final TrackingMutableRowSet prevLeftRowSet = leftTable.getIndex().getPrevIndex()) {
                            prevLeftRowSet.remove(upstream.removed);
                            jsm.applyLeftShift(prevLeftRowSet, upstream.shifted);
                            downstream.shifted = expandLeftOnlyShift(prevLeftRowSet, upstream.shifted, jsm);
                            downstream.shifted.apply(resultRowSet);
                        }

                        if (upstream.modifiedColumnSet.containsAny(leftKeyColumns)) {
                            // the jsm helper sets downstream.modified and appends to
                            // downstream.added/downstream.removed
                            jsm.processLeftModifies(upstream, downstream, resultRowSet);
                            if (downstream.modified.empty()) {
                                downstream.modifiedColumnSet = ModifiedColumnSet.EMPTY;
                            } else {
                                downstream.modifiedColumnSet = resultTable.modifiedColumnSet;
                                leftTransformer.transform(upstream.modifiedColumnSet, resultTable.modifiedColumnSet);
                            }
                        } else if (upstream.modified.nonempty()) {
                            final SequentialRowSetBuilder modBuilder = TrackingMutableRowSet.FACTORY.getSequentialBuilder();
                            upstream.modified.forAllLongs(ll -> {
                                final TrackingMutableRowSet rightRowSet = jsm.getRightIndexFromLeftIndex(ll);
                                if (rightRowSet.nonempty()) {
                                    final long currResultOffset = ll << jsm.getNumShiftBits();
                                    modBuilder.appendRange(currResultOffset, currResultOffset + rightRowSet.size() - 1);
                                }
                            });
                            downstream.modified = modBuilder.build();
                            downstream.modifiedColumnSet = resultTable.modifiedColumnSet;
                            leftTransformer.transform(upstream.modifiedColumnSet, resultTable.modifiedColumnSet);
                        } else {
                            downstream.modified = TrackingMutableRowSet.FACTORY.getEmptyRowSet();
                            downstream.modifiedColumnSet = ModifiedColumnSet.EMPTY;
                        }

                        final RowSetBuilder addBuilder = TrackingMutableRowSet.FACTORY.getRandomBuilder();
                        jsm.addLeft(upstream.added, (stateSlot, leftKey) -> {
                            final TrackingMutableRowSet rightRowSet = jsm.getRightIndex(stateSlot);
                            if (rightRowSet.nonempty()) {
                                final long regionStart = leftKey << jsm.getNumShiftBits();
                                addBuilder.addRange(regionStart, regionStart + rightRowSet.size() - 1);
                            }
                        });
                        try (final TrackingMutableRowSet added = addBuilder.build()) {
                            downstream.added.insert(added);
                            resultRowSet.insert(added);
                        }

                        resultTable.notifyListeners(downstream);
                    }
                });
                return resultTable;
            }

            final RightIncrementalChunkedCrossJoinStateManager jsm = new RightIncrementalChunkedCrossJoinStateManager(
                    bucketingContext.leftSources, control.initialBuildSize(), bucketingContext.rightSources, leftTable,
                    numRightBitsToReserve);
            jsm.setMaximumLoadFactor(control.getMaximumLoadFactor());
            jsm.setTargetLoadFactor(control.getTargetLoadFactor());

            final TrackingMutableRowSet resultRowSet = jsm.build(leftTable, rightTable);

            final QueryTable resultTable = makeResult(leftTable, rightTable, columnsToAdd, jsm, resultRowSet,
                    cs -> new CrossJoinRightColumnSource<>(jsm, cs, rightTable.isLive()));

            final ModifiedColumnSet.Transformer rightTransformer =
                    rightTable.newModifiedColumnSetTransformer(resultTable, columnsToAdd);

            if (leftTable.isLive()) {
                // LeftIndexToSlot needs prev value tracking
                jsm.startTrackingPrevValues();

                final ModifiedColumnSet.Transformer leftTransformer = leftTable.newModifiedColumnSetTransformer(
                        resultTable,
                        leftTable.getColumnSourceMap().keySet().toArray(CollectionUtil.ZERO_LENGTH_STRING_ARRAY));

                final JoinListenerRecorder leftRecorder =
                        new JoinListenerRecorder(true, bucketingContext.listenerDescription, leftTable, resultTable);
                final JoinListenerRecorder rightRecorder =
                        new JoinListenerRecorder(false, bucketingContext.listenerDescription, rightTable, resultTable);

                // The approach for both-sides-ticking is to:
                // - Aggregate all right side changes, queued to apply at the right time while processing left update.
                // - Handle left removes.
                // - Handle right removes (including right modified removes).
                // - Handle left shifts
                // - Handle left modifies.
                // - Handle right modifies and adds (including right modified adds and all downstream shift data).
                // - Handle left adds.
                // - Generate downstream MCS.
                // - Propagate and Profit.
                final MergedListener mergedListener = new MergedListener(Arrays.asList(leftRecorder, rightRecorder),
                        Collections.emptyList(), bucketingContext.listenerDescription, resultTable) {
                    private final CrossJoinModifiedSlotTracker tracker = new CrossJoinModifiedSlotTracker(jsm);

                    @Override
                    protected void process() {
                        final ShiftAwareListener.Update upstreamLeft = leftRecorder.getUpdate();
                        final ShiftAwareListener.Update upstreamRight = rightRecorder.getUpdate();
                        final boolean leftChanged = upstreamLeft != null;
                        final boolean rightChanged = upstreamRight != null;

                        final ShiftAwareListener.Update downstream = new ShiftAwareListener.Update();

                        // If there are any right changes, let's probe and aggregate them now.
                        if (rightChanged) {
                            tracker.rightShifted = upstreamRight.shifted;

                            if (upstreamRight.removed.nonempty()) {
                                jsm.rightRemove(upstreamRight.removed, tracker);
                            }
                            if (upstreamRight.shifted.nonempty()) {
                                try (final TrackingMutableRowSet prevIndex = rightTable.getIndex().getPrevIndex()) {
                                    jsm.rightShift(prevIndex, upstreamRight.shifted, tracker);
                                }
                            }
                            if (upstreamRight.added.nonempty()) {
                                jsm.rightAdd(upstreamRight.added, tracker);
                            }
                            if (upstreamRight.modified.nonempty()) {
                                jsm.rightModified(upstreamRight,
                                        upstreamRight.modifiedColumnSet.containsAny(rightKeyColumns), tracker);
                            }

                            // space needed for right rowSet might have changed, let's verify we have enough keyspace
                            jsm.validateKeySpaceSize();

                            // We must finalize all known slots, so that left accumulation does not mix with right
                            // accumulation.
                            if (upstreamRight.shifted.nonempty()) {
                                try (final TrackingMutableRowSet prevIndex = rightTable.getIndex().getPrevIndex()) {
                                    jsm.shiftRightIndexToSlot(prevIndex, upstreamRight.shifted);
                                }
                            }
                            tracker.finalizeRightProcessing();
                        }

                        final int prevRightBits = jsm.getPrevNumShiftBits();
                        final int currRightBits = jsm.getNumShiftBits();
                        final boolean allRowsShift = prevRightBits != currRightBits;

                        final boolean leftModifiedMightReslot =
                                leftChanged && upstreamLeft.modifiedColumnSet.containsAny(leftKeyColumns);

                        // Let us gather all removes from the left. This includes aggregating the results of left
                        // modified.
                        if (leftChanged) {
                            if (upstreamLeft.removed.nonempty()) {
                                jsm.leftRemoved(upstreamLeft.removed, tracker);
                            } else {
                                tracker.leftRemoved = TrackingMutableRowSet.FACTORY.getEmptyRowSet();
                            }

                            if (upstreamLeft.modified.nonempty()) {
                                // translates the left modified as rms/mods/adds and accumulates into
                                // tracker.{leftRemoved,leftModified,leftAdded}
                                jsm.leftModified(upstreamLeft, leftModifiedMightReslot, tracker);
                            } else {
                                tracker.leftModified = TrackingMutableRowSet.FACTORY.getEmptyRowSet();
                            }

                            downstream.removed = tracker.leftRemoved;
                            downstream.modified = tracker.leftModified;
                            resultRowSet.remove(downstream.removed);
                        } else {
                            downstream.removed = TrackingMutableRowSet.FACTORY.getEmptyRowSet();
                            downstream.modified = TrackingMutableRowSet.FACTORY.getEmptyRowSet();
                        }

                        if (rightChanged) {
                            // With left removes (and modified-removes) applied (yet adds and modified-adds pending),
                            // we can now easily calculate which rows are removed due to right removes.

                            try (final TrackingMutableRowSet leftIndexToVisitForRightRm = TrackingMutableRowSet.FACTORY.getEmptyRowSet()) {
                                tracker.forAllModifiedSlots(slotState -> {
                                    if (slotState.leftRowSet.size() > 0 && slotState.rightRemoved.nonempty()) {
                                        leftIndexToVisitForRightRm.insert(slotState.leftRowSet);
                                    }
                                });

                                try (final TrackingMutableRowSet toRemove = TrackingMutableRowSet.FACTORY.getEmptyRowSet()) {
                                    // This could use a sequential builder, however, since we are always appending
                                    // non-overlapping containers, inserting into an rowSet is actually rather efficient.
                                    leftIndexToVisitForRightRm.forAllLongs(ii -> {
                                        final long prevOffset = ii << prevRightBits;
                                        final CrossJoinModifiedSlotTracker.SlotState state = tracker
                                                .getFinalSlotState(jsm.getTrackerCookie(jsm.getSlotFromLeftIndex(ii)));
                                        toRemove.insertWithShift(prevOffset, state.rightRemoved);
                                    });
                                    downstream.removed.insert(toRemove);
                                }
                            }
                        }

                        // apply left shifts to tracker (so our mods/adds are in post-shift space)
                        if (leftChanged && upstreamLeft.shifted.nonempty()) {
                            tracker.leftShifted = upstreamLeft.shifted;
                            try (final TrackingMutableRowSet prevLeftMinusRemovals = leftTable.getIndex().getPrevIndex()) {
                                prevLeftMinusRemovals.remove(upstreamLeft.removed);
                                jsm.leftShift(prevLeftMinusRemovals, upstreamLeft.shifted, tracker);
                            }
                        }

                        // note rows to shift might have no shifts but still need result rowSet updated
                        final TrackingMutableRowSet rowsToShift;
                        final boolean mustCloseRowsToShift;

                        if (rightChanged) {
                            // process right mods / adds (in post-shift space)
                            final RowSetBuilder addsToVisit = TrackingMutableRowSet.FACTORY.getRandomBuilder();
                            final RowSetBuilder modsToVisit = TrackingMutableRowSet.FACTORY.getRandomBuilder();
                            tracker.forAllModifiedSlots(slotState -> {
                                if (slotState.leftRowSet.size() == 0) {
                                    return;
                                }
                                if (slotState.rightAdded.nonempty()) {
                                    addsToVisit.addRowSet(slotState.leftRowSet);
                                }
                                if (slotState.rightModified.nonempty()) {
                                    modsToVisit.addRowSet(slotState.leftRowSet);
                                }
                            });

                            try (final TrackingMutableRowSet leftIndexesToVisitForAdds = addsToVisit.build();
                                 final TrackingMutableRowSet leftIndexesToVisitForMods = modsToVisit.build();
                                 final TrackingMutableRowSet modified = TrackingMutableRowSet.FACTORY.getEmptyRowSet()) {
                                downstream.added = TrackingMutableRowSet.FACTORY.getEmptyRowSet();

                                leftIndexesToVisitForAdds.forAllLongs(ii -> {
                                    final long currOffset = ii << currRightBits;
                                    final CrossJoinModifiedSlotTracker.SlotState state = tracker
                                            .getFinalSlotState(jsm.getTrackerCookie(jsm.getSlotFromLeftIndex(ii)));
                                    downstream.added.insertWithShift(currOffset, state.rightAdded);
                                });

                                leftIndexesToVisitForMods.forAllLongs(ii -> {
                                    final long currOffset = ii << currRightBits;
                                    final CrossJoinModifiedSlotTracker.SlotState state = tracker
                                            .getFinalSlotState(jsm.getTrackerCookie(jsm.getSlotFromLeftIndex(ii)));
                                    modified.insertWithShift(currOffset, state.rightModified);
                                });
                                downstream.modified.insert(modified);

                                mustCloseRowsToShift = leftChanged || !allRowsShift;
                                if (allRowsShift) {
                                    rowsToShift = leftChanged ? leftTable.getIndex().minus(upstreamLeft.added)
                                            : leftTable.getIndex();
                                } else {
                                    rowsToShift = leftIndexesToVisitForAdds.clone();
                                }
                            }

                            if (!allRowsShift) {
                                // removals might generate shifts, so let's add those to our rowSet
                                final RowSetBuilder rmsToVisit = TrackingMutableRowSet.FACTORY.getRandomBuilder();
                                tracker.forAllModifiedSlots(slotState -> {
                                    if (slotState.leftRowSet.size() > 0 && slotState.rightRemoved.nonempty()) {
                                        rmsToVisit.addRowSet(slotState.leftRowSet);
                                    }
                                });
                                try (final TrackingMutableRowSet leftIndexesToVisitForRm = rmsToVisit.build()) {
                                    rowsToShift.insert(leftIndexesToVisitForRm);
                                }
                            }
                        } else {
                            mustCloseRowsToShift = false;
                            rowsToShift = TrackingMutableRowSet.FACTORY.getEmptyRowSet();
                        }

                        // Generate shift data; build up result rowSet changes for all but added left
                        final long prevCardinality = 1L << prevRightBits;
                        final long currCardinality = 1L << currRightBits;
                        final IndexShiftData.Builder shiftBuilder = new IndexShiftData.Builder();
                        final SequentialRowSetBuilder toRemoveFromResultIndex = TrackingMutableRowSet.FACTORY.getSequentialBuilder();
                        final SequentialRowSetBuilder toInsertIntoResultIndex = TrackingMutableRowSet.FACTORY.getSequentialBuilder();

                        if (rowsToShift.nonempty() && leftChanged && upstreamLeft.shifted.nonempty()) {
                            final MutableBoolean finishShifting = new MutableBoolean();
                            final MutableLong watermark = new MutableLong(0);
                            final MutableInt currLeftShiftIdx = new MutableInt(0);

                            try (final RowSequence.Iterator rsIt =
                                    allRowsShift ? null : resultRowSet.getRowSequenceIterator();
                                 final TrackingMutableRowSet unshiftedRowsToShift = rowsToShift.clone()) {
                                upstreamLeft.shifted.unapply(unshiftedRowsToShift);
                                final RowSet.SearchIterator prevIter = unshiftedRowsToShift.searchIterator();

                                final LongConsumer processLeftShiftsUntil = (ii) -> {
                                    // note: if all rows shift, then each row shifts by a different amount and
                                    // rowsToShift is inclusive
                                    if (!finishShifting.booleanValue() && watermark.longValue() >= ii || allRowsShift) {
                                        return;
                                    }

                                    for (; currLeftShiftIdx.intValue() < upstreamLeft.shifted.size(); currLeftShiftIdx
                                            .increment()) {
                                        final int shiftIdx = currLeftShiftIdx.intValue();
                                        final long beginRange =
                                                upstreamLeft.shifted.getBeginRange(shiftIdx) << prevRightBits;
                                        final long endRange =
                                                ((upstreamLeft.shifted.getEndRange(shiftIdx) + 1) << prevRightBits) - 1;
                                        final long shiftDelta =
                                                upstreamLeft.shifted.getShiftDelta(shiftIdx) << currRightBits;

                                        if (endRange < watermark.longValue()) {
                                            continue;
                                        }
                                        if (!finishShifting.booleanValue() && beginRange >= ii) {
                                            break;
                                        }

                                        final long maxTouched = Math.min(ii - 1, endRange);
                                        final long minTouched = Math.max(watermark.longValue(), beginRange);
                                        if (!rsIt.advance(minTouched)) {
                                            break;
                                        }

                                        shiftBuilder.shiftRange(minTouched, maxTouched, shiftDelta);
                                        rsIt.getNextRowSequenceThrough(maxTouched).forAllLongRanges((s, e) -> {
                                            toRemoveFromResultIndex.appendRange(s, e);
                                            toInsertIntoResultIndex.appendRange(s + shiftDelta, e + shiftDelta);
                                        });
                                        watermark.setValue(maxTouched + 1);

                                        if (!finishShifting.booleanValue() && maxTouched != endRange) {
                                            break;
                                        }
                                    }
                                };

                                rowsToShift.forAllLongs(ii -> {
                                    final long pi = prevIter.nextLong();

                                    final long prevOffset = pi << prevRightBits;
                                    final long currOffset = ii << currRightBits;
                                    final long slotFromLeftIndex = jsm.getSlotFromLeftIndex(ii);

                                    processLeftShiftsUntil.accept(prevOffset);

                                    if (slotFromLeftIndex == RightIncrementalChunkedCrossJoinStateManager.LEFT_MAPPING_MISSING) {
                                        // Since left rows that change key-column-groups are currently removed from all
                                        // JSM data structures,
                                        // they won't have a properly mapped slot. They will be added to their new slot
                                        // after we
                                        // generate-downstream shifts. The result rowSet is also updated for these rows
                                        // in
                                        // the left-rm/left-add code paths. This code path should only be hit when
                                        // prevRightBits != currRightBits.
                                        return;
                                    }
                                    final CrossJoinModifiedSlotTracker.SlotState slotState =
                                            tracker.getFinalSlotState(jsm.getTrackerCookie(slotFromLeftIndex));

                                    if (prevRightBits != currRightBits) {
                                        final TrackingMutableRowSet rightRowSet = jsm.getRightIndex(slotFromLeftIndex);
                                        if (rightRowSet.nonempty()) {
                                            toInsertIntoResultIndex.appendRange(currOffset,
                                                    currOffset + rightRowSet.size() - 1);
                                        }
                                    } else if (slotState != null) {
                                        final long prevSize = slotState.rightRowSet.sizePrev();
                                        final long currSize = slotState.rightRowSet.size();
                                        // note prevCardinality == currCardinality if prevRightBits == currRightBits
                                        if (prevOffset != currOffset) {
                                            // might be changing to an empty group
                                            if (currSize > 0) {
                                                toInsertIntoResultIndex.appendRange(currOffset,
                                                        currOffset + currSize - 1);
                                            }
                                            // might have changed from an empty group
                                            if (prevSize > 0) {
                                                toRemoveFromResultIndex.appendRange(prevOffset,
                                                        prevOffset + currCardinality - 1);
                                            }
                                        } else if (prevSize < currSize) {
                                            toInsertIntoResultIndex.appendRange(currOffset + prevSize,
                                                    currOffset + currSize - 1);
                                        } else if (currSize < prevSize && prevSize > 0) {
                                            toRemoveFromResultIndex.appendRange(prevOffset + currSize,
                                                    prevOffset + currCardinality - 1);
                                        }
                                    }

                                    // propagate inner shifts
                                    if (slotState != null && slotState.innerShifted.nonempty()) {
                                        shiftBuilder.appendShiftData(slotState.innerShifted, prevOffset,
                                                prevCardinality, currOffset, currCardinality);
                                    } else if (prevOffset != currOffset) {
                                        shiftBuilder.shiftRange(prevOffset, prevOffset + prevCardinality - 1,
                                                currOffset - prevOffset);
                                    }
                                    watermark.setValue((pi + 1) << prevRightBits);
                                });
                                // finish processing all shifts
                                finishShifting.setTrue();
                                processLeftShiftsUntil.accept(Long.MAX_VALUE);
                            }
                        } else if (rowsToShift.nonempty()) {
                            // note: no left shifts in this branch
                            rowsToShift.forAllLongs(ii -> {
                                final long prevOffset = ii << prevRightBits;
                                final long currOffset = ii << currRightBits;
                                final long slotFromLeftIndex = jsm.getSlotFromLeftIndex(ii);

                                if (slotFromLeftIndex == RightIncrementalChunkedCrossJoinStateManager.LEFT_MAPPING_MISSING) {
                                    // Since left rows that change key-column-groups are currently removed from all JSM
                                    // data structures,
                                    // they won't have a properly mapped slot. They will be added to their new slot
                                    // after we
                                    // generate-downstream shifts. The result rowSet is also updated for these rows in
                                    // the left-rm/left-add code paths. This code path should only be hit when
                                    // prevRightBits != currRightBits.
                                    return;
                                }

                                final CrossJoinModifiedSlotTracker.SlotState slotState =
                                        tracker.getFinalSlotState(jsm.getTrackerCookie(slotFromLeftIndex));

                                // calculate modifications to result rowSet
                                if (prevRightBits != currRightBits) {
                                    final TrackingMutableRowSet rightRowSet = jsm.getRightIndex(slotFromLeftIndex);
                                    if (rightRowSet.nonempty()) {
                                        toInsertIntoResultIndex.appendRange(currOffset,
                                                currOffset + rightRowSet.size() - 1);
                                    }
                                } else if (slotState != null) {
                                    final long prevSize = slotState.rightRowSet.sizePrev();
                                    final long currSize = slotState.rightRowSet.size();

                                    // note: prevOffset == currOffset (because left did not shift and right bits are
                                    // unchanged)
                                    if (prevSize < currSize) {
                                        toInsertIntoResultIndex.appendRange(currOffset + prevSize,
                                                currOffset + currSize - 1);
                                    } else if (currSize < prevSize && prevSize > 0) {
                                        // note prevCardinality == currCardinality if prevRightBits == currRightBits
                                        toRemoveFromResultIndex.appendRange(prevOffset + currSize,
                                                prevOffset + currCardinality - 1);
                                    }
                                }

                                // propagate inner shifts
                                if (slotState != null && slotState.innerShifted.nonempty()) {
                                    shiftBuilder.appendShiftData(slotState.innerShifted, prevOffset, prevCardinality,
                                            currOffset, currCardinality);
                                } else if (prevOffset != currOffset) {
                                    shiftBuilder.shiftRange(prevOffset, prevOffset + prevCardinality - 1,
                                            currOffset - prevOffset);
                                }
                            });
                        } else if (leftChanged && upstreamLeft.shifted.nonempty()) {
                            // upstream-left-shift our result rowSet, and build downstream shifts
                            try (final RowSequence.Iterator rsIt = resultRowSet.getRowSequenceIterator()) {
                                for (int idx = 0; idx < upstreamLeft.shifted.size(); ++idx) {
                                    final long beginRange = upstreamLeft.shifted.getBeginRange(idx) << prevRightBits;
                                    final long endRange =
                                            ((upstreamLeft.shifted.getEndRange(idx) + 1) << prevRightBits) - 1;
                                    final long shiftDelta = upstreamLeft.shifted.getShiftDelta(idx) << prevRightBits;

                                    if (!rsIt.advance(beginRange)) {
                                        break;
                                    }

                                    shiftBuilder.shiftRange(beginRange, endRange, shiftDelta);
                                    rsIt.getNextRowSequenceThrough(endRange).forAllLongRanges((s, e) -> {
                                        toRemoveFromResultIndex.appendRange(s, e);
                                        toInsertIntoResultIndex.appendRange(s + shiftDelta, e + shiftDelta);
                                    });
                                }
                            }
                        }

                        downstream.shifted = shiftBuilder.build();

                        try (final TrackingMutableRowSet toRemove = toRemoveFromResultIndex.build();
                             final TrackingMutableRowSet toInsert = toInsertIntoResultIndex.build()) {
                            if (prevRightBits != currRightBits) {
                                // every row shifted
                                resultRowSet.clear();
                            } else {
                                resultRowSet.remove(toRemove);
                            }
                            resultRowSet.insert(toInsert);
                        }

                        if (mustCloseRowsToShift) {
                            rowsToShift.close();
                        }

                        // propagate left adds / modded-adds to jsm
                        final boolean insertLeftAdded;
                        if (leftChanged && upstreamLeft.added.nonempty()) {
                            insertLeftAdded = true;
                            jsm.leftAdded(upstreamLeft.added, tracker);
                        } else if (leftModifiedMightReslot) {
                            // process any missing modified-adds
                            insertLeftAdded = true;
                            tracker.flushLeftAdds();
                        } else {
                            insertLeftAdded = false;
                        }

                        if (insertLeftAdded) {
                            resultRowSet.insert(tracker.leftAdded);
                            if (downstream.added != null) {
                                downstream.added.insert(tracker.leftAdded);
                                tracker.leftAdded.close();
                            } else {
                                downstream.added = tracker.leftAdded;
                            }
                        }

                        if (downstream.added == null) {
                            downstream.added = TrackingMutableRowSet.FACTORY.getEmptyRowSet();
                        }

                        if (leftChanged && tracker.leftModified.nonempty()) {
                            // We simply exploded the left rows to include all existing right rows; must remove the
                            // recently added.
                            downstream.modified.remove(downstream.added);
                        }
                        if (downstream.modified.empty()) {
                            downstream.modifiedColumnSet = ModifiedColumnSet.EMPTY;
                        } else {
                            downstream.modifiedColumnSet = resultTable.modifiedColumnSet;
                            downstream.modifiedColumnSet.clear();
                            if (leftChanged && tracker.hasLeftModifies) {
                                leftTransformer.transform(upstreamLeft.modifiedColumnSet, downstream.modifiedColumnSet);
                            }
                            if (rightChanged && tracker.hasRightModifies) {
                                rightTransformer.transform(upstreamRight.modifiedColumnSet,
                                        downstream.modifiedColumnSet);
                            }
                        }

                        resultTable.notifyListeners(downstream);

                        tracker.clear();
                    }
                };

                leftRecorder.setMergedListener(mergedListener);
                rightRecorder.setMergedListener(mergedListener);
                leftTable.listenForUpdates(leftRecorder);
                rightTable.listenForUpdates(rightRecorder);
                resultTable.addParentReference(mergedListener);
            } else {
                rightTable.listenForUpdates(new BaseTable.ShiftAwareListenerImpl(bucketingContext.listenerDescription,
                        rightTable, resultTable) {
                    private final CrossJoinModifiedSlotTracker tracker = new CrossJoinModifiedSlotTracker(jsm);

                    @Override
                    public void onUpdate(Update upstream) {
                        tracker.rightShifted = upstream.shifted;

                        final Update downstream = new Update();
                        final IndexShiftData.Builder shifted = new IndexShiftData.Builder();

                        if (upstream.removed.nonempty()) {
                            jsm.rightRemove(upstream.removed, tracker);
                        }
                        if (upstream.shifted.nonempty()) {
                            try (final TrackingMutableRowSet prevRowSet = rightTable.getIndex().getPrevIndex()) {
                                jsm.rightShift(prevRowSet, upstream.shifted, tracker);
                            }
                        }
                        if (upstream.added.nonempty()) {
                            jsm.rightAdd(upstream.added, tracker);
                        }
                        if (upstream.modified.nonempty()) {
                            jsm.rightModified(upstream, upstream.modifiedColumnSet.containsAny(rightKeyColumns),
                                    tracker);
                        }

                        // right changes are flushed now
                        if (upstream.shifted.nonempty()) {
                            try (final TrackingMutableRowSet prevRowSet = rightTable.getIndex().getPrevIndex()) {
                                jsm.shiftRightIndexToSlot(prevRowSet, upstream.shifted);
                            }
                        }
                        tracker.finalizeRightProcessing();

                        // space needed for right rowSet might have changed, let's verify we have enough keyspace
                        jsm.validateKeySpaceSize();

                        final int prevRightBits = jsm.getPrevNumShiftBits();
                        final int currRightBits = jsm.getNumShiftBits();

                        final TrackingMutableRowSet leftChanged;
                        final boolean numRightBitsChanged = currRightBits != prevRightBits;
                        if (numRightBitsChanged) {
                            // Must touch all left keys.
                            leftChanged = leftTable.getIndex();
                            // Must rebuild entire result rowSet.
                            resultRowSet.clear();
                        } else {
                            final RowSetBuilder leftChangedBuilder = TrackingMutableRowSet.FACTORY.getRandomBuilder();

                            tracker.forAllModifiedSlots(slotState -> {
                                // filter out slots that only have right shifts (these don't have downstream effects)
                                if (slotState.rightChanged) {
                                    leftChangedBuilder.addRowSet(jsm.getLeftIndex(slotState.slotLocation));
                                }
                            });

                            leftChanged = leftChangedBuilder.build();
                        }

                        final long prevCardinality = 1L << prevRightBits;
                        final SequentialRowSetBuilder added = TrackingMutableRowSet.FACTORY.getSequentialBuilder();
                        final SequentialRowSetBuilder removed = TrackingMutableRowSet.FACTORY.getSequentialBuilder();
                        final SequentialRowSetBuilder modified = TrackingMutableRowSet.FACTORY.getSequentialBuilder();

                        final SequentialRowSetBuilder removeFromResultIndex =
                                numRightBitsChanged ? null : TrackingMutableRowSet.FACTORY.getSequentialBuilder();
                        final SequentialRowSetBuilder addToResultIndex = TrackingMutableRowSet.FACTORY.getSequentialBuilder();

                        // Accumulate all changes by left row.
                        leftChanged.forAllLongs(ii -> {
                            final long prevOffset = ii << prevRightBits;
                            final long currOffset = ii << currRightBits;

                            final long slot = jsm.getSlotFromLeftIndex(ii);
                            final CrossJoinModifiedSlotTracker.SlotState slotState =
                                    tracker.getFinalSlotState(jsm.getTrackerCookie(slot));
                            final TrackingMutableRowSet rightRowSet = slotState == null ? jsm.getRightIndex(slot) : slotState.rightRowSet;

                            if (numRightBitsChanged) {
                                if (rightRowSet.nonempty()) {
                                    addToResultIndex.appendRange(currOffset, currOffset + rightRowSet.size() - 1);
                                }
                            } else if (slotState != null) {
                                final long prevSize = slotState.rightRowSet.sizePrev();
                                final long currSize = slotState.rightRowSet.size();

                                if (prevSize < currSize) {
                                    addToResultIndex.appendRange(currOffset + prevSize, currOffset + currSize - 1);
                                } else if (currSize < prevSize && prevSize > 0) {
                                    // note prevCardinality == currCardinality if prevRightBits == currRightBits
                                    removeFromResultIndex.appendRange(prevOffset + currSize,
                                            prevOffset + prevCardinality - 1);
                                }
                            }

                            if (slotState == null || !slotState.rightChanged) {
                                if (prevOffset != currOffset) {
                                    shifted.shiftRange(prevOffset, prevOffset + rightRowSet.sizePrev() - 1,
                                            currOffset - prevOffset);
                                }
                                return;
                            }

                            final long preShiftShiftAmt = prevOffset - (slotState.lastIndex << prevRightBits);
                            final long postShiftShiftAmt = currOffset - (slotState.lastIndex << currRightBits);
                            if (slotState.rightAdded.nonempty()) {
                                slotState.rightAdded.shiftInPlace(postShiftShiftAmt);
                                added.appendRowSet(slotState.rightAdded);
                            }
                            if (slotState.rightRemoved.nonempty()) {
                                slotState.rightRemoved.shiftInPlace(preShiftShiftAmt);
                                removed.appendRowSet(slotState.rightRemoved);
                            }
                            if (slotState.rightModified.nonempty()) {
                                slotState.rightModified.shiftInPlace(postShiftShiftAmt);
                                modified.appendRowSet(slotState.rightModified);
                            }
                            slotState.lastIndex = ii;

                            shifted.appendShiftData(slotState.innerShifted, prevOffset, rightRowSet.sizePrev(),
                                    currOffset, rightRowSet.size());
                        });

                        downstream.added = added.build();
                        downstream.modified = modified.build();
                        downstream.removed = removed.build();
                        downstream.shifted = shifted.build();

                        if (!numRightBitsChanged) {
                            leftChanged.close();
                            try (final TrackingMutableRowSet remove = removeFromResultIndex.build()) {
                                resultRowSet.remove(remove);
                            }
                        }
                        try (final TrackingMutableRowSet add = addToResultIndex.build()) {
                            resultRowSet.insert(add);
                        }

                        if (tracker.clear()) {
                            jsm.clearCookies();
                        }

                        if (downstream.modified.empty()) {
                            downstream.modifiedColumnSet = ModifiedColumnSet.EMPTY;
                        } else {
                            downstream.modifiedColumnSet = resultTable.modifiedColumnSet;
                            rightTransformer.clearAndTransform(upstream.modifiedColumnSet,
                                    downstream.modifiedColumnSet);
                        }

                        resultTable.notifyListeners(downstream);
                    }
                });
            }

            return resultTable;
        }
    }

    private static void validateZeroKeyIndexSpace(final QueryTable leftTable, final QueryTable rightTable,
            int numRightBitsReserved) {
        final long leftLastKey = leftTable.getIndex().lastRowKey();
        final long rightLastKey = rightTable.getIndex().lastRowKey();
        final int minLeftBits = CrossJoinShiftState.getMinBits(leftLastKey);
        final int minRightBits = CrossJoinShiftState.getMinBits(rightLastKey);
        numRightBitsReserved = Math.max(numRightBitsReserved, minRightBits);
        if (minLeftBits + numRightBitsReserved > 63) {
            throw new OutOfKeySpaceException(
                    "join with zero key columns out of rowSet space (left reqBits + right reserveBits > 63); "
                            + "(left table: {size: " + leftTable.getIndex().size() + " maxIndex: " + leftLastKey
                            + " reqBits: " + minLeftBits + "}) X "
                            + "(right table: {size: " + rightTable.getIndex().size() + " maxIndex: " + rightLastKey
                            + " reqBits: " + minRightBits + " reservedBits: " + numRightBitsReserved + "})"
                            + " exceeds Long.MAX_VALUE. Consider flattening either table or reserving fewer right bits if possible.");
        }
    }

    @NotNull
    private static Table zeroKeyColumnsJoin(QueryTable leftTable, QueryTable rightTable, MatchPair[] columnsToAdd,
            int numRightBitsToReserve, String listenerDescription) {
        // we are a single value join, we do not need to do any hash-related work
        validateZeroKeyIndexSpace(leftTable, rightTable, numRightBitsToReserve);
        final CrossJoinShiftState crossJoinState =
                new CrossJoinShiftState(Math.max(numRightBitsToReserve, CrossJoinShiftState.getMinBits(rightTable)));

        final TrackingMutableRowSet resultRowSet = TrackingMutableRowSet.FACTORY.getEmptyRowSet();
        final QueryTable result = makeResult(leftTable, rightTable, columnsToAdd, crossJoinState, resultRowSet,
                cs -> new BitMaskingColumnSource<>(crossJoinState, cs));
        final ModifiedColumnSet.Transformer leftTransformer =
                leftTable.newModifiedColumnSetTransformer(result, leftTable.getDefinition().getColumnNamesArray());
        final ModifiedColumnSet.Transformer rightTransformer =
                rightTable.newModifiedColumnSetTransformer(result, columnsToAdd);

        final BiConsumer<ShiftAwareListener.Update, ShiftAwareListener.Update> onUpdate = (leftUpdate, rightUpdate) -> {

            final boolean leftChanged = leftUpdate != null;
            final boolean rightChanged = rightUpdate != null;

            final int prevRightBits = crossJoinState.getNumShiftBits();
            final int currRightBits = Math.max(prevRightBits, CrossJoinShiftState.getMinBits(rightTable));
            validateZeroKeyIndexSpace(leftTable, rightTable, currRightBits);

            if (currRightBits != prevRightBits) {
                crossJoinState.setNumShiftBitsAndUpdatePrev(currRightBits);
            }

            final ShiftAwareListener.Update downstream = new ShiftAwareListener.Update();
            downstream.added = TrackingMutableRowSet.FACTORY.getEmptyRowSet();
            downstream.removed = TrackingMutableRowSet.FACTORY.getEmptyRowSet();
            downstream.modified = TrackingMutableRowSet.FACTORY.getEmptyRowSet();
            downstream.modifiedColumnSet = result.modifiedColumnSet;
            downstream.modifiedColumnSet.clear();

            final IndexShiftData.Builder shiftBuilder = new IndexShiftData.Builder();

            try (final SafeCloseableList closer = new SafeCloseableList()) {
                if (rightChanged && rightUpdate.modified.nonempty()) {
                    rightTransformer.transform(rightUpdate.modifiedColumnSet, result.modifiedColumnSet);
                }
                if (leftChanged && leftUpdate.modified.nonempty()) {
                    leftTransformer.transform(leftUpdate.modifiedColumnSet, result.modifiedColumnSet);
                }

                long currRightShift = 0; // how far currRight has been shifted
                final TrackingMutableRowSet currRight = closer.add(rightTable.getIndex().clone());

                if (rightChanged) {
                    // Must touch every left row. (Note: this code is accessible iff right changed.)
                    final TrackingMutableRowSet currLeft = leftTable.getIndex();
                    final TrackingMutableRowSet prevLeft = closer.add(currLeft.getPrevIndex());

                    long prevRightShift = 0; // how far prevRight has been shifted
                    final TrackingMutableRowSet prevRight = closer.add(rightTable.getIndex().getPrevIndex());

                    long rmRightShift = 0; // how far rmRight has been shifted
                    final TrackingMutableRowSet rmRight = closer.add(rightUpdate.removed.clone());

                    long addRightShift = 0; // how far addRight has been shifted
                    final TrackingMutableRowSet addRight = closer.add(rightUpdate.added.clone());

                    long modRightShift = 0; // how far modRight has been shifted
                    final TrackingMutableRowSet modRight = closer.add(rightUpdate.modified.clone());

                    long existingRightShift = 0; // how far existingRight has been shifted
                    final TrackingMutableRowSet existingRight = closer.add(currRight.minus(rightUpdate.added));

                    final boolean rightHasAdds = addRight.nonempty();
                    final boolean rightHasRemoves = rmRight.nonempty();
                    final boolean rightHasModifies = modRight.nonempty();

                    // Do note that add/mod's are in post-shift keyspace.
                    final TrackingMutableRowSet.SearchIterator leftAddIter = leftChanged ? leftUpdate.added.searchIterator() : null;
                    final TrackingMutableRowSet.SearchIterator leftRmIter = leftChanged ? leftUpdate.removed.searchIterator() : null;
                    final TrackingMutableRowSet.SearchIterator leftModIter = leftChanged ? leftUpdate.modified.searchIterator() : null;
                    boolean moreLeftAdd = leftChanged && advanceIterator(leftAddIter);
                    boolean moreLeftRm = leftChanged && advanceIterator(leftRmIter);
                    boolean moreLeftMod = leftChanged && advanceIterator(leftModIter);

                    // Prepare left-side iterators.
                    final TrackingMutableRowSet.SearchIterator leftPrevIter = prevLeft.searchIterator();
                    final TrackingMutableRowSet.SearchIterator leftCurrIter = leftTable.getIndex().searchIterator();
                    boolean moreLeftPrev = advanceIterator(leftPrevIter);
                    boolean moreLeftCurr = advanceIterator(leftCurrIter);

                    // It is more efficient to completely rebuild this rowSet, than to modify each row to right mapping.
                    resultRowSet.clear();

                    final long prevCardinality = 1L << prevRightBits;
                    final long currCardinality = 1L << currRightBits;

                    // Note: This assumes that shifts are not allowed to re-order data.
                    while (moreLeftPrev) {
                        final long currPrevIdx = leftPrevIter.currentValue();
                        final long prevResultOffset = currPrevIdx << prevRightBits;
                        moreLeftPrev = advanceIterator(leftPrevIter);

                        if (moreLeftRm && currPrevIdx == leftRmIter.currentValue()) {
                            // currPrevIdx is a left remove.
                            moreLeftRm = advanceIterator(leftRmIter);
                            prevRightShift = furtherShiftIndex(prevRight, prevRightShift, prevResultOffset);
                            downstream.removed.insert(prevRight);
                            continue;
                        }

                        // Note: Pre-existing row was not removed, therefore there must be an entry in curr rowSet.
                        Assert.eqTrue(moreLeftCurr, "moreLeftCurr");
                        long currCurrIdx = leftCurrIter.currentValue();
                        long currResultOffset = currCurrIdx << currRightBits;
                        moreLeftCurr = advanceIterator(leftCurrIter);

                        // Insert adds until we find our currCurrIdx that matches currPrevIdx.
                        while (moreLeftAdd && currCurrIdx == leftAddIter.currentValue()) {
                            // currCurrIdx is a left add.
                            moreLeftAdd = advanceIterator(leftAddIter);
                            currRightShift = furtherShiftIndex(currRight, currRightShift, currResultOffset);
                            downstream.added.insert(currRight);
                            resultRowSet.insert(currRight);

                            // Advance left current iterator.
                            Assert.eqTrue(moreLeftCurr, "moreLeftCurr");
                            currCurrIdx = leftCurrIter.currentValue();
                            currResultOffset = currCurrIdx << currRightBits;
                            moreLeftCurr = advanceIterator(leftCurrIter);
                        }

                        if (rightHasRemoves) {
                            rmRightShift = furtherShiftIndex(rmRight, rmRightShift, prevResultOffset);
                            downstream.removed.insert(rmRight);
                        }

                        if (rightHasAdds) {
                            addRightShift = furtherShiftIndex(addRight, addRightShift, currResultOffset);
                            downstream.added.insert(addRight);
                        }

                        if (moreLeftMod && currCurrIdx == leftModIter.currentValue()) {
                            // currCurrIdx is modify; paint all existing rows as modified
                            moreLeftMod = advanceIterator(leftModIter);
                            existingRightShift = furtherShiftIndex(existingRight, existingRightShift, currResultOffset);
                            downstream.modified.insert(existingRight);
                        } else if (rightHasModifies) {
                            modRightShift = furtherShiftIndex(modRight, modRightShift, currResultOffset);
                            downstream.modified.insert(modRight);
                        }

                        currRightShift = furtherShiftIndex(currRight, currRightShift, currResultOffset);
                        resultRowSet.insert(currRight);

                        if (rightUpdate.shifted.nonempty()) {
                            shiftBuilder.appendShiftData(rightUpdate.shifted, prevResultOffset, prevCardinality,
                                    currResultOffset, currCardinality);
                        } else if (currResultOffset != prevResultOffset) {
                            final long shiftDelta = currResultOffset - prevResultOffset;
                            final long lastResultIdx = prevResultOffset + prevCardinality - 1;
                            shiftBuilder.shiftRange(prevResultOffset, lastResultIdx, shiftDelta);
                        }
                    }

                    // Note: Only left adds remain.
                    while (moreLeftCurr) {
                        final long currCurrIdx = leftCurrIter.currentValue();
                        moreLeftCurr = advanceIterator(leftCurrIter);

                        Assert.eqTrue(moreLeftAdd, "moreLeftAdd");
                        assert leftAddIter != null;
                        Assert.eq(currCurrIdx, "currCurrIdx", leftAddIter.currentValue(), "leftAddIter.currentValue()");
                        moreLeftAdd = advanceIterator(leftAddIter);

                        final long currResultIdx = currCurrIdx << currRightBits;
                        currRightShift = furtherShiftIndex(currRight, currRightShift, currResultIdx);
                        downstream.added.insert(currRight);
                        resultRowSet.insert(currRight);
                    }

                    downstream.shifted = shiftBuilder.build();
                } else {
                    // Explode left updates to apply to all right rows.
                    assert leftUpdate != null;

                    TrackingMutableRowSet.SearchIterator iter = leftUpdate.removed.searchIterator();
                    while (iter.hasNext()) {
                        final long currIdx = iter.nextLong();
                        final long currResultIdx = currIdx << currRightBits;
                        currRightShift = furtherShiftIndex(currRight, currRightShift, currResultIdx);
                        downstream.removed.insert(currRight);
                        resultRowSet.removeRange(currResultIdx, ((currIdx + 1) << currRightBits) - 1);
                    }

                    downstream.shifted = expandLeftOnlyShift(leftTable.getIndex(), leftUpdate.shifted, crossJoinState);
                    downstream.shifted.apply(resultRowSet);

                    iter = leftUpdate.modified.searchIterator();
                    while (iter.hasNext()) {
                        final long currIdx = iter.nextLong();
                        final long currResultIdx = currIdx << currRightBits;
                        currRightShift = furtherShiftIndex(currRight, currRightShift, currResultIdx);
                        downstream.modified.insert(currRight);
                    }

                    iter = leftUpdate.added.searchIterator();
                    while (iter.hasNext()) {
                        final long currIdx = iter.nextLong();
                        final long currResultIdx = currIdx << currRightBits;
                        currRightShift = furtherShiftIndex(currRight, currRightShift, currResultIdx);
                        downstream.added.insert(currRight);
                        resultRowSet.insert(currRight);
                    }
                }
            }

            result.notifyListeners(downstream);
        };

        if (leftTable.isLive() && rightTable.isLive()) {
            final JoinListenerRecorder leftRecorder =
                    new JoinListenerRecorder(true, listenerDescription, leftTable, result);
            final JoinListenerRecorder rightRecorder =
                    new JoinListenerRecorder(false, listenerDescription, rightTable, result);

            final MergedListener mergedListener = new MergedListener(Arrays.asList(leftRecorder, rightRecorder),
                    Collections.emptyList(), listenerDescription, result) {
                @Override
                protected void process() {
                    onUpdate.accept(leftRecorder.getUpdate(), rightRecorder.getUpdate());
                }
            };

            leftRecorder.setMergedListener(mergedListener);
            rightRecorder.setMergedListener(mergedListener);
            leftTable.listenForUpdates(leftRecorder);
            rightTable.listenForUpdates(rightRecorder);
            result.addParentReference(mergedListener);
        } else if (leftTable.isLive() && rightTable.size() > 0) {
            leftTable.listenForUpdates(new BaseTable.ShiftAwareListenerImpl(listenerDescription, leftTable, result) {
                @Override
                public void onUpdate(final Update upstream) {
                    onUpdate.accept(upstream, null);
                }
            });
        } else if (rightTable.isLive() && leftTable.size() > 0) {
            rightTable.listenForUpdates(new BaseTable.ShiftAwareListenerImpl(listenerDescription, rightTable, result) {
                @Override
                public void onUpdate(final Update upstream) {
                    onUpdate.accept(null, upstream);
                }
            });
        }

        // Initialize result table.
        try (final TrackingMutableRowSet currRight = rightTable.getIndex().clone()) {
            final MutableLong currRightShift = new MutableLong();
            leftTable.getIndex().forAllLongs((currIdx) -> {
                final long currResultIdx = currIdx << crossJoinState.getNumShiftBits();
                currRightShift.setValue(furtherShiftIndex(currRight, currRightShift.longValue(), currResultIdx));
                resultRowSet.insert(currRight);
            });
        }
        resultRowSet.initializePreviousValue();
        return result;
    }

    private static boolean advanceIterator(final TrackingMutableRowSet.SearchIterator iter) {
        if (!iter.hasNext()) {
            return false;
        }
        iter.nextLong();
        return true;
    }

    private static long furtherShiftIndex(final TrackingMutableRowSet rowSet, final long currShift, final long destShift) {
        final long toShift = destShift - currShift;
        rowSet.shiftInPlace(toShift);
        return destShift;
    }

    private static IndexShiftData expandLeftOnlyShift(final TrackingMutableRowSet leftRowSet, final IndexShiftData leftShifts,
                                                      final CrossJoinShiftState shiftState) {
        final int currRightBits = shiftState.getNumShiftBits();
        final int prevRightBits = shiftState.getPrevNumShiftBits();
        final boolean needPerRowShift = currRightBits != prevRightBits;

        if (leftShifts.empty() && !needPerRowShift) {
            return IndexShiftData.EMPTY;
        }

        final IndexShiftData.Builder shiftBuilder = new IndexShiftData.Builder();
        if (needPerRowShift) {
            // Sadly, must shift everything.
            final LongRangeConsumer shiftUntil = new LongRangeConsumer() {
                final TrackingMutableRowSet.Iterator iter = leftRowSet.iterator();
                boolean hasNext = iter.hasNext();
                long next = hasNext ? iter.next() : Long.MAX_VALUE;

                @Override
                public void accept(long exclusiveEnd, long extraDelta) {
                    while (hasNext && next < exclusiveEnd) {
                        final long prevOffset = next << prevRightBits;
                        final long currOffset = (next + extraDelta) << currRightBits;
                        final long prevEndOffset = (next + 1) << prevRightBits - 1;
                        shiftBuilder.shiftRange(prevOffset, prevEndOffset, currOffset - prevOffset);
                        hasNext = iter.hasNext();
                        next = hasNext ? iter.next() : Long.MAX_VALUE;
                    }
                }
            };
            for (int ii = 0; ii < leftShifts.size(); ++ii) {
                final long begin = leftShifts.getBeginRange(ii);
                final long end = leftShifts.getEndRange(ii);
                final long delta = leftShifts.getShiftDelta(ii);
                shiftUntil.accept(begin, 0L);
                shiftUntil.accept(end + 1, delta);
            }
            // finishing shifting all left rows beyond the last upstream left shift
            shiftUntil.accept(Long.MAX_VALUE, 0L);
        } else {
            for (int si = 0; si < leftShifts.size(); ++si) {
                final long ss = leftShifts.getBeginRange(si);
                final long se = leftShifts.getEndRange(si);
                final long sd = leftShifts.getShiftDelta(si);
                shiftBuilder.shiftRange(ss << currRightBits, ((se + 1) << currRightBits) - 1, sd << currRightBits);
            }
        }

        return shiftBuilder.build();
    }

    @NotNull
    private static <T extends ColumnSource<?>> QueryTable makeResult(
            @NotNull final QueryTable leftTable,
            @NotNull final Table rightTable,
            @NotNull final MatchPair[] columnsToAdd,
            @NotNull final CrossJoinShiftState joinState,
            @NotNull final TrackingMutableRowSet resultRowSet,
            @NotNull final Function<ColumnSource<?>, T> newRightColumnSource) {
        final Map<String, ColumnSource<?>> columnSourceMap = new LinkedHashMap<>();

        for (final Map.Entry<String, ColumnSource<?>> leftColumn : leftTable.getColumnSourceMap().entrySet()) {
            final BitShiftingColumnSource<?> wrappedSource =
                    new BitShiftingColumnSource<>(joinState, leftColumn.getValue());
            columnSourceMap.put(leftColumn.getKey(), wrappedSource);
        }

        for (MatchPair mp : columnsToAdd) {
            final T wrappedSource = newRightColumnSource.apply(rightTable.getColumnSource(mp.right()));
            columnSourceMap.put(mp.left(), wrappedSource);
        }

        return new QueryTable(resultRowSet, columnSourceMap);
    }
}
