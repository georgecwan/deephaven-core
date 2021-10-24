/* ---------------------------------------------------------------------------------------------------------------------
 * AUTO-GENERATED CLASS - DO NOT EDIT MANUALLY - for any changes edit TestCharacterArraySource and regenerate
 * ------------------------------------------------------------------------------------------------------------------ */
package io.deephaven.engine.v2.sources;

import io.deephaven.engine.tables.live.LiveTableMonitor;
import io.deephaven.engine.v2.select.FormulaColumn;
import io.deephaven.engine.v2.sources.chunk.*;
import io.deephaven.engine.v2.sources.chunk.Attributes.RowKeys;
import io.deephaven.engine.v2.sources.chunk.Attributes.Values;
import io.deephaven.engine.v2.utils.SequentialRowSetBuilder;
import io.deephaven.engine.v2.utils.TrackingMutableRowSet;
import io.deephaven.engine.structures.RowSequence;
import io.deephaven.util.Shuffle;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.LongStream;

import static io.deephaven.engine.v2.sources.ArrayGenerator.indexDataGenerator;
import static io.deephaven.util.QueryConstants.NULL_SHORT;
import static junit.framework.TestCase.*;

public class TestShortArraySource {
    private ShortArraySource forArray(short[] values) {
        final ShortArraySource source = new ShortArraySource();
        source.ensureCapacity(values.length);
        for (int i = 0; i < values.length; i++) {
            source.set(i, values[i]);
        }
        source.startTrackingPrevValues();
        return source;
    }

    private void updateFromArray(ShortArraySource dest, short[] values) {
        dest.ensureCapacity(values.length);
        for (int i = 0; i < values.length; i++) {
            dest.set(i, values[i]);
        }
    }

    @Before
    public void setUp() throws Exception {
        LiveTableMonitor.DEFAULT.enableUnitTestMode();
        LiveTableMonitor.DEFAULT.resetForUnitTests(false);
    }

    @After
    public void tearDown() throws Exception {
        LiveTableMonitor.DEFAULT.resetForUnitTests(true);
    }

    private void testGetChunkGeneric(short[] values, short[] newValues, int chunkSize, TrackingMutableRowSet rowSet) {
        final ShortArraySource source;
        LiveTableMonitor.DEFAULT.startCycleForUnitTests();
        try {
            source = forArray(values);
            validateValues(chunkSize, values, rowSet, source);
        } finally {
            LiveTableMonitor.DEFAULT.completeCycleForUnitTests();
        }
        LiveTableMonitor.DEFAULT.startCycleForUnitTests();
        try {
            updateFromArray(source, newValues);
            validateValues(chunkSize, newValues, rowSet, source);
            validatePrevValues(chunkSize, values, rowSet, source);
        } finally {
            LiveTableMonitor.DEFAULT.completeCycleForUnitTests();
        }
    }

    private void validateValues(int chunkSize, short[] values, TrackingMutableRowSet rowSet, ShortArraySource source) {
        final RowSequence.Iterator rsIterator = rowSet.getRowSequenceIterator();
        final TrackingMutableRowSet.Iterator it = rowSet.iterator();
        final ChunkSource.GetContext context = source.makeGetContext(chunkSize);
        long pos = 0;
        while (it.hasNext()) {
            assertTrue(rsIterator.hasMore());
            final RowSequence okChunk = rsIterator.getNextRowSequenceWithLength(chunkSize);
            final ShortChunk chunk = source.getChunk(context, okChunk).asShortChunk();
            assertTrue(chunk.size() <= chunkSize);
            if (rsIterator.hasMore()) {
                assertEquals(chunkSize, chunk.size());
            }
            for (int i = 0; i < chunk.size(); i++) {
                assertTrue(it.hasNext());
                final long idx = it.nextLong();
                checkFromSource("idx=" + idx + ", i=" + i, source.getShort(idx), chunk.get(i));
                checkFromValues("idx=" + idx + ", i=" + i, values[(int) idx], chunk.get(i));
                pos++;
            }
            // region samecheck
            final LongChunk<Attributes.OrderedRowKeyRanges> ranges = okChunk.asRowKeyRangesChunk();
            if (ranges.size() > 2 || ranges.get(0) / ShortArraySource.BLOCK_SIZE != (ranges.get(1) / ShortArraySource.BLOCK_SIZE)) {
                assertTrue(DefaultGetContext.isMyWritableChunk(context, chunk));

            } else {
                assertTrue(DefaultGetContext.isMyResettableChunk(context, chunk));
            }
            // endregion samecheck
        }
        assertEquals(pos, rowSet.size());
    }


