/* ---------------------------------------------------------------------------------------------------------------------
 * AUTO-GENERATED CLASS - DO NOT EDIT MANUALLY - for any changes edit CharacterSparseArraySource and regenerate
 * ------------------------------------------------------------------------------------------------------------------ */
/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */

package io.deephaven.engine.v2.sources;

import io.deephaven.engine.util.BooleanUtils;

import static io.deephaven.engine.util.BooleanUtils.NULL_BOOLEAN_AS_BYTE;


import io.deephaven.engine.structures.chunk.*;
import io.deephaven.engine.structures.chunk.Attributes.Values;
import io.deephaven.engine.structures.chunk.Attributes.KeyIndices;
import io.deephaven.engine.structures.chunk.Attributes.OrderedKeyIndices;
import io.deephaven.engine.structures.chunk.Attributes.OrderedKeyRanges;
import io.deephaven.engine.v2.sources.sparse.ByteOneOrN;
import io.deephaven.engine.v2.sources.sparse.LongOneOrN;
import io.deephaven.engine.structures.rowset.Index;
import io.deephaven.engine.structures.rowsequence.OrderedKeys;
import io.deephaven.engine.v2.utils.UpdateCommitter;
import io.deephaven.util.SoftRecycler;
import gnu.trove.list.array.TLongArrayList;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Arrays;

// region boxing imports
import static io.deephaven.util.QueryConstants.NULL_BOOLEAN;
import static io.deephaven.util.type.TypeUtils.box;
import static io.deephaven.util.type.TypeUtils.unbox;
// endregion boxing imports

import static io.deephaven.engine.v2.sources.sparse.SparseConstants.*;

/**
 * Sparse array source for Boolean.
 * <p>
 * The C-haracterSparseArraySource is replicated to all other types with
 * io.deephaven.engine.v2.sources.Replicate.
 *
 * (C-haracter is deliberately spelled that way in order to prevent Replicate from altering this very comment).
 */
public class BooleanSparseArraySource extends SparseArrayColumnSource<Boolean> implements MutableColumnSourceGetDefaults.ForBoolean {
    // region recyclers
    private static final SoftRecycler<byte[]> recycler = new SoftRecycler<>(DEFAULT_RECYCLER_CAPACITY,
            () -> new byte[BLOCK_SIZE], null);
    private static final SoftRecycler<byte[][]> recycler2 = new SoftRecycler<>(DEFAULT_RECYCLER_CAPACITY,
            () -> new byte[BLOCK2_SIZE][], null);
    private static final SoftRecycler<ByteOneOrN.Block2[]> recycler1 = new SoftRecycler<>(DEFAULT_RECYCLER_CAPACITY,
            () -> new ByteOneOrN.Block2[BLOCK1_SIZE], null);
    private static final SoftRecycler<ByteOneOrN.Block1[]> recycler0 = new SoftRecycler<>(DEFAULT_RECYCLER_CAPACITY,
            () -> new ByteOneOrN.Block1[BLOCK0_SIZE], null);
    // endregion recyclers

    /**
     * The presence of a prevFlusher means that this ArraySource wants to track previous values. If prevFlusher is null,
     * the ArraySource does not want (or does not yet want) to track previous values. Deserialized ArraySources never
     * track previous values.
     */
    protected transient UpdateCommitter<BooleanSparseArraySource> prevFlusher = null;

    /**
     * Our previous page table could be very sparse, and we do not want to read through millions of nulls to find out
     * what blocks to recycle.  Instead we maintain a list of blocks that we have allocated (as the key shifted by
     * BLOCK0_SHIFT).  We recycle those blocks in the PrevFlusher; and accumulate the set of blocks that must be
     * recycled from the next level array, and so on until we recycle the top-level prevBlocks and prevInUse arrays.
     */
    private transient final TLongArrayList blocksToFlush = new TLongArrayList();

    protected ByteOneOrN.Block0 blocks;
    protected transient ByteOneOrN.Block0 prevBlocks;

    // region constructor
    public BooleanSparseArraySource() {
        super(Boolean.class);
        blocks = new ByteOneOrN.Block0();
    }
    // endregion constructor

    // region serialization
    WritableSource reinterpretForSerialization() {
        return (WritableSource)reinterpret(byte.class);
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        final Index.SequentialBuilder sb = Index.FACTORY.getSequentialBuilder();
        blocks.enumerate(NULL_BOOLEAN_AS_BYTE, sb::appendKey);
        final Index index = sb.getIndex();

        final int size = index.intSize();
        final byte[] data = (byte[])new byte[size];
        // noinspection unchecked
        final ColumnSource<Byte> reinterpreted = (ColumnSource<Byte>) reinterpretForSerialization();
        try (final FillContext context = reinterpreted.makeFillContext(size);
             final ResettableWritableByteChunk<Values> destChunk = ResettableWritableByteChunk.makeResettableChunk()) {
            destChunk.resetFromTypedArray(data, 0, size);
            // noinspection unchecked
            reinterpreted.fillChunk(context, destChunk, index);
        }
        out.writeObject(index);
        out.writeObject(data);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        blocks = new ByteOneOrN.Block0();

        final Index index = (Index)in.readObject();
        final byte[] data = (byte[])in.readObject();
        final ByteChunk<Values> srcChunk = ByteChunk.chunkWrap(data);
        // noinspection unchecked
        final WritableSource<Byte> reinterpreted = (WritableSource<Byte>) reinterpretForSerialization();
        try (final FillFromContext context = reinterpreted.makeFillFromContext(index.intSize())) {
            reinterpreted.fillFromChunk(context, srcChunk, index);
        }
    }
    // endregion serialization

    private void readObjectNoData() throws ObjectStreamException {
        throw new StreamCorruptedException();
    }

