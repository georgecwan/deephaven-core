/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */

package io.deephaven.engine.tables.dbarrays;

import io.deephaven.base.verify.Assert;
import io.deephaven.base.verify.Require;
import io.deephaven.engine.util.LongSizedDataStructure;
import io.deephaven.util.QueryConstants;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import static io.deephaven.base.ClampUtil.clampLong;
import static io.deephaven.engine.tables.dbarrays.DbArrayBase.clampIndex;

public class DbCharArraySlice extends DbCharArray.Indirect {

    private static final long serialVersionUID = 1L;

    private final DbCharArray innerArray;
    private final long offsetIndex;
    private final long length;
    private final long innerArrayValidFromInclusive;
    private final long innerArrayValidToExclusive;

    public DbCharArraySlice(@NotNull final DbCharArray innerArray, final long offsetIndex, final long length, final long innerArrayValidFromInclusive, final long innerArrayValidToExclusive) {
        Assert.geqZero(length, "length");
        Assert.leq(innerArrayValidFromInclusive, "innerArrayValidFromInclusive", innerArrayValidToExclusive, "innerArrayValidToExclusive");
        this.innerArray = innerArray;
        this.offsetIndex = offsetIndex;
        this.length = length;
        this.innerArrayValidFromInclusive = innerArrayValidFromInclusive;
        this.innerArrayValidToExclusive = innerArrayValidToExclusive;
    }

    public DbCharArraySlice(@NotNull final DbCharArray innerArray, final long offsetIndex, final long length) {
        this(innerArray, offsetIndex, length,
                clampLong(0, innerArray.size(), offsetIndex),
                clampLong(0, innerArray.size(), offsetIndex + length));
    }

    @Override
    public char get(final long index) {
        return innerArray.get(clampIndex(innerArrayValidFromInclusive, innerArrayValidToExclusive, index + offsetIndex));
    }

    @Override
    public DbCharArray subArray(final long fromIndexInclusive, final long toIndexExclusive) {
            Require.leq(fromIndexInclusive, "fromIndexInclusive", toIndexExclusive, "toIndexExclusive");
            final long newLength = toIndexExclusive - fromIndexInclusive;
            final long newOffsetIndex = offsetIndex + fromIndexInclusive;
            return new DbCharArraySlice(innerArray, newOffsetIndex, newLength,
                    clampLong(innerArrayValidFromInclusive, innerArrayValidToExclusive, newOffsetIndex),
                    clampLong(innerArrayValidFromInclusive, innerArrayValidToExclusive, newOffsetIndex + newLength));
        }

    @Override
    public DbCharArray subArrayByPositions(final long[] positions) {
        return innerArray.subArrayByPositions(Arrays.stream(positions).map(p -> clampIndex(innerArrayValidFromInclusive, innerArrayValidToExclusive, p + offsetIndex)).toArray());
    }

    @Override
    public char[] toArray() {
        if (innerArray instanceof DbCharArrayDirect && offsetIndex >= innerArrayValidFromInclusive && offsetIndex + length <= innerArrayValidToExclusive) {
            return Arrays.copyOfRange(innerArray.toArray(), LongSizedDataStructure.intSize("toArray", offsetIndex), LongSizedDataStructure.intSize("toArray", offsetIndex + length));
        }
        final char[] result = new char[LongSizedDataStructure.intSize("toArray", length)];
        for (int ii = 0; ii < length; ++ii) {
            result[ii] = get(ii);
        }
        return result;
    }

    @Override
    public long size() {
        return length;
    }

    @Override
    public char getPrev(final long index) {
        if (index < 0 || index >= length) {
            return QueryConstants.NULL_CHAR;
        }
        return innerArray.getPrev(offsetIndex + index);
    }

    @Override
    public boolean isEmpty() {
        return length == 0;
    }
}