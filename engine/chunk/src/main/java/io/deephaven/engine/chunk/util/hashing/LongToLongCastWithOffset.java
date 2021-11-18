/* ---------------------------------------------------------------------------------------------------------------------
 * AUTO-GENERATED CLASS - DO NOT EDIT MANUALLY - for any changes edit CharToLongCastWithOffset and regenerate
 * ------------------------------------------------------------------------------------------------------------------ */
package io.deephaven.engine.chunk.util.hashing;

import io.deephaven.engine.chunk.Attributes.Any;
import io.deephaven.engine.chunk.LongChunk;
import io.deephaven.engine.chunk.Chunk;
import io.deephaven.engine.chunk.WritableLongChunk;

/**
 * Cast the values in the input chunk to an long and add the specified offset.
 *
 * @param <T> the chunk's attribute
 */
public class LongToLongCastWithOffset<T extends Any> implements ToLongFunctor<T> {
    private final WritableLongChunk<T> result;
    private final long offset;

    LongToLongCastWithOffset(int size, long offset) {
        result = WritableLongChunk.makeWritableChunk(size);
        this.offset = offset;
    }

    @Override
    public LongChunk<T> apply(Chunk<T> input) {
        return castWithOffset(input.asLongChunk());
    }

    private LongChunk<T> castWithOffset(LongChunk<T> input) {
        castInto(input, result, offset);
        return result;
    }

    public static <T2 extends Any>  void castInto(LongChunk<T2> input, WritableLongChunk<T2> result, long offset) {
        for (int ii = 0; ii < input.size(); ++ii) {
            result.set(ii, (long)input.get(ii) + offset);
        }
        result.setSize(input.size());
    }

    @Override
    public void close() {
        result.close();
    }
}