/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */

package io.deephaven.engine.plot.datasets.data;

import io.deephaven.engine.plot.errors.PlotExceptionCause;
import io.deephaven.engine.plot.errors.PlotInfo;

import java.io.Serializable;

/**
 * Dataset where values can be retrieved via an integer rowSet.
 */
public abstract class IndexableData<T> implements Serializable, PlotExceptionCause {

    private final PlotInfo plotInfo;

    /**
     * @param plotInfo plot information
     */
    public IndexableData(final PlotInfo plotInfo) {
        this.plotInfo = plotInfo;
    }

    /**
     * Gets the size of this dataset.
     *
     * @return the size of this dataset
     */
    public abstract int size();

    /**
     * Gets the value at the given rowSet.
     *
     * @param index rowSet
     * @return value of this dataset at {@code rowSet}
     */
    public abstract T get(int index);

    @Override
    public PlotInfo getPlotInfo() {
        return plotInfo;
    }
}
