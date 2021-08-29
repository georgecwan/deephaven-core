package io.deephaven.engine.v2.select.analyzers;

import io.deephaven.engine.v2.ModifiedColumnSet;
import io.deephaven.engine.v2.ShiftAwareListener;
import io.deephaven.engine.v2.select.SelectColumn;
import io.deephaven.engine.v2.sources.ColumnSource;
import io.deephaven.engine.structures.rowset.ReadOnlyIndex;

final public class ViewColumnLayer extends SelectOrViewColumnLayer {
    ViewColumnLayer(SelectAndViewAnalyzer inner, String name, SelectColumn sc, ColumnSource cs, String[] deps,
            ModifiedColumnSet mcsBuilder) {
        super(inner, name, sc, cs, null, deps, mcsBuilder);
    }

    @Override
    public void applyUpdate(ShiftAwareListener.Update upstream, ReadOnlyIndex toClear, UpdateHelper helper) {
        // To be parallel with SelectColumnLayer, we would recurse here, but since this is ViewColumnLayer
        // (and all my inner layers are ViewColumnLayer), there's nothing to do.
    }
}