    private void validatePrevValues(int chunkSize, short[] values, TrackingMutableRowSet rowSet, ShortArraySource source) {
        final RowSequence.Iterator rsIterator = rowSet.getRowSequenceIterator();
        final TrackingMutableRowSet.Iterator it = rowSet.iterator();
        final ChunkSource.GetContext context = source.makeGetContext(chunkSize);
        long pos = 0;
        while (it.hasNext()) {
            assertTrue(rsIterator.hasMore());
            final RowSequence okChunk = rsIterator.getNextRowSequenceWithLength(chunkSize);
            final ShortChunk chunk = source.getPrevChunk(context, okChunk).asShortChunk();
            for (int i = 0; i < chunk.size(); i++) {
                assertTrue(it.hasNext());
                final long idx = it.nextLong();
                checkFromSource(source.getPrevShort(idx), chunk.get(i));
                checkFromValues(values[(int) idx], chunk.get(i));
                pos++;
            }
            assertTrue(DefaultGetContext.isMyWritableChunk(context, chunk));
        }
        assertEquals(pos, rowSet.size());
    }

    @Test
    public void testGetChunk() {
        final Random random = new Random(0);
        testGetChunkGeneric(new short[0], new short[0], 1, TrackingMutableRowSet.FACTORY.getRowSetByValues());
        testGetChunkGeneric(new short[0], new short[0], 16, TrackingMutableRowSet.FACTORY.getRowSetByValues());

        testGetChunkGeneric(ArrayGenerator.randomShorts(random, 16), ArrayGenerator.randomShorts(random, 16), 1, TrackingMutableRowSet.FACTORY.getRowSetByValues(0));
        testGetChunkGeneric(ArrayGenerator.randomShorts(random, 16), ArrayGenerator.randomShorts(random, 16), 1, TrackingMutableRowSet.FACTORY.getRowSetByValues(0, 1));
        testGetChunkGeneric(ArrayGenerator.randomShorts(random, 16), ArrayGenerator.randomShorts(random, 16), 1, TrackingMutableRowSet.FACTORY.getRowSetByValues(4));
        testGetChunkGeneric(ArrayGenerator.randomShorts(random, 16), ArrayGenerator.randomShorts(random, 16), 1, TrackingMutableRowSet.FACTORY.getRowSetByValues(4, 5, 6));
        testGetChunkGeneric(ArrayGenerator.randomShorts(random, 16), ArrayGenerator.randomShorts(random, 16), 1, TrackingMutableRowSet.FACTORY.getRowSetByValues(4,  6));
        testGetChunkGeneric(ArrayGenerator.randomShorts(random, 16), ArrayGenerator.randomShorts(random, 16), 1, TrackingMutableRowSet.FACTORY.getRowSetByValues(4, 5, 6, 7, 8));
        testGetChunkGeneric(ArrayGenerator.randomShorts(random, 16), ArrayGenerator.randomShorts(random, 16), 3, TrackingMutableRowSet.FACTORY.getRowSetByValues(5, 6, 7));
        testGetChunkGeneric(ArrayGenerator.randomShorts(random, 16), ArrayGenerator.randomShorts(random, 16), 4, TrackingMutableRowSet.FACTORY.getRowSetByValues(4, 5, 6, 7));
        testGetChunkGeneric(ArrayGenerator.randomShorts(random, 16), ArrayGenerator.randomShorts(random, 16), 5, TrackingMutableRowSet.FACTORY.getRowSetByValues(4, 5, 6, 7, 8));
        testGetChunkGeneric(ArrayGenerator.randomShorts(random, 512), ArrayGenerator.randomShorts(random, 512), 4, TrackingMutableRowSet.FACTORY.getRowSetByValues(254, 255, 256, 257));
        testGetChunkGeneric(ArrayGenerator.randomShorts(random, 512), ArrayGenerator.randomShorts(random, 512), 5, TrackingMutableRowSet.FACTORY.getRowSetByValues(254, 255, 256, 257, 258));

        for (int sourceSize = 32; sourceSize < 4096; sourceSize *= 4) {
            for (int v = -4; v < 5; v++) {
                testForGivenSourceSize(random, sourceSize + v);
            }
        }

        //References to block test
    }

