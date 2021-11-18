/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */

package io.deephaven.engine.v2.utils;

import gnu.trove.map.hash.TLongLongHashMap;
import io.deephaven.base.verify.Assert;
import io.deephaven.base.verify.Require;
import io.deephaven.engine.rowset.RowSequence;
import io.deephaven.engine.chunk.Attributes.RowKeys;
import io.deephaven.engine.table.ChunkSource;
import io.deephaven.engine.chunk.WritableLongChunk;
import io.deephaven.engine.updategraph.UpdateCommitter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class ContiguousWritableRowRedirection implements WritableRowRedirection {
    private static final long UPDATES_KEY_NOT_FOUND = -2L;

    // The current state of the world.
    private long[] redirections;
    // how many entries in redirections are actually valid
    int size;
    // How things looked on the last clock tick
    private volatile TLongLongHashMap checkpoint;
    private UpdateCommitter<ContiguousWritableRowRedirection> updateCommitter;

    @SuppressWarnings("unused")
    public ContiguousWritableRowRedirection(int initialCapacity) {
        redirections = new long[initialCapacity];
        Arrays.fill(redirections, RowSequence.NULL_ROW_KEY);
        size = 0;
        checkpoint = null;
        updateCommitter = null;
    }

    public ContiguousWritableRowRedirection(long[] redirections) {
        this.redirections = redirections;
        size = redirections.length;
        checkpoint = null;
        updateCommitter = null;
    }

    @Override
    public long put(long outerRowKey, long innerRowKey) {
        Require.requirement(outerRowKey <= Integer.MAX_VALUE && outerRowKey >= 0,
                "key <= Integer.MAX_VALUE && key >= 0", outerRowKey, "key");
        if (outerRowKey >= redirections.length) {
            final long[] newRedirections = new long[Math.max((int) outerRowKey + 100, redirections.length * 2)];
            System.arraycopy(redirections, 0, newRedirections, 0, redirections.length);
            Arrays.fill(newRedirections, redirections.length, newRedirections.length, RowSequence.NULL_ROW_KEY);
            redirections = newRedirections;
        }
        final long previous = redirections[(int) outerRowKey];
        if (previous == RowSequence.NULL_ROW_KEY) {
            size++;
        }
        redirections[(int) outerRowKey] = innerRowKey;

        if (previous != innerRowKey) {
            onRemove(outerRowKey, previous);
        }
        return previous;
    }

    private synchronized void onRemove(long key, long previous) {
        if (updateCommitter == null) {
            return;
        }
        updateCommitter.maybeActivate();
        checkpoint.putIfAbsent(key, previous);
    }

    @Override
    public long get(long outerRowKey) {
        if (outerRowKey < 0 || outerRowKey >= redirections.length) {
            return RowSequence.NULL_ROW_KEY;
        }
        return redirections[(int) outerRowKey];
    }

    @Override
    public void fillChunk(
            @NotNull final ChunkSource.FillContext fillContext,
            @NotNull final WritableLongChunk<? extends RowKeys> innerRowKeys,
            @NotNull final RowSequence outerRowKeys) {
        innerRowKeys.setSize(0);
        outerRowKeys.forAllRowKeyRanges((final long start, final long end) -> {
            for (long v = start; v <= end; ++v) {
                innerRowKeys.add(redirections[(int) v]);
            }
        });
    }

    @Override
    public long getPrev(long outerRowKey) {
        if (checkpoint != null) {
            synchronized (this) {
                final long result = checkpoint.get(outerRowKey);
                if (result != UPDATES_KEY_NOT_FOUND) {
                    return result;
                }
            }
        }
        return get(outerRowKey);
    }

    @Override
    public void fillPrevChunk(
            @NotNull final ChunkSource.FillContext fillContext,
            @NotNull final WritableLongChunk<? extends RowKeys> innerRowKeys,
            @NotNull final RowSequence outerRowKeys) {
        if (checkpoint == null) {
            fillChunk(fillContext, innerRowKeys, outerRowKeys);
            return;
        }

        synchronized (this) {
            innerRowKeys.setSize(0);
            outerRowKeys.forAllRowKeyRanges((final long start, final long end) -> {
                for (long v = start; v <= end; ++v) {
                    long result = checkpoint.get(v);
                    if (result == UPDATES_KEY_NOT_FOUND) {
                        result = redirections[(int) v];
                    }
                    innerRowKeys.add(result);
                }
            });
        }
    }

    @Override
    public long remove(long outerRowKey) {
        final long removed = redirections[(int) outerRowKey];
        redirections[(int) outerRowKey] = RowSequence.NULL_ROW_KEY;
        if (removed != RowSequence.NULL_ROW_KEY) {
            size--;
            onRemove(outerRowKey, removed);
        }
        return removed;
    }

    public synchronized void startTrackingPrevValues() {
        Assert.eqNull(updateCommitter, "updateCommitter");
        checkpoint =
                new TLongLongHashMap(Math.min(size, 1024 * 1024), 0.75f, UPDATES_KEY_NOT_FOUND, UPDATES_KEY_NOT_FOUND);
        updateCommitter = new UpdateCommitter<>(this, ContiguousWritableRowRedirection::commitUpdates);
    }

    private synchronized void commitUpdates() {
        checkpoint.clear();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("{ size = ").append(size);
        int printed = 0;
        for (int ii = 0; printed < size && ii < redirections.length; ii++) {
            if (redirections[ii] == RowSequence.NULL_ROW_KEY) {
                builder.append(", nil");
            } else {
                builder.append(", ").append(ii).append("=").append(redirections[ii]);
                printed++;
            }
        }
        if (printed != size) {
            builder.append(" ERROR, printed=").append(printed);
        }
        builder.append("}");
        return builder.toString();
    }
}