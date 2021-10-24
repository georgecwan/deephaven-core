package io.deephaven.engine.v2;

import io.deephaven.engine.tables.Table;
import io.deephaven.engine.tables.live.LiveTableMonitor;
import io.deephaven.engine.tables.utils.TableTools;
import io.deephaven.engine.v2.utils.TrackingMutableRowSet;

import static io.deephaven.engine.v2.TstUtils.i;

public class TestEvenlyDividedTableMap extends LiveTableTestCase {
    public void testStatic() {
        final Table t = TableTools.emptyTable(1000000).update("K=k");
        final TableMap tm = EvenlyDividedTableMap.makeEvenlyDividedTableMap(t, 16, 100000);
        assertEquals(10, tm.size());
        final Table t2 = ((TransformableTableMap) tm.asTable().update("K2=K*2")).merge();
        TstUtils.assertTableEquals(t.update("K2=K*2"), t2);
    }

    public void testIncremental() {
        final QueryTable t = TstUtils.testRefreshingTable(TrackingMutableRowSet.FACTORY.getFlatIndex(1000000));
        final Table tu = LiveTableMonitor.DEFAULT.sharedLock().computeLocked(() -> t.update("K=k*2"));
        final Table tk2 = LiveTableMonitor.DEFAULT.sharedLock().computeLocked(() -> tu.update("K2=K*2"));
        final TableMap tm = EvenlyDividedTableMap.makeEvenlyDividedTableMap(tu, 16, 100000);
        assertEquals(10, tm.size());
        final Table t2 = LiveTableMonitor.DEFAULT.sharedLock()
                .computeLocked(() -> ((TransformableTableMap) tm.asTable().update("K2=K*2")).merge());
        TstUtils.assertTableEquals(tk2, t2);

        LiveTableMonitor.DEFAULT.runWithinUnitTestCycle(() -> {
            final TrackingMutableRowSet addedRows = TrackingMutableRowSet.FACTORY.getRowSetByRange(1000000, 1250000);
            TstUtils.addToTable(t, addedRows);
            t.notifyListeners(addedRows, i(), i());
        });

        TstUtils.assertTableEquals(tk2, t2);
        assertEquals(13, tm.size());
    }
}
