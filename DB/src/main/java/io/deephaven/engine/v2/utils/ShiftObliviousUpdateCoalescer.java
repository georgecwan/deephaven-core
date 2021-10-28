package io.deephaven.engine.v2.utils;

import io.deephaven.base.verify.Assert;
import io.deephaven.configuration.Configuration;
import io.deephaven.engine.v2.ShiftObliviousListener;

/**
 * Helper utility for coalescing multiple {@link ShiftObliviousListener updates}.
 */
public class ShiftObliviousUpdateCoalescer {

    private static final boolean VALIDATE_COALESCED_UPDATES = Configuration.getInstance()
            .getBooleanWithDefault("ShiftObliviousUpdateCoalescer.validateCoalescedUpdates", true);

    private TrackingMutableRowSet added, modified, removed;

    public ShiftObliviousUpdateCoalescer() {
        reset();
    }

    /**
     * The class assumes ownership of one reference to the indices passed; the caller should ensure to
     * TrackingMutableRowSet.clone() them before passing them if they are shared.
     */
    public ShiftObliviousUpdateCoalescer(final TrackingMutableRowSet added, final TrackingRowSet removed,
            final TrackingRowSet modified) {
        this.added = added;
        this.removed = removed;
        this.modified = modified;
    }

    public void update(final RowSet addedOnUpdate, final RowSet removedOnUpdate,
                       final RowSet modifiedOnUpdate) {
        // Note: extract removes matching ranges from the source rowSet
        try (final RowSet addedBack = this.removed.extract(addedOnUpdate);
             final RowSet actuallyAdded = addedOnUpdate.minus(addedBack)) {
            this.added.insert(actuallyAdded);
            this.modified.insert(addedBack);
        }

        // Things we've added, but are now removing. Do not aggregate these as removed since client never saw them.
        try (final RowSet additionsRemoved = this.added.extract(removedOnUpdate);
             final RowSet actuallyRemoved = removedOnUpdate.minus(additionsRemoved)) {
            this.removed.insert(actuallyRemoved);
        }

        // If we've removed it, it should no longer be modified.
        this.modified.remove(removedOnUpdate);

        // And anything modified, should be added to the modified set; unless we've previously added it.
        try (final RowSet actuallyModified = modifiedOnUpdate.minus(this.added)) {
            this.modified.insert(actuallyModified);
        }

        if (TrackingMutableRowSet.VALIDATE_COALESCED_UPDATES
                && (this.added.overlaps(this.modified) || this.added.overlaps(this.removed)
                        || this.removed.overlaps(modified))) {
            final String assertionMessage = "Coalesced overlaps detected: " +
                    "added=" + added.toString() +
                    ", removed=" + removed.toString() +
                    ", modified=" + modified.toString() +
                    ", addedOnUpdate=" + addedOnUpdate.toString() +
                    ", removedOnUpdate=" + removedOnUpdate.toString() +
                    ", modifiedOnUpdate=" + modifiedOnUpdate.toString() +
                    "addedIntersectRemoved=" + added.intersect(removed).toString() +
                    "addedIntersectModified=" + added.intersect(modified).toString() +
                    "removedIntersectModified=" + removed.intersect(modified).toString();
            Assert.assertion(false, assertionMessage);
        }
    }

    public RowSet takeAdded() {
        final RowSet r = added;
        added = RowSetFactoryImpl.INSTANCE.getEmptyRowSet();
        return r;
    }

    public RowSet takeRemoved() {
        final RowSet r = removed;
        removed = RowSetFactoryImpl.INSTANCE.getEmptyRowSet();
        return r;
    }

    public RowSet takeModified() {
        final RowSet r = modified;
        modified = RowSetFactoryImpl.INSTANCE.getEmptyRowSet();
        return r;
    }

    public void reset() {
        added = RowSetFactoryImpl.INSTANCE.getEmptyRowSet();
        modified = RowSetFactoryImpl.INSTANCE.getEmptyRowSet();
        removed = RowSetFactoryImpl.INSTANCE.getEmptyRowSet();
    }
}