package io.deephaven.engine.v2.select;


import java.io.Serializable;

public abstract class WhereFilterImpl implements WhereFilter, Serializable {
    private boolean isAutomatedFilter = false;

    @Override
    public boolean isAutomatedFilter() {
        return isAutomatedFilter;
    }

    @Override
    public void setAutomatedFilter(boolean value) {
        isAutomatedFilter = value;
    }
}