    // region lazy
    private void testGetChunkGenericLazy(short[] values, int chunkSize, TrackingMutableRowSet rowSet) {
        final ShortArraySource sourceOrigin = forArray(values);
        final FormulaColumn formulaColumn = FormulaColumn.createFormulaColumn("Foo", "origin");
        final SequentialRowSetBuilder sequentialBuilder = TrackingMutableRowSet.FACTORY.getSequentialBuilder();
        if (values.length > 0) {
            sequentialBuilder.appendRange(0, values.length - 1);
        }
        final TrackingMutableRowSet fullRange = sequentialBuilder.build();
        final Map<String, ShortArraySource> oneAndOnly = new HashMap<>();
        oneAndOnly.put("origin", sourceOrigin);
        formulaColumn.initInputs(fullRange, oneAndOnly);
        final ColumnSource<?> source = formulaColumn.getDataView();
        final RowSequence.Iterator rsIterator = rowSet.getRowSequenceIterator();
        final TrackingMutableRowSet.Iterator it = rowSet.iterator();
        final ChunkSource.GetContext context = source.makeGetContext(chunkSize);
        long pos = 0;
        while (it.hasNext()) {
            assertTrue(rsIterator.hasMore());
            final RowSequence okChunk = rsIterator.getNextRowSequenceWithLength(chunkSize);
            final ShortChunk chunk = source.getChunk(context, okChunk).asShortChunk();
            for (int i = 0; i < chunk.size(); i++) {
                assertTrue(it.hasNext());
                assertEquals(chunk.get(i), source.getShort(it.nextLong()));
                pos++;
            }
        }
        assertEquals(pos, rowSet.size());
    }
    // endregion lazy

    @Test
    public void testGetChunkLazy() {
        final Random random = new Random(0);
        testGetChunkGenericLazy(new short[0], 1, TrackingMutableRowSet.FACTORY.getRowSetByValues());
        testGetChunkGenericLazy(new short[0], 16, TrackingMutableRowSet.FACTORY.getRowSetByValues());

        testGetChunkGenericLazy(ArrayGenerator.randomShorts(random, 16), 1, TrackingMutableRowSet.FACTORY.getRowSetByValues(0));
        testGetChunkGenericLazy(ArrayGenerator.randomShorts(random, 16), 1, TrackingMutableRowSet.FACTORY.getRowSetByValues(0, 1));
        testGetChunkGenericLazy(ArrayGenerator.randomShorts(random, 16), 1, TrackingMutableRowSet.FACTORY.getRowSetByValues(4));
        testGetChunkGenericLazy(ArrayGenerator.randomShorts(random, 16), 1, TrackingMutableRowSet.FACTORY.getRowSetByValues(4, 5, 6));
        testGetChunkGenericLazy(ArrayGenerator.randomShorts(random, 16), 1, TrackingMutableRowSet.FACTORY.getRowSetByValues(4,  6));
        testGetChunkGenericLazy(ArrayGenerator.randomShorts(random, 16), 1, TrackingMutableRowSet.FACTORY.getRowSetByValues(4, 5, 6, 7, 8));
        testGetChunkGenericLazy(ArrayGenerator.randomShorts(random, 16), 3, TrackingMutableRowSet.FACTORY.getRowSetByValues(5, 6, 7));
        testGetChunkGenericLazy(ArrayGenerator.randomShorts(random, 16), 4, TrackingMutableRowSet.FACTORY.getRowSetByValues(4, 5, 6, 7));
        testGetChunkGenericLazy(ArrayGenerator.randomShorts(random, 16), 5, TrackingMutableRowSet.FACTORY.getRowSetByValues(4, 5, 6, 7, 8));
        testGetChunkGenericLazy(ArrayGenerator.randomShorts(random, 512), 4, TrackingMutableRowSet.FACTORY.getRowSetByValues(254, 255, 256, 257));
        testGetChunkGenericLazy(ArrayGenerator.randomShorts(random, 512), 5, TrackingMutableRowSet.FACTORY.getRowSetByValues(254, 255, 256, 257, 258));

        for (int sourceSize = 512; sourceSize < 4096; sourceSize *= 4) {
            for (int v = -2; v < 3; v += 2) {
                testForGivenSourceLazySize(random, sourceSize + v);
            }
        }

    }