    @Override
    public void ensureCapacity(long capacity, boolean nullFill) {
        // Nothing to do here. Sparse array sources allocate on-demand and always null-fill.
    }

    @Override
    public final void set(long key, byte value) {
        final int block0 = (int) (key >> BLOCK0_SHIFT) & BLOCK0_MASK;
        final int block1 = (int) (key >> BLOCK1_SHIFT) & BLOCK1_MASK;
        final int block2 = (int) (key >> BLOCK2_SHIFT) & BLOCK2_MASK;
        final int indexWithinBlock = (int) (key & INDEX_MASK);

        final byte [] blocksInner = ensureBlock(block0, block1, block2);
        final byte [] prevBlocksInner = shouldRecordPrevious(key);
        if (prevBlocksInner != null) {
            prevBlocksInner[indexWithinBlock] = blocksInner[indexWithinBlock];
        }
        blocksInner[indexWithinBlock] = value;
    }

    @Override
    public void shift(final Index keysToShift, final long shiftDelta) {
        final Index.SearchIterator it = (shiftDelta > 0) ? keysToShift.reverseIterator() : keysToShift.searchIterator();
        it.forEachLong((i) -> {
            set(i + shiftDelta, getBoolean(i));
            set(i, NULL_BOOLEAN);
            return true;
        });
    }

    @Override
    public void remove(Index toRemove) {
        toRemove.forEachLong((i) -> { set(i, NULL_BOOLEAN); return true; });
    }

    // region boxed methods
    @Override
    public void set(long key, Boolean value) {
        set(key, BooleanUtils.booleanAsByte(value));
    }

    @Override
    public Boolean get(long index) {
        return BooleanUtils.byteAsBoolean(getByte(index));
    }

    @Override
    public Boolean getPrev(long index) {
        return BooleanUtils.byteAsBoolean(getPrevByte(index));
    }
    // endregion boxed methods

    // region copy method
    @Override
    public void copy(ColumnSource<? extends Boolean> sourceColumn, long sourceKey, long destKey) {
        set(destKey, sourceColumn.getBoolean(sourceKey));
    }
    // endregion copy method

    // region primitive get
    @Override
    public final byte getByte(long index) {
        if (index < 0) {
            return NULL_BOOLEAN_AS_BYTE;
        }
        return getByteFromBlock(blocks, index);
    }


    @Override
    public final byte getPrevByte(long index) {
        if (index < 0) {
            return NULL_BOOLEAN_AS_BYTE;
        }
        if (shouldUsePrevious(index)) {
            return getByteFromBlock(prevBlocks, index);
        }

        return getByteFromBlock(blocks, index);
    }

    private byte getByteFromBlock(ByteOneOrN.Block0 blocks, long key) {
        final byte [] blocks2 = blocks.getInnermostBlockByKeyOrNull(key);
        if (blocks2 == null) {
            return NULL_BOOLEAN_AS_BYTE;
        }
        return blocks2[(int)(key & INDEX_MASK)];
    }
    // endregion primitive get

    // region allocateNullFilledBlock
    @SuppressWarnings("SameParameterValue")
    final byte [] allocateNullFilledBlock(int size) {
        final byte [] newBlock = new byte[size];
        Arrays.fill(newBlock, NULL_BOOLEAN_AS_BYTE);
        return newBlock;
    }
    // endregion allocateNullFilledBlock

    /**
     * Make sure that we have an allocated block at the given point, allocating all of the required parents.
     * @return {@code blocks.get(block0).get(block1).get(block2)}, which is non-null.
     */
    byte [] ensureBlock(final int block0, final int block1, final int block2) {
        blocks.ensureIndex(block0, null);
        ByteOneOrN.Block1 blocks0 = blocks.get(block0);
        if (blocks0 == null) {
            blocks.set(block0, blocks0 = new ByteOneOrN.Block1());
        }
        ByteOneOrN.Block2 blocks1 = blocks0.get(block1);
        if (blocks1 == null) {
            blocks0.ensureIndex(block1, null);
            blocks0.set(block1, blocks1 = new ByteOneOrN.Block2());
        }

        byte [] result = blocks1.get(block2);
        if (result == null) {
            blocks1.ensureIndex(block2, null);
            // we do not use the recycler here, because the recycler need not sanitize the block (the inUse recycling
            // does that); yet we would like squeaky clean null filled blocks here.
            result = allocateNullFilledBlock(BLOCK_SIZE);
            blocks1.set(block2, result);
        }
        return result;
    }

    /**
     * Make sure that we have an allocated previous and inuse block at the given point, allocating all of the required
     * parents.
     * @return {@code prevBlocks.get(block0).get(block1).get(block2)}, which is non-null.
     */
    private byte [] ensurePrevBlock(final long key, final int block0, final int block1, final int block2) {
        if (prevBlocks == null) {
            prevBlocks = new ByteOneOrN.Block0();
            prevInUse = new LongOneOrN.Block0();
        }
        prevBlocks.ensureIndex(block0, recycler0);
        prevInUse.ensureIndex(block0, inUse0Recycler);
        ByteOneOrN.Block1 blocks0 = prevBlocks.get(block0);
        final LongOneOrN.Block1 inUse0;
        if (blocks0 == null) {
            prevBlocks.set(block0, blocks0 = new ByteOneOrN.Block1());
            prevInUse.set(block0, inUse0 = new LongOneOrN.Block1());
        } else {
            inUse0 = prevInUse.get(block0);
        }
        ByteOneOrN.Block2 blocks1 = blocks0.get(block1);
        final LongOneOrN.Block2 inUse1;
        if (blocks1 == null) {
            blocks0.ensureIndex(block1, recycler1);
            inUse0.ensureIndex(block1, inUse1Recycler);
            blocks0.set(block1, blocks1 = new ByteOneOrN.Block2());
            inUse0.set(block1, inUse1 = new LongOneOrN.Block2());
        } else {
            inUse1 = inUse0.get(block1);
        }
        byte[] result = blocks1.get(block2);
        if (result == null) {
            blocks1.ensureIndex(block2, recycler2);
            inUse1.ensureIndex(block2, inUse2Recycler);

            blocks1.set(block2, result = recycler.borrowItem());
            inUse1.set(block2, inUseRecycler.borrowItem());

            blocksToFlush.add(key >> BLOCK2_SHIFT);
        }
        return result;
    }

