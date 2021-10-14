/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */

package io.deephaven.engine.v2;

import io.deephaven.engine.tables.ColumnDefinition;

/**
 * Factory for source table components.
 */
public interface SourceTableComponentFactory {

    ColumnSourceManager createColumnSourceManager(
            boolean isRefreshing,
            ColumnToCodecMappings codecMappings,
            ColumnDefinition<?>... columnDefinitions);
}