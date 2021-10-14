package io.deephaven.engine.v2.locations.parquet.local;

import io.deephaven.engine.v2.locations.TableKey;
import io.deephaven.engine.v2.locations.TableLocation;
import io.deephaven.engine.v2.locations.impl.NonexistentTableLocation;
import io.deephaven.engine.v2.locations.impl.TableLocationFactory;
import io.deephaven.engine.v2.locations.util.TableDataRefreshService;
import io.deephaven.engine.v2.parquet.ParquetInstructions;
import io.deephaven.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * {@link TableLocationFactory} for {@link ParquetTableLocation}s.
 */
public final class ParquetTableLocationFactory implements TableLocationFactory<TableKey, ParquetTableLocationKey> {

    private final ParquetInstructions readInstructions;

    public ParquetTableLocationFactory(@NotNull final ParquetInstructions readInstructions) {
        this.readInstructions = readInstructions;
    }

    @Override
    @NotNull
    public TableLocation makeLocation(@NotNull final TableKey tableKey,
            @NotNull final ParquetTableLocationKey locationKey,
            @Nullable final TableDataRefreshService refreshService) {
        final File parquetFile = locationKey.getFile();
        if (Utils.fileExistsPrivileged(parquetFile)) {
            return new ParquetTableLocation(tableKey, locationKey, readInstructions);
        } else {
            return new NonexistentTableLocation(tableKey, locationKey);
        }
    }
}