    private void testForGivenSourceLazySize(Random random, int sourceSize) {
        final short[] values = ArrayGenerator.randomShorts(random, sourceSize);
        for (int indexSize = 2; indexSize < sourceSize; indexSize *= 4) {
            testIndexSizeVariationsLazy(random, sourceSize, values, indexSize);
        }
    }

    private void testIndexSizeVariationsLazy(Random random, int sourceSize, short[] values, int indexSize) {
        testParameterChunkAndIndexLazy(random, sourceSize, values, indexSize - 1);
        testParameterChunkAndIndexLazy(random, sourceSize, values, indexSize);
        testParameterChunkAndIndexLazy(random, sourceSize, values, indexSize + 1);
    }

    private void testParameterChunkAndIndexLazy(Random random, int sourceSize, short[] values, int indexSize) {
        final TrackingMutableRowSet rowSet = TrackingMutableRowSet.FACTORY.getRowSetByValues(indexDataGenerator(random, indexSize, .1, sourceSize / indexSize, sourceSize));
        for (int chunkSize = 2; chunkSize < sourceSize; chunkSize *= 4) {
            testGetChunkGenericLazy(values, chunkSize, rowSet);
            testGetChunkGenericLazy(values, chunkSize + 1, rowSet);
            testGetChunkGenericLazy(values, chunkSize - 1, rowSet);
        }
    }


    private void testForGivenSourceSize(Random random, int sourceSize) {
        final short[] values = ArrayGenerator.randomShorts(random, sourceSize);
        final short[] newValues = ArrayGenerator.randomShorts(random, sourceSize);
        for (int indexSize = 2; indexSize < sourceSize; indexSize *= 2) {
            testIndexSizeVariations(random, sourceSize, values, newValues, indexSize);
        }
    }

    private void testIndexSizeVariations(Random random, int sourceSize, short[] values, short[] newvalues, int indexSize) {
        testParameterChunkAndIndex(random, sourceSize, values, newvalues, indexSize - 1);
        testParameterChunkAndIndex(random, sourceSize, values, newvalues, indexSize);
        testParameterChunkAndIndex(random, sourceSize, values, newvalues, indexSize + 1);
    }

    private void testParameterChunkAndIndex(Random random, int sourceSize, short[] values, short[] newvalues, int indexSize) {
        final TrackingMutableRowSet rowSet = TrackingMutableRowSet.FACTORY.getRowSetByValues(indexDataGenerator(random, indexSize, .1, sourceSize / indexSize, sourceSize));
        for (int chunkSize = 2; chunkSize < sourceSize; chunkSize *= 2) {
            testGetChunkGeneric(values, newvalues, chunkSize, rowSet);
            testGetChunkGeneric(values, newvalues, chunkSize + 1, rowSet);
            testGetChunkGeneric(values, newvalues, chunkSize - 1, rowSet);
        }
    }

    private void testFillChunkGeneric(short[] values, short[] newValues, int chunkSize, TrackingMutableRowSet rowSet) {
        final ShortArraySource source;
        LiveTableMonitor.DEFAULT.startCycleForUnitTests();
        try {
            source = forArray(values);
            validateValuesWithFill(chunkSize, values, rowSet, source);
        } finally {
            LiveTableMonitor.DEFAULT.completeCycleForUnitTests();
        }
        LiveTableMonitor.DEFAULT.startCycleForUnitTests();
        try {
            updateFromArray(source, newValues);
            validateValuesWithFill(chunkSize, newValues, rowSet, source);
            validatePrevValuesWithFill(chunkSize, values, rowSet, source);
        } finally {
            LiveTableMonitor.DEFAULT.completeCycleForUnitTests();
        }
    }