    @Override
    public void startTrackingPrevValues() {
        if (prevFlusher != null) {
            throw new IllegalStateException("Can't call startTrackingPrevValues() twice: " +
                    this.getClass().getCanonicalName());
        }
        prevFlusher = new UpdateCommitter<>(this, BooleanSparseArraySource::commitUpdates);
    }

    private void commitUpdates() {
        blocksToFlush.sort();

        int destinationOffset = 0;
        long lastBlock2Key = -1;

        final ByteOneOrN.Block0 localPrevBlocks = prevBlocks;
        final LongOneOrN.Block0 localPrevInUse = prevInUse;

        // there is no reason to allow these to be used anymore; instead we just null them out so that any
        // getPrev calls will immediately return get().
        prevInUse = null;
        prevBlocks = null;

        // we are clearing out values from block0, block1, block2, block
        // we are accumulating values of block0, block1, block2
        for (int ii = 0; ii < blocksToFlush.size(); ii++) {
            // blockKey = block0 | block1 | block2
            final long blockKey = blocksToFlush.getQuick(ii);
            final long key = blockKey << LOG_BLOCK_SIZE;
            final long block2key = key >> BLOCK1_SHIFT;
            if (block2key != lastBlock2Key) {
                blocksToFlush.set(destinationOffset++, block2key);
                lastBlock2Key = block2key;
            }

            final int block0 = (int) (key >> BLOCK0_SHIFT) & BLOCK0_MASK;
            final int block1 = (int) (key >> BLOCK1_SHIFT) & BLOCK1_MASK;
            final int block2 = (int) (key >> BLOCK2_SHIFT) & BLOCK2_MASK;

            final ByteOneOrN.Block2 blocks1 = localPrevBlocks.get(block0).get(block1);
            final LongOneOrN.Block2 inUse1 = localPrevInUse.get(block0).get(block1);
            final byte [] pb = blocks1.get(block2);
            final long[] inuse = inUse1.get(block2);

            inUse1.set(block2, null);
            blocks1.set(block2, null);

            recycler.returnItem(pb);
            inUseRecycler.returnItem(inuse);
        }

        blocksToFlush.remove(destinationOffset, blocksToFlush.size() - destinationOffset);
        destinationOffset = 0;
        long lastBlock1key = -1;

        // we are clearing out values from block0, block1, block2
        // we are accumulating values of block0, block1
        for (int ii = 0; ii < blocksToFlush.size(); ii++) {
            final long blockKey = blocksToFlush.getQuick(ii);
            // blockKey = block0 | block1
            final long key = blockKey << BLOCK1_SHIFT;
            final long block1Key = key >> BLOCK0_SHIFT;

            if (block1Key != lastBlock1key) {
                blocksToFlush.set(destinationOffset++, block1Key);
                lastBlock1key = block1Key;
            }

            final int block0 = (int) (key >> BLOCK0_SHIFT) & BLOCK0_MASK;
            final int block1 = (int) (key >> BLOCK1_SHIFT) & BLOCK1_MASK;

            final ByteOneOrN.Block1 blocks0 = localPrevBlocks.get(block0);
            final LongOneOrN.Block1 prevs0 = localPrevInUse.get(block0);
            final ByteOneOrN.Block2 pb2 = blocks0.get(block1);
            final LongOneOrN.Block2 inuse = prevs0.get(block1);

            prevs0.set(block1, null);
            blocks0.set(block1, null);

            pb2.maybeRecycle(recycler2);
            inuse.maybeRecycle(inUse2Recycler);
        }

        blocksToFlush.remove(destinationOffset, blocksToFlush.size() - destinationOffset);

        // we are clearing out values from block0, block1
        for (int ii = 0; ii < blocksToFlush.size(); ii++) {
            final int block0 = (int) (blocksToFlush.getQuick(ii)) & BLOCK0_MASK;
            final ByteOneOrN.Block1 pb1 = localPrevBlocks.get(block0);
            final LongOneOrN.Block1 inuse = localPrevInUse.get(block0);

            pb1.maybeRecycle(recycler1);
            inuse.maybeRecycle(inUse1Recycler);

            localPrevInUse.set(block0, null);
            localPrevBlocks.set(block0, null);
        }

        blocksToFlush.clear();

        // and finally recycle the top level block of blocks of blocks of blocks
        localPrevBlocks.maybeRecycle(recycler0);
        localPrevInUse.maybeRecycle(inUse0Recycler);
    }

