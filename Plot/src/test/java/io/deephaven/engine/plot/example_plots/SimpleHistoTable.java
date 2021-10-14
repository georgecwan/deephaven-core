/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */

package io.deephaven.engine.plot.example_plots;

import io.deephaven.engine.plot.Figure;
import io.deephaven.engine.plot.FigureFactory;
import io.deephaven.engine.tables.Table;
import io.deephaven.engine.tables.utils.TableTools;


public class SimpleHistoTable {

    public static void main(String[] args) {
        final Number[] x1 = {1, 2, 2, 3, 3, 3, 4};

        Table t = TableTools.newTable(TableTools.col("x1", x1));

        Figure fig = FigureFactory.figure();
        Figure cht = fig.newChart(0)
                .chartTitle("Chart Title");
        Figure axs = cht.newAxes()
                .xLabel("X")
                .yLabel("Y")
                .histPlot("Test1", t, "x1", 4).pointColor("red");

        ExamplePlotUtils.display(axs);
    }

}
