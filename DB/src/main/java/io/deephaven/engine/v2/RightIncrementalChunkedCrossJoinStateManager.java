/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */
package io.deephaven.engine.v2;

import io.deephaven.base.verify.Require;
import io.deephaven.base.verify.Assert;
import io.deephaven.engine.structures.rowsequence.OrderedKeys;
import io.deephaven.engine.structures.rowredirection.RedirectionIndex;
import io.deephaven.engine.structures.rowset.Index;
import io.deephaven.engine.structures.rowset.ReadOnlyIndex;
import io.deephaven.engine.structures.rowshiftdata.IndexShiftData;
import io.deephaven.util.QueryConstants;
import io.deephaven.engine.v2.hashing.*;
// this is ugly to have twice, but we do need it twice for replication
// @StateChunkIdentityName@ from \QObjectChunkIdentity\E
import io.deephaven.engine.v2.hashing.ObjectChunkIdentityEquals;
import io.deephaven.engine.v2.sort.permute.PermuteKernel;
import io.deephaven.engine.v2.sort.timsort.LongIntTimsortKernel;
import io.deephaven.engine.v2.sources.*;
import io.deephaven.engine.structures.chunk.*;
import io.deephaven.engine.structures.chunk.Attributes.*;
import io.deephaven.engine.v2.utils.*;

// mixin rehash
import java.util.Arrays;
import io.deephaven.engine.v2.sort.permute.IntPermuteKernel;
// @StateChunkTypeEnum@ from \QObject\E
import io.deephaven.engine.v2.utils.compact.IntCompactKernel;
import io.deephaven.engine.v2.utils.compact.LongCompactKernel;
// endmixin rehash

import io.deephaven.util.SafeCloseableArray;
import org.jetbrains.annotations.NotNull;

// region extra imports
// endregion extra imports

import static io.deephaven.util.SafeCloseable.closeArray;

// region class visibility
/**
 * This is our JoinStateManager for cross join when right is ticking (left may be static or ticking).
 */
