/* ---------------------------------------------------------------------------------------------------------------------
 * AUTO-GENERATED CLASS - DO NOT EDIT MANUALLY - for any changes edit CharAggregateColumnSource and regenerate
 * ------------------------------------------------------------------------------------------------------------------ */
package io.deephaven.engine.v2.sources.aggregate;

import io.deephaven.engine.tables.dbarrays.DbIntArray;
import io.deephaven.engine.v2.dbarrays.DbIntArrayColumnWrapper;
import io.deephaven.engine.v2.dbarrays.DbPrevIntArrayColumnWrapper;
import io.deephaven.engine.v2.sources.ColumnSource;
import io.deephaven.engine.v2.sources.chunk.Attributes.Values;
import io.deephaven.engine.v2.sources.chunk.ObjectChunk;
import io.deephaven.engine.v2.sources.chunk.WritableChunk;
import io.deephaven.engine.v2.sources.chunk.WritableObjectChunk;
import io.deephaven.engine.v2.utils.TrackingMutableRowSet;
import io.deephaven.engine.structures.RowSequence;
import org.jetbrains.annotations.NotNull;

/**
 * {@link ColumnSource} implementation for aggregation result int columns.
 */
public final class IntAggregateColumnSource extends BaseAggregateColumnSource<DbIntArray, Integer> {

    IntAggregateColumnSource(@NotNull final ColumnSource<Integer> aggregatedSource,
                              @NotNull final ColumnSource<TrackingMutableRowSet> indexSource) {
        super(DbIntArray.class, aggregatedSource, indexSource);
    }

    @Override
    public final DbIntArray get(final long index) {
        if (index == TrackingMutableRowSet.NULL_ROW_KEY) {
            return null;
        }
        return new DbIntArrayColumnWrapper(aggregatedSource, indexSource.get(index));
    }

    @Override
    public final DbIntArray getPrev(final long index) {
        if (index == TrackingMutableRowSet.NULL_ROW_KEY) {
            return null;
        }
        return new DbPrevIntArrayColumnWrapper(aggregatedSource, indexSource.getPrev(index));
    }

    @Override
    public final void fillChunk(@NotNull final FillContext context, @NotNull final WritableChunk<? super Values> destination, @NotNull final RowSequence rowSequence) {
        final ObjectChunk<TrackingMutableRowSet, ? extends Values> indexChunk = indexSource.getChunk(((AggregateFillContext) context).indexGetContext, rowSequence).asObjectChunk();
        final WritableObjectChunk<DbIntArray, ? super Values> typedDestination = destination.asWritableObjectChunk();
        final int size = rowSequence.intSize();
        for (int di = 0; di < size; ++di) {
            typedDestination.set(di, new DbIntArrayColumnWrapper(aggregatedSource, indexChunk.get(di)));
        }
        typedDestination.setSize(size);
    }

    @Override
    public final void fillPrevChunk(@NotNull final FillContext context, @NotNull final WritableChunk<? super Values> destination, @NotNull final RowSequence rowSequence) {
        final ObjectChunk<TrackingMutableRowSet, ? extends Values> indexChunk = indexSource.getPrevChunk(((AggregateFillContext) context).indexGetContext, rowSequence).asObjectChunk();
        final WritableObjectChunk<DbIntArray, ? super Values> typedDestination = destination.asWritableObjectChunk();
        final int size = rowSequence.intSize();
        for (int di = 0; di < size; ++di) {
            typedDestination.set(di, new DbPrevIntArrayColumnWrapper(aggregatedSource, indexChunk.get(di)));
        }
        typedDestination.setSize(size);
    }
}
