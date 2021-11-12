package io.deephaven.engine.v2.sources;

import io.deephaven.engine.table.ColumnSource;

/**
 * Interface for {@link ColumnSource} implementations that are both {@link SizedColumnSource}s and
 * {@link ContextualizedObjectColumnSource}s.
 */
public interface SizedContextualizedObjectColumnSource<DATA_TYPE>
        extends SizedColumnSource<DATA_TYPE>, ContextualizedObjectColumnSource<DATA_TYPE> {
}