    private void validateValuesWithFill(int chunkSize, short[] values, TrackingMutableRowSet rowSet, ShortArraySource source) {
        final RowSequence.Iterator rsIterator = rowSet.getRowSequenceIterator();
        final TrackingMutableRowSet.Iterator it = rowSet.iterator();
        final ColumnSource.FillContext context = source.makeFillContext(chunkSize);
        final WritableShortChunk<Values> chunk = WritableShortChunk.makeWritableChunk(chunkSize);
        long pos = 0;
        while (it.hasNext()) {
            assertTrue(rsIterator.hasMore());
            final RowSequence okChunk = rsIterator.getNextRowSequenceWithLength(chunkSize);
            source.fillChunk(context, chunk, okChunk);
            for (int i = 0; i < chunk.size(); i++) {
                assertTrue(it.hasNext());
                final long idx = it.nextLong();
                checkFromSource(source.getShort(idx), chunk.get(i));
                checkFromValues(values[(int)idx], chunk.get(i));
                pos++;
            }
        }
        assertEquals(pos, rowSet.size());
    }

    private void validatePrevValuesWithFill(int chunkSize, short[] values, TrackingMutableRowSet rowSet, ShortArraySource source) {
        final RowSequence.Iterator rsIterator = rowSet.getRowSequenceIterator();
        final TrackingMutableRowSet.Iterator it = rowSet.iterator();
        final ColumnSource.FillContext context = source.makeFillContext(chunkSize);
        final WritableShortChunk<Values> chunk = WritableShortChunk.makeWritableChunk(chunkSize);
        long pos = 0;
        while (it.hasNext()) {
            assertTrue(rsIterator.hasMore());
            final RowSequence okChunk = rsIterator.getNextRowSequenceWithLength(chunkSize);
            source.fillPrevChunk(context, chunk, okChunk);
            for (int i = 0; i < chunk.size(); i++) {
                assertTrue(it.hasNext());
                final long idx = it.nextLong();
                checkFromSource(source.getPrevShort(idx), chunk.get(i));
                checkFromValues(values[(int)idx], chunk.get(i));
                pos++;
            }
        }
        assertEquals(pos, rowSet.size());
    }

    @Test
    public void testFillChunk() {
        final Random random = new Random(0);
        testFillChunkGeneric(new short[0], new short[0], 1, TrackingMutableRowSet.FACTORY.getRowSetByValues());
        testFillChunkGeneric(new short[0], new short[0], 16, TrackingMutableRowSet.FACTORY.getRowSetByValues());

        testFillChunkGeneric(ArrayGenerator.randomShorts(random, 16), ArrayGenerator.randomShorts(random, 16), 1, TrackingMutableRowSet.FACTORY.getRowSetByValues(0));
        testFillChunkGeneric(ArrayGenerator.randomShorts(random, 16), ArrayGenerator.randomShorts(random, 16), 1, TrackingMutableRowSet.FACTORY.getRowSetByValues(0, 1));
        testFillChunkGeneric(ArrayGenerator.randomShorts(random, 16), ArrayGenerator.randomShorts(random, 16), 1, TrackingMutableRowSet.FACTORY.getRowSetByValues(4));
        testFillChunkGeneric(ArrayGenerator.randomShorts(random, 16), ArrayGenerator.randomShorts(random, 16), 1, TrackingMutableRowSet.FACTORY.getRowSetByValues(4, 5, 6));
        testFillChunkGeneric(ArrayGenerator.randomShorts(random, 16), ArrayGenerator.randomShorts(random, 16), 1, TrackingMutableRowSet.FACTORY.getRowSetByValues(4,  6));
        testFillChunkGeneric(ArrayGenerator.randomShorts(random, 16), ArrayGenerator.randomShorts(random, 16), 1, TrackingMutableRowSet.FACTORY.getRowSetByValues(4, 5, 6, 7, 8));
        testFillChunkGeneric(ArrayGenerator.randomShorts(random, 16), ArrayGenerator.randomShorts(random, 16), 3, TrackingMutableRowSet.FACTORY.getRowSetByValues(5, 6, 7));
        testFillChunkGeneric(ArrayGenerator.randomShorts(random, 16), ArrayGenerator.randomShorts(random, 16), 4, TrackingMutableRowSet.FACTORY.getRowSetByValues(4, 5, 6, 7));
        testFillChunkGeneric(ArrayGenerator.randomShorts(random, 16), ArrayGenerator.randomShorts(random, 16), 5, TrackingMutableRowSet.FACTORY.getRowSetByValues(4, 5, 6, 7, 8));
        testFillChunkGeneric(ArrayGenerator.randomShorts(random, 512), ArrayGenerator.randomShorts(random, 512), 4, TrackingMutableRowSet.FACTORY.getRowSetByValues(254, 255, 256, 257));
        testFillChunkGeneric(ArrayGenerator.randomShorts(random, 512), ArrayGenerator.randomShorts(random, 512), 5, TrackingMutableRowSet.FACTORY.getRowSetByValues(254, 255, 256, 257, 258));

        for (int sourceSize = 32; sourceSize < 8192; sourceSize *= 4) {
            for (int v = -4; v < 5; v += 2) {
                testSetForGivenSourceSize(random, sourceSize + v);
            }
        }
    }