    /**
    * Decides whether to record the previous value.
    * @param key the index to record
    * @return If the caller should record the previous value, returns prev inner block, the value
    * {@code prevBlocks.get(block0).get(block1).get(block2)}, which is non-null. Otherwise (if the caller should not
     * record values), returns null.
    */
    final byte [] shouldRecordPrevious(final long key) {
        // prevFlusher == null means we are not tracking previous values yet (or maybe ever)
        if (prevFlusher == null) {
            return null;
        }
        // If we want to track previous values, we make sure we are registered with the LiveTableMonitor.
        prevFlusher.maybeActivate();

        final int block0 = (int) (key >> BLOCK0_SHIFT) & BLOCK0_MASK;
        final int block1 = (int) (key >> BLOCK1_SHIFT) & BLOCK1_MASK;
        final int block2 = (int) (key >> BLOCK2_SHIFT) & BLOCK2_MASK;

        final int indexWithinBlock = (int) (key & INDEX_MASK);
        final int indexWithinInUse = indexWithinBlock >> LOG_INUSE_BITSET_SIZE;
        final long maskWithinInUse = 1L << (indexWithinBlock & IN_USE_MASK);

        final byte[] prevBlockInner = ensurePrevBlock(key, block0, block1, block2);
        final long[] inUse = prevInUse.get(block0).get(block1).get(block2);

        // Set value only if not already in use
        if ((inUse[indexWithinInUse] & maskWithinInUse) == 0) {
            inUse[indexWithinInUse] |= maskWithinInUse;
            return prevBlockInner;
        }
        return null;
    }

    /**
     * This method supports the 'getPrev' method for its inheritors, doing some of the 'inUse' housekeeping that is
     * common to all inheritors.
     * @return true if the inheritor should return a value from its "prev" data structure; false if it should return a
     * value from its "current" data structure.
     */
    private boolean shouldUsePrevious(final long index) {
        if (prevFlusher == null) {
            return false;
        }

        if (prevInUse == null) {
            return false;
        }

        final long [] inUse = prevInUse.getInnermostBlockByKeyOrNull(index);
        if (inUse == null) {
            return false;
        }

        final int indexWithinBlock = (int) (index & INDEX_MASK);
        final int indexWithinInUse = indexWithinBlock >> LOG_INUSE_BITSET_SIZE;
        final long maskWithinInUse = 1L << (indexWithinBlock & IN_USE_MASK);

        return (inUse[indexWithinInUse] & maskWithinInUse) != 0;
    }

    // region fillByRanges
    @Override
    void fillByRanges(@NotNull WritableChunk<? super Values> dest, @NotNull OrderedKeys orderedKeys) {
        final WritableObjectChunk<Boolean, ? super Values> chunk = dest.asWritableObjectChunk();
        final FillByContext<byte[]> ctx = new FillByContext<>();
        orderedKeys.forAllLongRanges((long firstKey, final long lastKey) -> {
            if (firstKey > ctx.maxKeyInCurrentBlock) {
                ctx.block = blocks.getInnermostBlockByKeyOrNull(firstKey);
                ctx.maxKeyInCurrentBlock = firstKey | INDEX_MASK;
            }
            while (true) {
                final long rightKeyForThisBlock = Math.min(lastKey, ctx.maxKeyInCurrentBlock);
                final int length = (int) (rightKeyForThisBlock - firstKey + 1);
                if (ctx.block == null) {
                    chunk.fillWithNullValue(ctx.offset, length);
                } else {
                    final int sIndexWithinBlock = (int)(firstKey & INDEX_MASK);
                    // for the benefit of code generation.
                    final int offset = ctx.offset;
                    final byte[] block = ctx.block;
                    // region copyFromTypedArray
                    for (int jj = 0; jj < length; ++jj) {
                         chunk.set(jj + ctx.offset, BooleanUtils.byteAsBoolean(ctx.block[sIndexWithinBlock + jj]));
                    }
                    // endregion copyFromTypedArray
                }
                ctx.offset += length;
                firstKey += length;
                if (firstKey > lastKey) {
                    break;
                }
                ctx.block = blocks.getInnermostBlockByKeyOrNull(firstKey);
                ctx.maxKeyInCurrentBlock = firstKey | INDEX_MASK;
            }
        });
        dest.setSize(ctx.offset);
    }
    // endregion fillByRanges

    // region fillByKeys
    @Override
    void fillByKeys(@NotNull WritableChunk<? super Values> dest, @NotNull OrderedKeys orderedKeys) {
        final WritableObjectChunk<Boolean, ? super Values> chunk = dest.asWritableObjectChunk();
        final FillByContext<byte[]> ctx = new FillByContext<>();
        orderedKeys.forEachLong((final long v) -> {
            if (v > ctx.maxKeyInCurrentBlock) {
                ctx.block = blocks.getInnermostBlockByKeyOrNull(v);
                ctx.maxKeyInCurrentBlock = v | INDEX_MASK;
            }
            if (ctx.block == null) {
                chunk.fillWithNullValue(ctx.offset, 1);
            } else {
                chunk.set(ctx.offset, BooleanUtils.byteAsBoolean(ctx.block[(int) (v & INDEX_MASK)]));
            }
            ++ctx.offset;
            return true;
        });
        dest.setSize(ctx.offset);
    }
    // endregion fillByKeys

    // region fillByUnorderedKeys
    @Override
    void fillByUnorderedKeys(@NotNull WritableChunk<? super Values> dest, @NotNull LongChunk<? extends KeyIndices> keys) {
        final WritableObjectChunk<Boolean, ? super Values> booleanObjectChunk = dest.asWritableObjectChunk();
        for (int ii = 0; ii < keys.size(); ) {
            final long firstKey = keys.get(ii);
            if (firstKey == Index.NULL_KEY) {
                booleanObjectChunk.set(ii++, NULL_BOOLEAN);
                continue;
            }
            final long masked = firstKey & ~INDEX_MASK;
            int lastII = ii;
            while (lastII + 1 < keys.size()) {
                final int nextII = lastII + 1;
                final long nextKey = keys.get(nextII);
                final long nextMasked = nextKey & ~INDEX_MASK;
                if (nextMasked != masked) {
                    break;
                }
                lastII = nextII;
            }
            final byte [] block = blocks.getInnermostBlockByKeyOrNull(firstKey);
            if (block == null) {
                booleanObjectChunk.fillWithNullValue(ii, lastII - ii + 1);
                ii = lastII + 1;
                continue;
            }
            while (ii <= lastII) {
                final int indexWithinBlock = (int) (keys.get(ii) & INDEX_MASK);
                booleanObjectChunk.set(ii++, BooleanUtils.byteAsBoolean(block[indexWithinBlock]));
            }
        }
        dest.setSize(keys.size());
    }

