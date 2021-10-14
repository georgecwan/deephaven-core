/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */

package io.deephaven.engine.v2.sources.regioned;

import io.deephaven.engine.tables.ColumnDefinition;
import io.deephaven.engine.v2.ColumnToCodecMappings;
import io.deephaven.engine.v2.SourceTableComponentFactory;

/**
 * Factory interface for regioned source table components.
 */
public interface RegionedTableComponentFactory extends SourceTableComponentFactory {

    <DATA_TYPE> RegionedColumnSource<DATA_TYPE> createRegionedColumnSource(
            ColumnDefinition<DATA_TYPE> columnDefinition,
            ColumnToCodecMappings codecMappings);
}
