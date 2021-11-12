package io.deephaven.engine.v2.utils;

import io.deephaven.engine.rowset.RowSequence;
import io.deephaven.engine.rowset.RowSet;

public class RowRedirectionUtilities {
    private static final long SEARCH_ITERATOR_THRESHOLD = 512;

    static void applyRedirectionShift(final MutableRowRedirection rowRedirection, final RowSet filterIndex,
            final RowSetShiftData shiftData) {

        final RowSetShiftData.SingleElementShiftCallback applyOneShift = (key, delta) -> {
            final long oldKey = rowRedirection.remove(key);
            if (oldKey != RowSequence.NULL_ROW_KEY) {
                rowRedirection.putVoid(key + delta, oldKey);
            }
        };

        if (shiftData.getEffectiveSize() >= SEARCH_ITERATOR_THRESHOLD) {
            shiftData.forAllInIndex(filterIndex, applyOneShift);
        } else {
            shiftData.apply((start, end, delta) -> {
                final long len = end - start + 1;
                final boolean negativeShift = delta < 0;
                if (negativeShift) {
                    for (long offset = 0; offset < len; ++offset) {
                        applyOneShift.shift(start + offset, delta);
                    }
                } else {
                    for (long offset = len - 1; offset >= 0; --offset) {
                        applyOneShift.shift(start + offset, delta);
                    }
                }
            });
        }
    }
}
