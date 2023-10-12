/**
 * Copyright (c) 2016-2023 Deephaven Data Labs and Patent Pending
 */
package io.deephaven.parquet.table.transfer;

import io.deephaven.chunk.CharChunk;
import io.deephaven.chunk.attributes.Values;
import io.deephaven.engine.rowset.RowSet;
import io.deephaven.engine.table.ColumnSource;
import org.jetbrains.annotations.NotNull;

final class CharTransfer extends IntCastablePrimitiveTransfer<CharChunk<Values>> {
    CharTransfer(@NotNull final ColumnSource<?> columnSource, @NotNull final RowSet tableRowSet, final int targetSize) {
        super(columnSource, tableRowSet, targetSize);
    }

    @Override
    public void copyAllFromChunkToBuffer() {
        final int chunkSize = chunk.size();
        for (int chunkIdx = 0; chunkIdx < chunkSize; ++chunkIdx) {
            buffer.put(chunk.get(chunkIdx));
        }
    }
}