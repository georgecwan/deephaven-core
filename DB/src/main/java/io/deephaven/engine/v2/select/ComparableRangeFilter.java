package io.deephaven.engine.v2.select;

import io.deephaven.base.verify.Assert;
import io.deephaven.engine.tables.ColumnDefinition;
import io.deephaven.engine.tables.TableDefinition;
import io.deephaven.engine.util.DhObjectComparisons;
import io.deephaven.engine.v2.sources.ColumnSource;
import io.deephaven.engine.v2.sources.chunk.*;
import io.deephaven.engine.v2.sources.chunk.Attributes.OrderedKeyIndices;
import io.deephaven.engine.v2.sources.chunk.Attributes.Values;
import io.deephaven.engine.v2.utils.Index;
import io.deephaven.util.annotations.TestUseOnly;

public class ComparableRangeFilter extends AbstractRangeFilter {
    private final Comparable upper;
    private final Comparable lower;

    ComparableRangeFilter(String columnName, Comparable val1, Comparable val2, boolean lowerInclusive,
            boolean upperInclusive) {
        super(columnName, lowerInclusive, upperInclusive);

        if (DhObjectComparisons.compare(val1, val2) > 0) {
            upper = val1;
            lower = val2;
        } else {
            upper = val2;
            lower = val1;
        }
    }

    @TestUseOnly
    public static ComparableRangeFilter makeForTest(String columnName, Comparable lower, Comparable upper,
            boolean lowerInclusive, boolean upperInclusive) {
        return new ComparableRangeFilter(columnName, lower, upper, lowerInclusive, upperInclusive);
    }

    @Override
    public void init(TableDefinition tableDefinition) {
        if (chunkFilter != null) {
            return;
        }

        final ColumnDefinition def = tableDefinition.getColumn(columnName);
        if (def == null) {
            throw new RuntimeException("Column \"" + columnName + "\" doesn't exist in this table, available columns: "
                    + tableDefinition.getColumnNames());
        }

        Assert.assertion(Comparable.class.isAssignableFrom(def.getDataType()),
                "Comparable.class.isAssignableFrom(def.getDataType())", def.getDataType(), "def.getDataType()");

        chunkFilter = makeComparableChunkFilter(lower, upper, lowerInclusive, upperInclusive);
    }

    public static ChunkFilter makeComparableChunkFilter(Comparable lower, Comparable upper, boolean lowerInclusive,
            boolean upperInclusive) {
        if (lowerInclusive) {
            if (upperInclusive) {
                return new InclusiveInclusiveComparableChunkFilter(lower, upper);
            } else {
                return new InclusiveExclusiveComparableChunkFilter(lower, upper);
            }
        } else {
            if (upperInclusive) {
                return new ExclusiveInclusiveComparableChunkFilter(lower, upper);
            } else {
                return new ExclusiveExclusiveComparableChunkFilter(lower, upper);
            }
        }
    }

    @Override
    public SelectFilter copy() {
        return new ComparableRangeFilter(columnName, lower, upper, lowerInclusive, upperInclusive);
    }

    @Override
    public String toString() {
        return "ComparableRangeFilter(" + columnName + " in " +
                (lowerInclusive ? "[" : "(") + lower + "," + upper +
                (upperInclusive ? "]" : ")") + ")";
    }

    private static class InclusiveInclusiveComparableChunkFilter implements ChunkFilter {
        private final Comparable lower;
        private final Comparable upper;

        private InclusiveInclusiveComparableChunkFilter(Comparable lower, Comparable upper) {
            this.lower = lower;
            this.upper = upper;
        }

        @Override
        public void filter(Chunk<? extends Values> values, LongChunk<OrderedKeyIndices> keys,
                WritableLongChunk<OrderedKeyIndices> results) {
            final ObjectChunk<? extends Comparable, ? extends Values> objectChunk = values.asObjectChunk();

            results.setSize(0);
            for (int ii = 0; ii < values.size(); ++ii) {
                final Comparable value = objectChunk.get(ii);
                if (meetsLowerBound(value) && meetsUpperBound(value)) {
                    results.add(keys.get(ii));
                }
            }
        }

        boolean meetsLowerBound(Comparable<?> value) {
            return DhObjectComparisons.compare(lower, value) <= 0;
        }

        boolean meetsUpperBound(Comparable<?> value) {
            return DhObjectComparisons.compare(upper, value) >= 0;
        }
    }

    private static class InclusiveExclusiveComparableChunkFilter implements ChunkFilter {
        private final Comparable lower;
        private final Comparable upper;

        private InclusiveExclusiveComparableChunkFilter(Comparable lower, Comparable upper) {
            this.lower = lower;
            this.upper = upper;
        }


        @Override
        public void filter(Chunk<? extends Values> values, LongChunk<OrderedKeyIndices> keys,
                WritableLongChunk<OrderedKeyIndices> results) {
            final ObjectChunk<? extends Comparable, ? extends Values> objectChunk = values.asObjectChunk();

            results.setSize(0);
            for (int ii = 0; ii < values.size(); ++ii) {
                final Comparable value = objectChunk.get(ii);
                if (meetsLowerBound(value) && meetsUpperBound(value)) {
                    results.add(keys.get(ii));
                }
            }
        }