    @Override
    void fillPrevByUnorderedKeys(@NotNull WritableChunk<? super Values> dest, @NotNull LongChunk<? extends KeyIndices> keys) {
        final WritableObjectChunk<Boolean, ? super Values> booleanObjectChunk = dest.asWritableObjectChunk();
        for (int ii = 0; ii < keys.size(); ) {
            final long firstKey = keys.get(ii);
            if (firstKey == Index.NULL_KEY) {
                booleanObjectChunk.set(ii++, NULL_BOOLEAN);
                continue;
            }
            final long masked = firstKey & ~INDEX_MASK;
            int lastII = ii;
            while (lastII + 1 < keys.size()) {
                final int nextII = lastII + 1;
                final long nextKey = keys.get(nextII);
                final long nextMasked = nextKey & ~INDEX_MASK;
                if (nextMasked != masked) {
                    break;
                }
                lastII = nextII;
            }

            final byte [] block = blocks.getInnermostBlockByKeyOrNull(firstKey);
            if (block == null) {
                booleanObjectChunk.fillWithNullValue(ii, lastII - ii + 1);
                ii = lastII + 1;
                continue;
            }

            final long [] prevInUse = (prevFlusher == null || this.prevInUse == null) ? null : this.prevInUse.getInnermostBlockByKeyOrNull(firstKey);
            final byte [] prevBlock = prevInUse == null ? null : prevBlocks.getInnermostBlockByKeyOrNull(firstKey);
            while (ii <= lastII) {
                final int indexWithinBlock = (int) (keys.get(ii) & INDEX_MASK);
                final int indexWithinInUse = indexWithinBlock >> LOG_INUSE_BITSET_SIZE;
                final long maskWithinInUse = 1L << (indexWithinBlock & IN_USE_MASK);

                final byte[] blockToUse = (prevInUse != null && (prevInUse[indexWithinInUse] & maskWithinInUse) != 0) ? prevBlock : block;
                booleanObjectChunk.set(ii++, blockToUse == null ? NULL_BOOLEAN : BooleanUtils.byteAsBoolean(blockToUse[indexWithinBlock]));
            }
        }
        dest.setSize(keys.size());
    }
    // endregion fillByUnorderedKeys

    // region fillFromChunkByRanges
    @Override
    void fillFromChunkByRanges(@NotNull OrderedKeys orderedKeys, Chunk<? extends Values> src) {
        if (orderedKeys.size() == 0) {
            return;
        }
        final ObjectChunk<Boolean, ? extends Values> chunk = src.asObjectChunk();
        final LongChunk<OrderedKeyRanges> ranges = orderedKeys.asKeyRangesChunk();

        final boolean hasPrev = prevFlusher != null;

        if (hasPrev) {
            prevFlusher.maybeActivate();
        }

        int offset = 0;
        // This helps us reduce the number of calls to Chunk.isAlias
        byte[] knownUnaliasedBlock = null;
        for (int ii = 0; ii < ranges.size(); ii += 2) {
            long firstKey = ranges.get(ii);
            final long lastKey = ranges.get(ii + 1);

            while (firstKey <= lastKey) {
                final long maxKeyInCurrentBlock = firstKey | INDEX_MASK;
                final long lastKeyToUse = Math.min(maxKeyInCurrentBlock, lastKey);
                final int length = (int) (lastKeyToUse - firstKey + 1);

                final int block0 = (int) (firstKey >> BLOCK0_SHIFT) & BLOCK0_MASK;
                final int block1 = (int) (firstKey >> BLOCK1_SHIFT) & BLOCK1_MASK;
                final int block2 = (int) (firstKey >> BLOCK2_SHIFT) & BLOCK2_MASK;
                final byte [] block = ensureBlock(block0, block1, block2);

                if (block != knownUnaliasedBlock && chunk.isAlias(block)) {
                    throw new UnsupportedOperationException("Source chunk is an alias for target data");
                }
                knownUnaliasedBlock = block;

                final int sIndexWithinBlock = (int) (firstKey & INDEX_MASK);
                // This 'if' with its constant condition should be very friendly to the branch predictor.
                if (hasPrev) {
                    final byte[] prevBlock = ensurePrevBlock(firstKey, block0, block1, block2);
                    final long[] inUse = prevInUse.get(block0).get(block1).get(block2);

                    assert inUse != null;
                    assert prevBlock != null;

                    for (int jj = 0; jj < length; ++jj) {
                        final int indexWithinBlock = sIndexWithinBlock + jj;
                        final int indexWithinInUse = indexWithinBlock >> LOG_INUSE_BITSET_SIZE;
                        final long maskWithinInUse = 1L << (indexWithinBlock & IN_USE_MASK);

                        if ((inUse[indexWithinInUse] & maskWithinInUse) == 0) {
                            prevBlock[indexWithinBlock] = block[indexWithinBlock];
                            inUse[indexWithinInUse] |= maskWithinInUse;
                        }
                    }
                }

                // region copyToTypedArray
                for (int jj = 0; jj < length; ++jj) {
                    block[sIndexWithinBlock + jj] = BooleanUtils.booleanAsByte(chunk.get(offset + jj));
                }
                // endregion copyToTypedArray

                firstKey += length;
                offset += length;
            }
        }
    }
    // endregion fillFromChunkByRanges

