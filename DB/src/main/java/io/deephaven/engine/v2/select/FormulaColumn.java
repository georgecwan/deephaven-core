package io.deephaven.engine.v2.select;

import io.deephaven.engine.table.ColumnSource;
import io.deephaven.engine.v2.sources.WritableSource;

public interface FormulaColumn extends SelectColumn {

    static FormulaColumn createFormulaColumn(String columnName, String formulaString,
            FormulaParserConfiguration parser) {
        switch (parser) {
            case Deephaven:
                return new DhFormulaColumn(columnName, formulaString);
            case Numba:
                throw new UnsupportedOperationException("Python formula columns must be created from python");
            default:
                throw new UnsupportedOperationException("Parser support not implemented for " + parser);
        }
    }

    static FormulaColumn createFormulaColumn(String columnName, String formulaString) {
        return createFormulaColumn(columnName, formulaString, FormulaParserConfiguration.parser);
    }

    ColumnSource<?> updateData(WritableSource<?> result, long destPos, long sourcePos);
}
