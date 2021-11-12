/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */

package io.deephaven.engine.v2;

import io.deephaven.engine.tables.live.NotificationQueue;
import io.deephaven.engine.rowset.RowSet;

/**
 * ShiftObliviousListener for table changes.
 */
public interface ShiftObliviousListener extends ListenerBase {
    /**
     * Process notification of table changes.
     * 
     * @param added rows added
     * @param removed rows removed
     * @param modified rows modified
     */
    void onUpdate(RowSet added, RowSet removed, RowSet modified);

    /**
     * Creates a notification for the table changes.
     *
     * @param added rows added
     * @param removed rows removed
     * @param modified rows modified
     * @return table change notification
     */
    NotificationQueue.IndexUpdateNotification getNotification(RowSet added, RowSet removed, RowSet modified);

    /**
     * Sets the rowSet for the initial data.
     *
     * @param initialImage initial image
     */
    void setInitialImage(RowSet initialImage);
}