    // region fillFromChunkByKeys
    @Override
    void fillFromChunkByKeys(@NotNull OrderedKeys orderedKeys, Chunk<? extends Values> src) {
        if (orderedKeys.size() == 0) {
            return;
        }
        final ObjectChunk<Boolean, ? extends Values> chunk = src.asObjectChunk();
        final LongChunk<OrderedKeyIndices> keys = orderedKeys.asKeyIndicesChunk();

        final boolean hasPrev = prevFlusher != null;

        if (hasPrev) {
            prevFlusher.maybeActivate();
        }

        for (int ii = 0; ii < keys.size(); ) {
            final long firstKey = keys.get(ii);
            final long maxKeyInCurrentBlock = firstKey | INDEX_MASK;
            int lastII = ii;
            while (lastII + 1 < keys.size() && keys.get(lastII + 1) <= maxKeyInCurrentBlock) {
                ++lastII;
            }

            final int block0 = (int) (firstKey >> BLOCK0_SHIFT) & BLOCK0_MASK;
            final int block1 = (int) (firstKey >> BLOCK1_SHIFT) & BLOCK1_MASK;
            final int block2 = (int) (firstKey >> BLOCK2_SHIFT) & BLOCK2_MASK;
            final byte [] block = ensureBlock(block0, block1, block2);

            if (chunk.isAlias(block)) {
                throw new UnsupportedOperationException("Source chunk is an alias for target data");
            }

            // This conditional with its constant condition should be very friendly to the branch predictor.
            final byte[] prevBlock = hasPrev ? ensurePrevBlock(firstKey, block0, block1, block2) : null;
            final long[] inUse = hasPrev ? prevInUse.get(block0).get(block1).get(block2) : null;

            while (ii <= lastII) {
                final int indexWithinBlock = (int) (keys.get(ii) & INDEX_MASK);
                // This 'if' with its constant condition should be very friendly to the branch predictor.
                if (hasPrev) {
                    assert inUse != null;
                    assert prevBlock != null;

                    final int indexWithinInUse = indexWithinBlock >> LOG_INUSE_BITSET_SIZE;
                    final long maskWithinInUse = 1L << (indexWithinBlock & IN_USE_MASK);

                    if ((inUse[indexWithinInUse] & maskWithinInUse) == 0) {
                        prevBlock[indexWithinBlock] = block[indexWithinBlock];
                        inUse[indexWithinInUse] |= maskWithinInUse;
                    }
                }
                block[indexWithinBlock] = BooleanUtils.booleanAsByte(chunk.get(ii));
                ++ii;
            }
        }
    }
    // endregion fillFromChunkByKeys

    // region fillFromChunkUnordered
    @Override
    public void fillFromChunkUnordered(@NotNull FillFromContext context, @NotNull Chunk<? extends Values> src, @NotNull LongChunk<KeyIndices> keys) {
        if (keys.size() == 0) {
            return;
        }
        final ObjectChunk<Boolean, ? extends Values> chunk = src.asObjectChunk();

        final boolean hasPrev = prevFlusher != null;

        if (hasPrev) {
            prevFlusher.maybeActivate();
        }

        for (int ii = 0; ii < keys.size(); ) {
            final long firstKey = keys.get(ii);
            final long minKeyInCurrentBlock = firstKey & ~INDEX_MASK;
            final long maxKeyInCurrentBlock = firstKey | INDEX_MASK;

            final int block0 = (int) (firstKey >> BLOCK0_SHIFT) & BLOCK0_MASK;
            final int block1 = (int) (firstKey >> BLOCK1_SHIFT) & BLOCK1_MASK;
            final int block2 = (int) (firstKey >> BLOCK2_SHIFT) & BLOCK2_MASK;
            final byte [] block = ensureBlock(block0, block1, block2);

            if (chunk.isAlias(block)) {
                throw new UnsupportedOperationException("Source chunk is an alias for target data");
            }

            // This conditional with its constant condition should be very friendly to the branch predictor.
            final byte[] prevBlock = hasPrev ? ensurePrevBlock(firstKey, block0, block1, block2) : null;
            final long[] inUse = hasPrev ? prevInUse.get(block0).get(block1).get(block2) : null;

            long key = keys.get(ii);
            do {
                final int indexWithinBlock = (int) (key & INDEX_MASK);

                if (hasPrev) {
                    assert inUse != null;

                    final int indexWithinInUse = indexWithinBlock >> LOG_INUSE_BITSET_SIZE;
                    final long maskWithinInUse = 1L << (indexWithinBlock & IN_USE_MASK);

                    if ((inUse[indexWithinInUse] & maskWithinInUse) == 0) {
                        prevBlock[indexWithinBlock] = block[indexWithinBlock];
                        inUse[indexWithinInUse] |= maskWithinInUse;
                    }
                }
                block[indexWithinBlock] = BooleanUtils.booleanAsByte(chunk.get(ii));
                ++ii;
            } while (ii < keys.size() && (key = keys.get(ii)) >= minKeyInCurrentBlock && key <= maxKeyInCurrentBlock);
        }
    }
    // endregion fillFromChunkUnordered

    @Override
    public void fillPrevChunk(@NotNull FillContext context, @NotNull WritableChunk<? super Values> dest, @NotNull OrderedKeys orderedKeys) {
        if (prevFlusher == null) {
            fillChunk(context, dest, orderedKeys);
            return;
        }
        defaultFillPrevChunk(context, dest, orderedKeys);
    }

    // region getChunk
    @Override
    public ObjectChunk<Boolean, Values> getChunk(@NotNull GetContext context, @NotNull OrderedKeys orderedKeys) {
        return getChunkByFilling(context, orderedKeys).asObjectChunk();
    }
    // endregion getChunk

