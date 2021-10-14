/* ---------------------------------------------------------------------------------------------------------------------
 * AUTO-GENERATED CLASS - DO NOT EDIT MANUALLY - for any changes edit CharChunk and regenerate
 * ------------------------------------------------------------------------------------------------------------------ */
package io.deephaven.engine.v2.sources.chunk;

import io.deephaven.engine.tables.utils.ArrayUtils;
import io.deephaven.engine.v2.sources.chunk.Attributes.Any;
import io.deephaven.engine.v2.utils.ChunkUtils;
import org.jetbrains.annotations.NotNull;

// region ApplyDecoderImports
// endregion ApplyDecoderImports

// region BufferImports
import java.nio.Buffer;
import java.nio.DoubleBuffer;
// endregion BufferImports

/**
 * {@link Chunk} implementation for double data.
 */
public class DoubleChunk<ATTR extends Any> extends ChunkBase<ATTR> {

    private static final DoubleChunk EMPTY = new DoubleChunk<>(ArrayUtils.EMPTY_DOUBLE_ARRAY, 0, 0);

    public static <ATTR extends Any> DoubleChunk<ATTR> getEmptyChunk() {
        //noinspection unchecked
        return EMPTY;
    }

    private static final DoubleChunk[] EMPTY_DOUBLE_CHUNK_ARRAY = new DoubleChunk[0];

    static <ATTR extends Any> DoubleChunk<ATTR>[] getEmptyChunkArray() {
        //noinspection unchecked
        return EMPTY_DOUBLE_CHUNK_ARRAY;
    }

    // region makeArray
    public static double[] makeArray(int capacity) {
        if (capacity == 0) {
            return ArrayUtils.EMPTY_DOUBLE_ARRAY;
        }
        return new double[capacity];
    }
    // endregion makeArray

    public static <ATTR extends Any> DoubleChunk<ATTR> chunkWrap(double[] data) {
        return chunkWrap(data, 0, data.length);
    }

    public static <ATTR extends Any> DoubleChunk<ATTR> chunkWrap(double[] data, int offset, int capacity) {
        return new DoubleChunk<>(data, offset, capacity);
    }

    double[] data;

    protected DoubleChunk(double[] data, int offset, int capacity) {
        super(data.length, offset, capacity);
        this.data = data;
    }

    public final ChunkType getChunkType() {
        return ChunkType.Double;
    }

    public final double get(int index) {
        return data[offset + index];
    }

    @Override
    public DoubleChunk<ATTR> slice(int offset, int capacity) {
        ChunkUtils.checkSliceArgs(size, offset, capacity);
        return new DoubleChunk<>(data, this.offset + offset, capacity);
    }

    @Override
    public final void copyToChunk(int srcOffset, WritableChunk<? super ATTR> dest, int destOffset, int length) {
        final WritableDoubleChunk<? super ATTR> wDest = dest.asWritableDoubleChunk();
        copyToTypedArray(srcOffset, wDest.data, wDest.offset + destOffset, length);
    }

    @Override
    public final void copyToArray(int srcOffset, Object dest, int destOffset, int length) {
        final double[] realType = (double[])dest;
        copyToTypedArray(srcOffset, realType, destOffset, length);
    }

    public final void copyToTypedArray(int srcOffset, double[] destData, int destOffset, int length) {
        final int sStart = offset + srcOffset;
        if (length >= SYSTEM_ARRAYCOPY_THRESHOLD) {
            System.arraycopy(data, sStart, destData, destOffset, length);
            return;
        }
        if (ChunkUtils.canCopyForward(data, sStart, destData, destOffset, length)) {
            //noinspection ManualArrayCopy
            for (int ii = 0; ii < length; ++ii) {
                destData[destOffset + ii] = data[sStart + ii];
            }
            return;
        }
        //noinspection ManualArrayCopy
        for (int ii = length - 1; ii >= 0; --ii) {
            destData[destOffset + ii] = data[sStart + ii];
        }
    }

    @Override
    public final boolean isAlias(Object array) {
        return data == array;
    }

    @Override
    public final boolean isAlias(Chunk chunk) {
        return chunk.isAlias(data);
    }

    @Override
    public final <V extends Visitor<ATTR>> V walk(V visitor) {
        visitor.visit(this);
        return visitor;
    }

    // region ApplyDecoder
    // endregion ApplyDecoder

    // region CopyToBuffer
    @Override
    public final void copyToBuffer(final int srcOffset, @NotNull final Buffer destBuffer, final int destOffset, final int length) {
        final DoubleBuffer doubleDestBuffer = (DoubleBuffer) destBuffer;
        copyToTypedBuffer(srcOffset, doubleDestBuffer, destOffset, length);
    }

    /**
     * <p>Copy a sub-range of this DoubleChunk to a {@link DoubleBuffer}.
     *
     * <p>See {@link #copyToBuffer(int, Buffer, int, int)} for general documentation.
     *
     * @param srcOffset  The offset into this chunk to start copying from
     * @param destBuffer The destination {@link DoubleBuffer}
     * @param destOffset The absolute offset into {@code destBuffer} to start copying to
     * @param length     The number of elements to copy
     */
    public final void copyToTypedBuffer(final int srcOffset, @NotNull final DoubleBuffer destBuffer, final int destOffset, final int length) {
        if (destBuffer.hasArray()) {
            copyToTypedArray(srcOffset, destBuffer.array(), destBuffer.arrayOffset() + destOffset, length);
            return;
        }
        final int initialPosition = destBuffer.position();
        destBuffer.position(destOffset);
        destBuffer.put(data, offset + srcOffset, length);
        destBuffer.position(initialPosition);
    }
    // endregion CopyToBuffer

    // region downcast
    public static <ATTR extends Any, ATTR_DERIV extends ATTR> WritableDoubleChunk<ATTR_DERIV> downcast(WritableDoubleChunk<ATTR> self) {
        //noinspection unchecked
        return (WritableDoubleChunk<ATTR_DERIV>) self;
    }
    // endregion downcast
}