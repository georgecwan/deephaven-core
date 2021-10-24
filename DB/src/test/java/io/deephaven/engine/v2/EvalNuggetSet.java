/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */

package io.deephaven.engine.v2;

import io.deephaven.datastructures.util.SmartKey;
import io.deephaven.engine.tables.Table;
import io.deephaven.engine.tables.utils.TableTools;
import io.deephaven.engine.v2.sources.ColumnSource;
import io.deephaven.engine.v2.utils.TrackingMutableRowSet;
import org.junit.Assert;

import java.util.*;

public abstract class EvalNuggetSet extends EvalNugget {
    public EvalNuggetSet(String description) {
        super(description);
    }

    @Override
    public void validate(final String msg) {
        final Table expected = e();
        try {
            TableTools.show(expected);
            TableTools.show(originalValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Collection<? extends ColumnSource> sources = originalValue.getColumnSources();
        // TODO create a smart key for each row and go from there
        Map<SmartKey, Long> originalSet = new HashMap<>();
        Assert.assertEquals(expected.size(), originalValue.size());
        for (TrackingMutableRowSet.Iterator iterator = originalValue.getIndex().iterator(); iterator.hasNext();) {
            long next = iterator.nextLong();
            Object key[] = new Object[sources.size()];
            int i = 0;
            for (ColumnSource source : sources) {
                key[i++] = source.get(next);
            }
            final SmartKey k = new SmartKey(key);

            Assert.assertEquals(msg + " k = " + k, originalSet.put(k, next), null);
        }
        sources = expected.getColumnSources();
        for (TrackingMutableRowSet.Iterator iterator = expected.getIndex().iterator(); iterator.hasNext();) {
            long next = iterator.nextLong();
            Object key[] = new Object[sources.size()];
            int i = 0;
            for (ColumnSource source : sources) {
                key[i++] = source.get(next);
            }
            Assert.assertNotSame(msg, originalSet.remove(new SmartKey(key)), null);
        }

    }
}