    // region getPrevChunk
    @Override
    public ObjectChunk<Boolean, Values> getPrevChunk(@NotNull GetContext context, @NotNull OrderedKeys orderedKeys) {
        return getPrevChunkByFilling(context, orderedKeys).asObjectChunk();
    }
    // endregion getPrevChunk

    // region reinterpretation
    @Override
    public <ALTERNATE_DATA_TYPE> boolean allowsReinterpret(@NotNull Class<ALTERNATE_DATA_TYPE> alternateDataType) {
        return alternateDataType.equals(byte.class);
    }

    @Override
    protected <ALTERNATE_DATA_TYPE> ColumnSource<ALTERNATE_DATA_TYPE> doReinterpret(@NotNull Class<ALTERNATE_DATA_TYPE> alternateDataType) {
        //noinspection unchecked
        return (ColumnSource<ALTERNATE_DATA_TYPE>) new BooleanSparseArraySource.ReinterpretedAsByte(this);
    }

    private static class ReinterpretedAsByte extends AbstractColumnSource<Byte> implements MutableColumnSourceGetDefaults.ForByte, FillUnordered, WritableSource<Byte> {
        private final BooleanSparseArraySource wrapped;

        private ReinterpretedAsByte(BooleanSparseArraySource wrapped) {
            super(byte.class);
            this.wrapped = wrapped;
        }

        @Override
        public byte getByte(long index) {
            return wrapped.getByte(index);
        }

        @Override
        public byte getPrevByte(long index) {
            return wrapped.getPrevByte(index);
        }

        @Override
        public void set(long key, Byte value) {
            wrapped.set(key, value);
        }

        @Override
        public void set(long key, byte value) {
            wrapped.set(key, value);
        }

        @Override
        public void ensureCapacity(long capacity, boolean nullFilled) {
            wrapped.ensureCapacity(capacity, nullFilled);
        }

        @Override
        public void copy(ColumnSource<? extends Byte> sourceColumn, long sourceKey, long destKey) {
            set(destKey, sourceColumn.getByte(sourceKey));
        }

        @Override
        public <ALTERNATE_DATA_TYPE> boolean allowsReinterpret(@NotNull Class<ALTERNATE_DATA_TYPE> alternateDataType) {
            return alternateDataType == Boolean.class;
        }

        @Override
        protected <ALTERNATE_DATA_TYPE> ColumnSource<ALTERNATE_DATA_TYPE> doReinterpret(@NotNull Class<ALTERNATE_DATA_TYPE> alternateDataType) {
            // noinspection unchecked
            return (ColumnSource<ALTERNATE_DATA_TYPE>)wrapped;
        }

        @Override
        public void fillChunk(@NotNull final ColumnSource.FillContext context, @NotNull final WritableChunk<? super Values> destination, @NotNull final OrderedKeys orderedKeys) {
            fillSparseChunk(destination, orderedKeys);
        }

        @Override
        public void fillPrevChunk(@NotNull final ColumnSource.FillContext context, @NotNull final WritableChunk<? super Values> destination, @NotNull final OrderedKeys orderedKeys) {
            fillSparsePrevChunk(destination, orderedKeys);
        }

        @Override
        public void fillChunkUnordered(@NotNull final FillContext context, @NotNull final WritableChunk<? super Values> destination, @NotNull final LongChunk<? extends KeyIndices> keyIndices) {
            fillSparseChunkUnordered(destination, keyIndices);
        }

        @Override
        public void fillPrevChunkUnordered(@NotNull final FillContext context, @NotNull final WritableChunk<? super Values> destination, @NotNull final LongChunk<? extends KeyIndices> keyIndices) {
            fillSparsePrevChunkUnordered(destination, keyIndices);
        }

        private void fillSparseChunk(@NotNull final WritableChunk<? super Values> destGeneric, @NotNull final OrderedKeys indices) {
            if (indices.size() == 0) {
                destGeneric.setSize(0);
                return;
            }
            // This implementation is in "key" style (rather than range style).
            final WritableByteChunk<? super Values> chunk = destGeneric.asWritableByteChunk();
            final FillByContext<byte[]> ctx = new FillByContext<>();
            indices.forEachLong((final long v) -> {
                if (v > ctx.maxKeyInCurrentBlock) {
                    ctx.block = wrapped.blocks.getInnermostBlockByKeyOrNull(v);
                    ctx.maxKeyInCurrentBlock = v | INDEX_MASK;
                }
                if (ctx.block == null) {
                    chunk.fillWithNullValue(ctx.offset, 1);
                } else {
                    chunk.set(ctx.offset, ctx.block[(int) (v & INDEX_MASK)]);
                }
                ++ctx.offset;
                return true;
            });
            destGeneric.setSize(ctx.offset);
        }

        private void fillSparsePrevChunk(@NotNull final WritableChunk<? super Values> destGeneric, @NotNull final OrderedKeys indices) {
            final long sz = indices.size();
            if (sz == 0) {
                destGeneric.setSize(0);
                return;
            }

            if (wrapped.prevFlusher == null) {
                fillSparseChunk(destGeneric, indices);
                return;
            }
            fillSparsePrevChunkUnordered(destGeneric, indices.asKeyIndicesChunk());
        }