    private void testSetForGivenSourceSize(Random random, int sourceSize) {
        final short[] values = ArrayGenerator.randomShorts(random, sourceSize);
        final short[] newValues = ArrayGenerator.randomShorts(random, sourceSize);
        for (int indexSize = 2; indexSize < sourceSize; indexSize *= 4) {
            testFillChunkIndexSizeVariations(random, sourceSize, values, newValues, indexSize);
        }
    }

    private void testFillChunkIndexSizeVariations(Random random, int sourceSize, short[] values, short[] newValues, int indexSize) {
        testParameterFillChunkAndIndex(random, sourceSize, values, newValues, indexSize - 1);
        testParameterFillChunkAndIndex(random, sourceSize, values, newValues, indexSize);
        testParameterFillChunkAndIndex(random, sourceSize, values, newValues, indexSize + 1);
    }

    private void testParameterFillChunkAndIndex(Random random, int sourceSize, short[] values, short[] newValues, int indexSize) {
        final TrackingMutableRowSet rowSet = TrackingMutableRowSet.FACTORY.getRowSetByValues(indexDataGenerator(random, indexSize, .1, sourceSize / indexSize, sourceSize));
        for (int chunkSize = 2; chunkSize < sourceSize; chunkSize *= 2) {
            testFillChunkGeneric(values, newValues, chunkSize, rowSet);
            testFillChunkGeneric(values, newValues, chunkSize + 1, rowSet);
            testFillChunkGeneric(values, newValues, chunkSize - 1, rowSet);
        }
    }

    // region lazygeneric
    private void testFillChunkLazyGeneric(short[] values, int chunkSize, TrackingMutableRowSet rowSet) {
        final ShortArraySource sourceOrigin = forArray(values);
        final FormulaColumn formulaColumn = FormulaColumn.createFormulaColumn("Foo", "origin");
        final SequentialRowSetBuilder sequentialBuilder = TrackingMutableRowSet.FACTORY.getSequentialBuilder();
        if (values.length > 0) {
            sequentialBuilder.appendRange(0, values.length - 1);
        }
        final TrackingMutableRowSet fullRange = sequentialBuilder.build();
        final Map<String, ShortArraySource> oneAndOnly = new HashMap<>();
        oneAndOnly.put("origin", sourceOrigin);
        formulaColumn.initInputs(fullRange, oneAndOnly);
        final ColumnSource source = formulaColumn.getDataView();
        final RowSequence.Iterator rsIterator = rowSet.getRowSequenceIterator();
        final TrackingMutableRowSet.Iterator it = rowSet.iterator();
        final ColumnSource.FillContext context = source.makeFillContext(chunkSize);
        final WritableShortChunk<Values> chunk = WritableShortChunk.makeWritableChunk(chunkSize);
        long pos = 0;
        while (it.hasNext()) {
            assertTrue(rsIterator.hasMore());
            final RowSequence okChunk = rsIterator.getNextRowSequenceWithLength(chunkSize);
            source.fillChunk(context, chunk, okChunk);
            for (int i = 0; i < chunk.size(); i++) {
                assertTrue(it.hasNext());
                final long idx = it.nextLong();
                assertEquals(chunk.get(i), source.getShort(idx));
                pos++;
            }
        }
        assertEquals(pos, rowSet.size());
    }
    // endregion lazygeneric


