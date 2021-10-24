/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */

package io.deephaven.engine.v2.sources;

import io.deephaven.engine.v2.ReverseOperation;
import io.deephaven.engine.v2.sources.chunk.Attributes;
import io.deephaven.engine.v2.sources.chunk.SharedContext;
import io.deephaven.engine.v2.sources.chunk.WritableChunk;
import io.deephaven.engine.structures.RowSequence;
import io.deephaven.engine.v2.utils.reverse.ReverseKernel;
import org.jetbrains.annotations.NotNull;

/**
 * This column source wraps another column source, and returns the values in the opposite order. It must be paired with
 * a ReverseOperation (that can be shared among reversed column sources) that implements the rowSet transformations for
 * this source.
 */
public class ReversedColumnSource<T> extends AbstractColumnSource<T> {
    private final ColumnSource<T> innerSource;
    private final ReverseOperation indexReverser;
    private long maxInnerIndex = 0;

    @Override
    public Class<?> getComponentType() {
        return innerSource.getComponentType();
    }

    public ReversedColumnSource(@NotNull ColumnSource<T> innerSource, @NotNull ReverseOperation indexReverser) {
        super(innerSource.getType());
        this.innerSource = innerSource;
        this.indexReverser = indexReverser;
    }

    @Override
    public void startTrackingPrevValues() {
        // Nothing to do.
    }

    @Override
    public T get(long index) {
        return innerSource.get(indexReverser.transform(index));
    }

    @Override
    public Boolean getBoolean(long index) {
        return innerSource.getBoolean(indexReverser.transform(index));
    }

    @Override
    public byte getByte(long index) {
        return innerSource.getByte(indexReverser.transform(index));
    }

    @Override
    public char getChar(long index) {
        return innerSource.getChar(indexReverser.transform(index));
    }

    @Override
    public double getDouble(long index) {
        return innerSource.getDouble(indexReverser.transform(index));
    }

    @Override
    public float getFloat(long index) {
        return innerSource.getFloat(indexReverser.transform(index));
    }

    @Override
    public int getInt(long index) {
        return innerSource.getInt(indexReverser.transform(index));
    }

    @Override
    public long getLong(long index) {
        return innerSource.getLong(indexReverser.transform(index));
    }

    @Override
    public short getShort(long index) {
        return innerSource.getShort(indexReverser.transform(index));
    }

    @Override
    public T getPrev(long index) {
        return innerSource.getPrev(indexReverser.transformPrev(index));
    }

    @Override
    public Boolean getPrevBoolean(long index) {
        return innerSource.getPrevBoolean(indexReverser.transformPrev(index));
    }

    @Override
    public byte getPrevByte(long index) {
        return innerSource.getPrevByte(indexReverser.transformPrev(index));
    }

    @Override
    public char getPrevChar(long index) {
        return innerSource.getPrevChar(indexReverser.transformPrev(index));
    }

    @Override
    public double getPrevDouble(long index) {
        return innerSource.getPrevDouble(indexReverser.transformPrev(index));
    }

    @Override
    public float getPrevFloat(long index) {
        return innerSource.getPrevFloat(indexReverser.transformPrev(index));
    }

    @Override
    public int getPrevInt(long index) {
        return innerSource.getPrevInt(indexReverser.transformPrev(index));
    }

    @Override
    public long getPrevLong(long index) {
        return innerSource.getPrevLong(indexReverser.transformPrev(index));
    }

    @Override
    public short getPrevShort(long index) {
        return innerSource.getPrevShort(indexReverser.transformPrev(index));
    }

    @Override
    public boolean isImmutable() {
        return false;
    }

    private class FillContext implements ColumnSource.FillContext {
        final ColumnSource.FillContext innerContext;
        final ReverseKernel reverseKernel = ReverseKernel.makeReverseKernel(getChunkType());

        FillContext(int chunkCapacity) {
            this.innerContext = innerSource.makeFillContext(chunkCapacity);
        }

        @Override
        public final void close() {
            innerContext.close();
        }
    }

    @Override
    public FillContext makeFillContext(final int chunkCapacity, final SharedContext sharedContext) {
        return new FillContext(chunkCapacity);
    }

    @Override
    public void fillChunk(@NotNull ColumnSource.FillContext _context,
            @NotNull WritableChunk<? super Attributes.Values> destination,
            @NotNull RowSequence rowSequence) {
        // noinspection unchecked
        final FillContext context = (FillContext) _context;
        final RowSequence reversedIndex = indexReverser.transform(rowSequence.asIndex());
        innerSource.fillChunk(context.innerContext, destination, reversedIndex);
        context.reverseKernel.reverse(destination);
    }

    @Override
    public void fillPrevChunk(@NotNull ColumnSource.FillContext _context,
            @NotNull WritableChunk<? super Attributes.Values> destination,
            @NotNull RowSequence rowSequence) {
        // noinspection unchecked
        final FillContext context = (FillContext) _context;
        final RowSequence reversedIndex = indexReverser.transformPrev(rowSequence.asIndex());
        innerSource.fillPrevChunk(context.innerContext, destination, reversedIndex);
        context.reverseKernel.reverse(destination);
    }
}
