package io.deephaven.engine.v2.select;

import io.deephaven.engine.tables.ColumnDefinition;
import io.deephaven.engine.v2.sources.ColumnSource;
import io.deephaven.engine.structures.rowset.Index;

import java.util.List;
import java.util.Map;

public interface FormulaGenerator {
    List<String> initDef(Map<String, ColumnDefinition> columnDefinitionMap,
            Map<String, ? extends ColumnSource> columnsOverride);

    Formula getFormula(Index index, Class returnType, boolean initLazyMap,
            Map<String, ? extends ColumnSource> columnsToData,
            Map<String, ? extends ColumnSource> fallThroughColumns, boolean fallThroughContiguous,
            Map<String, ? extends ColumnSource> columnsOverride, Index overrideIndex);

    Class getReturnedType();

    List<String> getColumnArrays();

    boolean disallowRefresh();
}