    @Test
    public void testFillChunkLazy() {
        final Random random = new Random(0);
        testFillChunkLazyGeneric(new short[0], 1, TrackingMutableRowSet.FACTORY.getRowSetByValues());
        testFillChunkLazyGeneric(new short[0], 16, TrackingMutableRowSet.FACTORY.getRowSetByValues());

        testFillChunkLazyGeneric(ArrayGenerator.randomShorts(random, 16), 1, TrackingMutableRowSet.FACTORY.getRowSetByValues(0));
        testFillChunkLazyGeneric(ArrayGenerator.randomShorts(random, 16), 1, TrackingMutableRowSet.FACTORY.getRowSetByValues(0, 1));
        testFillChunkLazyGeneric(ArrayGenerator.randomShorts(random, 16), 1, TrackingMutableRowSet.FACTORY.getRowSetByValues(4));
        testFillChunkLazyGeneric(ArrayGenerator.randomShorts(random, 16), 1, TrackingMutableRowSet.FACTORY.getRowSetByValues(4, 5, 6));
        testFillChunkLazyGeneric(ArrayGenerator.randomShorts(random, 16), 1, TrackingMutableRowSet.FACTORY.getRowSetByValues(4,  6));
        testFillChunkLazyGeneric(ArrayGenerator.randomShorts(random, 16), 1, TrackingMutableRowSet.FACTORY.getRowSetByValues(4, 5, 6, 7, 8));
        testFillChunkLazyGeneric(ArrayGenerator.randomShorts(random, 16), 3, TrackingMutableRowSet.FACTORY.getRowSetByValues(5, 6, 7));
        testFillChunkLazyGeneric(ArrayGenerator.randomShorts(random, 16), 4, TrackingMutableRowSet.FACTORY.getRowSetByValues(4, 5, 6, 7));
        testFillChunkLazyGeneric(ArrayGenerator.randomShorts(random, 16), 5, TrackingMutableRowSet.FACTORY.getRowSetByValues(4, 5, 6, 7, 8));
        testFillChunkLazyGeneric(ArrayGenerator.randomShorts(random, 512), 4, TrackingMutableRowSet.FACTORY.getRowSetByValues(254, 255, 256, 257));
        testFillChunkLazyGeneric(ArrayGenerator.randomShorts(random, 512), 5, TrackingMutableRowSet.FACTORY.getRowSetByValues(254, 255, 256, 257, 258));

        for (int sourceSize = 512; sourceSize < 4096; sourceSize *= 4) {
            for (int v = -2; v < 3; v++) {
                testSetForGivenLazySourceSize(random, sourceSize + v);
            }
        }
    }

    private void testSetForGivenLazySourceSize(Random random, int sourceSize) {
        final short[] values = ArrayGenerator.randomShorts(random, sourceSize);
        for (int indexSize = 2; indexSize < sourceSize; indexSize *= 4) {
            testFillChunkIndexSizeVariationsLazy(random, sourceSize, values, indexSize);
        }
    }

    private void testFillChunkIndexSizeVariationsLazy(Random random, int sourceSize, short[] values, int indexSize) {
        testParameterFillChunkAndIndexLazy(random, sourceSize, values, indexSize - 1);
        testParameterFillChunkAndIndexLazy(random, sourceSize, values, indexSize);
        testParameterFillChunkAndIndexLazy(random, sourceSize, values, indexSize + 1);
    }

    private void testParameterFillChunkAndIndexLazy(Random random, int sourceSize, short[] values, int indexSize) {
        final TrackingMutableRowSet rowSet = TrackingMutableRowSet.FACTORY.getRowSetByValues(indexDataGenerator(random, indexSize, .1, sourceSize / indexSize, sourceSize));
        for (int chunkSize = 2; chunkSize < sourceSize; chunkSize *= 4) {
            testFillChunkLazyGeneric(values, chunkSize, rowSet);
            testFillChunkLazyGeneric(values, chunkSize + 1, rowSet);
            testFillChunkLazyGeneric(values, chunkSize - 1, rowSet);
        }
    }


    // region fromvalues
    private void checkFromValues(String msg, short fromValues, short fromChunk) {
        assertEquals(msg, fromValues, fromChunk);
    }

    private void checkFromValues(short fromValues, short fromChunk) {
        assertEquals(fromValues, fromChunk);
    }
    // endregion fromvalues

    // region fromsource
    private void checkFromSource(String msg, short fromSource, short fromChunk) {
        assertEquals(msg, fromSource, fromChunk);
    }

