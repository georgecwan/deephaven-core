/**
 * Copyright (c) 2016-2022 Deephaven Data Labs and Patent Pending
 */
package io.deephaven.engine.table.impl.util;

import io.deephaven.base.Function;
import io.deephaven.engine.rowset.*;
import io.deephaven.engine.table.*;
import io.deephaven.engine.updategraph.UpdateGraphProcessor;
import io.deephaven.engine.table.impl.QueryTable;
import io.deephaven.engine.table.impl.BaseTable;
import io.deephaven.engine.table.impl.sources.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An abstract table that represents the result of a function.
 *
 * The table will run by regenerating the full values (using the tableGenerator Function passed in). The resultant
 * table's values are copied into the result table and appropriate listener notifications are fired.
 *
 * All the rows in the output table are modified on every tick, even if no actual changes occurred. The output table
 * also has a contiguous RowSet.
 *
 * The generator function must produce a V2 table, and the table definition must not change between invocations.
 *
 * If you are transforming a table, you should generally prefer to use the regular table operations as opposed to this
 * factory, because they are capable of performing some operations incrementally. However, for small tables this might
 * prove to require less development effort.
 */
public class FunctionGeneratedTableFactory {
    private final Function.Nullary<Table> tableGenerator;
    private final int refreshIntervalMs;
    private long nextRefresh;
    private final Map<String, WritableColumnSource<?>> writableSources = new LinkedHashMap<>();
    private final Map<String, ColumnSource<?>> columns = new LinkedHashMap<>();

    private final TrackingWritableRowSet rowSet;

    /**
     * Create a table that refreshes based on the value of your function, automatically called every refreshIntervalMs.
     *
     * @param tableGenerator a function returning a table to copy into the output table
     * @return a ticking table (assuming sourceTables have been specified) generated by tableGenerator
     */
    public static Table create(Function.Nullary<Table> tableGenerator, int refreshIntervalMs) {
        return new FunctionGeneratedTableFactory(tableGenerator, refreshIntervalMs).getTable();
    }

    /**
     * Create a table that refreshes based on the value of your function, automatically called when any of the
     * sourceTables tick.
     *
     * @param tableGenerator a function returning a table to copy into the output table
     * @param sourceTables The query engine does not know the details of your function inputs. If you are dependent on a
     *        ticking table tables in your tableGenerator function, you can add it to this list so that the function
     *        will be recomputed on each tick.
     * @return a ticking table (assuming sourceTables have been specified) generated by tableGenerator
     */
    public static Table create(Function.Nullary<Table> tableGenerator, Table... sourceTables) {
        final FunctionGeneratedTableFactory factory = new FunctionGeneratedTableFactory(tableGenerator, 0);

        final FunctionBackedTable result = factory.getTable();

        for (Table source : sourceTables) {
            source.addUpdateListener(new BaseTable.ListenerImpl("FunctionGeneratedTable", source, result) {
                @Override
                public void onUpdate(final TableUpdate upstream) {
                    result.doRefresh();
                }
            });
        }

        return result;
    }

    private FunctionGeneratedTableFactory(final Function.Nullary<Table> tableGenerator, final int refreshIntervalMs) {
        this.tableGenerator = tableGenerator;
        this.refreshIntervalMs = refreshIntervalMs;
        nextRefresh = System.currentTimeMillis() + this.refreshIntervalMs;

        Table initialTable = tableGenerator.call();
        for (Map.Entry<String, ? extends ColumnSource<?>> entry : initialTable.getColumnSourceMap().entrySet()) {
            ColumnSource<?> columnSource = entry.getValue();
            final WritableColumnSource<?> memoryColumnSource = ArrayBackedColumnSource.getMemoryColumnSource(
                    0, columnSource.getType(), columnSource.getComponentType());
            columns.put(entry.getKey(), memoryColumnSource);
            writableSources.put(entry.getKey(), memoryColumnSource);
        }

        copyTable(initialTable);

        // enable prev tracking after columns are initialized
        columns.values().forEach(ColumnSource::startTrackingPrevValues);

        rowSet = RowSetFactory.flat(initialTable.size()).toTracking();
    }

    private FunctionBackedTable getTable() {
        return new FunctionBackedTable(rowSet, columns);
    }

    private long updateTable() {
        Table newTable = tableGenerator.call();

        copyTable(newTable);

        return newTable.size();
    }

    private void copyTable(Table source) {
        final Map<String, ? extends ColumnSource<?>> sourceColumns = source.getColumnSourceMap();
        final ChunkSource.WithPrev[] sourceColumnsArray = new ChunkSource.WithPrev[sourceColumns.size()];
        final WritableColumnSource[] destColumnsArray = new WritableColumnSource[sourceColumns.size()];

        final RowSet sourceRowSet = source.getRowSet();
        int cc = 0;
        for (Map.Entry<String, ? extends ColumnSource<?>> entry : sourceColumns.entrySet()) {
            WritableColumnSource<?> destColumn = writableSources.get(entry.getKey());
            destColumn.ensureCapacity(sourceRowSet.size());
            sourceColumnsArray[cc] = entry.getValue();
            destColumnsArray[cc++] = destColumn;
        }

        // noinspection unchecked
        ChunkUtils.copyData(sourceColumnsArray, sourceRowSet, destColumnsArray,
                RowSequenceFactory.forRange(0, sourceRowSet.size() - 1),
                false);
    }

    /**
     * @implNote The constructor publishes {@code this} to the {@link UpdateGraphProcessor} and cannot be subclassed.
     */
    private final class FunctionBackedTable extends QueryTable implements Runnable {
        FunctionBackedTable(TrackingRowSet rowSet, Map<String, ColumnSource<?>> columns) {
            super(rowSet, columns);
            if (refreshIntervalMs >= 0) {
                setRefreshing(true);
                if (refreshIntervalMs > 0) {
                    UpdateGraphProcessor.DEFAULT.addSource(this);
                }
            }
        }

        @Override
        public void run() {
            if (System.currentTimeMillis() < nextRefresh) {
                return;
            }
            nextRefresh = System.currentTimeMillis() + refreshIntervalMs;

            doRefresh();
        }

        protected void doRefresh() {
            long size = rowSet.size();

            long newSize = updateTable();

            if (newSize < size) {
                final RowSet removed = RowSetFactory.fromRange(newSize, size - 1);
                rowSet.remove(removed);
                final RowSet modified = rowSet.copy();
                notifyListeners(RowSetFactory.empty(), removed, modified);
                return;
            }
            if (newSize > size) {
                final RowSet added = RowSetFactory.fromRange(size, newSize - 1);
                final RowSet modified = rowSet.copy();
                rowSet.insert(added);
                notifyListeners(added, RowSetFactory.empty(), modified);
                return;
            }
            if (size > 0) {
                // no size change, just modified
                final RowSet modified = rowSet.copy();
                notifyListeners(RowSetFactory.empty(), RowSetFactory.empty(), modified);
            }
        }

        @Override
        public void destroy() {
            super.destroy();
            if (refreshIntervalMs > 0) {
                UpdateGraphProcessor.DEFAULT.removeSource(this);
            }
        }
    }
}
