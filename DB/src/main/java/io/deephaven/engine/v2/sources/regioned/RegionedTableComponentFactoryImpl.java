/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */

package io.deephaven.engine.v2.sources.regioned;

import io.deephaven.engine.tables.ColumnDefinition;
import io.deephaven.engine.tables.utils.DBDateTime;
import io.deephaven.engine.v2.ColumnSourceManager;
import io.deephaven.engine.v2.ColumnToCodecMappings;
import io.deephaven.util.type.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Factory that assembles modular components for regioned source tables.
 */
public class RegionedTableComponentFactoryImpl implements RegionedTableComponentFactory {

    private static final Map<Class<?>, Supplier<RegionedColumnSource<?>>> SIMPLE_DATA_TYPE_TO_REGIONED_COLUMN_SOURCE_SUPPLIER;

    static {
        Map<Class<?>, Supplier<RegionedColumnSource<?>>> typeToSupplier = new HashMap<>();
        typeToSupplier.put(Byte.class, RegionedColumnSourceByte.AsValues::new);
        typeToSupplier.put(Character.class, RegionedColumnSourceChar.AsValues::new);
        typeToSupplier.put(Double.class, RegionedColumnSourceDouble.AsValues::new);
        typeToSupplier.put(Float.class, RegionedColumnSourceFloat.AsValues::new);
        typeToSupplier.put(Integer.class, RegionedColumnSourceInt.AsValues::new);
        typeToSupplier.put(Long.class, RegionedColumnSourceLong.AsValues::new);
        typeToSupplier.put(Short.class, RegionedColumnSourceShort.AsValues::new);
        typeToSupplier.put(Boolean.class, RegionedColumnSourceBoolean::new);
        typeToSupplier.put(DBDateTime.class, RegionedColumnSourceDBDateTime::new);
        SIMPLE_DATA_TYPE_TO_REGIONED_COLUMN_SOURCE_SUPPLIER = Collections.unmodifiableMap(typeToSupplier);
    }

    public static final RegionedTableComponentFactory INSTANCE = new RegionedTableComponentFactoryImpl();

    private RegionedTableComponentFactoryImpl() {}

    @Override
    public ColumnSourceManager createColumnSourceManager(
            final boolean isRefreshing,
            @NotNull final ColumnToCodecMappings codecMappings,
            @NotNull final ColumnDefinition<?>... columnDefinitions) {
        return new RegionedColumnSourceManager(isRefreshing, this, codecMappings, columnDefinitions);
    }

    /**
     * Create a new {@link RegionedColumnSource} appropriate to implement the supplied {@link ColumnDefinition}.
     *
     * @param columnDefinition The column definition
     * @param <DATA_TYPE> The data type of the column
     * @return A new RegionedColumnSource.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <DATA_TYPE> RegionedColumnSource<DATA_TYPE> createRegionedColumnSource(
            @NotNull final ColumnDefinition<DATA_TYPE> columnDefinition,
            @NotNull final ColumnToCodecMappings codecMappings) {
        Class<DATA_TYPE> dataType = (Class<DATA_TYPE>) TypeUtils.getBoxedType(columnDefinition.getDataType());

        if (columnDefinition.isPartitioning()) {
            return PartitioningSourceFactory.makePartitioningSource(dataType);
        }

        final Supplier<RegionedColumnSource<?>> simpleImplementationSupplier =
                SIMPLE_DATA_TYPE_TO_REGIONED_COLUMN_SOURCE_SUPPLIER.get(dataType);
        if (simpleImplementationSupplier != null) {
            return (RegionedColumnSource<DATA_TYPE>) simpleImplementationSupplier.get();
        }

        try {
            if (CharSequence.class.isAssignableFrom(dataType)) {
                return new RegionedColumnSourceWithDictionary<>(dataType, null);
            } else {
                return new RegionedColumnSourceObject.AsValues<>(dataType, columnDefinition.getComponentType());
            }
        } catch (IllegalArgumentException except) {
            throw new UnsupportedOperationException(
                    "Can't create column for " + dataType + " in column definition " + columnDefinition, except);
        }
    }
}