    private void checkFromSource(short fromSource, short fromChunk) {
        assertEquals(fromSource, fromChunk);
    }
    // endregion fromsource

    @Test
    public void testSourceSink() {
        TestSourceSink.runTests(ChunkType.Short, size -> {
            final ShortArraySource src = new ShortArraySource();
            src.ensureCapacity(size);
            return src;
        });
    }

    @Test
    public void confirmAliasingForbidden() {
        final Random rng = new Random(438269476);
        final int arraySize = 100;
        final int rangeStart = 20;
        final int rangeEnd = 80;
        final ShortArraySource source = new ShortArraySource();
        source.ensureCapacity(arraySize);

        final short[] data = ArrayGenerator.randomShorts(rng, arraySize);
        for (int ii = 0; ii < data.length; ++ii) {
            source.set(ii, data[ii]);
        }
        // super hack
        final short[] peekedBlock = (short[])source.getBlock(0);

        try (TrackingMutableRowSet srcKeys = TrackingMutableRowSet.FACTORY.getRowSetByRange(rangeStart, rangeEnd)) {
            try (TrackingMutableRowSet destKeys = TrackingMutableRowSet.FACTORY.getRowSetByRange(rangeStart + 1, rangeEnd + 1)) {
                try (ChunkSource.GetContext srcContext = source.makeGetContext(arraySize)) {
                    try (WritableChunkSink.FillFromContext destContext = source.makeFillFromContext(arraySize)) {
                        Chunk chunk = source.getChunk(srcContext, srcKeys);
                        if (chunk.isAlias(peekedBlock)) {
                            // If the ArraySource gives out aliases of its blocks, then it should throw when we try to
                            // fill from that aliased chunk
                            try {
                                source.fillFromChunk(destContext, chunk, destKeys);
                                TestCase.fail();
                            } catch (UnsupportedOperationException uoe) {
                                // Expected
                            }
                        }
                    }
                }
            }
        }
    }

    // In the Sparse versions of the code, this tickles a bug causing a null pointer exception. For the non-sparse
    // versions, the bug isn't tickled. But we should probably have this test anyway, in case some future change
    // triggers it.
    @Test
    public void testFillEmptyChunkWithPrev() {
        final ShortArraySource src = new ShortArraySource();
        src.startTrackingPrevValues();
        LiveTableMonitor.DEFAULT.startCycleForUnitTests();
        try (final TrackingMutableRowSet keys = TrackingMutableRowSet.FACTORY.getEmptyRowSet();
             final WritableShortChunk<Values> chunk = WritableShortChunk.makeWritableChunk(0)) {
            // Fill from an empty chunk
            src.fillFromChunkByKeys(keys, chunk);
        }
        // NullPointerException in ShortSparseArraySource.commitUpdates()
        LiveTableMonitor.DEFAULT.completeCycleForUnitTests();
    }

    @Test
    public void testFillUnorderedWithNulls() {
        final ShortArraySource source = new ShortArraySource();

        final Random rng = new Random(438269476);
        final int arraySize = 100;
        source.ensureCapacity(arraySize);

        final short[] data = ArrayGenerator.randomShorts(rng, arraySize);
        for (int ii = 0; ii < data.length; ++ii) {
            source.set(ii, data[ii]);
        }

        final long [] keys = LongStream.concat(LongStream.of(TrackingMutableRowSet.NULL_ROW_KEY), LongStream.range(0, data.length - 1)).toArray();
        Shuffle.shuffleArray(rng, keys);

        try (final ChunkSource.FillContext ctx = source.makeFillContext(keys.length);
             final WritableShortChunk<Values> dest = WritableShortChunk.makeWritableChunk(keys.length);
             final ResettableLongChunk<RowKeys> rlc = ResettableLongChunk.makeResettableChunk()) {
            rlc.resetFromTypedArray(keys, 0, keys.length);
            source.fillChunkUnordered(ctx, dest, rlc);
            assertEquals(keys.length, dest.size());
            for (int ii = 0; ii < keys.length; ++ii) {
                if (keys[ii] == TrackingMutableRowSet.NULL_ROW_KEY) {
                    assertEquals(NULL_SHORT, dest.get(ii));
                } else {
                    checkFromValues(data[(int)keys[ii]], dest.get(ii));
                }
            }
        }
    }
}
