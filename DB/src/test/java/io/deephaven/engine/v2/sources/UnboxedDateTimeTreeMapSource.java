package io.deephaven.engine.v2.sources;

import io.deephaven.engine.tables.utils.DBDateTime;
import io.deephaven.engine.v2.utils.TrackingMutableRowSet;

/**
 * Wrap a regular {@code TreeMapSource<Long>} to make it reinterpretable as a DBDateTime column source.
 */
public class UnboxedDateTimeTreeMapSource extends UnboxedDateTimeColumnSource implements ColumnSource<Long> {

    // the actual data storage
    private final TreeMapSource<Long> treeMapSource;

    public UnboxedDateTimeTreeMapSource(ColumnSource<DBDateTime> alternateColumnSource,
            TreeMapSource<Long> treeMapSource) {
        super(alternateColumnSource);
        this.treeMapSource = treeMapSource;
    }

    public void add(TrackingMutableRowSet rowSet, Long[] data) {
        treeMapSource.add(rowSet, data);
    }

    public void remove(TrackingMutableRowSet rowSet) {
        treeMapSource.remove(rowSet);
    }
}
