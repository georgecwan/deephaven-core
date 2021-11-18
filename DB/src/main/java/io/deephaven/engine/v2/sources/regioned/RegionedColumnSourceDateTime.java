package io.deephaven.engine.v2.sources.regioned;

import io.deephaven.engine.time.DateTime;
import io.deephaven.engine.time.DateTimeUtils;
import io.deephaven.engine.table.impl.ColumnSourceGetDefaults;
import io.deephaven.engine.chunk.*;
import io.deephaven.engine.rowset.RowSequence;

/**
 * Regioned column source implementation for columns of {@link DateTime}s.
 */
final class RegionedColumnSourceDateTime
        extends
        RegionedColumnSourceReferencing<DateTime, Attributes.Values, Long, ColumnRegionLong<Attributes.Values>>
        implements ColumnSourceGetDefaults.ForObject<DateTime> {

    public RegionedColumnSourceDateTime() {
        super(ColumnRegionLong.createNull(PARAMETERS.regionMask), DateTime.class,
                RegionedColumnSourceLong.NativeType.AsValues::new);
    }

    @Override
    public void convertRegion(WritableChunk<? super Attributes.Values> destination,
            Chunk<? extends Attributes.Values> source, RowSequence rowSequence) {
        WritableObjectChunk<DateTime, ? super Attributes.Values> objectChunk = destination.asWritableObjectChunk();
        LongChunk<? extends Attributes.Values> longChunk = source.asLongChunk();

        final int size = objectChunk.size();
        final int length = longChunk.size();

        for (int i = 0; i < length; ++i) {
            objectChunk.set(size + i, DateTimeUtils.nanosToTime(longChunk.get(i)));
        }
        objectChunk.setSize(size + length);
    }

    @Override
    public DateTime get(long elementIndex) {
        return elementIndex == RowSequence.NULL_ROW_KEY ? null
                : DateTimeUtils.nanosToTime(lookupRegion(elementIndex).getReferencedRegion().getLong(elementIndex));
    }
}