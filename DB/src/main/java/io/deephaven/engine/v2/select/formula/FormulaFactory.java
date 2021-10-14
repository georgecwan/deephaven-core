package io.deephaven.engine.v2.select.formula;

import io.deephaven.engine.tables.select.Param;
import io.deephaven.engine.v2.select.Formula;
import io.deephaven.engine.v2.sources.ColumnSource;
import io.deephaven.engine.v2.utils.Index;

import java.util.Map;

public interface FormulaFactory {
    Formula createFormula(Index index, boolean initLazyMap, Map<String, ? extends ColumnSource> columnsToData,
            Param... params);
}