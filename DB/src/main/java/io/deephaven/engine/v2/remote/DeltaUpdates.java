/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */

package io.deephaven.engine.v2.remote;

import io.deephaven.engine.v2.Listener;
import io.deephaven.engine.v2.ModifiedColumnSet;
import io.deephaven.engine.rowset.RowSet;
import io.deephaven.engine.v2.utils.RowSetShiftData;

import java.io.Serializable;

public class DeltaUpdates implements Serializable, Cloneable {

    public static class ColumnAdditions implements Serializable {
        public int columnIndex;
        public Object serializedRows;
    }
    public static class ColumnModifications implements Serializable {
        public int columnIndex;
        public RowSet modified;
        public RowSet rowsIncluded;
        public Object serializedRows;
    }

    public long deltaSequence;
    public long firstStep;
    public long lastStep;
    public RowSet added;
    public RowSet removed;
    public RowSetShiftData shifted;
    public RowSet includedAdditions;
    public ColumnAdditions[] serializedAdditions;
    public ColumnModifications[] serializedModifications;

    public DeltaUpdates clone() {
        try {
            return (DeltaUpdates) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    public Listener.Update asUpdate(RowSet modified, ModifiedColumnSet mcs) {
        return new Listener.Update(added, removed, modified, shifted, mcs);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("added=").append(added);
        builder.append(", removed=").append(removed);
        builder.append(", shifted=").append(shifted);
        builder.append(", includedAdditions=").append(includedAdditions);
        if (serializedAdditions != null) {
            builder.append(", serializedAdditions={");
            for (int i = 0; i < serializedAdditions.length; ++i) {
                if (i != 0) {
                    builder.append(",");
                } else {
                    builder.append(serializedAdditions[i].columnIndex);
                }
            }
            builder.append("}");
        } else {
            builder.append(", serializedAdditions=null");
        }
        if (serializedModifications != null) {
            builder.append(", serializedModifications={i=");
            for (int i = 0; i < serializedModifications.length; ++i) {
                if (i != 0) {
                    builder.append(";");
                } else {
                    builder.append(serializedModifications[i].columnIndex);
                    builder.append(",modified=");
                    builder.append(serializedModifications[i].modified);
                    builder.append(",rowsIncluded=");
                    builder.append(serializedModifications[i].rowsIncluded);
                }
            }
            builder.append("}");
        } else {
            builder.append(", serializedModifications=null");
        }
        return builder.toString();
    }
}
