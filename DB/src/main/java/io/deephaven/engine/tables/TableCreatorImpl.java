package io.deephaven.engine.tables;

import io.deephaven.engine.tables.utils.DBDateTime;
import io.deephaven.engine.tables.utils.DBTimeUtils;
import io.deephaven.engine.tables.utils.TableTools;
import io.deephaven.engine.v2.InMemoryTable;
import io.deephaven.engine.v2.UpdatableTable;
import io.deephaven.engine.v2.utils.AppendOnlyArrayBackedMutableTable;
import io.deephaven.engine.v2.utils.KeyedArrayBackedMutableTable;
import io.deephaven.qst.TableCreator;
import io.deephaven.qst.table.EmptyTable;
import io.deephaven.qst.table.InMemoryAppendOnlyInputTable;
import io.deephaven.qst.table.InMemoryKeyBackedInputTable;
import io.deephaven.qst.table.InputTable;
import io.deephaven.qst.table.NewTable;
import io.deephaven.qst.table.TableHeader;
import io.deephaven.qst.table.TableSchema;
import io.deephaven.qst.table.TableSpec;
import io.deephaven.qst.table.TicketTable;
import io.deephaven.qst.table.TimeProvider;
import io.deephaven.qst.table.TimeProviderSystem;
import io.deephaven.qst.table.TimeTable;

import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

enum TableCreatorImpl implements TableCreator<Table> {
    INSTANCE;

    public static Table create(TableSpec table) {
        return table.logic().create(INSTANCE);
    }

    @Override
    public final Table of(NewTable newTable) {
        return InMemoryTable.from(newTable);
    }

    @Override
    public final Table of(EmptyTable emptyTable) {
        return TableTools.emptyTable(emptyTable.size());
    }

    @Override
    public final Table of(TimeTable timeTable) {
        final io.deephaven.engine.v2.utils.TimeProvider provider = TimeProviderAdapter
                .of(timeTable.timeProvider());
        final DBDateTime firstTime = timeTable.startTime().map(DBDateTime::of).orElse(null);
        return TableTools.timeTable(provider, firstTime, timeTable.interval().toNanos());
    }

    @Override
    public final Table of(TicketTable ticketTable) {
        throw new UnsupportedOperationException("Ticket tables can't be referenced in a static context;" +
                "no access to TicketRouter nor SessionState - see deephaven-core#1172 for more details");
    }

    @Override
    public final UpdatableTable of(InputTable inputTable) {
        return UpdatableTableAdapter.of(inputTable);
    }


    @Override
    public final Table merge(Iterable<Table> tables) {
        return TableTools.merge(StreamSupport.stream(tables.spliterator(), false).toArray(Table[]::new));
    }

    @Override
    public final Table merge(Table t1, Table t2) {
        return TableTools.merge(t1, t2);
    }

    @Override
    public final Table merge(Table t1, Table t2, Table t3) {
        return TableTools.merge(t1, t2, t3);
    }

    @Override
    public final Table merge(Table t1, Table t2, Table t3, Table t4) {
        return TableTools.merge(t1, t2, t3, t4);
    }

    @Override
    public final Table merge(Table t1, Table t2, Table t3, Table t4, Table t5) {
        return TableTools.merge(t1, t2, t3, t4, t5);
    }

    @Override
    public final Table merge(Table t1, Table t2, Table t3, Table t4, Table t5, Table t6) {
        return TableTools.merge(t1, t2, t3, t4, t5, t6);
    }

    @Override
    public final Table merge(Table t1, Table t2, Table t3, Table t4, Table t5, Table t6, Table t7) {
        return TableTools.merge(t1, t2, t3, t4, t5, t6, t7);
    }

    @Override
    public final Table merge(Table t1, Table t2, Table t3, Table t4, Table t5, Table t6, Table t7, Table t8) {
        return TableTools.merge(t1, t2, t3, t4, t5, t6, t7, t8);
    }

    @Override
    public final Table merge(Table t1, Table t2, Table t3, Table t4, Table t5, Table t6, Table t7, Table t8, Table t9) {
        return TableTools.merge(t1, t2, t3, t4, t5, t6, t7, t8, t9);
    }

    @Override
    public final Table merge(Table t1, Table t2, Table t3, Table t4, Table t5, Table t6, Table t7, Table t8, Table t9,
            Table... remaining) {
        return TableTools.merge(Stream.concat(Stream.of(t1, t2, t3, t4, t5, t6, t7, t8, t9), Stream.of(remaining))
                .toArray(Table[]::new));
    }

    @Override
    public final Table merge(Table[] tables) {
        return TableTools.merge(tables);
    }

    static class TimeProviderAdapter implements TimeProvider.Visitor {

        public static io.deephaven.engine.v2.utils.TimeProvider of(TimeProvider provider) {
            return provider.walk(new TimeProviderAdapter()).getOut();
        }

        private static final io.deephaven.engine.v2.utils.TimeProvider SYSTEM_PROVIDER = DBTimeUtils::currentTime;

        private io.deephaven.engine.v2.utils.TimeProvider out;

        public io.deephaven.engine.v2.utils.TimeProvider getOut() {
            return Objects.requireNonNull(out);
        }

        @Override
        public void visit(TimeProviderSystem system) {
            out = SYSTEM_PROVIDER;
        }
    }

    static class UpdatableTableAdapter implements InputTable.Visitor {

        public static UpdatableTable of(InputTable inputTable) {
            return inputTable.walk(new UpdatableTableAdapter()).out();
        }

        private UpdatableTable out;

        public UpdatableTable out() {
            return Objects.requireNonNull(out);
        }

        @Override
        public void visit(InMemoryAppendOnlyInputTable inMemoryAppendOnly) {
            final TableDefinition definition = DefinitionAdapter.of(inMemoryAppendOnly.schema());
            out = AppendOnlyArrayBackedMutableTable.make(definition);
        }

        @Override
        public void visit(InMemoryKeyBackedInputTable inMemoryKeyBacked) {
            final TableDefinition definition = DefinitionAdapter.of(inMemoryKeyBacked.schema());
            final String[] keyColumnNames = inMemoryKeyBacked.keys().toArray(String[]::new);
            out = KeyedArrayBackedMutableTable.make(definition, keyColumnNames);
        }
    }

    static class DefinitionAdapter implements TableSchema.Visitor {

        public static TableDefinition of(TableSchema schema) {
            return schema.walk(new DefinitionAdapter()).out();
        }

        private TableDefinition out;

        public TableDefinition out() {
            return Objects.requireNonNull(out);
        }

        @Override
        public void visit(TableSpec spec) {
            out = create(spec).getDefinition();
        }

        @Override
        public void visit(TableHeader header) {
            out = TableDefinition.from(header);
        }
    }
}