// endregion class visibility
class RightIncrementalChunkedCrossJoinStateManager
        // region extensions
        extends CrossJoinShiftState
        implements CrossJoinStateManager
    // endregion extensions
{
    // region constants
    private static final int CHUNK_SIZE = 4096;
    private static final int MINIMUM_INITIAL_HASH_SIZE = CHUNK_SIZE;
    private static final long MAX_TABLE_SIZE = 1L << 30;
    // endregion constants

    // mixin rehash
    static final double DEFAULT_MAX_LOAD_FACTOR = 0.75;
    static final double DEFAULT_TARGET_LOAD_FACTOR = 0.70;
    // endmixin rehash

    // region preamble variables
    @FunctionalInterface
    interface StateTrackingCallback {

        /**
         * Invoke a callback that will allow external trackers to record changes to states in build or probe calls.
         *
         * @param cookie    The last known cookie for state slot (in main table space)
         * @param stateSlot The state slot (in main table space)
         * @param index     The probed index key
         * @param prevIndex The probed prev index key (applicable only when prevIndex provided to build/probe otherwise Index.NULL_KEY)
         * @return The new cookie for the state
         */
        long invoke(long cookie, long stateSlot, long index, long prevIndex);
    }
    // endregion preamble variables

    @ReplicateHashTable.EmptyStateValue
    // @NullStateValue@ from \Qnull\E, @StateValueType@ from \QIndex\E
    private static final Index EMPTY_RIGHT_VALUE = null;

    // mixin getStateValue
    // region overflow pivot
    private static final long OVERFLOW_PIVOT_VALUE = -2;
    // endregion overflow pivot
    // endmixin getStateValue

    // the number of slots in our table
    // mixin rehash
    private int tableSize;
    // endmixin rehash
    // altmixin rehash: private final int tableSize;

    // how many key columns we have
    private final int keyColumnCount;

    // mixin rehash
    private long numEntries = 0;

    /** Our table size must be 2^L (i.e. a power of two); and the pivot is between 2^(L-1) and 2^L.
     *
     * <p>When hashing a value, if hashCode % 2^L < tableHashPivot; then the destination location is hashCode % 2^L.
     * If hashCode % 2^L >= tableHashPivot, then the destination location is hashCode % 2^(L-1).  Once the pivot reaches
     * the table size, we can simply double the table size and repeat the process.</p>
     *
     * <p>This has the effect of only using hash table locations < hashTablePivot.  When we want to expand the table
     * we can move some of the entries from the location {@code tableHashPivot - 2^(L-1)} to tableHashPivot.  This
     * provides for incremental expansion of the hash table, without the need for a full rehash.</p>
      */
    private int tableHashPivot;

    // the table will be rehashed to a load factor of targetLoadFactor if our loadFactor exceeds maximumLoadFactor
    // or if it falls below minimum load factor we will instead contract the table
    private double targetLoadFactor = DEFAULT_TARGET_LOAD_FACTOR;
    private double maximumLoadFactor = DEFAULT_MAX_LOAD_FACTOR;
    // TODO: We do not yet support contraction
    // private final double minimumLoadFactor = 0.5;

    private final IntegerArraySource freeOverflowLocations = new IntegerArraySource();
    private int freeOverflowCount = 0;
    // endmixin rehash

    // the keys for our hash entries
    private final ArrayBackedColumnSource<?>[] keySources;
    // the location of any overflow entry in this bucket
    private final IntegerArraySource overflowLocationSource = new IntegerArraySource();

    // we are going to also reuse this for our state entry, so that we do not need additional storage
    @ReplicateHashTable.StateColumnSource
    // @StateColumnSourceType@ from \QObjectArraySource<Index>\E
    private final ObjectArraySource<Index> rightIndexSource
            // @StateColumnSourceConstructor@ from \QObjectArraySource<>(Index.class)\E
            = new ObjectArraySource<>(Index.class);

    // the keys for overflow
    private int nextOverflowLocation = 0;
    private final ArrayBackedColumnSource<?> [] overflowKeySources;
    // the location of the next key in an overflow bucket
    private final IntegerArraySource overflowOverflowLocationSource = new IntegerArraySource();
    // the overflow buckets for the right Index
    @ReplicateHashTable.OverflowStateColumnSource
    // @StateColumnSourceType@ from \QObjectArraySource<Index>\E
    private final ObjectArraySource<Index> overflowRightIndexSource
            // @StateColumnSourceConstructor@ from \QObjectArraySource<>(Index.class)\E
            = new ObjectArraySource<>(Index.class);

    // the type of each of our key chunks
    private final ChunkType[] keyChunkTypes;

    // the operators for hashing and various equality methods
    private final ChunkHasher[] chunkHashers;
    private final ChunkEquals[] chunkEquals;
    private final PermuteKernel[] chunkCopiers;

    // mixin rehash
    // If we have objects in our key columns, then we should null them out if we delete an overflow row, this only
    // applies to ObjectArraySources, for primitives we are content to leave the dead entries in the tables, because
    // they will not affect GC.
    private final ObjectArraySource<?>[] overflowKeyColumnsToNull;
    // endmixin rehash

    // region extra variables
    // maintain a mapping from left index to its slot
    private final RedirectionIndex leftIndexToSlot;
    private final RedirectionIndex rightIndexToSlot;
    private final ColumnSource<?>[] leftKeySources;
    private final ColumnSource<?>[] rightKeySources;
    private final ObjectArraySource<Index> leftIndexSource
            = new ObjectArraySource<>(Index.class);
    private final ObjectArraySource<Index> overflowLeftIndexSource
            = new ObjectArraySource<>(Index.class);
    private final boolean isLeftTicking;

    // we must maintain our cookie for modified state tracking
    private final LongArraySource modifiedTrackerCookieSource;
    private final LongArraySource overflowModifiedTrackerCookieSource;
    private final long EMPTY_RIGHT_SLOT = Index.NULL_KEY;
    private final QueryTable leftTable;
    private long maxRightGroupSize = 0;

    public static final long LEFT_MAPPING_MISSING = Index.NULL_KEY;
    // endregion extra variables

    RightIncrementalChunkedCrossJoinStateManager(ColumnSource<?>[] tableKeySources
                                         , int tableSize
                                                 // region constructor arguments
            , ColumnSource<?>[] rightKeySources
            , QueryTable leftTable
            , int initialNumRightBits
                                              // endregion constructor arguments
    ) {
        // region super
        super(initialNumRightBits);
        // endregion super
        keyColumnCount = tableKeySources.length;

        this.tableSize = tableSize;
        Require.leq(tableSize, "tableSize", MAX_TABLE_SIZE);
        Require.gtZero(tableSize, "tableSize");
        Require.eq(Integer.bitCount(tableSize), "Integer.bitCount(tableSize)", 1);
        // mixin rehash
        this.tableHashPivot = tableSize;
        // endmixin rehash

        overflowKeySources = new ArrayBackedColumnSource[keyColumnCount];
        keySources = new ArrayBackedColumnSource[keyColumnCount];

        keyChunkTypes = new ChunkType[keyColumnCount];
        chunkHashers = new ChunkHasher[keyColumnCount];
        chunkEquals = new ChunkEquals[keyColumnCount];
        chunkCopiers = new PermuteKernel[keyColumnCount];

        for (int ii = 0; ii < keyColumnCount; ++ii) {
            // the sources that we will use to store our hash table
            keySources[ii] = ArrayBackedColumnSource.getMemoryColumnSource(tableSize, tableKeySources[ii].getType());
            keyChunkTypes[ii] = tableKeySources[ii].getChunkType();

            overflowKeySources[ii] = ArrayBackedColumnSource.getMemoryColumnSource(CHUNK_SIZE, tableKeySources[ii].getType());

            chunkHashers[ii] = ChunkHasher.makeHasher(keyChunkTypes[ii]);
            chunkEquals[ii] = ChunkEquals.makeEqual(keyChunkTypes[ii]);
            chunkCopiers[ii] = PermuteKernel.makePermuteKernel(keyChunkTypes[ii]);
        }

        // mixin rehash
        overflowKeyColumnsToNull = Arrays.stream(overflowKeySources).filter(x -> x instanceof ObjectArraySource).map(x -> (ObjectArraySource)x).toArray(ObjectArraySource[]::new);
        // endmixin rehash

        // region constructor
        this.leftIndexToSlot = RedirectionIndex.FACTORY.createRedirectionIndex(tableSize);
        this.rightIndexToSlot = RedirectionIndex.FACTORY.createRedirectionIndex(tableSize);
        this.leftKeySources = tableKeySources;
        this.rightKeySources = rightKeySources;
        this.isLeftTicking = leftTable.isLive();
        this.leftTable = leftTable;
        this.modifiedTrackerCookieSource = new LongArraySource();
        this.overflowModifiedTrackerCookieSource = new LongArraySource();
        // endregion constructor

        ensureCapacity(tableSize);
    }

    private void ensureCapacity(int tableSize) {
        rightIndexSource.ensureCapacity(tableSize);
        overflowLocationSource.ensureCapacity(tableSize);
        for (int ii = 0; ii < keyColumnCount; ++ii) {
            keySources[ii].ensureCapacity(tableSize);
        }
        // region ensureCapacity
        leftIndexSource.ensureCapacity(tableSize);
        modifiedTrackerCookieSource.ensureCapacity(tableSize);
        // endregion ensureCapacity
    }

    private void ensureOverflowCapacity(WritableIntChunk<ChunkPositions> chunkPositionsToInsertInOverflow) {
        final int locationsToAllocate = chunkPositionsToInsertInOverflow.size();
        // mixin rehash
        if (freeOverflowCount >= locationsToAllocate) {
            return;
        }
        final int newCapacity = nextOverflowLocation + locationsToAllocate - freeOverflowCount;
        // endmixin rehash
        // altmixin rehash: final int newCapacity = nextOverflowLocation + locationsToAllocate;
        overflowOverflowLocationSource.ensureCapacity(newCapacity);
        overflowRightIndexSource.ensureCapacity(newCapacity);
        //noinspection ForLoopReplaceableByForEach
        for (int ii = 0; ii < overflowKeySources.length; ++ii) {
            overflowKeySources[ii].ensureCapacity(newCapacity);
        }
        // region ensureOverflowCapacity
        overflowLeftIndexSource.ensureCapacity(newCapacity);
        overflowModifiedTrackerCookieSource.ensureCapacity(newCapacity);
        // endregion ensureOverflowCapacity
    }

    // region build wrappers
    @NotNull
    Index build(@NotNull final QueryTable leftTable,
                @NotNull final QueryTable rightTable) {
        // This state manager assumes right side is ticking.
        Assert.eqTrue(rightTable.isLive(), "rightTable.isLive()");
        if (!leftTable.isEmpty()) {
            try (final BuildContext bc = makeBuildContext(leftKeySources, leftTable.getIndex().size())) {
                final boolean isLeft = true;
                buildTable(bc, leftTable.getIndex(), leftKeySources, null, null,
                        (cookie, slot, index, prevIndex) -> addToIndex(isLeft, slot, index));
            }
        }

        if (!rightTable.isEmpty()) {
            if (isLeftTicking) {
                try (final BuildContext bc = makeBuildContext(rightKeySources, rightTable.getIndex().size())) {
                    final boolean isLeft = false;
                    buildTable(bc, rightTable.getIndex(), rightKeySources, null, null,
                            (cookie, slot, index, prevIndex) -> addToIndex(isLeft, slot, index));
                }
            } else {
                // we don't actually need to create groups that don't (and will never) exist on the left; thus can probe-only
                try (final ProbeContext pc = makeProbeContext(rightKeySources, rightTable.size())) {
                    final boolean isLeft = false;
                    final boolean usePrev = false;
                    decorationProbe(pc, rightTable.getIndex(), rightKeySources, usePrev, null,
                            (cookie, slot, index, prevIndex) -> addToIndex(isLeft, slot, index));
                }
            }
        }

        // We can now validate key-space after all of our right rows have been aggregated into groups, which determined
        // how many bits we need for the right indexes.
        validateKeySpaceSize();

        final Index.SequentialBuilder resultIndex = Index.FACTORY.getSequentialBuilder();
        leftTable.getIndex().forAllLongs(index -> {
            final long regionStart = index << getNumShiftBits();
            final Index rightIndex = getRightIndexFromLeftIndex(index);
            if (rightIndex.nonempty()) {
                resultIndex.appendRange(regionStart, regionStart + rightIndex.size() - 1);
            }
        });

        return resultIndex.getIndex();
    }

    void rightRemove(final ReadOnlyIndex removed, final CrossJoinModifiedSlotTracker tracker) {
        if (removed.isEmpty()) {
            return;
        }
        try (final ProbeContext pc = makeProbeContext(rightKeySources, removed.size())) {
            final boolean usePrev = true;
            decorationProbe(pc, removed, rightKeySources, usePrev, null, (cookie, slot, index, prevIndex) -> {
                rightIndexToSlot.removeVoid(index);
                long newCookie = tracker.appendChunkRemove(cookie, slot, index);
                if (isOverflowLocation(slot)) {
                    overflowModifiedTrackerCookieSource.set(overflowLocationToHashLocation(slot), newCookie);
                } else {
                    modifiedTrackerCookieSource.set(slot, newCookie);
                }
                return newCookie;
            });
        }
    }

    void shiftRightIndexToSlot(final ReadOnlyIndex filterIndex, final IndexShiftData shifted) {
        rightIndexToSlot.applyShift(filterIndex, shifted);
    }

    void rightShift(final ReadOnlyIndex filterIndex, final IndexShiftData shifted, final CrossJoinModifiedSlotTracker tracker) {
        shifted.forAllInIndex(filterIndex, (ii, delta) -> {
            final long slot = rightIndexToSlot.get(ii);
            if (slot == Index.NULL_KEY) {
                // right-ticking w/static-left does not maintain group states that will never be used
                return;
            }

            final long cookie;
            if (isOverflowLocation(slot)) {
                cookie = overflowModifiedTrackerCookieSource.getUnsafe(overflowLocationToHashLocation(slot));
            } else {
                cookie = modifiedTrackerCookieSource.getUnsafe(slot);
            }

            long newCookie = tracker.needsRightShift(cookie, slot);
            if (isOverflowLocation(slot)) {
                overflowModifiedTrackerCookieSource.set(overflowLocationToHashLocation(slot), newCookie);
            } else {
                modifiedTrackerCookieSource.set(slot, newCookie);
            }
        });
    }

    void rightAdd(final Index added, final CrossJoinModifiedSlotTracker tracker) {
        if (added.isEmpty()) {
            return;
        }

        final StateTrackingCallback addKeyCallback = (cookie, slot, index, prevIndex) -> {
            ensureSlotExists(slot);
            return tracker.appendChunkAdd(cookie, slot, index);
        };

        if (!isLeftTicking) {
            // when left is static we have no business creating new slots
            try (final ProbeContext pc = makeProbeContext(rightKeySources, added.size())) {
                final boolean usePrev = false;
                decorationProbe(pc, added, rightKeySources, usePrev, null, addKeyCallback);
            }
        } else {
            try (final BuildContext bc = makeBuildContext(rightKeySources, added.size())) {
                buildTable(bc, added, rightKeySources, tracker, null, addKeyCallback);
            }
        }
    }

    void rightModified(final ShiftAwareListener.Update upstream, final boolean keyColumnsChanged, final CrossJoinModifiedSlotTracker tracker) {
        if (upstream.modified.isEmpty()) {
            return;
        }

        if (!keyColumnsChanged) {
            try (final ProbeContext pc = makeProbeContext(rightKeySources, upstream.modified.size())) {
                final boolean usePrev = false;
                decorationProbe(pc, upstream.modified, rightKeySources, usePrev, null,
                        (cookie, slot, index, prevIndex) -> tracker.appendChunkModify(cookie, slot, index));
            }
            return;
        }

        try (final ProbeContext pc = isLeftTicking ? null : makeProbeContext(rightKeySources, upstream.modified.size());
             final BuildContext bc = isLeftTicking ? makeBuildContext(rightKeySources, upstream.modified.size()) : null;
             final OrderedKeys.Iterator preShiftModified = upstream.getModifiedPreShift().getOrderedKeysIterator();
             final OrderedKeys.Iterator postShiftModified = upstream.modified.getOrderedKeysIterator()) {

            while (preShiftModified.hasMore()) {
                final OrderedKeys postChunk = postShiftModified.getNextOrderedKeysWithLength(CHUNK_SIZE);
                final OrderedKeys preChunk = preShiftModified.getNextOrderedKeysWithLength(CHUNK_SIZE);
                Assert.eq(preChunk.size(), "preChunk.size()", postChunk.size(), "postChunk.size()");

                final StateTrackingCallback callback = (cookie, postSlot, index, prevIndex) -> {
                    long preSlot = rightIndexToSlot.get(prevIndex);

                    final boolean preIsOverflow = isOverflowLocation(preSlot);
                    final LongArraySource preCookieSource = preIsOverflow ? overflowModifiedTrackerCookieSource : modifiedTrackerCookieSource;
                    final long preHashSlot = preIsOverflow ? overflowLocationToHashLocation(preSlot) : preSlot;

                    if (preSlot != postSlot) {
                        if (preSlot != EMPTY_RIGHT_SLOT) {
                            final long oldCookie = preCookieSource.getUnsafe(preHashSlot);
                            final long newCookie = tracker.appendChunkRemove(oldCookie, preSlot, prevIndex);
                            if (oldCookie != newCookie) {
                                preCookieSource.set(preHashSlot, newCookie);
                            }
                            rightIndexToSlot.removeVoid(prevIndex);
                        }
                        if (postSlot != EMPTY_RIGHT_SLOT) {
                            cookie = tracker.appendChunkAdd(cookie, postSlot, index);
                        }
                    } else if (preSlot != EMPTY_RIGHT_SLOT) {
                        // note we must mark postShiftIndex as the modification
                        cookie = tracker.appendChunkModify(cookie, postSlot, index);
                    }
                    return cookie;
                };

                if (isLeftTicking) {
                    buildTable(bc, postChunk, rightKeySources, tracker, preChunk, callback);
                } else {
                    final boolean usePrevOnPost = false;
                    decorationProbe(pc, postChunk, rightKeySources, usePrevOnPost, preChunk, callback);
                }
            }
        }
    }

    void leftRemoved(final ReadOnlyIndex removed, final CrossJoinModifiedSlotTracker tracker) {
        if (removed.nonempty()) {
            try (final ProbeContext pc = makeProbeContext(leftKeySources, removed.size())) {
                final boolean usePrev = true;
                decorationProbe(pc, removed, leftKeySources, usePrev, null, (cookie, slot, index, prevIndex) -> {
                    leftIndexToSlot.removeVoid(index);
                    long newCookie = tracker.appendToBuilder(cookie, slot, index);
                    if (isOverflowLocation(slot)) {
                        overflowModifiedTrackerCookieSource.set(overflowLocationToHashLocation(slot), newCookie);
                    } else {
                        modifiedTrackerCookieSource.set(slot, newCookie);
                    }
                    return newCookie;
                });
            }
        }
        tracker.flushLeftRemoves();
    }


    void leftAdded(final ReadOnlyIndex added, final CrossJoinModifiedSlotTracker tracker) {
        if (added.nonempty()) {
            try (final BuildContext pc = makeBuildContext(leftKeySources, added.size())) {
                buildTable(pc, added, leftKeySources, tracker, null, (cookie, slot, index, prevIndex) -> {
                    ensureSlotExists(slot);
                    long newCookie = tracker.appendToBuilder(cookie, slot, index);
                    if (isOverflowLocation(slot)) {
                        overflowModifiedTrackerCookieSource.set(overflowLocationToHashLocation(slot), newCookie);
                    } else {
                        modifiedTrackerCookieSource.set(slot, newCookie);
                    }
                    return newCookie;
                });
            }
        }
        tracker.flushLeftAdds();
    }

    void leftModified(final ShiftAwareListener.Update upstream, final boolean keyColumnsChanged, final CrossJoinModifiedSlotTracker tracker) {
        if (upstream.modified.isEmpty()) {
            tracker.flushLeftModifies();
            return;
        }

        if (!keyColumnsChanged) {
            try (final ProbeContext pc = makeProbeContext(leftKeySources, upstream.modified.size())) {
                final boolean usePrev = false;
                decorationProbe(pc, upstream.modified, leftKeySources, usePrev, null,
                        (cookie, slot, index, prevIndex) -> tracker.appendChunkModify(cookie, slot, index));
            }
            tracker.flushLeftModifies();
            return;
        }

        // note: at this point left shifts have not yet been applied to our internal data structures
        try (final BuildContext bc = makeBuildContext(leftKeySources, upstream.modified.size());
             final OrderedKeys.Iterator preShiftModified = upstream.getModifiedPreShift().getOrderedKeysIterator();
             final OrderedKeys.Iterator postShiftModified = upstream.modified.getOrderedKeysIterator()) {

            while (preShiftModified.hasMore()) {
                final OrderedKeys preChunk = preShiftModified.getNextOrderedKeysWithLength(CHUNK_SIZE);
                final OrderedKeys postChunk = postShiftModified.getNextOrderedKeysWithLength(CHUNK_SIZE);
                Assert.eq(preChunk.size(), "preChunk.size()", postChunk.size(), "postChunk.size()");

                final StateTrackingCallback callback = (cookie, postSlot, index, prevIndex) -> {
                    long preSlot = leftIndexToSlot.get(prevIndex);

                    final boolean preIsOverflow = isOverflowLocation(preSlot);
                    final LongArraySource preCookieSource = preIsOverflow ? overflowModifiedTrackerCookieSource : modifiedTrackerCookieSource;
                    final long preHashSlot = preIsOverflow ? overflowLocationToHashLocation(preSlot) : preSlot;

                    if (preSlot != postSlot) {
                        // unlike rightModified, leftIndexToSlot is not yet shifted; so we operate in terms of pre-shift value.
                        if (preSlot != EMPTY_RIGHT_SLOT) {
                            final long oldCookie = preCookieSource.getUnsafe(preHashSlot);
                            final long newCookie = tracker.appendToBuilder(oldCookie, preSlot, prevIndex);
                            if (oldCookie != newCookie) {
                                preCookieSource.set(preHashSlot, newCookie);
                            }
                        }
                        if (postSlot != EMPTY_RIGHT_SLOT) {
                            cookie = tracker.appendChunkAdd(cookie, postSlot, index);
                        } else {
                            leftIndexToSlot.removeVoid(prevIndex);
                        }
                    } else if (preSlot != EMPTY_RIGHT_SLOT) {
                        // note we must mark post shift index as the modification
                        cookie = tracker.appendChunkModify(cookie, postSlot, index);
                    }
                    return cookie;
                };

                buildTable(bc, postChunk, leftKeySources, tracker, preChunk, callback);
            }
        }
        tracker.flushLeftModifies();
    }

    void leftShift(final ReadOnlyIndex filterIndex, final IndexShiftData shifted, final CrossJoinModifiedSlotTracker tracker) {
        shifted.forAllInIndex(filterIndex, (ii, delta) -> {
            final long slot = leftIndexToSlot.get(ii);
            if (slot == Index.NULL_KEY) {
                // This might happen if an index is moving from one slot to another; we shift after removes but before
                // the adds. We don't need to shift the slot that was related to this index.
                return;
            }

            final long cookie;
            if (isOverflowLocation(slot)) {
                cookie = overflowModifiedTrackerCookieSource.getUnsafe(overflowLocationToHashLocation(slot));
            } else {
                cookie = modifiedTrackerCookieSource.getUnsafe(slot);
            }

            long newCookie = tracker.needsLeftShift(cookie, slot);
            if (newCookie != cookie) {
                if (isOverflowLocation(slot)) {
                    overflowModifiedTrackerCookieSource.set(overflowLocationToHashLocation(slot), newCookie);
                } else {
                    modifiedTrackerCookieSource.set(slot, newCookie);
                }
            }
        });
        leftIndexToSlot.applyShift(filterIndex, shifted);
    }

    private void ensureSlotExists(final long slot) {
        final boolean isOverflowLocation = isOverflowLocation(slot);
        final long location = isOverflowLocation ? overflowLocationToHashLocation(slot) : slot;
        final ObjectArraySource<Index> source = isOverflowLocation ? overflowRightIndexSource : rightIndexSource;

        final Index index = source.getUnsafe(location);
        if (index == null) {
            source.set(location, Index.FACTORY.getEmptyIndex());
        }
    }

    private long addToIndex(final boolean isLeft, final long slot, final long keyToAdd) {
        final boolean isOverflowLocation = isOverflowLocation(slot);
        final long location = isOverflowLocation ? overflowLocationToHashLocation(slot) : slot;
        final ObjectArraySource<Index> source;
        if (isOverflowLocation) {
            source = isLeft ? overflowLeftIndexSource : overflowRightIndexSource;
        } else {
            source = isLeft ? leftIndexSource : rightIndexSource;
        }

        final long size;
        final Index index = source.get(location);
        if (index == null) {
            if (!isLeft && !isLeftTicking) {
                // when left table is static and this grouping does not exist then ignore
                return CrossJoinModifiedSlotTracker.NULL_COOKIE;
            }

            source.set(location, Index.FACTORY.getIndexByValues(keyToAdd));
            size = 1;
        } else {
            index.insert(keyToAdd);
            size = index.size();
        }

        if (isLeft) {
            leftIndexToSlot.put(keyToAdd, slot);

            // ensure a right mapping exists, or else it will appear that this mapping is non-existent
            ensureSlotExists(slot);
        } else {
            rightIndexToSlot.put(keyToAdd, slot);

            // only right side insertions can cause shifts
            final int numBitsNeeded = CrossJoinShiftState.getMinBits(size - 1);
            if (numBitsNeeded > getNumShiftBits()) {
                setNumShiftBits(numBitsNeeded);
            }
            if (size > maxRightGroupSize) {
                maxRightGroupSize = size;
            }
        }

        return CrossJoinModifiedSlotTracker.NULL_COOKIE;
    }

    private void moveModifiedSlot(final CrossJoinModifiedSlotTracker modifiedSlotTracker, final long oldTableLocation, final long tableLocation) {
        if (modifiedSlotTracker != null) {
            final long cookie = modifiedTrackerCookieSource.getUnsafe(oldTableLocation);
            modifiedSlotTracker.moveTableLocation(cookie, tableLocation);
            modifiedTrackerCookieSource.set(tableLocation, cookie);
        }
    }

    private void promoteModifiedSlot(final CrossJoinModifiedSlotTracker modifiedSlotTracker, final long overflowLocation, final long tableLocation) {
        if (modifiedSlotTracker != null) {
            final long cookie = overflowModifiedTrackerCookieSource.getUnsafe(overflowLocation);
            modifiedSlotTracker.moveTableLocation(cookie, tableLocation);
            modifiedTrackerCookieSource.set(tableLocation, cookie);
        }
    }

    void startTrackingPrevValues() {
        this.leftIndexToSlot.startTrackingPrevValues();
        this.rightIndexSource.startTrackingPrevValues();
        this.overflowRightIndexSource.startTrackingPrevValues();
    }

    public void updateLeftRedirectionIndex(Index leftAdded, long slotLocation) {
        if (slotLocation == Index.NULL_KEY) {
            leftAdded.forAllLongs(leftIndexToSlot::removeVoid);
        } else {
            leftAdded.forAllLongs(ii -> leftIndexToSlot.putVoid(ii, slotLocation));
        }
    }

    public void onRightGroupInsertion(Index rightIndex, Index rightAdded, long slotLocation) {
        // only right side insertions can cause shifts
        final long size = rightIndex.size();
        final int numBitsNeeded = CrossJoinShiftState.getMinBits(size - 1);
        if (numBitsNeeded > getNumShiftBits()) {
            setNumShiftBitsAndUpdatePrev(numBitsNeeded);
        }
        if (size > maxRightGroupSize) {
            maxRightGroupSize = size;
        }
        rightAdded.forAllLongs(ii -> rightIndexToSlot.putVoid(ii, slotLocation));
    }
    // endregion build wrappers

    class BuildContext implements Context {
        final int chunkSize;

        final LongIntTimsortKernel.LongIntSortKernelContext sortContext;
        final ColumnSource.FillContext stateSourceFillContext;
        // mixin rehash
        final ColumnSource.FillContext overflowStateSourceFillContext;
        // endmixin rehash
        final ColumnSource.FillContext overflowFillContext;
        final ColumnSource.FillContext overflowOverflowFillContext;

        // the chunk of hashcodes
        final WritableIntChunk<HashCode> hashChunk;
        // the chunk of positions within our table
        final WritableLongChunk<KeyIndices> tableLocationsChunk;

        final ResettableWritableChunk<Values>[] writeThroughChunks = getResettableWritableKeyChunks();
        final WritableIntChunk<ChunkPositions> sourcePositions;
        final WritableIntChunk<ChunkPositions> destinationLocationPositionInWriteThrough;

        final WritableBooleanChunk<Any> filledValues;
        final WritableBooleanChunk<Any> equalValues;

        // the overflow locations that we need to get from the overflowLocationSource (or overflowOverflowLocationSource)
        final WritableLongChunk<KeyIndices> overflowLocationsToFetch;
        // the overflow position in the working key chunks, parallel to the overflowLocationsToFetch
        final WritableIntChunk<ChunkPositions> overflowPositionInSourceChunk;

        // the position with our hash table that we should insert a value into
        final WritableLongChunk<KeyIndices> insertTableLocations;
        // the position in our chunk, parallel to the workingChunkInsertTablePositions
        final WritableIntChunk<ChunkPositions> insertPositionsInSourceChunk;

        // we sometimes need to check two positions within a single chunk for equality, this contains those positions as pairs
        final WritableIntChunk<ChunkPositions> chunkPositionsToCheckForEquality;
        // While processing overflow insertions, parallel to the chunkPositions to check for equality, the overflow location that
        // is represented by the first of the pairs in chunkPositionsToCheckForEquality
        final WritableLongChunk<KeyIndices> overflowLocationForEqualityCheck;

        // the chunk of state values that we read from the hash table
        // @WritableStateChunkType@ from \QWritableObjectChunk<Index,Values>\E
        final WritableObjectChunk<Index,Values> workingStateEntries;

        // the chunks for getting key values from the hash table
        final WritableChunk<Values>[] workingKeyChunks;
        final WritableChunk<Values>[] overflowKeyChunks;

        // when fetching from the overflow, we record which chunk position we are fetching for
        final WritableIntChunk<ChunkPositions> chunkPositionsForFetches;
        // which positions in the chunk we are inserting into the overflow
        final WritableIntChunk<ChunkPositions> chunkPositionsToInsertInOverflow;
        // which table locations we are inserting into the overflow
        final WritableLongChunk<ChunkPositions> tableLocationsToInsertInOverflow;

        // values we have read from the overflow locations sources
        final WritableIntChunk<Values> overflowLocations;

        // mixin rehash
        final WritableLongChunk<KeyIndices> rehashLocations;
        final WritableIntChunk<Values> overflowLocationsToMigrate;
        final WritableLongChunk<KeyIndices> overflowLocationsAsKeyIndices;
        final WritableBooleanChunk<Any> shouldMoveBucket;

        final ResettableWritableLongChunk<Any> overflowLocationForPromotionLoop = ResettableWritableLongChunk.makeResettableChunk();

        final ResettableWritableIntChunk<Values> writeThroughOverflowLocations = ResettableWritableIntChunk.makeResettableChunk();
        // endmixin rehash

        final SharedContext sharedFillContext;
        final ColumnSource.FillContext[] workingFillContexts;
        final SharedContext sharedOverflowContext;
        final ColumnSource.FillContext[] overflowContexts;
        final SharedContext sharedBuildContext;
        final ChunkSource.GetContext[] buildContexts;

        // region build context
        final WritableLongChunk<OrderedKeyIndices> sourceIndexKeys;
        final WritableLongChunk<OrderedKeyIndices> sourcePrevIndexKeys;
        // endregion build context

        final boolean haveSharedContexts;

        private BuildContext(ColumnSource<?>[] buildSources,
                            int chunkSize
                             // region build context constructor args
                            // endregion build context constructor args
                            ) {
            Assert.gtZero(chunkSize, "chunkSize");
            this.chunkSize = chunkSize;
            haveSharedContexts = buildSources.length > 1;
            if (haveSharedContexts) {
                sharedFillContext = SharedContext.makeSharedContext();
                sharedOverflowContext = SharedContext.makeSharedContext();
                sharedBuildContext = SharedContext.makeSharedContext();
            } else {
                // no point in the additional work implied by these not being null.
                sharedFillContext = null;
                sharedOverflowContext = null;
                sharedBuildContext = null;
            }
            workingFillContexts = makeFillContexts(keySources, sharedFillContext, chunkSize);
            overflowContexts = makeFillContexts(overflowKeySources, sharedOverflowContext, chunkSize);
            buildContexts = makeGetContexts(buildSources, sharedBuildContext, chunkSize);
            // region build context constructor
            sourceIndexKeys = WritableLongChunk.makeWritableChunk(chunkSize);
            sourcePrevIndexKeys = WritableLongChunk.makeWritableChunk(chunkSize);
            // endregion build context constructor
            sortContext = LongIntTimsortKernel.createContext(chunkSize);
            stateSourceFillContext = rightIndexSource.makeFillContext(chunkSize);
            overflowFillContext = overflowLocationSource.makeFillContext(chunkSize);
            overflowOverflowFillContext = overflowOverflowLocationSource.makeFillContext(chunkSize);
            hashChunk = WritableIntChunk.makeWritableChunk(chunkSize);
            tableLocationsChunk = WritableLongChunk.makeWritableChunk(chunkSize);
            sourcePositions = WritableIntChunk.makeWritableChunk(chunkSize);
            destinationLocationPositionInWriteThrough = WritableIntChunk.makeWritableChunk(chunkSize);
            filledValues = WritableBooleanChunk.makeWritableChunk(chunkSize);
            equalValues = WritableBooleanChunk.makeWritableChunk(chunkSize);
            overflowLocationsToFetch = WritableLongChunk.makeWritableChunk(chunkSize);
            overflowPositionInSourceChunk = WritableIntChunk.makeWritableChunk(chunkSize);
            insertTableLocations = WritableLongChunk.makeWritableChunk(chunkSize);
            insertPositionsInSourceChunk = WritableIntChunk.makeWritableChunk(chunkSize);
            chunkPositionsToCheckForEquality = WritableIntChunk.makeWritableChunk(chunkSize * 2);
            overflowLocationForEqualityCheck = WritableLongChunk.makeWritableChunk(chunkSize);
            // @WritableStateChunkName@ from \QWritableObjectChunk\E
            workingStateEntries = WritableObjectChunk.makeWritableChunk(chunkSize);
            workingKeyChunks = getWritableKeyChunks(chunkSize);
            overflowKeyChunks = getWritableKeyChunks(chunkSize);
            chunkPositionsForFetches = WritableIntChunk.makeWritableChunk(chunkSize);
            chunkPositionsToInsertInOverflow = WritableIntChunk.makeWritableChunk(chunkSize);
            tableLocationsToInsertInOverflow = WritableLongChunk.makeWritableChunk(chunkSize);
            overflowLocations = WritableIntChunk.makeWritableChunk(chunkSize);
            // mixin rehash
            rehashLocations = WritableLongChunk.makeWritableChunk(chunkSize);
            overflowStateSourceFillContext = overflowRightIndexSource.makeFillContext(chunkSize);
            overflowLocationsToMigrate = WritableIntChunk.makeWritableChunk(chunkSize);
            overflowLocationsAsKeyIndices = WritableLongChunk.makeWritableChunk(chunkSize);
            shouldMoveBucket = WritableBooleanChunk.makeWritableChunk(chunkSize);
            // endmixin rehash
        }

        private void resetSharedContexts() {
            if (!haveSharedContexts) {
                return;
            }
            sharedFillContext.reset();
            sharedOverflowContext.reset();
            sharedBuildContext.reset();
        }

        private void closeSharedContexts() {
            if (!haveSharedContexts) {
                return;
            }
            sharedFillContext.close();
            sharedOverflowContext.close();
            sharedBuildContext.close();
        }

        @Override
        public void close() {
            sortContext.close();
            stateSourceFillContext.close();
            // mixin rehash
            overflowStateSourceFillContext.close();
            // endmixin rehash
            overflowFillContext.close();
            overflowOverflowFillContext.close();
            closeArray(workingFillContexts);
            closeArray(overflowContexts);
            closeArray(buildContexts);

            hashChunk.close();
            tableLocationsChunk.close();
            closeArray(writeThroughChunks);

            sourcePositions.close();
            destinationLocationPositionInWriteThrough.close();
            filledValues.close();
            equalValues.close();
            overflowLocationsToFetch.close();
            overflowPositionInSourceChunk.close();
            insertTableLocations.close();
            insertPositionsInSourceChunk.close();
            chunkPositionsToCheckForEquality.close();
            overflowLocationForEqualityCheck.close();
            workingStateEntries.close();
            closeArray(workingKeyChunks);
            closeArray(overflowKeyChunks);
            chunkPositionsForFetches.close();
            chunkPositionsToInsertInOverflow.close();
            tableLocationsToInsertInOverflow.close();
            overflowLocations.close();
            // mixin rehash
            rehashLocations.close();
            overflowLocationsToMigrate.close();
            overflowLocationsAsKeyIndices.close();
            shouldMoveBucket.close();
            overflowLocationForPromotionLoop.close();
            writeThroughOverflowLocations.close();
            // endmixin rehash
            // region build context close
            sourceIndexKeys.close();
            sourcePrevIndexKeys.close();
            // endregion build context close
            closeSharedContexts();
        }

    }

    BuildContext makeBuildContext(ColumnSource<?>[] buildSources,
                                  long maxSize
                                  // region makeBuildContext args
                                  // endregion makeBuildContext args
    ) {
        return new BuildContext(buildSources, (int)Math.min(CHUNK_SIZE, maxSize)
                // region makeBuildContext arg pass
                // endregion makeBuildContext arg pass
        );
    }

    private void buildTable(final BuildContext bc,
                            final OrderedKeys buildIndex,
                            ColumnSource<?>[] buildSources
                            // region extra build arguments
            , final CrossJoinModifiedSlotTracker modifiedSlotTracker
            , final OrderedKeys prevIndex
            , final StateTrackingCallback trackingCallback
                            // endregion extra build arguments
    ) {
        long hashSlotOffset = 0;
        // region build start
        // endregion build start

        try (final OrderedKeys.Iterator okIt = buildIndex.getOrderedKeysIterator();
             // region build initialization try
             // endregion build initialization try
        ) {
            // region build initialization
            final OrderedKeys.Iterator prevOkIt = prevIndex == null ? null : prevIndex.getOrderedKeysIterator();
            if (prevIndex != null) {
                Assert.eq(prevIndex.size(), "prevIndex.size()", buildIndex.size(), "buildIndex.size()");
            }
            // endregion build initialization

            // chunks to write through to the table key sources


            //noinspection unchecked
            final Chunk<Values> [] sourceKeyChunks = new Chunk[buildSources.length];

            while (okIt.hasMore()) {
                // we reset early to avoid carrying around state for old OrderedKeys which can't be reused.
                bc.resetSharedContexts();

                final OrderedKeys chunkOk = okIt.getNextOrderedKeysWithLength(bc.chunkSize);

                getKeyChunks(buildSources, bc.buildContexts, sourceKeyChunks, chunkOk);
                hashKeyChunks(bc.hashChunk, sourceKeyChunks);

                // region build loop initialization
                chunkOk.fillKeyIndicesChunk(bc.sourceIndexKeys);
                if (prevIndex != null) {
                    prevOkIt.getNextOrderedKeysWithLength(bc.chunkSize).fillKeyIndicesChunk(bc.sourcePrevIndexKeys);
                }
                // endregion build loop initialization

                // turn hash codes into indices within our table
                convertHashToTableLocations(bc.hashChunk, bc.tableLocationsChunk);

                // now fetch the values from the table, note that we do not order these fetches
                fillKeys(bc.workingFillContexts, bc.workingKeyChunks, bc.tableLocationsChunk);

                // and the corresponding states, if a value is null, we've found our insertion point
                rightIndexSource.fillChunkUnordered(bc.stateSourceFillContext, bc.workingStateEntries, bc.tableLocationsChunk);

                // find things that exist
                // @StateChunkIdentityName@ from \QObjectChunkIdentity\E
                ObjectChunkIdentityEquals.notEqual(bc.workingStateEntries, EMPTY_RIGHT_VALUE, bc.filledValues);

                // to be equal, the location must exist; and each of the keyChunks must match
                bc.equalValues.setSize(bc.filledValues.size());
                bc.equalValues.copyFromChunk(bc.filledValues, 0, 0, bc.filledValues.size());
                checkKeyEquality(bc.equalValues, bc.workingKeyChunks, sourceKeyChunks);

                bc.overflowPositionInSourceChunk.setSize(0);
                bc.overflowLocationsToFetch.setSize(0);
                bc.insertPositionsInSourceChunk.setSize(0);
                bc.insertTableLocations.setSize(0);

                for (int ii = 0; ii < bc.equalValues.size(); ++ii) {
                    final long tableLocation = bc.tableLocationsChunk.get(ii);
                    if (bc.equalValues.get(ii)) {
                        // region build found main
                        final long keyToAdd = bc.sourceIndexKeys.get(ii);
                        final long prevKey = prevIndex == null ? Index.NULL_KEY : bc.sourcePrevIndexKeys.get(ii);
                        final long oldCookie = modifiedTrackerCookieSource.getUnsafe(tableLocation);
                        final long newCookie = trackingCallback.invoke(oldCookie, (int) tableLocation, keyToAdd, prevKey);
                        if (oldCookie != newCookie) {
                            modifiedTrackerCookieSource.set(tableLocation, newCookie);
                        }
                        // endregion build found main
                    } else if (bc.filledValues.get(ii)) {
                        // we must handle this as part of the overflow bucket
                        bc.overflowPositionInSourceChunk.add(ii);
                        bc.overflowLocationsToFetch.add(tableLocation);
                    } else {
                        // for the values that are empty, we record them in the insert chunks
                        bc.insertPositionsInSourceChunk.add(ii);
                        bc.insertTableLocations.add(tableLocation);
                    }
                }

                // we first sort by position; so that we'll not insert things into the table twice or overwrite
                // collisions
                LongIntTimsortKernel.sort(bc.sortContext, bc.insertPositionsInSourceChunk, bc.insertTableLocations);

                // the first and last valid table location in our writeThroughChunks
                long firstBackingChunkLocation = -1;
                long lastBackingChunkLocation = -1;

                bc.chunkPositionsToCheckForEquality.setSize(0);
                bc.destinationLocationPositionInWriteThrough.setSize(0);
                bc.sourcePositions.setSize(0);

                for (int ii = 0; ii < bc.insertPositionsInSourceChunk.size(); ) {
                    final int firstChunkPositionForHashLocation = bc.insertPositionsInSourceChunk.get(ii);
                    final long currentHashLocation = bc.insertTableLocations.get(ii);

                    // region main insert
                    final long keyToAdd = bc.sourceIndexKeys.get(firstChunkPositionForHashLocation);
                    final long prevKey = prevIndex == null ? Index.NULL_KEY : bc.sourcePrevIndexKeys.get(firstChunkPositionForHashLocation);
                    ensureSlotExists(currentHashLocation);
                    final long cookie = trackingCallback.invoke(CrossJoinModifiedSlotTracker.NULL_COOKIE, currentHashLocation, keyToAdd, prevKey);
                    modifiedTrackerCookieSource.set(currentHashLocation, cookie);
                    // endregion main insert
                    // mixin rehash
                    numEntries++;
                    // endmixin rehash

                    if (currentHashLocation > lastBackingChunkLocation) {
                        flushWriteThrough(bc.sourcePositions, sourceKeyChunks, bc.destinationLocationPositionInWriteThrough, bc.writeThroughChunks);
                        firstBackingChunkLocation = updateWriteThroughChunks(bc.writeThroughChunks, currentHashLocation, keySources);
                        lastBackingChunkLocation = firstBackingChunkLocation + bc.writeThroughChunks[0].size() - 1;
                    }

                    bc.sourcePositions.add(firstChunkPositionForHashLocation);
                    bc.destinationLocationPositionInWriteThrough.add((int)(currentHashLocation - firstBackingChunkLocation));

                    final int currentHashValue = bc.hashChunk.get(firstChunkPositionForHashLocation);

                    while (++ii < bc.insertTableLocations.size() && bc.insertTableLocations.get(ii) == currentHashLocation) {
                        // if this thing is equal to the first one; we should mark the appropriate slot, we don't
                        // know the types and don't want to make the virtual calls, so we need to just accumulate
                        // the things to check for equality afterwards
                        final int chunkPosition = bc.insertPositionsInSourceChunk.get(ii);
                        if (bc.hashChunk.get(chunkPosition) != currentHashValue) {
                            // we must be an overflow
                            bc.overflowPositionInSourceChunk.add(chunkPosition);
                            bc.overflowLocationsToFetch.add(currentHashLocation);
                        } else {
                            // we need to check equality, equal things are the same slot; unequal things are overflow
                            bc.chunkPositionsToCheckForEquality.add(firstChunkPositionForHashLocation);
                            bc.chunkPositionsToCheckForEquality.add(chunkPosition);
                        }
                    }
                }

                flushWriteThrough(bc.sourcePositions, sourceKeyChunks, bc.destinationLocationPositionInWriteThrough, bc.writeThroughChunks);

                checkPairEquality(bc.chunkPositionsToCheckForEquality, sourceKeyChunks, bc.equalValues);

                for (int ii = 0; ii < bc.equalValues.size(); ii++) {
                    final int chunkPosition = bc.chunkPositionsToCheckForEquality.get(ii * 2 + 1);
                    final long tableLocation = bc.tableLocationsChunk.get(chunkPosition);

                    if (bc.equalValues.get(ii)) {
                        // region build main duplicate
                        final long keyToAdd = bc.sourceIndexKeys.get(chunkPosition);
                        final long prevKey = prevIndex == null ? Index.NULL_KEY : bc.sourcePrevIndexKeys.get(chunkPosition);
                        // NB: We just inserted this slot, so there's no way that its cookie can have changed.
                        trackingCallback.invoke(modifiedTrackerCookieSource.getUnsafe(tableLocation), (int) tableLocation, keyToAdd, prevKey);
                        // endregion build main duplicate
                    } else {
                        // we are an overflow element
                        bc.overflowPositionInSourceChunk.add(chunkPosition);
                        bc.overflowLocationsToFetch.add(tableLocation);
                    }
                }

                // now handle overflow
                if (bc.overflowPositionInSourceChunk.size() > 0) {
                    // on the first pass we fill from the table's locations
                    overflowLocationSource.fillChunkUnordered(bc.overflowFillContext, bc.overflowLocations, bc.overflowLocationsToFetch);
                    bc.chunkPositionsToInsertInOverflow.setSize(0);
                    bc.tableLocationsToInsertInOverflow.setSize(0);

                    // overflow slots now contains the positions in the overflow columns

                    while (bc.overflowPositionInSourceChunk.size() > 0) {
                        // now we have the overflow slot for each of the things we are interested in.
                        // if the slot is null, then we can insert it and we are complete.

                        bc.overflowLocationsToFetch.setSize(0);
                        bc.chunkPositionsForFetches.setSize(0);

                        // TODO: Crunch it down
                        for (int ii = 0; ii < bc.overflowLocations.size(); ++ii) {
                            final int overflowLocation = bc.overflowLocations.get(ii);
                            final int chunkPosition = bc.overflowPositionInSourceChunk.get(ii);
                            if (overflowLocation == QueryConstants.NULL_INT) {
                                // insert me into overflow in the next free overflow slot
                                bc.chunkPositionsToInsertInOverflow.add(chunkPosition);
                                bc.tableLocationsToInsertInOverflow.add(bc.tableLocationsChunk.get(chunkPosition));
                            } else {
                                // add to the key positions to fetch
                                bc.chunkPositionsForFetches.add(chunkPosition);
                                bc.overflowLocationsToFetch.add(overflowLocation);
                            }
                        }

                        // if the slot is non-null, then we need to fetch the overflow values for comparison
                        fillOverflowKeys(bc.overflowContexts, bc.overflowKeyChunks, bc.overflowLocationsToFetch);

                        // now compare the value in our overflowKeyChunk to the value in the sourceChunk
                        checkLhsPermutedEquality(bc.chunkPositionsForFetches, sourceKeyChunks, bc.overflowKeyChunks, bc.equalValues);

                        int writePosition = 0;
                        for (int ii = 0; ii < bc.equalValues.size(); ++ii) {
                            final int chunkPosition = bc.chunkPositionsForFetches.get(ii);
                            final long overflowLocation = bc.overflowLocationsToFetch.get(ii);
                            if (bc.equalValues.get(ii)) {
                                // region build overflow found
                                final long keyToAdd = bc.sourceIndexKeys.get(chunkPosition);
                                final long prevKey = prevIndex == null ? Index.NULL_KEY : bc.sourcePrevIndexKeys.get(chunkPosition);
                                final long hashLocation = overflowLocationToHashLocation(overflowLocation);
                                final long oldCookie = overflowModifiedTrackerCookieSource.getUnsafe(overflowLocation);
                                final long newCookie = trackingCallback.invoke(oldCookie, hashLocation, keyToAdd, prevKey);
                                if (oldCookie != newCookie) {
                                    overflowModifiedTrackerCookieSource.set(overflowLocation, newCookie);
                                }
                                // endregion build overflow found
                            } else {
                                // otherwise, we need to repeat the overflow calculation, with our next overflow fetch
                                bc.overflowLocationsToFetch.set(writePosition, overflowLocation);
                                bc.overflowPositionInSourceChunk.set(writePosition++, chunkPosition);
                            }
                        }
                        bc.overflowLocationsToFetch.setSize(writePosition);
                        bc.overflowPositionInSourceChunk.setSize(writePosition);

                        // on subsequent iterations, we are following the overflow chains, so we fill from the overflowOverflowLocationSource
                        if (bc.overflowPositionInSourceChunk.size() > 0) {
                            overflowOverflowLocationSource.fillChunkUnordered(bc.overflowOverflowFillContext, bc.overflowLocations, bc.overflowLocationsToFetch);
                        }
                    }

                    // make sure we actually have enough room to insert stuff where we would like
                    ensureOverflowCapacity(bc.chunkPositionsToInsertInOverflow);

                    firstBackingChunkLocation = -1;
                    lastBackingChunkLocation = -1;
                    bc.destinationLocationPositionInWriteThrough.setSize(0);
                    bc.sourcePositions.setSize(0);

                    // do the overflow insertions, one per table position at a time; until we have no insertions left
                    while (bc.chunkPositionsToInsertInOverflow.size() > 0) {
                        // sort by table position
                        LongIntTimsortKernel.sort(bc.sortContext, bc.chunkPositionsToInsertInOverflow, bc.tableLocationsToInsertInOverflow);

                        bc.chunkPositionsToCheckForEquality.setSize(0);
                        bc.overflowLocationForEqualityCheck.setSize(0);

                        for (int ii = 0; ii < bc.chunkPositionsToInsertInOverflow.size(); ) {
                            final long tableLocation = bc.tableLocationsToInsertInOverflow.get(ii);
                            final int chunkPosition = bc.chunkPositionsToInsertInOverflow.get(ii);

                            final int allocatedOverflowLocation = allocateOverflowLocation();

                            // we are inserting into the head of the list, so we move the existing overflow into our overflow
                            overflowOverflowLocationSource.set(allocatedOverflowLocation, overflowLocationSource.getUnsafe(tableLocation));
                            // and we point the overflow at our slot
                            overflowLocationSource.set(tableLocation, allocatedOverflowLocation);

                            // region build overflow insert
                            final long keyToAdd = bc.sourceIndexKeys.get(chunkPosition);
                            final long prevKey = prevIndex == null ? Index.NULL_KEY : bc.sourcePrevIndexKeys.get(chunkPosition);
                            final long hashLocation = overflowLocationToHashLocation(allocatedOverflowLocation);
                            ensureSlotExists(hashLocation);
                            final long cookie = trackingCallback.invoke(CrossJoinModifiedSlotTracker.NULL_COOKIE, hashLocation, keyToAdd, prevKey);
                            overflowModifiedTrackerCookieSource.set(allocatedOverflowLocation, cookie);
                            // endregion build overflow insert

                            // mixin rehash
                            numEntries++;
                            // endmixin rehash

                            // get the backing chunk from the overflow keys
                            if (allocatedOverflowLocation > lastBackingChunkLocation || allocatedOverflowLocation < firstBackingChunkLocation) {
                                flushWriteThrough(bc.sourcePositions, sourceKeyChunks, bc.destinationLocationPositionInWriteThrough, bc.writeThroughChunks);
                                firstBackingChunkLocation = updateWriteThroughChunks(bc.writeThroughChunks, allocatedOverflowLocation, overflowKeySources);
                                lastBackingChunkLocation = firstBackingChunkLocation + bc.writeThroughChunks[0].size() - 1;
                            }

                            // now we must set all of our key values in the overflow
                            bc.sourcePositions.add(chunkPosition);
                            bc.destinationLocationPositionInWriteThrough.add((int)(allocatedOverflowLocation - firstBackingChunkLocation));

                            while (++ii < bc.tableLocationsToInsertInOverflow.size() && bc.tableLocationsToInsertInOverflow.get(ii) == tableLocation) {
                                bc.overflowLocationForEqualityCheck.add(allocatedOverflowLocation);
                                bc.chunkPositionsToCheckForEquality.add(chunkPosition);
                                bc.chunkPositionsToCheckForEquality.add(bc.chunkPositionsToInsertInOverflow.get(ii));
                            }
                        }

                        // now we need to do the equality check; so that we can mark things appropriately
                        int remainingInserts = 0;

                        checkPairEquality(bc.chunkPositionsToCheckForEquality, sourceKeyChunks, bc.equalValues);
                        for (int ii = 0; ii < bc.equalValues.size(); ii++) {
                            final int chunkPosition = bc.chunkPositionsToCheckForEquality.get(ii * 2 + 1);
                            final long tableLocation = bc.tableLocationsChunk.get(chunkPosition);

                            if (bc.equalValues.get(ii)) {
                                final long insertedOverflowLocation = bc.overflowLocationForEqualityCheck.get(ii);
                                // region build overflow duplicate
                                final long keyToAdd = bc.sourceIndexKeys.get(chunkPosition);
                                final long prevKey = prevIndex == null ? Index.NULL_KEY : bc.sourcePrevIndexKeys.get(chunkPosition);
                                final long hashLocation = overflowLocationToHashLocation(insertedOverflowLocation);
                                // we match the first element, so should use the overflow slow we allocated for it (note expect cookie does not change)
                                trackingCallback.invoke(overflowModifiedTrackerCookieSource.getUnsafe(insertedOverflowLocation), hashLocation, keyToAdd, prevKey);
                                // endregion build overflow duplicate
                            } else {
                                // we need to try this element again in the next round
                                bc.chunkPositionsToInsertInOverflow.set(remainingInserts, chunkPosition);
                                bc.tableLocationsToInsertInOverflow.set(remainingInserts++, tableLocation);
                            }
                        }

                        bc.chunkPositionsToInsertInOverflow.setSize(remainingInserts);
                        bc.tableLocationsToInsertInOverflow.setSize(remainingInserts);
                    }
                    flushWriteThrough(bc.sourcePositions, sourceKeyChunks, bc.destinationLocationPositionInWriteThrough, bc.writeThroughChunks);
                    // mixin rehash
                    // region post-build rehash
                    doRehash(bc, modifiedSlotTracker);
                    // endregion post-build rehash
                    // endmixin rehash
                }

                // region copy hash slots
                // endregion copy hash slots
                hashSlotOffset += chunkOk.size();
            }
            // region post build loop
            if (prevIndex != null) {
                prevOkIt.close();
            }
            // endregion post build loop
        }
    }

    // mixin rehash
    public void doRehash(BuildContext bc
                          // region extra rehash arguments
                          , CrossJoinModifiedSlotTracker modifiedSlotTracker
                          // endregion extra rehash arguments
    ) {
        long firstBackingChunkLocation;
        long lastBackingChunkLocation;// mixin rehash
                    // region rehash start
        // endregion rehash start
        while (rehashRequired()) {
                        // region rehash loop start
            // endregion rehash loop start
            if (tableHashPivot == tableSize) {
                tableSize *= 2;
                ensureCapacity(tableSize);
                            // region rehash ensure capacity
                // endregion rehash ensure capacity
            }

            final long targetBuckets = Math.min(MAX_TABLE_SIZE, (long)(numEntries / targetLoadFactor));
            final int bucketsToAdd = Math.max(1, (int)Math.min(Math.min(targetBuckets, tableSize) - tableHashPivot, bc.chunkSize));

            initializeRehashLocations(bc.rehashLocations, bucketsToAdd);

            // fill the overflow bucket locations
            overflowLocationSource.fillChunk(bc.overflowFillContext, bc.overflowLocations, OrderedKeys.wrapKeyIndicesChunkAsOrderedKeys(LongChunk.downcast(bc.rehashLocations)));
            // null out the overflow locations in the table
            setOverflowLocationsToNull(tableHashPivot - (tableSize >> 1), bucketsToAdd);

            while (bc.overflowLocations.size() > 0) {
                // figure out which table location each overflow location maps to
                compactOverflowLocations(bc.overflowLocations, bc.overflowLocationsToFetch);
                if (bc.overflowLocationsToFetch.size() == 0) {
                    break;
                }

                fillOverflowKeys(bc.overflowContexts, bc.workingKeyChunks, bc.overflowLocationsToFetch);
                hashKeyChunks(bc.hashChunk, bc.workingKeyChunks);
                convertHashToTableLocations(bc.hashChunk, bc.tableLocationsChunk, tableHashPivot + bucketsToAdd);

                // read the next chunk of overflow locations, which we will be overwriting in the next step
                overflowOverflowLocationSource.fillChunkUnordered(bc.overflowOverflowFillContext, bc.overflowLocations, bc.overflowLocationsToFetch);

                // swap the table's overflow pointer with our location
                swapOverflowPointers(bc.tableLocationsChunk, bc.overflowLocationsToFetch);
            }

            // now rehash the main entries

            rightIndexSource.fillChunkUnordered(bc.stateSourceFillContext, bc.workingStateEntries, bc.rehashLocations);
            // @StateChunkIdentityName@ from \QObjectChunkIdentity\E
            ObjectChunkIdentityEquals.notEqual(bc.workingStateEntries, EMPTY_RIGHT_VALUE, bc.shouldMoveBucket);

            // crush down things that don't exist
            LongCompactKernel.compact(bc.rehashLocations, bc.shouldMoveBucket);

            // get the keys from the table
            fillKeys(bc.workingFillContexts, bc.workingKeyChunks, bc.rehashLocations);
            hashKeyChunks(bc.hashChunk, bc.workingKeyChunks);
            convertHashToTableLocations(bc.hashChunk, bc.tableLocationsChunk, tableHashPivot + bucketsToAdd);

            // figure out which ones must move
            LongChunkEquals.notEqual(bc.tableLocationsChunk, bc.rehashLocations, bc.shouldMoveBucket);

            firstBackingChunkLocation = -1;
            lastBackingChunkLocation = -1;
            // flushWriteThrough will have zero-ed out the sourcePositions and destinationLocationPositionInWriteThrough size

            int moves = 0;
            for (int ii = 0; ii < bc.shouldMoveBucket.size(); ++ii) {
                if (bc.shouldMoveBucket.get(ii)) {
                    moves++;
                    final long newHashLocation = bc.tableLocationsChunk.get(ii);
                    final long oldHashLocation = bc.rehashLocations.get(ii);

                    if (newHashLocation > lastBackingChunkLocation) {
                        flushWriteThrough(bc.sourcePositions, bc.workingKeyChunks, bc.destinationLocationPositionInWriteThrough, bc.writeThroughChunks);
                        firstBackingChunkLocation = updateWriteThroughChunks(bc.writeThroughChunks, newHashLocation, keySources);
                        lastBackingChunkLocation = firstBackingChunkLocation + bc.writeThroughChunks[0].size() - 1;
                    }

                    // @StateValueType@ from \QIndex\E
                    final Index stateValueToMove = rightIndexSource.getUnsafe(oldHashLocation);
                    rightIndexSource.set(newHashLocation, stateValueToMove);
                    rightIndexSource.set(oldHashLocation, EMPTY_RIGHT_VALUE);
                    // region rehash move values
                    final Index leftIndexValue = leftIndexSource.getUnsafe(oldHashLocation);
                    leftIndexSource.set(newHashLocation, leftIndexValue);
                    leftIndexSource.set(oldHashLocation, null);
                    if (leftIndexValue != null) {
                        leftIndexValue.forAllLongs(left -> leftIndexToSlot.putVoid(left, newHashLocation));
                    }
                    stateValueToMove.forAllLongs(right -> {
                        // stateValueToMove may not yet be up to date and may include modifications
                        final long prevLocation = rightIndexToSlot.get(right);
                        if (prevLocation == oldHashLocation) {
                            rightIndexToSlot.putVoid(right, newHashLocation);
                        }
                    });
                    moveModifiedSlot(modifiedSlotTracker, oldHashLocation, newHashLocation);
                    // endregion rehash move values

                    bc.sourcePositions.add(ii);
                    bc.destinationLocationPositionInWriteThrough.add((int)(newHashLocation - firstBackingChunkLocation));
                }
            }
            flushWriteThrough(bc.sourcePositions, bc.workingKeyChunks, bc.destinationLocationPositionInWriteThrough, bc.writeThroughChunks);

            // everything has been rehashed now, but we have some table locations that might have an overflow,
            // without actually having a main entry.  We walk through the empty main entries, pulling non-empty
            // overflow locations into the main table

            // figure out which of the two possible locations is empty, because (1) we moved something from it
            // or (2) we did not move something to it
            bc.overflowLocationsToFetch.setSize(bc.shouldMoveBucket.size());
            final int totalPromotionsToProcess = bc.shouldMoveBucket.size();
            createOverflowPartitions(bc.overflowLocationsToFetch, bc.rehashLocations, bc.shouldMoveBucket, moves);

            for (int loop = 0; loop < 2; loop++) {
                final boolean firstLoop = loop == 0;

                if (firstLoop) {
                    bc.overflowLocationForPromotionLoop.resetFromTypedChunk(bc.overflowLocationsToFetch, 0, moves);
                } else {
                    bc.overflowLocationForPromotionLoop.resetFromTypedChunk(bc.overflowLocationsToFetch, moves, totalPromotionsToProcess - moves);
                }

                overflowLocationSource.fillChunk(bc.overflowFillContext, bc.overflowLocations, OrderedKeys.wrapKeyIndicesChunkAsOrderedKeys(bc.overflowLocationForPromotionLoop));
                IntChunkEquals.notEqual(bc.overflowLocations, QueryConstants.NULL_INT, bc.shouldMoveBucket);

                // crunch the chunk down to relevant locations
                LongCompactKernel.compact(bc.overflowLocationForPromotionLoop, bc.shouldMoveBucket);
                IntCompactKernel.compact(bc.overflowLocations, bc.shouldMoveBucket);

                IntToLongCast.castInto(IntChunk.downcast(bc.overflowLocations), bc.overflowLocationsAsKeyIndices);

                // now fetch the overflow key values
                fillOverflowKeys(bc.overflowContexts, bc.workingKeyChunks, bc.overflowLocationsAsKeyIndices);
                // and their state values
                overflowRightIndexSource.fillChunkUnordered(bc.overflowStateSourceFillContext, bc.workingStateEntries, bc.overflowLocationsAsKeyIndices);
                // and where their next pointer is
                overflowOverflowLocationSource.fillChunkUnordered(bc.overflowOverflowFillContext, bc.overflowLocationsToMigrate, bc.overflowLocationsAsKeyIndices);

                // we'll have two sorted regions intermingled in the overflowLocationsToFetch, one of them is before the pivot, the other is after the pivot
                // so that we can use our write through chunks, we first process the things before the pivot; then have a separate loop for those
                // that go after
                firstBackingChunkLocation = -1;
                lastBackingChunkLocation = -1;

                for (int ii = 0; ii < bc.overflowLocationForPromotionLoop.size(); ++ii) {
                    final long tableLocation = bc.overflowLocationForPromotionLoop.get(ii);
                    if ((firstLoop && tableLocation < tableHashPivot) || (!firstLoop && tableLocation >= tableHashPivot)) {
                        if (tableLocation > lastBackingChunkLocation) {
                            if (bc.sourcePositions.size() > 0) {
                                // the permutes here are flushing the write through for the state and overflow locations

                                IntPermuteKernel.permute(bc.sourcePositions, bc.overflowLocationsToMigrate, bc.destinationLocationPositionInWriteThrough, bc.writeThroughOverflowLocations);
                                flushWriteThrough(bc.sourcePositions, bc.workingKeyChunks, bc.destinationLocationPositionInWriteThrough, bc.writeThroughChunks);
                            }

                            firstBackingChunkLocation = updateWriteThroughChunks(bc.writeThroughChunks, tableLocation, keySources);
                            lastBackingChunkLocation = firstBackingChunkLocation + bc.writeThroughChunks[0].size() - 1;
                            updateWriteThroughOverflow(bc.writeThroughOverflowLocations, firstBackingChunkLocation, lastBackingChunkLocation);
                        }
                        bc.sourcePositions.add(ii);
                        bc.destinationLocationPositionInWriteThrough.add((int)(tableLocation - firstBackingChunkLocation));
                        // region promotion move
                        final long overflowLocation = bc.overflowLocationsAsKeyIndices.get(ii);
                        final long overflowHashLocation = overflowLocationToHashLocation(overflowLocation);
                        // Right index source move
                        final Index stateValueToMove = overflowRightIndexSource.getUnsafe(overflowLocation);
                        rightIndexSource.set(tableLocation, stateValueToMove);
                        stateValueToMove.forAllLongs(right -> {
                            // stateValueToMove may not yet be up to date and may include modifications
                            final long prevLocation = rightIndexToSlot.get(right);
                            if (prevLocation == overflowHashLocation) {
                                rightIndexToSlot.putVoid(right, tableLocation);
                            }
                        });
                        overflowRightIndexSource.set(overflowLocation, EMPTY_RIGHT_VALUE);

                        // Left index source move
                        final Index leftIndexValue = overflowLeftIndexSource.getUnsafe(overflowLocation);
                        leftIndexSource.set(tableLocation, leftIndexValue);
                        overflowLeftIndexSource.set(overflowLocation, null);
                        if (leftIndexValue != null) {
                            leftIndexValue.forAllLongs(left -> leftIndexToSlot.putVoid(left, tableLocation));
                        }

                        // notify tracker and move/update cookie
                        promoteModifiedSlot(modifiedSlotTracker, overflowLocation, tableLocation);
                        // endregion promotion move
                    }
                }

                // the permutes are completing the state and overflow promotions write through
                IntPermuteKernel.permute(bc.sourcePositions, bc.overflowLocationsToMigrate, bc.destinationLocationPositionInWriteThrough, bc.writeThroughOverflowLocations);
                flushWriteThrough(bc.sourcePositions, bc.workingKeyChunks, bc.destinationLocationPositionInWriteThrough, bc.writeThroughChunks);

                // now mark these overflow locations as free, so that we can reuse them
                freeOverflowLocations.ensureCapacity(freeOverflowCount + bc.overflowLocations.size());
                // by sorting them, they will be more likely to be in the same write through chunk when we pull them from the free list
                bc.overflowLocations.sort();
                for (int ii = 0; ii < bc.overflowLocations.size(); ++ii) {
                    freeOverflowLocations.set(freeOverflowCount++, bc.overflowLocations.get(ii));
                }
                nullOverflowObjectSources(bc.overflowLocations);
            }

            tableHashPivot += bucketsToAdd;
                        // region rehash loop end
            // endregion rehash loop end
        }
                    // region rehash final
        // endregion rehash final
    }

    public boolean rehashRequired() {
        return numEntries > (tableHashPivot * maximumLoadFactor) && tableHashPivot < MAX_TABLE_SIZE;
    }

    /**
     * This function can be stuck in for debugging if you are breaking the table to make sure each slot still corresponds
     * to the correct location.
     */
    @SuppressWarnings({"unused", "unchecked"})
    private void verifyKeyHashes() {
        final int maxSize = tableHashPivot;

        final ChunkSource.FillContext [] keyFillContext = makeFillContexts(keySources, SharedContext.makeSharedContext(), maxSize);
        final WritableChunk [] keyChunks = getWritableKeyChunks(maxSize);

        try (final WritableLongChunk<KeyIndices> positions = WritableLongChunk.makeWritableChunk(maxSize);
             final WritableBooleanChunk exists = WritableBooleanChunk.makeWritableChunk(maxSize);
             final WritableIntChunk hashChunk = WritableIntChunk.makeWritableChunk(maxSize);
             final WritableLongChunk<KeyIndices> tableLocationsChunk = WritableLongChunk.makeWritableChunk(maxSize);
             final SafeCloseableArray ignored = new SafeCloseableArray<>(keyFillContext);
             final SafeCloseableArray ignored2 = new SafeCloseableArray<>(keyChunks);
             // @StateChunkName@ from \QObjectChunk\E
             final WritableObjectChunk stateChunk = WritableObjectChunk.makeWritableChunk(maxSize);
             final ChunkSource.FillContext fillContext = rightIndexSource.makeFillContext(maxSize)) {

            rightIndexSource.fillChunk(fillContext, stateChunk, Index.FACTORY.getFlatIndex(tableHashPivot));

            ChunkUtils.fillInOrder(positions);

            // @StateChunkIdentityName@ from \QObjectChunkIdentity\E
            ObjectChunkIdentityEquals.notEqual(stateChunk, EMPTY_RIGHT_VALUE, exists);

            // crush down things that don't exist
            LongCompactKernel.compact(positions, exists);

            // get the keys from the table
            fillKeys(keyFillContext, keyChunks, positions);
            hashKeyChunks(hashChunk, keyChunks);
            convertHashToTableLocations(hashChunk, tableLocationsChunk, tableHashPivot);

            for (int ii = 0; ii < positions.size(); ++ii) {
                if (tableLocationsChunk.get(ii) != positions.get(ii)) {
                    throw new IllegalStateException();
                }
            }
        }
    }

    void setTargetLoadFactor(final double targetLoadFactor) {
        this.targetLoadFactor = targetLoadFactor;
    }

    void setMaximumLoadFactor(final double maximumLoadFactor) {
        this.maximumLoadFactor = maximumLoadFactor;
    }

    private void createOverflowPartitions(WritableLongChunk<KeyIndices> overflowLocationsToFetch, WritableLongChunk<KeyIndices> rehashLocations, WritableBooleanChunk<Any> shouldMoveBucket, int moves) {
        int startWritePosition = 0;
        int endWritePosition = moves;
        for (int ii = 0; ii < shouldMoveBucket.size(); ++ii) {
            if (shouldMoveBucket.get(ii)) {
                final long oldHashLocation = rehashLocations.get(ii);
                // this needs to be promoted, because we moved it
                overflowLocationsToFetch.set(startWritePosition++, oldHashLocation);
            } else {
                // we didn't move anything into the destination slot; so we need to promote its overflow
                final long newEmptyHashLocation = rehashLocations.get(ii) + (tableSize >> 1);
                overflowLocationsToFetch.set(endWritePosition++, newEmptyHashLocation);
            }
        }
    }

    private void setOverflowLocationsToNull(long start, int count) {
        for (int ii = 0; ii < count; ++ii) {
            overflowLocationSource.set(start + ii, QueryConstants.NULL_INT);
        }
    }

    private void initializeRehashLocations(WritableLongChunk<KeyIndices> rehashLocations, int bucketsToAdd) {
        rehashLocations.setSize(bucketsToAdd);
        for (int ii = 0; ii < bucketsToAdd; ++ii) {
            rehashLocations.set(ii, tableHashPivot + ii - (tableSize >> 1));
        }
    }

    private void compactOverflowLocations(IntChunk<Values> overflowLocations, WritableLongChunk<KeyIndices> overflowLocationsToFetch) {
        overflowLocationsToFetch.setSize(0);
        for (int ii = 0; ii < overflowLocations.size(); ++ii) {
            final int overflowLocation = overflowLocations.get(ii);
            if (overflowLocation != QueryConstants.NULL_INT) {
                overflowLocationsToFetch.add(overflowLocation);
            }
        }
    }

    private void swapOverflowPointers(LongChunk<KeyIndices> tableLocationsChunk, LongChunk<KeyIndices> overflowLocationsToFetch) {
        for (int ii = 0; ii < overflowLocationsToFetch.size(); ++ii) {
            final long newLocation = tableLocationsChunk.get(ii);
            final int existingOverflow = overflowLocationSource.getUnsafe(newLocation);
            final long overflowLocation = overflowLocationsToFetch.get(ii);
            overflowOverflowLocationSource.set(overflowLocation, existingOverflow);
            overflowLocationSource.set(newLocation, (int)overflowLocation);
        }
    }


    private void updateWriteThroughOverflow(ResettableWritableIntChunk writeThroughOverflow, long firstPosition, long expectedLastPosition) {
        final long firstBackingChunkPosition = overflowLocationSource.resetWritableChunkToBackingStore(writeThroughOverflow, firstPosition);
        if (firstBackingChunkPosition != firstPosition) {
            throw new IllegalStateException("ArrayBackedColumnSources have different block sizes!");
        }
        if (firstBackingChunkPosition + writeThroughOverflow.size() - 1 != expectedLastPosition) {
            throw new IllegalStateException("ArrayBackedColumnSources have different block sizes!");
        }
    }

    // endmixin rehash

    private int allocateOverflowLocation() {
        // mixin rehash
        if (freeOverflowCount > 0) {
            return freeOverflowLocations.getUnsafe(--freeOverflowCount);
        }
        // endmixin rehash
        return nextOverflowLocation++;
    }

    private static long updateWriteThroughChunks(ResettableWritableChunk<Values>[] writeThroughChunks, long currentHashLocation, ArrayBackedColumnSource<?>[] sources) {
        final long firstBackingChunkPosition = sources[0].resetWritableChunkToBackingStore(writeThroughChunks[0], currentHashLocation);
        for (int jj = 1; jj < sources.length; ++jj) {
            if (sources[jj].resetWritableChunkToBackingStore(writeThroughChunks[jj], currentHashLocation) != firstBackingChunkPosition) {
                throw new IllegalStateException("ArrayBackedColumnSources have different block sizes!");
            }
            if (writeThroughChunks[jj].size() != writeThroughChunks[0].size()) {
                throw new IllegalStateException("ArrayBackedColumnSources have different block sizes!");
            }
        }
        return firstBackingChunkPosition;
    }

    private void flushWriteThrough(WritableIntChunk<ChunkPositions> sourcePositions, Chunk<Values>[] sourceKeyChunks, WritableIntChunk<ChunkPositions> destinationLocationPositionInWriteThrough, WritableChunk<Values>[] writeThroughChunks) {
        if (sourcePositions.size() < 0) {
            return;
        }
        for (int jj = 0; jj < keySources.length; ++jj) {
            chunkCopiers[jj].permute(sourcePositions, sourceKeyChunks[jj], destinationLocationPositionInWriteThrough, writeThroughChunks[jj]);
        }
        sourcePositions.setSize(0);
        destinationLocationPositionInWriteThrough.setSize(0);
    }

    // mixin rehash
    private void nullOverflowObjectSources(IntChunk<Values> locationsToNull) {
        for (ObjectArraySource<?> objectArraySource : overflowKeyColumnsToNull) {
            for (int ii = 0; ii < locationsToNull.size(); ++ii) {
                objectArraySource.set(locationsToNull.get(ii), null);
            }
        }
        // region nullOverflowObjectSources
        for (int ii = 0; ii < locationsToNull.size(); ++ii) {
            overflowRightIndexSource.set(locationsToNull.get(ii), null);
        }
        // endregion nullOverflowObjectSources
    }
    // endmixin rehash

    private void checkKeyEquality(WritableBooleanChunk<Any> equalValues, WritableChunk<Values>[] workingKeyChunks, Chunk<Values>[] sourceKeyChunks) {
        for (int ii = 0; ii < sourceKeyChunks.length; ++ii) {
            chunkEquals[ii].andEqual(workingKeyChunks[ii], sourceKeyChunks[ii], equalValues);
        }
    }

    private void checkLhsPermutedEquality(WritableIntChunk<ChunkPositions> chunkPositionsForFetches, Chunk<Values>[] sourceKeyChunks, WritableChunk<Values>[] overflowKeyChunks, WritableBooleanChunk<Any> equalValues) {
        chunkEquals[0].equalLhsPermuted(chunkPositionsForFetches, sourceKeyChunks[0], overflowKeyChunks[0], equalValues);
        for (int ii = 1; ii < overflowKeySources.length; ++ii) {
            chunkEquals[ii].andEqualLhsPermuted(chunkPositionsForFetches, sourceKeyChunks[ii], overflowKeyChunks[ii], equalValues);
        }
    }

    private void checkPairEquality(WritableIntChunk<ChunkPositions> chunkPositionsToCheckForEquality, Chunk<Values>[] sourceKeyChunks, WritableBooleanChunk<Any> equalPairs) {
        chunkEquals[0].equalPairs(chunkPositionsToCheckForEquality, sourceKeyChunks[0], equalPairs);
        for (int ii = 1; ii < keyColumnCount; ++ii) {
            chunkEquals[ii].andEqualPairs(chunkPositionsToCheckForEquality, sourceKeyChunks[ii], equalPairs);
        }
    }

    private void fillKeys(ColumnSource.FillContext[] fillContexts, WritableChunk<Values>[] keyChunks, WritableLongChunk<KeyIndices> tableLocationsChunk) {
        fillKeys(keySources, fillContexts, keyChunks, tableLocationsChunk);
    }

    private void fillOverflowKeys(ColumnSource.FillContext[] fillContexts, WritableChunk<Values>[] keyChunks, WritableLongChunk<KeyIndices> overflowLocationsChunk) {
        fillKeys(overflowKeySources, fillContexts, keyChunks, overflowLocationsChunk);
    }

    private static void fillKeys(ArrayBackedColumnSource<?>[] keySources, ColumnSource.FillContext[] fillContexts, WritableChunk<Values>[] keyChunks, WritableLongChunk<KeyIndices> keyIndices) {
        for (int ii = 0; ii < keySources.length; ++ii) {
            keySources[ii].fillChunkUnordered(fillContexts[ii], keyChunks[ii], keyIndices);
        }
    }

    private void hashKeyChunks(WritableIntChunk<HashCode> hashChunk, Chunk<Values>[] sourceKeyChunks) {
        chunkHashers[0].hashInitial(sourceKeyChunks[0], hashChunk);
        for (int ii = 1; ii < sourceKeyChunks.length; ++ii) {
            chunkHashers[ii].hashUpdate(sourceKeyChunks[ii], hashChunk);
        }
    }

    private void getKeyChunks(ColumnSource<?>[] sources, ColumnSource.GetContext[] contexts, Chunk<? extends Values>[] chunks, OrderedKeys orderedKeys) {
        for (int ii = 0; ii < chunks.length; ++ii) {
            chunks[ii] = sources[ii].getChunk(contexts[ii], orderedKeys);
        }
    }

    // mixin prev
    private void getPrevKeyChunks(ColumnSource<?>[] sources, ColumnSource.GetContext[] contexts, Chunk<? extends Values>[] chunks, OrderedKeys orderedKeys) {
        for (int ii = 0; ii < chunks.length; ++ii) {
            chunks[ii] = sources[ii].getPrevChunk(contexts[ii], orderedKeys);
        }
    }
    // endmixin prev

    // region probe wrappers
    // endregion probe wrappers

    // mixin decorationProbe
    class ProbeContext implements Context {
        final int chunkSize;

        final ColumnSource.FillContext stateSourceFillContext;
        final ColumnSource.FillContext overflowFillContext;
        final ColumnSource.FillContext overflowOverflowFillContext;

        final SharedContext sharedFillContext;
        final ColumnSource.FillContext[] workingFillContexts;
        final SharedContext sharedOverflowContext;
        final ColumnSource.FillContext[] overflowContexts;

        // the chunk of hashcodes
        final WritableIntChunk<HashCode> hashChunk;
        // the chunk of positions within our table
        final WritableLongChunk<KeyIndices> tableLocationsChunk;

        // the chunk of right indices that we read from the hash table, the empty right index is used as a sentinel that the
        // state exists; otherwise when building from the left it is always null
        // @WritableStateChunkType@ from \QWritableObjectChunk<Index,Values>\E
        final WritableObjectChunk<Index,Values> workingStateEntries;

        // the overflow locations that we need to get from the overflowLocationSource (or overflowOverflowLocationSource)
        final WritableLongChunk<KeyIndices> overflowLocationsToFetch;
        // the overflow position in the working keychunks, parallel to the overflowLocationsToFetch
        final WritableIntChunk<ChunkPositions> overflowPositionInWorkingChunk;
        // values we have read from the overflow locations sources
        final WritableIntChunk<Values> overflowLocations;
        // when fetching from the overflow, we record which chunk position we are fetching for
        final WritableIntChunk<ChunkPositions> chunkPositionsForFetches;

        final WritableBooleanChunk<Any> equalValues;
        final WritableChunk<Values>[] workingKeyChunks;

        final SharedContext sharedProbeContext;
        // the contexts for filling from our key columns
        final ChunkSource.GetContext[] probeContexts;

        // region probe context

        // the chunk of indices created from our OrderedKeys, used to write into the hash table
        final WritableLongChunk<OrderedKeyIndices> keyIndices;
        final WritableLongChunk<OrderedKeyIndices> prevKeyIndices;

        // endregion probe context
        final boolean haveSharedContexts;

        private ProbeContext(ColumnSource<?>[] probeSources,
                             int chunkSize
                             // region probe context constructor args
                             // endregion probe context constructor args
                            ) {
            Assert.gtZero(chunkSize, "chunkSize");
            this.chunkSize = chunkSize;
            haveSharedContexts = probeSources.length > 1;
            if (haveSharedContexts) {
                sharedFillContext = SharedContext.makeSharedContext();
                sharedOverflowContext = SharedContext.makeSharedContext();
                sharedProbeContext = SharedContext.makeSharedContext();
            } else {
                // No point in the additional work implied by these being non null.
                sharedFillContext = null;
                sharedOverflowContext = null;
                sharedProbeContext = null;
            }
            workingFillContexts = makeFillContexts(keySources, sharedFillContext, chunkSize);
            overflowContexts = makeFillContexts(overflowKeySources, sharedOverflowContext, chunkSize);
            probeContexts = makeGetContexts(probeSources, sharedProbeContext, chunkSize);
            // region probe context constructor
            keyIndices = WritableLongChunk.makeWritableChunk(chunkSize);
            prevKeyIndices = WritableLongChunk.makeWritableChunk(chunkSize);
            // endregion probe context constructor
            stateSourceFillContext = rightIndexSource.makeFillContext(chunkSize);
            overflowFillContext = overflowLocationSource.makeFillContext(chunkSize);
            overflowOverflowFillContext = overflowOverflowLocationSource.makeFillContext(chunkSize);
            hashChunk = WritableIntChunk.makeWritableChunk(chunkSize);
            tableLocationsChunk = WritableLongChunk.makeWritableChunk(chunkSize);
            // @WritableStateChunkName@ from \QWritableObjectChunk\E
            workingStateEntries = WritableObjectChunk.makeWritableChunk(chunkSize);
            overflowLocationsToFetch = WritableLongChunk.makeWritableChunk(chunkSize);
            overflowPositionInWorkingChunk = WritableIntChunk.makeWritableChunk(chunkSize);
            overflowLocations = WritableIntChunk.makeWritableChunk(chunkSize);
            chunkPositionsForFetches = WritableIntChunk.makeWritableChunk(chunkSize);
            equalValues = WritableBooleanChunk.makeWritableChunk(chunkSize);
            workingKeyChunks = getWritableKeyChunks(chunkSize);
        }

        private void resetSharedContexts() {
            if (!haveSharedContexts) {
                return;
            }
            sharedFillContext.reset();
            sharedOverflowContext.reset();
            sharedProbeContext.reset();
        }

        private void closeSharedContexts() {
            if (!haveSharedContexts) {
                return;
            }
            sharedFillContext.close();
            sharedOverflowContext.close();
            sharedProbeContext.close();
        }

        @Override
        public void close() {
            stateSourceFillContext.close();
            overflowFillContext.close();
            overflowOverflowFillContext.close();
            closeArray(workingFillContexts);
            closeArray(overflowContexts);
            closeArray(probeContexts);
            hashChunk.close();
            tableLocationsChunk.close();
            workingStateEntries.close();
            overflowLocationsToFetch.close();
            overflowPositionInWorkingChunk.close();
            overflowLocations.close();
            chunkPositionsForFetches.close();
            equalValues.close();
            closeArray(workingKeyChunks);
            closeSharedContexts();
            // region probe context close
            keyIndices.close();
            prevKeyIndices.close();
            // endregion probe context close
            closeSharedContexts();
        }
    }

    ProbeContext makeProbeContext(ColumnSource<?>[] probeSources,
                                  long maxSize
                                  // region makeProbeContext args
                                  // endregion makeProbeContext args
    ) {
        return new ProbeContext(probeSources, (int)Math.min(maxSize, CHUNK_SIZE)
                // region makeProbeContext arg pass
                // endregion makeProbeContext arg pass
        );
    }

    private void decorationProbe(ProbeContext pc
                                , OrderedKeys probeIndex
                                , final ColumnSource<?>[] probeSources
                                 // mixin prev
                                , boolean usePrev
                                 // endmixin prev
                                 // region additional probe arguments
            , OrderedKeys probePrevIndex
            , final StateTrackingCallback trackingCallback
                                 // endregion additional probe arguments
    )  {
        // region probe start
        // endregion probe start
        long hashSlotOffset = 0;

        try (final OrderedKeys.Iterator okIt = probeIndex.getOrderedKeysIterator();
             // region probe additional try resources
             final OrderedKeys.Iterator prevOkIt = probePrevIndex == null ? null : probePrevIndex.getOrderedKeysIterator()
             // endregion probe additional try resources
            ) {
            //noinspection unchecked
            final Chunk<Values> [] sourceKeyChunks = new Chunk[keyColumnCount];

            // region probe initialization
            // endregion probe initialization

            while (okIt.hasMore()) {
                // we reset shared contexts early to avoid carrying around state that can't be reused.
                pc.resetSharedContexts();
                final OrderedKeys chunkOk = okIt.getNextOrderedKeysWithLength(pc.chunkSize);
                final int chunkSize = chunkOk.intSize();

                // region probe loop initialization
                pc.keyIndices.setSize(chunkSize);
                chunkOk.fillKeyIndicesChunk(pc.keyIndices);
                if (prevOkIt != null) {
                    pc.prevKeyIndices.setSize(chunkSize);
                    prevOkIt.getNextOrderedKeysWithLength(pc.chunkSize).fillKeyIndicesChunk(pc.prevKeyIndices);
                }
                // endregion probe loop initialization

                // get our keys, hash them, and convert them to table locations
                // mixin prev
                if (usePrev) {
                    getPrevKeyChunks(probeSources, pc.probeContexts, sourceKeyChunks, chunkOk);
                } else {
                    // endmixin prev
                    getKeyChunks(probeSources, pc.probeContexts, sourceKeyChunks, chunkOk);
                    // mixin prev
                }
                // endmixin prev
                hashKeyChunks(pc.hashChunk, sourceKeyChunks);
                convertHashToTableLocations(pc.hashChunk, pc.tableLocationsChunk);

                // get the keys from the table
                fillKeys(pc.workingFillContexts, pc.workingKeyChunks, pc.tableLocationsChunk);

                // and the corresponding states
                // - if a value is empty; we don't care about it
                // - otherwise we check for equality; if we are equal, we have found our thing to set
                //   (or to complain if we are already set)
                // - if we are not equal, then we are an overflow block
                rightIndexSource.fillChunkUnordered(pc.stateSourceFillContext, pc.workingStateEntries, pc.tableLocationsChunk);

                // @StateChunkIdentityName@ from \QObjectChunkIdentity\E
                ObjectChunkIdentityEquals.notEqual(pc.workingStateEntries, EMPTY_RIGHT_VALUE, pc.equalValues);
                checkKeyEquality(pc.equalValues, pc.workingKeyChunks, sourceKeyChunks);

                pc.overflowPositionInWorkingChunk.setSize(0);
                pc.overflowLocationsToFetch.setSize(0);

                for (int ii = 0; ii < pc.equalValues.size(); ++ii) {
                    if (pc.equalValues.get(ii)) {
                        // region probe main found
                        final long tableLocation = pc.tableLocationsChunk.get(ii);
                        final long prevKey = probePrevIndex == null ? Index.NULL_KEY : pc.prevKeyIndices.get(ii);
                        final long oldCookie = modifiedTrackerCookieSource.getUnsafe(tableLocation);
                        final long newCookie = trackingCallback.invoke(oldCookie, tableLocation, pc.keyIndices.get(ii), prevKey);
                        if (oldCookie != newCookie) {
                            modifiedTrackerCookieSource.set(tableLocation, newCookie);
                        }
                        // endregion probe main found
                    } else if (pc.workingStateEntries.get(ii) != EMPTY_RIGHT_VALUE) {
                        // we must handle this as part of the overflow bucket
                        pc.overflowPositionInWorkingChunk.add(ii);
                        pc.overflowLocationsToFetch.add(pc.tableLocationsChunk.get(ii));
                    } else {
                        // region probe main not found
                        if (probePrevIndex != null) {
                            trackingCallback.invoke(CrossJoinModifiedSlotTracker.NULL_COOKIE, EMPTY_RIGHT_SLOT, pc.keyIndices.get(ii), pc.prevKeyIndices.get(ii));
                        }
                        // endregion probe main not found
                    }
                }

                overflowLocationSource.fillChunkUnordered(pc.overflowFillContext, pc.overflowLocations, pc.overflowLocationsToFetch);

                while (pc.overflowLocationsToFetch.size() > 0) {
                    pc.overflowLocationsToFetch.setSize(0);
                    pc.chunkPositionsForFetches.setSize(0);
                    for (int ii = 0; ii < pc.overflowLocations.size(); ++ii) {
                        final int overflowLocation = pc.overflowLocations.get(ii);
                        final int chunkPosition = pc.overflowPositionInWorkingChunk.get(ii);

                        // if the overflow slot is null, this state is not responsive to the join so we can ignore it
                        if (overflowLocation != QueryConstants.NULL_INT) {
                            pc.overflowLocationsToFetch.add(overflowLocation);
                            pc.chunkPositionsForFetches.add(chunkPosition);
                        } else {
                            // region probe overflow not found
                            if (probePrevIndex != null) {
                                trackingCallback.invoke(CrossJoinModifiedSlotTracker.NULL_COOKIE, EMPTY_RIGHT_SLOT,
                                        pc.keyIndices.get(chunkPosition), pc.prevKeyIndices.get(chunkPosition));
                            }
                            // endregion probe overflow not found
                        }
                    }

                    // if the slot is non-null, then we need to fetch the overflow values for comparison
                    fillOverflowKeys(pc.overflowContexts, pc.workingKeyChunks, pc.overflowLocationsToFetch);

                    // region probe overflow state source fill
                    // endregion probe overflow state source fill

                    // now compare the value in our workingKeyChunks to the value in the sourceChunk
                    checkLhsPermutedEquality(pc.chunkPositionsForFetches, sourceKeyChunks, pc.workingKeyChunks, pc.equalValues);

                    // we write back into the overflowLocationsToFetch, so we can't set its size to zero.  Instead
                    // we overwrite the elements in the front of the chunk referenced by a position cursor
                    int overflowPosition = 0;
                    for (int ii = 0; ii < pc.equalValues.size(); ++ii) {
                        final long overflowLocation = pc.overflowLocationsToFetch.get(ii);
                        final int chunkPosition = pc.chunkPositionsForFetches.get(ii);

                        if (pc.equalValues.get(ii)) {
                            // region probe overflow found
                            final long indexKey = pc.keyIndices.get(chunkPosition);
                            final long prevKey = probePrevIndex == null ? Index.NULL_KEY : pc.prevKeyIndices.get(chunkPosition);
                            final long hashLocation = overflowLocationToHashLocation(overflowLocation);
                            final long oldCookie = overflowModifiedTrackerCookieSource.getUnsafe(overflowLocation);
                            final long newCookie = trackingCallback.invoke(oldCookie, hashLocation, indexKey, prevKey);
                            if (oldCookie != newCookie) {
                                overflowModifiedTrackerCookieSource.set(overflowLocation, newCookie);
                            }
                            // endregion probe overflow found
                        } else {
                            // otherwise, we need to repeat the overflow calculation, with our next overflow fetch
                            pc.overflowLocationsToFetch.set(overflowPosition, overflowLocation);
                            pc.overflowPositionInWorkingChunk.set(overflowPosition, chunkPosition);
                            overflowPosition++;
                        }
                    }
                    pc.overflowLocationsToFetch.setSize(overflowPosition);
                    pc.overflowPositionInWorkingChunk.setSize(overflowPosition);

                    overflowOverflowLocationSource.fillChunkUnordered(pc.overflowOverflowFillContext, pc.overflowLocations, pc.overflowLocationsToFetch);
                }

                // region probe complete
                // endregion probe complete
                hashSlotOffset += chunkSize;
            }

            // region probe cleanup
            // endregion probe cleanup
        }
        // region probe final
        // endregion probe final
    }
    // endmixin decorationProbe

    private void convertHashToTableLocations(WritableIntChunk<HashCode> hashChunk, WritableLongChunk<KeyIndices> tablePositionsChunk) {
        // mixin rehash
        // NOTE that this mixin section is a bit ugly, we are spanning the two functions so that we can avoid using tableHashPivot and having the unused pivotPoint parameter
        convertHashToTableLocations(hashChunk, tablePositionsChunk, tableHashPivot);
    }

    private void convertHashToTableLocations(WritableIntChunk<HashCode> hashChunk, WritableLongChunk<KeyIndices> tablePositionsChunk, int pivotPoint) {
        // endmixin rehash

        // turn hash codes into indices within our table
        for (int ii = 0; ii < hashChunk.size(); ++ii) {
            final int hash = hashChunk.get(ii);
            // mixin rehash
            final int location = hashToTableLocation(pivotPoint, hash);
            // endmixin rehash
            // altmixin rehash: final int location = hashToTableLocation(hash);
            tablePositionsChunk.set(ii, location);
        }
        tablePositionsChunk.setSize(hashChunk.size());
    }

    private int hashToTableLocation(
            // mixin rehash
            int pivotPoint,
            // endmixin rehash
            int hash) {
        // altmixin rehash: final \
        int location = hash & (tableSize - 1);
        // mixin rehash
        if (location >= pivotPoint) {
            location -= (tableSize >> 1);
        }
        // endmixin rehash
        return location;
    }

    // region extraction functions
    public Index getRightIndex(long slot) {
        Index retVal;
        if (isOverflowLocation(slot)) {
            retVal = overflowRightIndexSource.get(hashLocationToOverflowLocation(slot));
        } else {
            retVal = rightIndexSource.get(slot);
        }
        if (retVal == null) {
            retVal = Index.FACTORY.getEmptyIndex();
        }
        return retVal;
    }

    public Index getPrevRightIndex(long prevSlot) {
        Index retVal;
        if (isOverflowLocation(prevSlot)) {
            retVal = overflowRightIndexSource.getPrev(hashLocationToOverflowLocation(prevSlot));
        } else {
            retVal = rightIndexSource.getPrev(prevSlot);
        }
        if (retVal == null) {
            retVal = Index.FACTORY.getEmptyIndex();
        }
        return retVal;
    }

    @Override
    public Index getRightIndexFromLeftIndex(long leftIndex) {
        long slot = leftIndexToSlot.get(leftIndex);
        if (slot == Index.NULL_KEY) {
            return Index.FACTORY.getEmptyIndex();
        }
        return getRightIndex(slot);
    }

    @Override
    public Index getRightIndexFromPrevLeftIndex(long leftIndex) {
        long slot = leftIndexToSlot.getPrev(leftIndex);
        if (slot == Index.NULL_KEY) {
            return Index.FACTORY.getEmptyIndex();
        }
        return getPrevRightIndex(slot);
    }

    public Index getLeftIndex(long slot) {
        Index retVal;
        final boolean isOverflow = isOverflowLocation(slot);

        slot = isOverflow ? hashLocationToOverflowLocation(slot) : slot;
        final ObjectArraySource<Index> leftSource = isOverflow ? overflowLeftIndexSource : leftIndexSource;

        retVal = leftSource.get(slot);
        if (retVal == null) {
            retVal = Index.FACTORY.getEmptyIndex();
            if (isLeftTicking) {
                leftSource.set(slot, retVal);
            }
        }
        return retVal;
    }

    public long getTrackerCookie(long slot) {
        if (slot == EMPTY_RIGHT_SLOT) {
            return -1;
        } else if (isOverflowLocation(slot)) {
            return overflowModifiedTrackerCookieSource.getUnsafe(hashLocationToOverflowLocation(slot));
        } else {
            return modifiedTrackerCookieSource.getUnsafe(slot);
        }
    }

    public long getSlotFromLeftIndex(long leftIndex) {
        return leftIndexToSlot.get(leftIndex);
    }

    void clearCookies() {
        for (int si = 0; si < tableSize; ++si) {
            modifiedTrackerCookieSource.set(si, CrossJoinModifiedSlotTracker.NULL_COOKIE);
        }
        for (int osi = 0; osi < nextOverflowLocation; ++ osi) {
            overflowModifiedTrackerCookieSource.set(osi, CrossJoinModifiedSlotTracker.NULL_COOKIE);
        }
    }

    void validateKeySpaceSize() {
        final long leftLastKey = leftTable.getIndex().lastKey();
        final long rightLastKey = maxRightGroupSize - 1;
        final int minLeftBits = CrossJoinShiftState.getMinBits(leftLastKey);
        final int minRightBits = CrossJoinShiftState.getMinBits(rightLastKey);
        final int numShiftBits = getNumShiftBits();
        if (minLeftBits + numShiftBits > 63) {
            throw new OutOfKeySpaceException("join out of index space (left reqBits + right reservedBits > 63): "
                    + "(left table: {size: " + leftTable.getIndex().size() + " maxIndex: " + leftLastKey + " reqBits: " + minLeftBits + "}) X "
                    + "(right table: {maxIndexUsed: " + rightLastKey + " reqBits: " + minRightBits + " reservedBits: " + numShiftBits + "})"
                    + " exceeds Long.MAX_VALUE. Consider flattening left table or reserving fewer right bits if possible.");
        }
    }
    // endregion extraction functions

    @NotNull
    private static ColumnSource.FillContext[] makeFillContexts(ColumnSource<?>[] keySources, final SharedContext sharedContext, int chunkSize) {
        final ColumnSource.FillContext[] workingFillContexts = new ColumnSource.FillContext[keySources.length];
        for (int ii = 0; ii < keySources.length; ++ii) {
            workingFillContexts[ii] = keySources[ii].makeFillContext(chunkSize, sharedContext);
        }
        return workingFillContexts;
    }

    private static ColumnSource.GetContext[] makeGetContexts(ColumnSource<?> [] sources, final SharedContext sharedState, int chunkSize) {
        final ColumnSource.GetContext[] contexts = new ColumnSource.GetContext[sources.length];
        for (int ii = 0; ii < sources.length; ++ii) {
            contexts[ii] = sources[ii].makeGetContext(chunkSize, sharedState);
        }
        return contexts;
    }

    @NotNull
    private WritableChunk<Values>[] getWritableKeyChunks(int chunkSize) {
        //noinspection unchecked
        final WritableChunk<Values>[] workingKeyChunks = new WritableChunk[keyChunkTypes.length];
        for (int ii = 0; ii < keyChunkTypes.length; ++ii) {
            workingKeyChunks[ii] = keyChunkTypes[ii].makeWritableChunk(chunkSize);
        }
        return workingKeyChunks;
    }

    @NotNull
    private ResettableWritableChunk<Values>[] getResettableWritableKeyChunks() {
        //noinspection unchecked
        final ResettableWritableChunk<Values>[] workingKeyChunks = new ResettableWritableChunk[keyChunkTypes.length];
        for (int ii = 0; ii < keyChunkTypes.length; ++ii) {
            workingKeyChunks[ii] = keyChunkTypes[ii].makeResettableWritableChunk();
        }
        return workingKeyChunks;
    }

    // region getStateValue
    // endregion getStateValue

    // region overflowLocationToHashLocation
    private static boolean isOverflowLocation(long hashSlot) {
        return hashSlot < OVERFLOW_PIVOT_VALUE;
    }

    private static long hashLocationToOverflowLocation(long hashSlot) {
        return -hashSlot - 1 + OVERFLOW_PIVOT_VALUE;
    }

    private static long overflowLocationToHashLocation(long overflowSlot) {
        return -overflowSlot - 1 + OVERFLOW_PIVOT_VALUE;
    }
    // endregion overflowLocationToHashLocation


    static int hashTableSize(long initialCapacity) {
        return (int)Math.max(MINIMUM_INITIAL_HASH_SIZE, Math.min(MAX_TABLE_SIZE, Long.highestOneBit(initialCapacity) * 2));
    }

}
