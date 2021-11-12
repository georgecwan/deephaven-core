package io.deephaven.engine.v2.select;

import io.deephaven.engine.table.DefaultChunkSource;
import io.deephaven.engine.table.SharedContext;
import io.deephaven.engine.vector.Vector;
import io.deephaven.engine.table.ChunkSource;
import io.deephaven.engine.chunk.*;
import io.deephaven.engine.rowset.RowSequence;
import org.jetbrains.annotations.NotNull;

/**
 * This class wraps an inner ChunkSource holding a Vector. The purpose of doing so is to apply Vector#getDirect to the
 * underlying Vector values returned by the underlying ChunkSource. This is the strategy for implementing this class:
 * makeGetContext() - doesn't need to change. The default implementation in our parent, namely
 * DefaultChunkSource#makeGetContext, already does the right thing. getChunk() - likewise. makeFillContext() - We don't
 * need to add anything to the "inner" context, so we just delegate to inner and return its context fillContext() - We
 * first let the inner fill the chunk, then we overwrite each value (where non-null) with the result of
 * Vector#getDirect() invoked on that value.
 */
public class VectorChunkAdapter<ATTR extends Attributes.Any> implements DefaultChunkSource<ATTR> {
    private final ChunkSource<ATTR> underlying;

    public VectorChunkAdapter(ChunkSource<ATTR> underlying) {
        this.underlying = underlying;
    }

    @Override
    public ChunkType getChunkType() {
        return underlying.getChunkType();
    }

    @Override
    public void fillChunk(@NotNull FillContext context, @NotNull WritableChunk<? super ATTR> destination,
            @NotNull RowSequence rowSequence) {
        // First let the underlying ChunkSource fill the chunk, and then we overwrite the values with the result
        // of applying Vector#getDirect to each element.
        underlying.fillChunk(context, destination, rowSequence);
        final WritableObjectChunk<Vector, ? super ATTR> typedDest = destination.asWritableObjectChunk();
        for (int ii = 0; ii < destination.size(); ++ii) {
            final Vector vector = typedDest.get(ii);
            if (vector != null) {
                final Vector direct = vector.getDirect();
                typedDest.set(ii, direct);
            }
        }
    }

    @Override
    public FillContext makeFillContext(int chunkCapacity, SharedContext sharedContext) {
        return underlying.makeFillContext(chunkCapacity, sharedContext);
    }
}
