package io.deephaven.clientsupport.plotdownsampling;

import io.deephaven.base.verify.Assert;
import io.deephaven.engine.v2.sources.ColumnSource;
import io.deephaven.engine.v2.sources.chunk.Attributes;
import io.deephaven.engine.v2.sources.chunk.Chunk;
import io.deephaven.engine.v2.sources.chunk.ChunkSource;
import io.deephaven.engine.v2.sources.chunk.LongChunk;
import io.deephaven.engine.v2.utils.OrderedKeys;

import java.util.Arrays;
import java.util.List;

/**
 * Provides chunks for given sources so that downsampling can walk several columns at once, allowing GetContext
 * instances to be lazily created as needed, and all tracked together so they can all be closed with a single call.
 */
public class DownsampleChunkContext implements AutoCloseable {
    private final ColumnSource<Long> xColumnSource;
    private final ChunkSource.GetContext xContext;

    private final List<ColumnSource<?>> yColumnSources;

    private final ChunkSource.GetContext[] yContexts;// may contain nulls
    private final int chunkSize;
    private final Chunk<? extends Attributes.Values>[] valuesArray;

    /**
     * Creates an object to track the contexts to be used to read data from an upstream table for a given operation
     * 
     * @param xColumnSource the X column source, always a long column source, currently reinterpreted from DBDateTime
     * @param yColumnSources any Y value column source which may be used. Indexes into this list are used when
     *        specifying columns which are used later
     * @param chunkSize the size of chunks to specify when actually creating any GetContext
     */
    DownsampleChunkContext(final ColumnSource<Long> xColumnSource, final List<ColumnSource<?>> yColumnSources,
            final int chunkSize) {
        this.xColumnSource = xColumnSource;
        this.xContext = xColumnSource.makeGetContext(chunkSize);
        this.yColumnSources = yColumnSources;
        this.yContexts = new ChunkSource.GetContext[yColumnSources.size()];
        this.chunkSize = chunkSize;
        // noinspection unchecked
        this.valuesArray = new Chunk[this.yColumnSources.size()];
    }

    /**
     * Indicates that any of these Y columns will actually be used, and should be pre-populated if not yet present
     * 
     * @param yCols an array of indexes into the original yColumnSources constructor parameter
     */
    public void addYColumnsOfInterest(final int[] yCols) {
        for (final int yColIndex : yCols) {
            final ColumnSource<?> columnSource = yColumnSources.get(yColIndex);
            final ChunkSource.GetContext getContext = yContexts[yColIndex];
            if (getContext == null) {
                yContexts[yColIndex] = columnSource.makeGetContext(chunkSize);
            }
        }
    }

    /**
     * Requests a chunk from the X column source, using the internally tracked GetContext
     * 
     * @param keys the keys in the column that values are needed for
     * @param usePrev whether or not previous values should be fetched
     * @return a LongChunk containing the values specified
     */
    public LongChunk<Attributes.Values> getXValues(final OrderedKeys keys, final boolean usePrev) {
        // noinspection unchecked
        return (LongChunk<Attributes.Values>) (usePrev ? xColumnSource.getPrevChunk(xContext, keys)
                : xColumnSource.getChunk(xContext, keys));
    }

    /**
     * Requests an array of chunks from the given Y column sources, using the internally tracked GetContexts.
     *
     * This assumes that addYColumnsOfInterest has been called on at least the columns indicated in yCols.
     *
     * Do not retain or reuse the array, this DownsampleChunkContext will reuse it.
     *
     * @param yCols the indexes of the columns from the original yColumnSources to get data from
     * @param keys the keys in the columns that values are needed for
     * @param usePrev whether or not previous values should be fetched
     * @return an array containing the data in the specified rows. The array will be the same size as the original
     *         yColumnSources, with only the indexes in yCols populated.
     */
    public Chunk<? extends Attributes.Values>[] getYValues(final int[] yCols, final OrderedKeys keys,
            final boolean usePrev) {
        Arrays.fill(valuesArray, null);
        for (final int yCol : yCols) {
            valuesArray[yCol] = getYValues(yCol, keys, usePrev);
        }
        return valuesArray;
    }

    /**
     * Requests a chunk of data from the specified Y column source, using the internally tracked GetContexts.
     * 
     * @param yColIndex the index of the column from the original yColumnSources to get data from
     * @param keys the keys in the column that values are needed for
     * @param usePrev whether or not previous values should be fetched
     * @return a chunk containing the values specified
     */
    public Chunk<? extends Attributes.Values> getYValues(final int yColIndex, final OrderedKeys keys,
            final boolean usePrev) {
        final ColumnSource<?> columnSource = yColumnSources.get(yColIndex);
        final ChunkSource.GetContext getContext = yContexts[yColIndex];
        Assert.neqNull(getContext, "yContexts.get(yColIndex)");

        return usePrev ? columnSource.getPrevChunk(getContext, keys) : columnSource.getChunk(getContext, keys);
    }

    public void close() {
        xContext.close();
        for (final ChunkSource.GetContext context : yContexts) {
            if (context != null) {
                context.close();
            }
        }
    }
}
