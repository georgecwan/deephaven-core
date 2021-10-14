/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */

package io.deephaven.engine.v2.sources;

import io.deephaven.engine.v2.utils.Index;
import io.deephaven.util.type.TypeUtils;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.Arrays;
import java.util.TreeMap;
import java.util.function.LongConsumer;

/**
 * The TreeMapSource is a ColumnSource used only for testing; not in live code.
 *
 * It boxes all of it's objects, and maps Long key values to the data values.
 */
@AbstractColumnSource.IsSerializable(value = true)
public class TreeMapSource<T> extends AbstractColumnSource<T> {
    private long lastAdditionTime = LogicalClock.DEFAULT.currentStep();
    protected final TreeMap<Long, T> data = new TreeMap<>();
    private TreeMap<Long, T> prevData = new TreeMap<>();

    public TreeMapSource(Class<T> type) {
        super(type);
    }

    public TreeMapSource(Class<T> type, Index index, T[] data) {
        // noinspection unchecked
        super(convertType(type));
        add(index, data);
        prevData = this.data;
    }

    private static Class convertType(Class type) {
        if (type == Boolean.class || type.isPrimitive()) {
            return type;
        }
        if (io.deephaven.util.type.TypeUtils.getUnboxedType(type) == null) {
            return type;
        } else {
            return io.deephaven.util.type.TypeUtils.getUnboxedType(type);
        }
    }

    public synchronized void add(final Index index, T[] vs) {
        if (groupToRange != null) {
            setGroupToRange(null);
        }

        final long currentStep = LogicalClock.DEFAULT.currentStep();
        if (currentStep != lastAdditionTime) {
            prevData = new TreeMap<>(this.data);
            lastAdditionTime = currentStep;
        }
        if (index.size() != vs.length) {
            throw new IllegalArgumentException("Index=" + index + ", data(" + vs.length + ")=" + Arrays.toString(vs));
        }

        index.forAllLongs(new LongConsumer() {
            private final MutableInt ii = new MutableInt(0);

            @Override
            public void accept(final long v) {
                data.put(v, vs[ii.intValue()]);
                ii.increment();
            }
        });
    }

    public synchronized void remove(Index index) {
        if (groupToRange != null) {
            setGroupToRange(null);
        }

        final long currentStep = LogicalClock.DEFAULT.currentStep();
        if (currentStep != lastAdditionTime) {
            prevData = new TreeMap<>(this.data);
            lastAdditionTime = currentStep;
        }
        for (final Index.Iterator iterator = index.iterator(); iterator.hasNext();) {
            this.data.remove(iterator.nextLong());
        }
    }

    public synchronized void shift(long startKeyInclusive, long endKeyInclusive, long shiftDelta) {
        if (groupToRange != null) {
            setGroupToRange(null);
        }

        // Note: moving to the right, we need to start with rightmost data first.
        final long dir = shiftDelta > 0 ? -1 : 1;
        final long len = endKeyInclusive - startKeyInclusive + 1;
        for (long offset = dir < 0 ? len - 1 : 0; dir < 0 ? offset >= 0 : offset < len; offset += dir) {
            data.put(startKeyInclusive + offset + shiftDelta, data.remove(startKeyInclusive + offset));
        }
    }

    @Override
    public synchronized T get(long index) {
        // If a test asks for a non-existent positive index something is wrong.
        // We have to accept negative values, because e.g. a join may find no matching right key, in which case it
        // has an empty redirection index entry that just gets passed through to the inner column source as -1.
        if (index >= 0 && !data.containsKey(index))
            throw new IllegalStateException("Asking for a non-existent key: " + index);
        return data.get(index);
    }

    @Override
    public boolean isImmutable() {
        return false;
    }

    @Override
    public Boolean getBoolean(long index) {
        return (Boolean) get(index);
    }

    @Override
    public byte getByte(long index) {
        return io.deephaven.util.type.TypeUtils.unbox((Byte) get(index));
    }

    @Override
    public char getChar(long index) {
        return io.deephaven.util.type.TypeUtils.unbox((Character) get(index));
    }

    @Override
    public double getDouble(long index) {
        return io.deephaven.util.type.TypeUtils.unbox((Double) get(index));
    }

    @Override
    public float getFloat(long index) {
        return io.deephaven.util.type.TypeUtils.unbox((Float) get(index));
    }

    @Override
    public int getInt(long index) {
        return io.deephaven.util.type.TypeUtils.unbox((Integer) get(index));
    }

    @Override
    public long getLong(long index) {
        return io.deephaven.util.type.TypeUtils.unbox((Long) get(index));
    }

    @Override
    public short getShort(long index) {
        return io.deephaven.util.type.TypeUtils.unbox((Short) get(index));
    }

    @Override
    synchronized public T getPrev(long index) {
        final long currentStep = LogicalClock.DEFAULT.currentStep();
        if (currentStep != lastAdditionTime) {
            prevData = new TreeMap<>(this.data);
            lastAdditionTime = currentStep;
        }
        return prevData.get(index);
    }

    @Override
    public Boolean getPrevBoolean(long index) {
        return (Boolean) getPrev(index);
    }

    @Override
    public byte getPrevByte(long index) {
        return io.deephaven.util.type.TypeUtils.unbox((Byte) getPrev(index));
    }

    @Override
    public char getPrevChar(long index) {
        return io.deephaven.util.type.TypeUtils.unbox((Character) getPrev(index));
    }

    @Override
    public double getPrevDouble(long index) {
        return io.deephaven.util.type.TypeUtils.unbox((Double) getPrev(index));
    }

    @Override
    public float getPrevFloat(long index) {
        return io.deephaven.util.type.TypeUtils.unbox((Float) getPrev(index));
    }

    @Override
    public int getPrevInt(long index) {
        return io.deephaven.util.type.TypeUtils.unbox((Integer) getPrev(index));
    }

    @Override
    public long getPrevLong(long index) {
        return io.deephaven.util.type.TypeUtils.unbox((Long) getPrev(index));
    }

    @Override
    public short getPrevShort(long index) {
        return TypeUtils.unbox((Short) getPrev(index));
    }

    @Override
    public void startTrackingPrevValues() {}
}