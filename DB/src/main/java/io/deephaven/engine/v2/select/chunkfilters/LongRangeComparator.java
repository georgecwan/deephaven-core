/* ---------------------------------------------------------------------------------------------------------------------
 * AUTO-GENERATED CLASS - DO NOT EDIT MANUALLY - for any changes edit CharRangeComparator and regenerate
 * ------------------------------------------------------------------------------------------------------------------ */
package io.deephaven.engine.v2.select.chunkfilters;

import io.deephaven.util.compare.LongComparisons;
import io.deephaven.engine.v2.select.ChunkFilter;
import io.deephaven.engine.chunk.*;
import io.deephaven.engine.chunk.Attributes.OrderedRowKeys;
import io.deephaven.engine.chunk.Attributes.Values;

public class LongRangeComparator {
    private LongRangeComparator() {} // static use only

    private abstract static class LongLongFilter implements ChunkFilter.LongChunkFilter {
        final long lower;
        final long upper;

        LongLongFilter(long lower, long upper) {
            this.lower = lower;
            this.upper = upper;
        }

        abstract public void filter(LongChunk<? extends Values> values, LongChunk<Attributes.OrderedRowKeys> keys, WritableLongChunk<OrderedRowKeys> results);
    }

    static class LongLongInclusiveInclusiveFilter extends LongLongFilter {
        private LongLongInclusiveInclusiveFilter(long lower, long upper) {
            super(lower, upper);
        }

        public void filter(LongChunk<? extends Values> values, LongChunk<OrderedRowKeys> keys, WritableLongChunk<Attributes.OrderedRowKeys> results) {
            results.setSize(0);
            for (int ii = 0; ii < values.size(); ++ii) {
                final long value = values.get(ii);
                if (LongComparisons.geq(value, lower) && LongComparisons.leq(value, upper)) {
                    results.add(keys.get(ii));
                }
            }
        }
    }

    static class LongLongInclusiveExclusiveFilter extends LongLongFilter {
        private LongLongInclusiveExclusiveFilter(long lower, long upper) {
            super(lower, upper);
        }

        public void filter(LongChunk<? extends Values> values, LongChunk<OrderedRowKeys> keys, WritableLongChunk<Attributes.OrderedRowKeys> results) {
            results.setSize(0);
            for (int ii = 0; ii < values.size(); ++ii) {
                final long value = values.get(ii);
                if (LongComparisons.geq(value, lower) && LongComparisons.lt(value, upper)) {
                    results.add(keys.get(ii));
                }
            }
        }
    }

    static class LongLongExclusiveInclusiveFilter extends LongLongFilter {
        private LongLongExclusiveInclusiveFilter(long lower, long upper) {
            super(lower, upper);
        }

        public void filter(LongChunk<? extends Values> values, LongChunk<Attributes.OrderedRowKeys> keys, WritableLongChunk<Attributes.OrderedRowKeys> results) {
            results.setSize(0);
            for (int ii = 0; ii < values.size(); ++ii) {
                final long value = values.get(ii);
                if (LongComparisons.gt(value, lower) && LongComparisons.leq(value, upper)) {
                    results.add(keys.get(ii));
                }
            }
        }
    }

    static class LongLongExclusiveExclusiveFilter extends LongLongFilter {
        private LongLongExclusiveExclusiveFilter(long lower, long upper) {
            super(lower, upper);
        }

        public void filter(LongChunk<? extends Values> values, LongChunk<Attributes.OrderedRowKeys> keys, WritableLongChunk<OrderedRowKeys> results) {
            results.setSize(0);
            for (int ii = 0; ii < values.size(); ++ii) {
                final long value = values.get(ii);
                if (LongComparisons.gt(value, lower) && LongComparisons.lt(value, upper)) {
                    results.add(keys.get(ii));
                }
            }
        }
    }

    public static ChunkFilter.LongChunkFilter makeLongFilter(long lower, long upper, boolean lowerInclusive, boolean upperInclusive) {
        if (lowerInclusive) {
            if (upperInclusive) {
                return new LongLongInclusiveInclusiveFilter(lower, upper);
            } else {
                return new LongLongInclusiveExclusiveFilter(lower, upper);
            }
        } else {
            if (upperInclusive) {
                return new LongLongExclusiveInclusiveFilter(lower, upper);
            } else {
                return new LongLongExclusiveExclusiveFilter(lower, upper);
            }
        }
    }
}