        boolean meetsLowerBound(Comparable value) {
            return DhObjectComparisons.compare(lower, value) <= 0;
        }

        boolean meetsUpperBound(Comparable value) {
            return DhObjectComparisons.compare(upper, value) > 0;
        }
    }

    private static class ExclusiveInclusiveComparableChunkFilter implements ChunkFilter {
        private final Comparable lower;
        private final Comparable upper;

        private ExclusiveInclusiveComparableChunkFilter(Comparable lower, Comparable upper) {
            this.lower = lower;
            this.upper = upper;
        }


        @Override
        public void filter(Chunk<? extends Values> values, LongChunk<OrderedKeyIndices> keys,
                WritableLongChunk<OrderedKeyIndices> results) {
            final ObjectChunk<? extends Comparable, ? extends Values> objectChunk = values.asObjectChunk();

            results.setSize(0);
            for (int ii = 0; ii < values.size(); ++ii) {
                final Comparable value = objectChunk.get(ii);
                if (meetsLowerBound(value) && meetsUpperBound(value)) {
                    results.add(keys.get(ii));
                }
            }
        }

        boolean meetsLowerBound(Comparable value) {
            // noinspection unchecked
            return DhObjectComparisons.compare(lower, value) < 0;
        }

        boolean meetsUpperBound(Comparable value) {
            // noinspection unchecked
            return DhObjectComparisons.compare(upper, value) >= 0;
        }
    }

    private static class ExclusiveExclusiveComparableChunkFilter implements ChunkFilter {
        private final Comparable lower;
        private final Comparable upper;

        private ExclusiveExclusiveComparableChunkFilter(Comparable lower, Comparable upper) {
            this.lower = lower;
            this.upper = upper;
        }


        @Override
        public void filter(Chunk<? extends Values> values, LongChunk<OrderedKeyIndices> keys,
                WritableLongChunk<OrderedKeyIndices> results) {
            final ObjectChunk<? extends Comparable, ? extends Values> objectChunk = values.asObjectChunk();

            results.setSize(0);
            for (int ii = 0; ii < values.size(); ++ii) {
                final Comparable value = objectChunk.get(ii);
                if (meetsLowerBound(value) && meetsUpperBound(value)) {
                    results.add(keys.get(ii));
                }
            }
        }

        boolean meetsLowerBound(Comparable value) {
            // noinspection unchecked
            return DhObjectComparisons.compare(lower, value) < 0;
        }

        boolean meetsUpperBound(Comparable value) {
            // noinspection unchecked
            return DhObjectComparisons.compare(upper, value) > 0;
        }
    }

    @Override
    Index binarySearch(Index selection, ColumnSource columnSource, boolean usePrev, boolean reverse) {
        if (selection.isEmpty()) {
            return selection;
        }

        // noinspection unchecked
        final ColumnSource<Comparable> comparableColumnSource = (ColumnSource<Comparable>) columnSource;

        final Comparable<?> startValue = reverse ? upper : lower;
        final Comparable<?> endValue = reverse ? lower : upper;
        final boolean startInclusive = reverse ? upperInclusive : lowerInclusive;
        final boolean endInclusive = reverse ? lowerInclusive : upperInclusive;
        final int compareSign = reverse ? -1 : 1;

        long lowerBoundMin = bound(selection, usePrev, comparableColumnSource, 0, selection.size(), startValue,
                startInclusive, compareSign, false);
        long upperBoundMin = bound(selection, usePrev, comparableColumnSource, lowerBoundMin, selection.size(),
                endValue, endInclusive, compareSign, true);

        return selection.subindexByPos(lowerBoundMin, upperBoundMin);
    }


    static long bound(Index selection, boolean usePrev, ColumnSource<Comparable> comparableColumnSource,
            long minPosition, long maxPosition, Comparable targetValue, boolean inclusive, int compareSign,
            boolean end) {
        while (minPosition < maxPosition) {
            final long midPos = (minPosition + maxPosition) / 2;
            final long midIdx = selection.get(midPos);

            final Comparable<?> compareValue =
                    usePrev ? comparableColumnSource.getPrev(midIdx) : comparableColumnSource.get(midIdx);
            final int compareResult = compareSign * DhObjectComparisons.compare(compareValue, targetValue);

            if (compareResult < 0) {
                minPosition = midPos + 1;
            } else if (compareResult > 0) {
                maxPosition = midPos;
            } else {
                if (end) {
                    if (inclusive) {
                        minPosition = midPos + 1;
                    } else {
                        maxPosition = midPos;
                    }
                } else {
                    if (inclusive) {
                        maxPosition = midPos;
                    } else {
                        minPosition = midPos + 1;
                    }
                }
            }
        }
        return minPosition;
    }
}