        private void fillSparseChunkUnordered(@NotNull final WritableChunk<? super Values> destGeneric, @NotNull final LongChunk<? extends KeyIndices> indices) {
            final WritableByteChunk<? super Values> chunk = destGeneric.asWritableByteChunk();
            // This implementation is in "key" style (rather than range style).
            for (int ii = 0; ii < indices.size(); ) {
                final long firstKey = indices.get(ii);
                if (firstKey == Index.NULL_KEY) {
                    chunk.set(ii++, NULL_BOOLEAN_AS_BYTE);
                    continue;
                }
                final long masked = firstKey & ~INDEX_MASK;
                int lastII = ii;
                while (lastII + 1 < indices.size()) {
                    final int nextII = lastII + 1;
                    final long nextKey = indices.get(nextII);
                    final long nextMasked = nextKey & ~INDEX_MASK;
                    if (nextMasked != masked) {
                        break;
                    }
                    lastII = nextII;
                }
                final byte [] block = wrapped.blocks.getInnermostBlockByKeyOrNull(firstKey);
                if (block == null) {
                    chunk.fillWithNullValue(ii, lastII - ii + 1);
                    ii = lastII + 1;
                    continue;
                }
                while (ii <= lastII) {
                    final int indexWithinBlock = (int) (indices.get(ii) & INDEX_MASK);
                    chunk.set(ii++, block[indexWithinBlock]);
                }
            }
            destGeneric.setSize(indices.size());
        }

        private void fillSparsePrevChunkUnordered(@NotNull final WritableChunk<? super Values> destGeneric, @NotNull final LongChunk<? extends KeyIndices> indices) {
            final WritableByteChunk<? super Values> booleanObjectChunk = destGeneric.asWritableByteChunk();
            for (int ii = 0; ii < indices.size(); ) {
                final long firstKey = indices.get(ii);
                if (firstKey == Index.NULL_KEY) {
                    booleanObjectChunk.set(ii++, NULL_BOOLEAN_AS_BYTE);
                    continue;
                }
                final long masked = firstKey & ~INDEX_MASK;
                int lastII = ii;
                while (lastII + 1 < indices.size()) {
                    final int nextII = lastII + 1;
                    final long nextKey = indices.get(nextII);
                    final long nextMasked = nextKey & ~INDEX_MASK;
                    if (nextMasked != masked) {
                        break;
                    }
                    lastII = nextII;
                }

                final byte [] block = wrapped.blocks.getInnermostBlockByKeyOrNull(firstKey);
                if (block == null) {
                    booleanObjectChunk.fillWithNullValue(ii, lastII - ii + 1);
                    ii = lastII + 1;
                    continue;
                }

                final long [] prevInUse = (wrapped.prevFlusher == null || wrapped.prevInUse == null) ? null :
                        wrapped.prevInUse.getInnermostBlockByKeyOrNull(firstKey);
                final byte [] prevBlock = prevInUse == null ? null : wrapped.prevBlocks.getInnermostBlockByKeyOrNull(firstKey);
                while (ii <= lastII) {
                    final int indexWithinBlock = (int) (indices.get(ii) & INDEX_MASK);
                    final int indexWithinInUse = indexWithinBlock >> LOG_INUSE_BITSET_SIZE;
                    final long maskWithinInUse = 1L << (indexWithinBlock & IN_USE_MASK);

                    final byte[] blockToUse = (prevInUse != null && (prevInUse[indexWithinInUse] & maskWithinInUse) != 0) ? prevBlock : block;
                    booleanObjectChunk.set(ii++, blockToUse == null ? NULL_BOOLEAN_AS_BYTE : blockToUse[indexWithinBlock]);
                }
            }
            destGeneric.setSize(indices.size());
        }

        @Override
        public void fillFromChunk(@NotNull FillFromContext context_unused, @NotNull Chunk<? extends Values> src, @NotNull OrderedKeys orderedKeys) {
            // This implementation is in "key" style (rather than range style).
            if (orderedKeys.size() == 0) {
                return;
            }
            final ByteChunk<? extends Values> chunk = src.asByteChunk();
            final LongChunk<OrderedKeyIndices> keys = orderedKeys.asKeyIndicesChunk();

            final boolean hasPrev = wrapped.prevFlusher != null;

            if (hasPrev) {
                wrapped.prevFlusher.maybeActivate();
            }

            for (int ii = 0; ii < keys.size(); ) {
                final long firstKey = keys.get(ii);
                final long maxKeyInCurrentBlock = firstKey | INDEX_MASK;
                int lastII = ii;
                while (lastII + 1 < keys.size() && keys.get(lastII + 1) <= maxKeyInCurrentBlock) {
                    ++lastII;
                }

                final int block0 = (int) (firstKey >> BLOCK0_SHIFT) & BLOCK0_MASK;
                final int block1 = (int) (firstKey >> BLOCK1_SHIFT) & BLOCK1_MASK;
                final int block2 = (int) (firstKey >> BLOCK2_SHIFT) & BLOCK2_MASK;
                final byte [] block = wrapped.ensureBlock(block0, block1, block2);

                if (chunk.isAlias(block)) {
                    throw new UnsupportedOperationException("Source chunk is an alias for target data");
                }

                // This conditional with its constant condition should be very friendly to the branch predictor.
                final byte[] prevBlock = hasPrev ? wrapped.ensurePrevBlock(firstKey, block0, block1, block2) : null;
                final long[] inUse = hasPrev ? wrapped.prevInUse.get(block0).get(block1).get(block2) : null;

                while (ii <= lastII) {
                    final int indexWithinBlock = (int) (keys.get(ii) & INDEX_MASK);
                    // This 'if' with its constant condition should be very friendly to the branch predictor.
                    if (hasPrev) {
                        assert inUse != null;
                        assert prevBlock != null;

                        final int indexWithinInUse = indexWithinBlock >> LOG_INUSE_BITSET_SIZE;
                        final long maskWithinInUse = 1L << (indexWithinBlock & IN_USE_MASK);

                        if ((inUse[indexWithinInUse] & maskWithinInUse) == 0) {
                            prevBlock[indexWithinBlock] = block[indexWithinBlock];
                            inUse[indexWithinInUse] |= maskWithinInUse;
                        }
                    }
                    block[indexWithinBlock] = chunk.get(ii);
                    ++ii;
                }
            }
        }
    }
    // endregion reinterpretation
}
