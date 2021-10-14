/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */

package io.deephaven.engine.v2.utils;

import io.deephaven.base.verify.AssertionFailure;
import io.deephaven.engine.v2.TstUtils;
import io.deephaven.engine.v2.sources.chunk.Attributes;
import io.deephaven.engine.v2.sources.chunk.LongChunk;
import io.deephaven.engine.v2.sources.chunk.WritableLongChunk;
import io.deephaven.engine.v2.utils.rsp.RspBitmap;
import io.deephaven.engine.v2.utils.singlerange.SingleRange;
import io.deephaven.engine.v2.utils.sortedranges.SortedRanges;
import io.deephaven.engine.v2.utils.sortedranges.SortedRangesInt;
import io.deephaven.engine.v2.utils.sortedranges.SortedRangesLong;
import io.deephaven.engine.v2.utils.sortedranges.SortedRangesShort;
import gnu.trove.list.array.TLongArrayList;
import io.deephaven.test.types.OutOfBandTest;
import junit.framework.TestCase;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.jetbrains.annotations.NotNull;
import io.deephaven.engine.v2.utils.rsp.container.ImmutableContainer;

import static io.deephaven.engine.v2.utils.TstIndexUtil.indexFromString;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;
import org.junit.experimental.categories.Category;

import static io.deephaven.engine.v2.sources.chunk.Attributes.OrderedKeyIndices;
import static io.deephaven.engine.v2.utils.rsp.RspArray.BLOCK_SIZE;
import static io.deephaven.engine.v2.utils.rsp.RspArray.BLOCK_LAST;

@Category(OutOfBandTest.class)
public class TreeIndexTest extends SortedIndexTestBase {
    @NotNull
    @Override
    protected SortedIndex getSortedIndex(long... keys) {
        final Index.RandomBuilder treeIndexBuilder = TreeIndex.makeRandomBuilder();
        for (long key : keys) {
            treeIndexBuilder.addKey(key);
        }
        return (SortedIndex) treeIndexBuilder.getIndex();
    }


    @NotNull
    @Override
    protected final Index fromTreeIndexImpl(@NotNull final TreeIndexImpl treeIndexImpl) {
        return new TreeIndex(treeIndexImpl);
    }

    @NotNull
    @Override
    protected Index.Factory getFactory() {
        return Index.FACTORY;
    }

    public void testSimple() {
        assertEquals(1, Index.FACTORY.getIndexByRange(2, 2).size());
        final Index.Iterator it = Index.FACTORY.getIndexByRange(2, 2).iterator();
        assertEquals(2, it.nextLong());
        assertFalse(it.hasNext());
    }

    public void testSimpleRangeIterator() {
        final Index index = Index.FACTORY.getIndexByRange(3, 100);
        final Index.RangeIterator rit = index.rangeIterator();
        assertTrue(rit.hasNext());
        rit.next();
        assertEquals(3, rit.currentRangeStart());
        assertEquals(100, rit.currentRangeEnd());
        assertFalse(rit.hasNext());
    }

    public void testSerialize() throws IOException, ClassNotFoundException {
        Index index = Index.FACTORY.getIndexByRange(0, 100);
        Index copy = (Index) doSerDeser(index);
        assertEquals(index, copy);
        index.insert(1000000);

        index.insertRange(1000002, 1000005);
        copy = (Index) doSerDeser(index);
        assertEquals(index, copy);

        index.insertRange(8_000_000_000L, 8_000_100_000L);
        copy = (Index) doSerDeser(index);
        assertEquals(index, copy);

        index.insertRange(1030, 1030);
        copy = (Index) doSerDeser(index);
        assertEquals(index, copy);

        index.insertRange(1035, 1036);
        copy = (Index) doSerDeser(index);
        assertEquals(index, copy);

        index.insertRange(1038, 1039);
        copy = (Index) doSerDeser(index);
        assertEquals(index, copy);

        index.insertRange(1041, 1042);
        copy = (Index) doSerDeser(index);
        assertEquals(index, copy);

        index.insertRange(8_000_000_003L, 8_000_100_004L);
        copy = (Index) doSerDeser(index);
        assertEquals(index, copy);

        index.insertRange(8_000_000_006L, 8_000_100_007L);
        copy = (Index) doSerDeser(index);
        assertEquals(index, copy);

        index.insert(8_000_000_0010L);
        copy = (Index) doSerDeser(index);
        assertEquals(index, copy);

        index.insert(8_000_001_0010L);
        copy = (Index) doSerDeser(index);
        assertEquals(index, copy);

        index.insert(8_000_002_0010L);
        copy = (Index) doSerDeser(index);
        assertEquals(index, copy);


        index.insert(8_000_002_0011L);
        copy = (Index) doSerDeser(index);
        assertEquals(index, copy);
        index.insert(8_000_002_0013L);
        copy = (Index) doSerDeser(index);
        assertEquals(index, copy);

        index.insert(8_000_002_0015L);
        copy = (Index) doSerDeser(index);
        assertEquals(index, copy);

        index.insertRange(8_000_002_0017L, 8_000_002_0018L);
        copy = (Index) doSerDeser(index);
        assertEquals(index, copy);

        index = indexFromString(
                "0,2,4-5,8-9,12-17,19-20,22,25-29,33-38,41-45,47,49-54,56,58-59,61-73,75-78,80,82-85,89,91-104,106-110,112-114,116-117,120-132,135-136,138-141,143-144,146,148-154,157-159,161,163-166,168,170,172-189,193-196,198-206,209-210,213,215-235,237-245,248-250,252-262,264-267,269-271,273-276,278-281,284-289,291,293-294,296-301,303,305-307,309-317,319-345,347-354,356-366,368-378,380-387,389-393,395-401,403-409,411-421,424-428,430-437,439-460,462-463,465-473,475-488,490-497,499-503,505-510,512-519,521,523-531,533-540,542-543,545-562,564-570,572-595,597-642,644,646-664,666-670,674-675,677-680,682-689,691-718,720-740,742-764,766-801,803-821,823-826,828-831,833-840,842-887,890-892,894-901,903-928,930-951,953-957,959-964,966-996,998-1010,1013-1016,1018-1022,1024-1064,1066-1072,1074-1094,1096-1107,1109-1181,1183-1193,1195-1199,1201-1253,1255-1288,1290-1291,1293-1311,1313-1328,1330-1345,1347-1349,1352-1446,1448-1473,1475-1511,1513-1567,1569-1572,1574-1580,1582-1615,1617-1643,1645,1647-1687,1689-1691,1693-1695,1697-1698,1700-1709,1711-1732,1734,1735-1751,1753-1792,1794-1831,1833-1849,1851-1857,1859-1866,1868-1880,1882-1885,1887-1920,1922-1926,1931-2566");
        copy = (Index) doSerDeser(index);
        assertEquals(index, copy);

        index = indexFromString(
                "1-2,4,6,9-12,14-18,20-21,24-26,28-33,35,37-39,41-42,44,46-53,55-59,61-62,64,67,69,71-72,74-75,77-79,83-85,87-91,93-95,97,100-112,114,116,118,120-122,124-126,128-130,134-138,140-146,148-149,151,153-154,156,158-159,161-163,165-169,171-172,174-178,182-192,198-200,202,205,207-234,236-243,245,247-249,252-260,262-267,269,271-273,275-279,283-284,286-291,293-295,297-303,306-311,313,315-320,322,324-326,328-330,332-335,337-350,352-355,357,359-364,366,369-376,378-379,381-391,393,395-406,408-425,427-466,470-471,473-477,480-490,492-497,499-513,515-522,524-529,531-535,539-549,552-553,555-563,565-570,572-578,580-624,626-634,636-650,652-654,656-657,659,661-665,667-673,675-677,679,681,683-684,686,688-692,694-695,697-717,719-733,735-739,741-743,745-750,752-755,757-760,762-778,780,782-799,801-809,811-817,819-822,824-827,829-835,838-907,909-924,926-928,930-942,944-1049,1051,1053-1058,1060-1064,1066-1069,1071-1080,1082-1089,1091-1092,1094,1096-1098,1100-1102,1104-1109,1111-1121,1123-1142,1144-1156,1158-1162,1164-1169,1171-1175,1177-1190,1192-1195,1198-1199,1201-1211,1214,1216-1218,1220-1221,1223-1231,1233-1234,1236-1239,1241-1287,1289-1304,1306-1307,1309-1317,1319-1327,1329-1331,1333-1335,1337-1340,1342-1344,1346-1350,1352-1354,1356-1371,1373-1393,1395-1398,1400,1402-1479,1481-1486,1488-1490,1493,1495-1507,1509,1511-1543,1545-1550,1553-1556,1558-1564,1566-1582,1584,1586,1589-1590,1592-1615,1617-1626,1628-1634,1636-1643,1645,1647-1657,1659-1668,1670-1673,1675-1681,1683-1690,1693-1695,1697-1699,1701-1713,1715-1716,1718-1722,1724-1746,1748-1750,1753-1755,1757-1794,1796-1804,1806-1821,1823-1826,1828-1830,1832-1835,1837-1843,1846,1847-1856,1858-1894,1896-1908,1910-1916,1918-1924,1926,1928-1929,1936-2566");
        copy = (Index) doSerDeser(index);
        assertEquals(index, copy);
    }

    public void testSerializeRandom() throws IOException, ClassNotFoundException {
        final Random random = new Random(42);
        for (int ii = 0; ii < 100; ++ii) {
            final Index index = TstUtils.getRandomIndex(0, 100000, random);
            final Index copy = (Index) doSerDeser(index);
            assertEquals(index, copy);
        }
    }

    private Object doSerDeser(Object object) throws IOException, ClassNotFoundException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(object);
        final byte[] bytes = bos.toByteArray();

        final ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        final ObjectInputStream in = new ObjectInputStream(bis);

        return in.readObject();
    }

    public void testSerializeRandomSize() throws IOException, ClassNotFoundException {
        final Random random = new Random(42);

        for (int ii = 0; ii < 10; ++ii) {
            final Index index = TstUtils.getRandomIndex(0, 100000, random);

            final DoSerDerWithSize doSerDerWithSize = new DoSerDerWithSize(index).invoke();
            final Object desIndex = doSerDerWithSize.getDeserialized();
            assertEquals("ii=" + ii, index, desIndex);
            assertEquals(1, ((Index) desIndex).refCount());
        }
    }

    public void testRange() {
        final SortedIndex index = TreeIndex.getEmptyIndex();
        assertEquals(1, index.refCount());
        final Index range = index.subindexByPos(0, 10);
        // The refCount of an empty Index is implementation dependant.
        assertEquals(range.size(), 0);
        // The refCount of an empty Index is implementation dependant.
        index.insert(1L);
        assertEquals(1, index.refCount());
        index.insert(10L);
        assertEquals(1, index.refCount());
        final Index range2 = index.subindexByPos(0, 3);
        assertEquals(2, index.refCount());
        assertEquals(2, range2.refCount());
        assertEquals(range2.size(), 2);
    }

    public void testsubindexByKey() {
        final Index index = getSortedIndex(1, 2, 3);
        final Index front = index.subindexByKey(1, 2);
        assertEquals(front.size(), 2);
        final Index back = index.subindexByKey(2, 3);
        assertEquals(back.size(), 2);

        for (int ii = 1; ii <= 3; ++ii) {
            final Index oneElement = index.subindexByKey(ii, ii);
            assertEquals(oneElement.size(), 1);
        }

        Index emptyFront = index.subindexByKey(0, 0);
        assertEquals(emptyFront.size(), 0);
        assertEquals(1, emptyFront.refCount());

        final Index emptyBack = index.subindexByKey(4, 4);
        assertEquals(emptyBack.size(), 0);
        assertEquals(1, emptyBack.refCount());

        final Index index2 = getSortedIndex(2);
        emptyFront = index2.subindexByKey(0, 1);
        assertEquals(emptyFront.size(), 0);
        assertEquals(1, index2.refCount());
    }

    public void testsubindexByKeyRegression0() {
        final Index ix1 = getSortedIndex(1073741813L, 1073741814L);
        final Index ix2 = ix1.subindexByKey(1073741814L, 1073741825L);
        assertEquals(1, ix2.size());
        assertEquals(1073741814L, ix2.get(0));
    }

    private class DoSerDerWithSize {
        private final Index index;
        private byte[] bytes;
        private Object deserialized;

        DoSerDerWithSize(Index index) {
            this.index = index;
        }

        public byte[] getBytes() {
            return bytes;
        }

        Object getDeserialized() {
            return deserialized;
        }

        public DoSerDerWithSize invoke() throws IOException, ClassNotFoundException {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(index);
            bytes = bos.toByteArray();

            final ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            final ObjectInputStream in = new ObjectInputStream(bis);
            deserialized = in.readObject();
            return this;
        }
    }

    public void testSplitRangeIterator() {
        // insert merging two adjacent nodes
        final IndexBuilder builder = Index.FACTORY.getBuilder();
        for (int ii = 0; ii < 65; ++ii) {
            builder.addKey(ii * 2);
        }
        final Index index = builder.getIndex();
        index.validate();
        index.insert(63);
        index.validate();

        // insert range merging two adjacent nodes
        final IndexBuilder builder2 = Index.FACTORY.getBuilder();
        for (int ii = 0; ii < 65; ++ii) {
            builder2.addKey(ii * 4);
        }
        final Index index2 = builder2.getIndex();
        index2.validate();
        index2.insertRange(125, 127);
        index2.validate();

        // force two nodes into existence
        final IndexBuilder builder3 = Index.FACTORY.getBuilder();
        for (int ii = 0; ii < 65; ++ii) {
            builder3.addKey(ii * 4);
        }
        final Index index3 = builder3.getIndex();
        index3.validate();
        for (int ii = 0; ii < 31; ++ii) {
            index3.remove(ii * 4);
        }
        // we've got the mostly empty node, let's force a merger
        index3.validate();
        index3.insertRange(125, 127);
        index3.validate();
    }

    public void testSequentialSplitRange() {
        final Index.SequentialBuilder builder = Index.FACTORY.getSequentialBuilder();
        for (int ii = 0; ii < 64; ++ii) {
            builder.appendKey(ii * 2);
        }
        builder.appendKey(127);
        final Index checkIndex = builder.getIndex();
        checkIndex.validate();

        final Index.SequentialBuilder builder2 = Index.FACTORY.getSequentialBuilder();
        for (int ii = 0; ii < 63; ++ii) {
            builder2.appendKey(ii * 4);
        }
        builder2.appendRange(252, 253);
        builder2.appendKey(256);
        final Index checkIndex2 = builder2.getIndex();
        checkIndex2.validate();

        final Index.SequentialBuilder builder3 = Index.FACTORY.getSequentialBuilder();
        for (int ii = 0; ii < 63; ++ii) {
            builder3.appendKey(ii * 4);
        }
        builder3.appendRange(253, 254);
        builder2.appendKey(256);
        final Index checkIndex3 = builder3.getIndex();
        checkIndex3.validate();
    }

    public void testAddIndex2() {
        final String[] indexStrings = {
                "347608-350624",
                "2538579-2541595",
                "2047153-2048293",
                "776021-779037",
                "2614005-2617021",
                "66374-69390",
                "1346342-1348816",
                "175638-178654",
                "36204-39220",
                "2553665-2556681",
                "2644175-2647191",
                "2390294-2390469",
                "2547631-2550647",
                "872423-875439",
                "1203295-1206051",
                "15085-18101",
                "308387-311403",
                "791106-794122",
                "2472202-2475218",
                "1377574-1379642",
                "766970-769986",
                "254081-257097",
                "335540-338556",
                "2098001-2099035",
                "217876-220892",
                "760936-763952",
                "794123-797139",
                "2635124-2638140",
                "229944-232960",
                "248047-251063",
                "1364897-1367080",
                "498458-501474",
                "450186-453202",
                "2283304-2284005",
                "2638141-2641157",
        };

        System.out.println(indexStrings.length);

        unionIndexStrings(indexStrings);
    }

    public void testAddIndex3() {
        final IndexBuilder result = Index.FACTORY.getRandomBuilder();
        for (int ii = 0; ii < 64; ++ii) {
            result.addKey(ii * 100);
        }
        for (int ii = 0; ii < 32; ++ii) {
            result.addKey(3101 + ii * 2);
        }
        final Index indexToAdd = Index.FACTORY.getIndexByValues(3164, 3166);
        result.addIndex(indexToAdd);
        final Index checkIndex = result.getIndex();
        checkIndex.validate();
    }

    public void testAddIndex() {
        final IndexBuilder result = Index.FACTORY.getRandomBuilder();
        for (int ii = 0; ii < 64; ++ii) {
            result.addKey(ii * 100);
        }
        for (int ii = 0; ii < 32; ++ii) {
            result.addKey(3101 + ii * 2);
        }
        final Index idx = result.getIndex();

        // we've got an index which is split where we want it at this point

        // now let's nuke out the end of the second node
        for (int ii = 33; ii < 64; ++ii) {
            idx.remove(ii * 100);
        }
        // and the beginning of the first node
        for (int ii = 0; ii < 32; ++ii) {
            idx.remove(ii * 100);
        }
        for (int ii = 0; ii < 31; ++ii) {
            idx.remove(3101 + ii * 2);
        }
        // we've got {3163,3200} now, each of them are in their own nodes
        System.out.println("Idx: " + idx);

        final Index idx2 = idx.clone();

        final Index indexToAdd = Index.FACTORY.getIndexByValues(3164);
        idx.insert(indexToAdd);
        idx.validate();

        final Index indexToAdd2 = Index.FACTORY.getIndexByRange(3164, 3199);
        indexToAdd2.insert(idx2);
        indexToAdd2.validate();

        final Index indexToAdd3 = Index.FACTORY.getIndexByRange(3164, 3199);
        idx2.insert(indexToAdd3);
        idx2.validate();
    }

    private static final String[] indexStrings4 = {
            "776277-779294",
            "2609755-2612772",
            "66396-69413",
            "1343784-1346259",
            "175695-178712",
            "36216-39233",
            "2552413-2555430",
            "2549395-2552412",
            "4773551-4776568",
            "2385928-2386104",
            "2543359-2546376",
            "872711-875728",
            "1200678-1203435",
            "15090-18107",
            "308488-311505",
            "791367-794384",
            "2467905-2470922",
            "1375030-1377099",
            "767223-770240",
            "254164-257181",
            "335650-338667",
            "2093187-2094222",
            "217947-220964",
            "761187-764204",
            "3146458-3149230",
            "794385-797402",
            "2630881-2633898",
            "230019-233036",
            "248128-251145",
            "1362347-1364531",
            "498622-501639",
            "450334-453351",
            "344704-347721",
            "54324-57341",
            "574072-577089",
            "12072-15089",
            "2327652-2328217",
            "2275869-2276578",
            "764205-767222",
            "257182-260199",
            "263218-266235",
            "2633899-2636916",
    };

    public void testAddIndex4() {
        System.out.println(indexStrings4.length);

        unionIndexStrings(indexStrings4);
    }

    public void testAddIndex5() {
        final String[] indexStrings = {
                "20890721338586-20890721340299",
                "14293652098976-14293652099080",
                "20890721335246-20890721335570",
                "17592186911604-17592186911843",
                "18691698441720-18691698442025",
                "18691698355087-18691698356104",
                "13194139849620-13194139865324,14293651241565-14293651249155,15393163269181-15393163274091,16492674867941-16492674872953,17592186468315-17592186471886,18691698033945-18691698037464,19791209645628-19791209650138",
                "6597070358078-6597070358247,7696582078507-7696582078887,8796093797454-8796093797809,9895605339344-9895605339689,10995116943144-10995116943389,12094628415200-12094628415436,13194140499685-13194140499920,14293651878206-14293651878521,15393163489511-15393163489841,16492675174913-16492675175237,18691698404283-18691698404616,19791210304155-19791210304633",
                "3298535296586-3298535320516,5497558501937-5497558526825,6597069828029-6597069848924,7696581444778-7696581462133,8796093386487-8796093404324,9895605001605-9895605016364,10995116581285-10995116594304,12094628131217-12094628142672,13194140003636-13194140013824,14293651705606-14293651722609,15393163213455-15393163226255,16492674756516-16492674770413,17592186439132-17592186449020,18691698082588-18691698099678,19791209892626-19791209914086",
                "7696581923067-7696581923163,8796093888811-8796093888884,9895605534398-9895605534522,10995117081302-10995117081412,12094628461209-12094628461308,14293652309829-14293652310101,15393163715437-15393163715725,16492675079437-16492675080736,18691698179803-18691698180181,19791210306910-19791210307028",
                "4398047280621-4398047280691,5497559244045-5497559244167,7696582261735-7696582261780,9895605218777-9895605218872,10995117097381-10995117097411,12094628679612-12094628679668,13194140531630-13194140531726,14293652346091-14293652346201,15393163424987-15393163425114,16492675038098-16492675038168,17592186768465-17592186768571",
                "2199023716037-2199023716449,5497558967107-5497558967500,6597069930173-6597069930457,7696581962330-7696581962440,8796093522111-8796093522208,9895605163585-9895605163861,10995116895263-10995116895395,12094628479377-12094628479499,13194140447291-13194140447531,14293652331842-14293652331965,16492674982044-16492674982266,17592186695206-17592186695314,18691698674743-18691698674933,19791209973552-19791209973840",
                "2199023854256-2199023854510,3298535535808-3298535536033,4398047056450-4398047056585,5497559034965-5497559035201,6597070662957-6597070663127,7696582060084-7696582060191,8796093677795-8796093678185,9895605251901-9895605252131,12094628619913-12094628620034,13194140214761-13194140215242,14293652224772-14293652224947,15393163633688-15393163633774,16492675384837-16492675384890,17592186798509-17592186798562,18691698477304-18691698477406",
                "2199023702859-2199023702937,3298535423884-3298535423967,4398047109121-4398047109198,5497559188322-5497559188474,6597070663757-6597070663882,7696582130568-7696582130700,8796093647162-8796093647236,9895605205441-9895605205606,10995116738139-10995116738297,12094628669969-12094628670271,13194140464393-13194140464526,14293651959747-14293651959977,15393163580342-15393163580549,16492675364287-16492675364421,17592186986814-17592186986953,18691698402219-18691698402384",
                "6597070657242-6597070657249,12094628590268-12094628590310,13194140343952-13194140343992,17592186971550-17592186971575,18691698214942-18691698214982",
                "2199024032537-2199024032556,3298535513197-3298535513207,4398047310053-4398047310061,5497558814261-5497558814299,6597070347521-6597070347563,7696581965538-7696581965548,8796093831065-8796093831076,9895605186922-9895605186943,10995116876972-10995116876986,12094628465140-12094628465153,13194140142231-13194140142272,14293651884119-14293651884164,15393163414388-15393163414444,16492675036217-16492675036220,17592186692354-17592186692385,18691698407007-18691698407036",
                "3298535373448-3298535378683,6597070330489-6597070333771,7696581889995-7696581893185,8796093421494-8796093427691,9895605016917-9895605023073,10995116697104-10995116701290,12094628120342-12094628126080,13194140124982-13194140130464,14293651739639-14293651745677,15393163314042-15393163317215,17592186607646-17592186610395",
                "1099512173190-1099512173269,2199023700973-2199023701071,3298535519750-3298535519839,4398047020835-4398047021015,5497558735822-5497558736027,6597070627770-6597070627867,7696582129975-7696582130042,8796093888361-8796093888432,9895605539993-9895605540040,10995116799054-10995116799111,12094628553210-12094628553269,13194140413152-13194140413338,15393163491051-15393163491147,16492675334675-16492675334737",
                "1099512181413-1099512181776,2199023742010-2199023742328,3298535665499-3298535665831,4398047026401-4398047026828,5497558851103-5497558851462,6597070396391-6597070396907,7696582024517-7696582025272,8796093746683-8796093747126,9895605277891-9895605278301,10995116788239-10995116788544,12094628453152-12094628453503,13194140236223-13194140236481,14293651932566-14293651933406,15393163517448-15393163517900,16492675048107-16492675048545,17592186701116-17592186701711,18691698327507-18691698327966,19791210337439-19791210338574",
                "17592186437322-17592186437945",
                "5497558554505-5497558558927,6597070224939-6597070229411,7696581701497-7696581704601,8796093535917-8796093540437,9895604782111-9895604786426,10995116655806-10995116662915,13194140065137-13194140070500,14293651797689-14293651803712,19791209950696-19791209964823,20890721273695-20890721283715",
                "6597070387250-6597070387411,7696581994603-7696581994831,8796093666968-8796093667065,9895605238259-9895605239015,12094628604005-12094628604106,15393163819942-15393163820032,16492675097568-16492675097698,17592186914765-17592186914961",
                "9895605407539-9895605407569,15393163786489-15393163786544,17592186588558-17592186588609,18691698732684-18691698732708",
                "9895605173288-9895605173597,12094628478457-12094628478800,13194139871592-13194139871832,14293652328687-14293652329152,15393163411656-15393163411951,17592186760028-17592186760541,18691698259712-18691698262609,19791210057704-19791210061023",
                "2199023884290-2199023885000,3298535411530-3298535412066,4398047231198-4398047231945,9895605321260-9895605321762,10995116978363-10995116978742,12094628452199-12094628452651,13194140475122-13194140476863,14293652156155-14293652156878,15393163445819-15393163446709",
                "6597070359804-6597070359987,7696582120578-7696582120705,8796093510080-8796093510381,12094628668315-12094628668452,13194140236984-13194140237129,14293651974973-14293651975212,15393163460090-15393163460712,16492675095750-16492675096248,17592186634787-17592186635052,18691698683683-18691698684170,19791210029595-19791210030151",
                "4398046563908-4398046564826,5497558783354-5497558784061,6597070372816-6597070373396,7696581986300-7696581987578,8796093412588-8796093413420,9895605129558-9895605130345,10995116625675-10995116626272,12094628528022-12094628529095,13194140083344-13194140084624,14293651964002-14293651965122,15393163246921-15393163247709,18691698292585-18691698293123,19791210257228-19791210258122",
                "8796093742381-8796093744716,9895605208496-9895605210190,10995116952939-10995116954058,12094628604295-12094628604729,13194140403736-13194140405456,15393163628237-15393163629388,16492675196378-16492675197141,17592186843513-17592186844100,18691698451421-18691698451969,19791210251569-19791210252128",
                "12094628556183-12094628556440,13194140471460-13194140471762,14293652007217-14293652007547,15393163455360-15393163455651,16492674853429-16492674853600,17592186694662-17592186694915,18691698342569-18691698342911,19791209927633-19791209927873",
                "18691698326715-18691698326882,19791210490648-19791210490918",
                "13194140464256-13194140464389,14293651912922-14293651913056,15393163518656-15393163518740,16492675044256-16492675044322,17592186744913-17592186745140,18691698310412-18691698310889,19791210141975-19791210142158",
                "12094628746513-12094628746735,13194140380425-13194140380732,14293652093591-14293652093792,15393163317496-15393163318200,16492675106791-16492675107135,17592186532636-17592186533281,19791210249619-19791210249873",
                "15393163763728-15393163764214,16492675391751-16492675392119,17592186913329-17592186913553,18691698545693-18691698545992,19791210555858-19791210556034",
                "19791210298407-19791210298454",
                "19791210041897-19791210042553",
                "18691698318610-18691698318704",
                "16492674989303-16492674989435",
                "16492675074590-16492675074708,17592186677099-17592186677298,19791210405925-19791210406235",
                "18691698412790-18691698412970,19791210263229-19791210263574",
                "18691698284171-18691698285566,19791210044649-19791210047657",
                "18691698527522-18691698527583,19791210259905-19791210260002",
                "19791210569655-19791210569804",
                "19791210330220-19791210332602",
                "19791210334965-19791210335670",
                "20890721335571-20890721338585,21990232637825-21990232640388"
        };

        unionIndexStrings(indexStrings);

        // doCutDown(indexStrings);
    }

    // This method will take one range out of one the index and run the test. If we find a range we can take out and
    // still get a failure, recursively try again, so that we have a minimal set of index ranges that actually produce
    // a failure for us. We don't need it during normal testing, so it is unused.
    @SuppressWarnings("unused")
    private void doCutDown(String[] indexStrings) {
        for (int ii = 0; ii < indexStrings.length; ++ii) {
            final String[] newStrings = Arrays.copyOf(indexStrings, indexStrings.length);
            final String[] ranges = indexStrings[ii].split(",");

            for (int jj = 0; jj < ranges.length; ++jj) {
                if (ranges.length == 1) {
                    continue;
                }
                System.out.println("ii = " + ii + ", jj=" + jj + ", " + indexStrings[ii]);

                final ArrayList<String> newRanges = new ArrayList<>();
                for (int kk = 0; kk < ranges.length; ++kk) {
                    if (jj != kk) {
                        newRanges.add(ranges[kk]);
                    }
                }


                newStrings[ii] = StringUtils.join(newRanges, ",");

                try {
                    unionIndexStrings(newStrings);
                } catch (AssertionFailure afe) {
                    afe.printStackTrace(System.err);
                    try {
                        Thread.sleep(100);
                        dumpArray(newStrings);
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {
                    }
                    doCutDown(newStrings);
                    return;
                }
            }
        }
    }

    private void dumpArray(String[] strArray) {
        for (int ii = 0; ii < strArray.length; ++ii) {
            System.out.print("                \"" + strArray[ii] + "\"");
            if (ii == strArray.length - 1) {
                System.out.println();
            } else {
                System.out.println(",");
            }
        }
    }

    private Index getUnionIndexStrings(final String[] indexStrings) {
        final IndexBuilder result = Index.FACTORY.getRandomBuilder();
        for (String indexString : indexStrings) {
            final Index indexToAdd = indexFromString(indexString);
            indexToAdd.validate();
            result.addIndex(indexToAdd);
            assertEquals(1, indexToAdd.refCount());
        }
        return result.getIndex();
    }

    private void unionIndexStrings(final String[] indexStrings) {
        final Index checkIndex = getUnionIndexStrings(indexStrings);
        checkIndex.validate();
        assertEquals(1, checkIndex.refCount());
    }

    public void testRandomBuilder() {
        final Random random = new Random(0);

        final TLongArrayList values = new TLongArrayList();

        for (int step = 0; step < 1000; ++step) {
            final int size = random.nextInt(10);

            final RangePriorityQueueBuilder priorityQueueBuilder = new RangePriorityQueueBuilder(16);
            final Index.RandomBuilder treeBuilder = TreeIndex.makeRandomBuilder();

            values.clear();

            for (int ii = 0; ii < size; ++ii) {
                final long value = random.nextInt(100000);
                final long rangeSize = 1 + random.nextInt(1000);
                for (int jj = 0; jj < rangeSize; ++jj) {
                    values.add(value + jj);
                }
                priorityQueueBuilder.addRange(value, value + rangeSize - 1);
                treeBuilder.addRange(value, value + rangeSize - 1);
                // System.out.println(value + " -> " + (value + rangeSize - 1));
            }

            final Index prioIndex = new TreeIndex(priorityQueueBuilder.getTreeIndexImpl());
            final Index treeIndex = treeBuilder.getIndex();

            values.sort();
            long lastValue = -1;
            int jj = 1, ii = 1;
            for (; ii < values.size(); ++ii) {
                if (values.get(ii) != lastValue) {
                    // keep it
                    lastValue = values.get(ii);
                    values.set(jj++, lastValue);
                }
            }
            if (jj < ii) {
                values.remove(jj, values.size() - jj);
            }
            // System.out.println(prioIndex);
            // System.out.println(treeIndex);
            // dumpValues(values);

            assertEquals(treeIndex, prioIndex);

            ii = 0;
            for (final Index.Iterator it = prioIndex.iterator(); it.hasNext();) {
                final long next = it.nextLong();
                TestCase.assertEquals(values.get(ii++), next);
            }

            if (values.size() < 2) {
                continue;
            }
            final Index.SearchIterator it = prioIndex.searchIterator();
            final int j = values.size() / 2;
            final long v = values.get(j);
            final long prev = values.get(j - 1);
            final String m = "step=" + step;
            if ((step & 1) != 0) {
                assertTrue(m, it.advance(v));
                assertEquals(m, v, it.currentValue());
            } else {
                assertTrue(m, it.advance(v - 1));
                if (v - 1 == prev) {
                    assertEquals(m, v - 1, it.currentValue());
                    assertTrue(m, it.hasNext());
                    assertEquals(m, v, it.nextLong());
                } else {
                    assertEquals(m, v, it.currentValue());
                }
            }
            assertFalse(it.advance(values.get(values.size() - 1) + 1));
        }
    }

    public void testOverlappingRanges() {
        final RangePriorityQueueBuilder priorityQueueBuilder = new RangePriorityQueueBuilder(16);
        priorityQueueBuilder.addRange(10, 100);
        assertEquals(1, priorityQueueBuilder.rangeCount());
        priorityQueueBuilder.addRange(99, 110);
        assertEquals(1, priorityQueueBuilder.rangeCount());
        priorityQueueBuilder.addRange(120, 130);
        assertEquals(2, priorityQueueBuilder.rangeCount());
        priorityQueueBuilder.addRange(125, 135);
        assertEquals(2, priorityQueueBuilder.rangeCount());
        priorityQueueBuilder.addRange(140, 150);
        assertEquals(3, priorityQueueBuilder.rangeCount());
        priorityQueueBuilder.addRange(135, 139);
        assertEquals(3, priorityQueueBuilder.rangeCount());
        priorityQueueBuilder.addRange(160, 165);
        assertEquals(4, priorityQueueBuilder.rangeCount());
        priorityQueueBuilder.addRange(155, 160);
        assertEquals(4, priorityQueueBuilder.rangeCount());
        priorityQueueBuilder.addKey(171);
        assertEquals(5, priorityQueueBuilder.rangeCount());
        priorityQueueBuilder.addKey(172);
        assertEquals(5, priorityQueueBuilder.rangeCount());
        priorityQueueBuilder.addKey(173);
        assertEquals(5, priorityQueueBuilder.rangeCount());
        priorityQueueBuilder.addKey(174);
        assertEquals(5, priorityQueueBuilder.rangeCount());
        priorityQueueBuilder.addKey(170);
        assertEquals(5, priorityQueueBuilder.rangeCount());
        priorityQueueBuilder.addRange(180, 185);
        assertEquals(6, priorityQueueBuilder.rangeCount());
        priorityQueueBuilder.addRange(178, 179);
        assertEquals(6, priorityQueueBuilder.rangeCount());
        priorityQueueBuilder.addRange(225, 250);
        assertEquals(7, priorityQueueBuilder.rangeCount());
        priorityQueueBuilder.addRange(220, 260);
        assertEquals(7, priorityQueueBuilder.rangeCount());

        final Index prioIndex = new TreeIndex(priorityQueueBuilder.getTreeIndexImpl());

        final Index.RangeIterator rit = prioIndex.rangeIterator();
        assertTrue(rit.hasNext());
        assertEquals(10, rit.next());
        assertEquals(110, rit.currentRangeEnd());

        assertTrue(rit.hasNext());
        assertEquals(120, rit.next());
        assertEquals(150, rit.currentRangeEnd());

        assertTrue(rit.hasNext());
        assertEquals(155, rit.next());
        assertEquals(165, rit.currentRangeEnd());

        assertTrue(rit.hasNext());
        assertEquals(170, rit.next());
        assertEquals(174, rit.currentRangeEnd());

        assertTrue(rit.hasNext());
        assertEquals(178, rit.next());
        assertEquals(185, rit.currentRangeEnd());

        assertTrue(rit.hasNext());
        assertEquals(220, rit.next());
        assertEquals(260, rit.currentRangeEnd());

        assertFalse(rit.hasNext());
    }

    private void dumpValues(TLongArrayList values) {
        System.out.print("{");
        for (int startPosition = 0; startPosition < values.size();) {
            if (startPosition > 0) {
                System.out.print(",");
            }
            final long size = rangeSize(values, startPosition);
            final long startValue = values.get(startPosition);
            if (size > 1) {
                System.out.print(startValue + "-" + (startValue + size - 1));
            } else {
                System.out.print(startValue);
            }
            startPosition += size;
        }
        System.out.println("}");
    }

    private long rangeSize(TLongArrayList values, int start) {
        int end = start + 1;
        while (end < values.size() && values.get(end) == values.get(end - 1) + 1) {
            end++;
        }
        return end - start;
    }


    // This test would be way too brittle to include, even if I wasn't reading the deserialized stuff from a file.
    // The following function will write something useful to read in.
    // String dumpSerialized(TreeIndex obj) {
    // try {
    // ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    // ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
    // obj.writeImpl(objectOutputStream);
    // return Base64.byteArrayToBase64(byteArrayOutputStream.toByteArray());
    // } catch (IOException ioe) {
    // return ioe.getMessage();
    // }
    // }
    //
    // public void testDeserializeIt() throws IOException, ClassNotFoundException {
    // try (BufferedReader br = new BufferedReader(new FileReader("/home/cwright/b64.txt"))) {
    // ArrayList<TreeIndexImpl> readImpls = new ArrayList<>();
    // String line;
    // while ((line = br.readLine()) != null) {
    // if (line.isEmpty())
    // continue;
    // // process the line.
    // byte [] decoded = Base64.decode(line.getBytes());
    // ByteArrayInputStream bis = new ByteArrayInputStream(decoded);
    // ObjectInputStream ois = new ObjectInputStream(bis);
    // Object read = ois.readObject();
    // if (read instanceof TreeIndexImpl) {
    // readImpls.add((TreeIndexImpl) read);
    // }
    // }
    //
    // Assert.eq(3, "3", readImpls.size(), "readImpls.size()");
    //
    // Index toInsertInto = new TreeIndex(readImpls.get(0));
    // toInsertInto.validate();
    //
    // Index indexToInsert = new TreeIndex(readImpls.get(1));
    // indexToInsert.validate();
    //
    // System.out.println(toInsertInto);
    // System.out.println(indexToInsert);
    //
    // toInsertInto.insert(indexToInsert);
    // toInsertInto.validate();
    // }
    // }

    // public void testDeserializeIt() throws IOException, ClassNotFoundException {
    // Map<String, TreeIndex> readIndices = new LinkedHashMap<>();
    //
    // try (BufferedReader br = new BufferedReader(new FileReader("/home/cwright/tmp/t"))) {
    // String line;
    // while ((line = br.readLine()) != null) {
    // if (line.isEmpty())
    // continue;
    // String[] splits = line.split(":");
    // String name = splits[0];
    // String value = splits[1];
    // // process the line.
    // byte[] decoded = Base64.decode(value.getBytes());
    // ByteArrayInputStream bis = new ByteArrayInputStream(decoded);
    // ObjectInputStream ois = new ObjectInputStream(bis);
    // Object read = ois.readObject();
    // if (read instanceof TreeIndexImpl) {
    // readIndices.put(name, new TreeIndex((TreeIndexImpl) read));
    // }
    // }
    // }
    // for (Map.Entry<String, TreeIndex> entry : readIndices.entrySet()) {
    // System.out.println(entry.getKey() + " -> " + entry.getValue());
    // }
    //
    // Index saveModified = readIndices.get("saveModified");
    // Index saveModified2 = readIndices.get("saveModified2");
    // Index minus = readIndices.get("minus");
    // Index intersect = readIndices.get("intersect");
    //
    // saveModified.validate();
    // saveModified2.validate();
    // minus.validate();
    // intersect.validate();
    //
    //// saveModified.insert(minus);
    //// saveModified.validate();
    //// saveModified.insert(intersect);
    //// saveModified.validate();
    ////
    //// saveModified2.insert(intersect);
    //// for (Index.RangeIterator rit = saveModified2.rangeIterator(); rit.hasNext(); ) {
    //// rit.next();
    //// System.out.println(rit.currentRangeStart() + "-" + rit.currentRangeEnd());
    //// }
    ////
    //// saveModified2.validate();
    //
    // Index modified = readIndices.get("modifiedIndices");
    //
    //// Assert.eq(saveModified2, "saveModified2", modified, "modified");
    //
    //
    //
    // modified.validate();
    //
    //// saveModified2.insert(intersect);
    //// saveModified2.validate();
    // }

    public void testRandomBuilderEmptyAdds() {
        // Make a perfect merge between two leaves, collapsing to a single range.
        final Index.RandomBuilder b = TreeIndex.makeRandomBuilder();
        for (int i = 1; i <= 32; ++i) {
            b.addKey(2 * i);
        }
        for (int i = 0; i < 32; ++i) {
            b.addKey(2 * i + 1);
        }
        // make a few more leaves.
        for (int i = 0; i < 10 * 32; ++i) {
            b.addKey(1000 + 2 * i);
        }
        final Index ix = b.getIndex();
        assertEquals(12 * 32, ix.size());
    }

    private Index.TargetComparator makeCompFor(final long v) {
        final Index.TargetComparator comp = (long k, int dir) -> {
            if (v < k) {
                return -dir;
            }
            if (v > k) {
                return dir;
            }
            return 0;
        };
        return comp;
    }

    public void testIteratorBinarySearch() {
        final Index.RandomBuilder b = Index.FACTORY.getRandomBuilder();
        b.addRange(2, 3);
        b.addRange(5, 9);
        b.addRange(15, 19);
        b.addRange(21, 29);
        b.addKey(41);
        b.addRange(46, 49);
        b.addKey(100);
        b.addRange(120, 140);
        final Index ix = b.getIndex();
        final Index.SearchIterator it = ix.searchIterator();
        Index.TargetComparator comp = makeCompFor(1);
        long v = it.binarySearchValue(comp, 1);
        assertEquals(-1, v);

        comp = makeCompFor(14);
        v = it.binarySearchValue(comp, 1);
        assertEquals(9, v);
        assertEquals(9, it.currentValue());
        assertTrue(it.hasNext());
        assertEquals(15, it.nextLong());
        Index.SearchIterator newIt = ix.searchIterator();
        v = newIt.binarySearchValue(comp, 1);
        assertEquals(9, v);
        assertEquals(9, newIt.currentValue());
        assertTrue(newIt.hasNext());
        assertEquals(15, newIt.nextLong());

        comp = makeCompFor(17);
        v = it.binarySearchValue(comp, 1);
        assertEquals(17, v);
        assertEquals(17, it.currentValue());
        assertTrue(it.hasNext());
        assertEquals(18, it.nextLong());
        newIt = ix.searchIterator();
        v = newIt.binarySearchValue(comp, 1);
        assertEquals(17, v);
        assertEquals(17, newIt.currentValue());
        assertTrue(newIt.hasNext());
        assertEquals(18, newIt.nextLong());

        comp = makeCompFor(24);
        v = it.binarySearchValue(comp, 1);
        assertEquals(24, v);
        assertEquals(24, it.currentValue());
        assertTrue(it.hasNext());
        assertEquals(25, it.nextLong());
        newIt = ix.searchIterator();
        v = newIt.binarySearchValue(comp, 1);
        assertEquals(24, v);
        assertEquals(24, newIt.currentValue());
        assertTrue(newIt.hasNext());
        assertEquals(25, newIt.nextLong());

        comp = makeCompFor(50);
        v = it.binarySearchValue(comp, 1);
        assertEquals(49, v);
        assertTrue(it.hasNext());
        assertEquals(100, it.nextLong());
        newIt = ix.searchIterator();
        v = newIt.binarySearchValue(comp, 1);
        assertEquals(49, v);
        assertTrue(newIt.hasNext());
        assertEquals(100, newIt.nextLong());

        comp = makeCompFor(139);
        v = it.binarySearchValue(comp, 1);
        assertEquals(139, v);
        assertEquals(139, it.currentValue());
        assertTrue(it.hasNext());
        newIt = ix.searchIterator();
        v = newIt.binarySearchValue(comp, 1);
        assertEquals(139, v);
        assertEquals(139, newIt.currentValue());
        assertTrue(newIt.hasNext());

        comp = makeCompFor(140);
        v = it.binarySearchValue(comp, 1);
        assertEquals(140, v);
        assertEquals(140, it.currentValue());
        assertFalse(it.hasNext());
        newIt = ix.searchIterator();
        v = newIt.binarySearchValue(comp, 1);
        assertEquals(140, v);
        assertEquals(140, newIt.currentValue());
        assertFalse(newIt.hasNext());

        comp = makeCompFor(141);
        v = it.binarySearchValue(comp, 1);
        assertEquals(140, v);
        assertEquals(140, it.currentValue());
        assertFalse(it.hasNext());
        newIt = ix.searchIterator();
        v = newIt.binarySearchValue(comp, 1);
        assertEquals(140, v);
        assertEquals(140, newIt.currentValue());
        assertFalse(newIt.hasNext());
    }

    public void testIteratorBinarySearchForSingleRangeIndex() {
        final Index sr = Index.FACTORY.getIndexByRange(5, 9);
        final Index.SearchIterator it = sr.searchIterator();
        Index.TargetComparator comp = makeCompFor(4);
        long v = it.binarySearchValue(comp, 1);
        assertEquals(-1, v);
        comp = makeCompFor(5);
        v = it.binarySearchValue(comp, 1);
        assertEquals(5, v);
        assertEquals(5, it.currentValue());
        assertTrue(it.hasNext());
        assertEquals(6, it.nextLong());
        comp = makeCompFor(9);
        v = it.binarySearchValue(comp, 1);
        assertEquals(9, v);
        assertEquals(9, it.currentValue());
        assertFalse(it.hasNext());
        comp = makeCompFor(8);
        v = it.binarySearchValue(comp, 1);
        assertEquals(v, -1);
        assertEquals(9, it.currentValue());
    }

    public void testRangeIteratorAdvance() {
        final Index.RandomBuilder bl = Index.FACTORY.getRandomBuilder();
        bl.addRange(2, 3);
        bl.addRange(5, 9);
        bl.addRange(15, 19);
        bl.addRange(21, 29);
        bl.addKey(41);
        bl.addRange(46, 49);
        bl.addKey(100);
        bl.addRange(120, 140);
        final Index ix = bl.getIndex();
        final Index.RangeIterator it = ix.rangeIterator();
        final Index.RangeIterator it2 = ix.rangeIterator();
        long laste = -1;
        while (it.hasNext()) {
            it.next();
            final long s = it.currentRangeStart();
            for (long v = laste + 1; v < s; ++v) {
                final String m = "v=" + v;
                boolean b = it2.advance(v);
                assertTrue(m, b);
                assertEquals(m, (laste == -1) ? 2 : s, it2.currentRangeStart());
                final Index.RangeIterator it3 = ix.rangeIterator();
                b = it3.advance(v);
                assertTrue(m, b);
                assertEquals(m, (laste == -1) ? 2 : s, it3.currentRangeStart());
            }
            final long e = it.currentRangeEnd();
            for (long v = s; v <= e; ++v) {
                final String m = "v=" + v;
                boolean b = it2.advance(v);
                assertTrue(m, b);
                assertEquals(m, v, it2.currentRangeStart());
                final Index.RangeIterator it3 = ix.rangeIterator();
                b = it3.advance(v);
                assertTrue(m, b);
                assertEquals(m, v, it3.currentRangeStart());
            }
            laste = e;
        }
        boolean b = it.advance(laste + 1);
        assertFalse(b);
        assertFalse(it.hasNext());
        b = it2.advance(laste + 1);
        assertFalse(b);
        assertFalse(it2.hasNext());
        final Index.RangeIterator it3 = ix.rangeIterator();
        b = it3.advance(laste + 1);
        assertFalse(b);
        assertFalse(it3.hasNext());
    }

    public void testRangeIteratorAdvanceEmptyIndex() {
        final Index ix = Index.FACTORY.getEmptyIndex();
        final Index.RangeIterator rit = ix.rangeIterator();
        assertFalse(rit.advance(0));
        assertFalse(rit.hasNext());
    }

    public void testRangeIteratorAdvanceBack() {
        final Index ix = Index.FACTORY.getEmptyIndex();
        final long s = 300;
        final long e = 600;
        ix.insertRange(s, e);
        final Index.RangeIterator it = ix.rangeIterator();
        final long v = 500;
        assertTrue(it.advance(v));
        assertEquals(v, it.currentRangeStart());
        assertEquals(e, it.currentRangeEnd());
        assertTrue(it.advance(400));
        assertEquals(v, it.currentRangeStart());
        assertEquals(e, it.currentRangeEnd());
    }

    private Index singleRangeIndex(final long start, final long end) {
        final Index.RandomBuilder b = Index.FACTORY.getRandomBuilder();
        b.addRange(start, end);
        return b.getIndex();
    }

    public void testSimpleOpsRefCounts() {
        final Index ix0 = singleRangeIndex(10, 1000);
        assertEquals(1, ix0.refCount());
        final Index ix1 = ix0.clone();
        assertEquals(1, ix0.refCount());
        assertEquals(1, ix1.refCount());
        final Index ix2 = singleRangeIndex(100, 900);
        assertEquals(1, ix2.refCount());
        final Index ix3 = ix2.clone();
        assertEquals(1, ix2.refCount());
        assertEquals(1, ix3.refCount());
        ix3.remove(101);
        assertEquals(1, ix2.refCount()); // one from ix3.prev
        assertEquals(1, ix3.refCount());
        final Index ix4 = singleRangeIndex(1002, 1003);
        assertEquals(1, ix4.refCount());
        ix1.update(ix4, ix3);
        assertEquals(1, ix0.refCount());
        assertEquals(1, ix1.refCount());
        assertEquals(1, ix3.refCount());
        assertEquals(1, ix4.refCount());
        final Index ix5 = ix2.intersect(ix0);
        assertEquals(1, ix5.refCount());
        assertEquals(1, ix2.refCount());
        assertEquals(1, ix0.refCount());
    }

    public void testSimpleIteratorForEach() {
        final Index ix = getUnionIndexStrings(indexStrings4);

        final Index.Iterator fit = ix.iterator();
        final Index.RangeIterator rit = ix.rangeIterator();
        final long[] buf = new long[7];
        final int[] count = new int[1];
        final int[] voffset = new int[1];
        final Predicate<Index.Iterator> hasNextForIter =
                (it) -> (count[0] > 0) || it.hasNext();
        final Function<Index.Iterator, Long> nextForIter =
                (it) -> {
                    if (count[0] == 0) {
                        voffset[0] = 0;
                        it.forEachLong((final long v) -> {
                            buf[voffset[0] + count[0]] = v;
                            ++count[0];
                            return count[0] < buf.length;
                        });
                        assertTrue(count[0] > 0);
                    }
                    final long v = buf[voffset[0]];
                    ++voffset[0];
                    --count[0];
                    return v;
                };

        final long[] end = new long[1];
        final long[] curr = new long[1];
        final Predicate<Index.RangeIterator> hasNextForRanges =
                (it) -> (curr[0] < end[0] || it.hasNext());
        final Function<Index.RangeIterator, Long> nextForRanges =
                (it) -> {
                    if (curr[0] >= end[0]) {
                        it.next();
                        curr[0] = it.currentRangeStart();
                        end[0] = it.currentRangeEnd() + 1;
                    }
                    return curr[0]++;
                };
        int r = 0;
        while (hasNextForIter.test(fit)) {
            final String m = "r=" + r;
            assertTrue(m, hasNextForRanges.test(rit));
            final long cv = nextForIter.apply(fit);
            final long av = nextForRanges.apply(rit);
            assertEquals(m, av, cv);
            ++r;
        }
        assertFalse(hasNextForRanges.test(rit));
    }

    public void testGetAverageRunLengthEstimate() {
        final Index e = Index.FACTORY.getEmptyIndex();
        assertEquals(1, e.getAverageRunLengthEstimate());
        final Index r1 = singleRangeIndex(0, 65535);
        assertEquals(BLOCK_SIZE, r1.getAverageRunLengthEstimate());
        final Index r2 = Index.FACTORY.getEmptyIndex();
        r2.insert(1);
        r2.insert(3);
        r2.insert(5);
        r2.insert(7);
        assertEquals(1, r2.getAverageRunLengthEstimate());
        final long k = 1 << 16;
        r2.insertRange(k + 9, k + 11);
        r2.insertRange(k + 13, k + 15);
        r2.insertRange(k + 17, k + 19);
        r2.insertRange(k + 21, k + 23);
        assertEquals(2, r2.getAverageRunLengthEstimate());
    }

    public void testGetOrderedKeysByKeyRange() {
        final Index.SequentialBuilder b = Index.FACTORY.getSequentialBuilder();
        final long[] vs = new long[] {3, 4, 5, 8, 10, 12, 29, 31, 44, 45, 46, 59, 60, 61, 72, 65537, 65539, 65536 * 3,
                65536 * 3 + 5};
        for (long v : vs) {
            b.appendKey(v);
        }
        final Index ix = b.getIndex();
        final long[] ends = new long[] {44, 45, 46, 58, 61, 62, 72, 65535, 65536, 65536 * 2, 65536 * 3 - 1, 65536 * 3,
                65536 * 3 + 1};
        final long start = 8;
        for (long end : ends) {
            final String m = "end==" + end;
            final OrderedKeys oks = ix.getOrderedKeysByKeyRange(start, end);
            final Index ioks = oks.asIndex();
            boolean firstTime = true;
            long n = 0;
            for (final long v : vs) {
                final String m2 = m + " && v==" + v;
                if (v < start || v > end) {
                    assertTrue(m2, ioks.find(v) < 0);
                } else {
                    ++n;
                    if (firstTime) {
                        assertEquals(m2, v, oks.firstKey());
                        firstTime = false;
                    }
                    assertTrue(m2, ioks.find(v) >= 0);
                }
            }
            assertEquals(m, n, ioks.size());
        }
    }

    public void testGetOrderedKeysByPosition() {
        final Index.SequentialBuilder b = Index.FACTORY.getSequentialBuilder();
        final long[] vs = new long[] {3, 4, 5, 8, 10, 12, 29, 31, 44, 45, 46, 59, 60, 61, 72, 65537, 65539, 65536 * 3,
                65536 * 3 + 5};
        for (long v : vs) {
            b.appendKey(v);
        }
        final Index ix = b.getIndex();
        final long sz = ix.size();

        for (long startPos = 0; startPos < sz; ++startPos) {
            final String m = "startPos==" + startPos;
            for (long endPos = startPos; endPos <= sz; ++endPos) {
                final String m2 = m + " && endPos==" + endPos;
                final OrderedKeys oks = ix.getOrderedKeysByPosition(startPos, endPos - startPos + 1);
                final Index ioks = oks.asIndex();
                long n = 0;
                boolean firstTime = true;
                for (int p = 0; p < sz; ++p) {
                    final String m3 = m2 + " && p==" + p;
                    if (p < startPos || p > endPos) {
                        assertTrue(m3, ioks.find(ix.get(p)) < 0);
                    } else {
                        ++n;
                        if (firstTime) {
                            assertEquals(m3, vs[p], oks.firstKey());
                            firstTime = false;
                        }
                        assertTrue(m3, ioks.find(ix.get(p)) >= 0);
                    }
                }
                assertEquals(m2, n, ioks.size());
            }
        }
    }

    public void testFillChunk() {
        final Index.SequentialBuilder b = Index.FACTORY.getSequentialBuilder();
        final long[] vs = new long[] {3, 4, 5, 8, 10, 12, 29, 31, 44, 45, 46, 59, 60, 61, 72, 65537, 65539, 65536 * 3,
                65536 * 3 + 5};
        for (long v : vs) {
            b.appendKey(v);
        }
        final Index ix = b.getIndex();
        final WritableLongChunk<OrderedKeyIndices> kixchunk = WritableLongChunk.makeWritableChunk(vs.length);
        ix.fillKeyIndicesChunk(kixchunk);
        assertEquals(vs.length, kixchunk.size());
    }

    public void testBuilderAddKeys() {
        final Index.RandomBuilder b = Index.FACTORY.getRandomBuilder();
        b.addKey(27);
        final long[] vs =
                {3, 4, 5, 8, 10, 12, 29, 31, 44, 45, 46, 59, 60, 61, 72, 65537, 65539, 65536 * 3, 65536 * 3 + 5};
        b.addKeys(new PrimitiveIterator.OfLong() {
            int vi = 0;

            @Override
            public long nextLong() {
                return vs[vi++];
            }

            @Override
            public boolean hasNext() {
                return vi < vs.length;
            }
        });
        final Index ix = b.getIndex();
        assertEquals(vs.length + 1, ix.size());
        for (int i = 0; i < vs.length; ++i) {
            assertTrue(ix.find(vs[i]) >= 0);
        }
        assertTrue(ix.find(27) >= 0);
    }

    public void testBuilderAppendKeys() {
        final Index.SequentialBuilder b = Index.FACTORY.getSequentialBuilder();
        b.appendKey(1);
        final long[] vs =
                {3, 4, 5, 8, 10, 12, 29, 31, 44, 45, 46, 59, 60, 61, 72, 65537, 65539, 65536 * 3, 65536 * 3 + 5};
        b.appendKeys(new PrimitiveIterator.OfLong() {
            int vi = 0;

            @Override
            public long nextLong() {
                return vs[vi++];
            }

            @Override
            public boolean hasNext() {
                return vi < vs.length;
            }
        });
        final Index ix = b.getIndex();
        assertEquals(vs.length + 1, ix.size());
        for (int i = 0; i < vs.length; ++i) {
            assertTrue(ix.find(vs[i]) >= 0);
        }
        assertTrue(ix.find(1) >= 0);
    }

    public void testBuilderAddRanges() {
        final Index.RandomBuilder b = Index.FACTORY.getRandomBuilder();
        final long[] vs =
                {3, 4, 5, 8, 10, 12, 29, 31, 44, 45, 46, 59, 60, 61, 72, 91, 65537, 65539, 65536 * 3, 65536 * 3 + 5};
        b.addRanges(new LongRangeIterator() {
            int vi = -2;

            @Override
            public void next() {
                vi += 2;
            }

            @Override
            public boolean hasNext() {
                return vi < vs.length - 2;
            }

            @Override
            public long start() {
                return vs[vi];
            }

            @Override
            public long end() {
                return vs[vi + 1];
            }
        });
        final Index ix = b.getIndex();
        final Index.RandomBuilder b2 = Index.FACTORY.getRandomBuilder();
        for (int i = 0; i < vs.length; i += 2) {
            b2.addRange(vs[i], vs[i + 1]);
        }
        final Index ix2 = b2.getIndex();
        assertEquals(ix2.size(), ix.size());
        assertTrue(ix2.subsetOf(ix));
    }

    public void testBuilderAppendRanges() {
        final Index.SequentialBuilder b = Index.FACTORY.getSequentialBuilder();
        final long[] vs =
                {3, 4, 5, 8, 10, 12, 29, 31, 44, 45, 46, 59, 60, 61, 72, 91, 65537, 65539, 65536 * 3, 65536 * 3 + 5};
        b.appendRanges(new LongRangeIterator() {
            int vi = -2;

            @Override
            public void next() {
                vi += 2;
            }

            @Override
            public boolean hasNext() {
                return vi < vs.length - 2;
            }

            @Override
            public long start() {
                return vs[vi];
            }

            @Override
            public long end() {
                return vs[vi + 1];
            }
        });
        final Index ix = b.getIndex();
        final Index.RandomBuilder b2 = Index.FACTORY.getRandomBuilder();
        for (int i = 0; i < vs.length; i += 2) {
            b2.addRange(vs[i], vs[i + 1]);
        }
        final Index ix2 = b2.getIndex();
        assertEquals(ix2.size(), ix.size());
        assertTrue(ix2.subsetOf(ix));
    }

    public void testArrayOfSmallIndices() {
        final int sz = 10;
        final Index[] ixs = new Index[sz];
        for (int i = 0; i < sz; ++i) {
            ixs[i] = Index.FACTORY.getEmptyIndex();
            ixs[i].insert(i);
        }
        final Index r = Index.FACTORY.getEmptyIndex();
        for (int i = 0; i < sz; ++i) {
            r.insert(ixs[i]);
        }
        assertEquals(sz, r.size());
        assertEquals(0, r.firstKey());
        assertEquals(sz - 1, r.lastKey());
    }

    public void testSubindexByPosRegreesion0() {
        final Index ix = Index.FACTORY.getEmptyIndex();
        ix.insertRange(0, 95 * ((long) BLOCK_SIZE));
        final long s1 = 0x1000000 * ((long) BLOCK_SIZE);
        ix.insertRange(s1, s1 + 95 * 65536);
        final long s2 = s1 + 0x5F * 65535L;
        ix.insertRange(s2, s2 + 1);
        final long s3 = 0x2000000 * ((long) BLOCK_SIZE);
        ix.insertRange(s3, s3 + 95 * ((long) BLOCK_SIZE));
        final long s4 = s3 + 0x5F * ((long) BLOCK_SIZE);
        ix.insertRange(s4, s4 + 1);
        final Index ix2 = ix.subindexByPos(0L, 9_999_999L);
        ix2.validate();
        final Index ix3 = ix.subindexByPos(9_999_999L, ix.size());
        ix3.validate();
        assertFalse(ix2.overlaps(ix3));
        final Index ix4 = ix2.clone();
        ix4.insert(ix3);
        assertEquals(ix.size(), ix4.size());
        assertTrue(ix.subsetOf(ix4));
    }

    public void testContainsRange() {
        final Index ix = Index.FACTORY.getEmptyIndex();
        ix.insertRange(1, 3);
        ix.insertRange(BLOCK_LAST, BLOCK_SIZE);
        ix.insertRange(2 * BLOCK_SIZE, 2 * BLOCK_SIZE + 1);
        ix.insertRange(3 * BLOCK_SIZE - 2, 3 * BLOCK_SIZE - 1);
        ix.insertRange(6 * BLOCK_SIZE, 6 * BLOCK_SIZE + BLOCK_LAST);
        ix.insertRange(10 * BLOCK_SIZE, 20 * BLOCK_SIZE - 1);
        final Index.RangeIterator rit = ix.rangeIterator();
        while (rit.hasNext()) {
            rit.next();
            final long start = rit.currentRangeStart();
            final long end = rit.currentRangeEnd();
            final String m = "start==" + start + " && end==" + end;
            for (int ds = -1; ds <= 1; ++ds) {
                for (int de = -1; de <= 1; ++de) {
                    final long s = start + ds;
                    if (s < 0) {
                        continue;
                    }
                    final long e = end + de;
                    if (e < s) {
                        continue;
                    }
                    final boolean expected = (start <= s && e <= end);
                    final boolean containsRange = ix.containsRange(s, e);
                    final String m2 = m + " && s==" + s + " && e==" + e;
                    assertEquals(m2, expected, containsRange);
                }
            }
            if (rit.hasNext()) {
                assertFalse(m, ix.containsRange(start, ix.lastKey()));
            }
        }
    }

    public void testOverlapsRange() {
        final Index ix = Index.FACTORY.getEmptyIndex();
        ix.insertRange(3, 5);
        ix.insertRange(BLOCK_LAST, BLOCK_SIZE);
        ix.insertRange(2 * BLOCK_SIZE, 2 * BLOCK_SIZE + 1);
        ix.insertRange(3 * BLOCK_SIZE - 2, 3 * BLOCK_SIZE - 1);
        ix.insertRange(6 * BLOCK_SIZE, 6 * BLOCK_SIZE + BLOCK_LAST);
        ix.insertRange(10 * BLOCK_SIZE, 20 * BLOCK_SIZE - 1);
        final Index.RangeIterator rit = ix.rangeIterator();
        long prevEnd = -1;
        while (rit.hasNext()) {
            rit.next();
            final long start = rit.currentRangeStart();
            final long end = rit.currentRangeEnd();
            final String m = "start==" + start + " && end==" + end;
            if (prevEnd == -1) {
                assertFalse(m, ix.overlapsRange(0, start - 1));
                assertTrue(m, ix.overlapsRange(0, start));
                prevEnd = end;
                continue;
            }
            for (int ds = -1; ds <= 1; ++ds) {
                for (int de = -1; de <= 1; ++de) {
                    final long rs = prevEnd + ds;
                    if (rs < 0) {
                        continue;
                    }
                    final long re = start + de;
                    if (re < rs) {
                        continue;
                    }
                    final boolean expected = rs <= prevEnd || re >= start;
                    final boolean overlapsRange = ix.overlapsRange(rs, re);
                    final String m2 = m + " && rs==" + rs + " && re==" + re;
                    assertEquals(m2, expected, overlapsRange);
                }
            }
            if (end < ix.lastKey()) {
                assertTrue(m, ix.overlapsRange(end + 1, ix.lastKey()));
            }
            if (start > 0) {
                // this is not the first range.
                assertTrue(m, ix.overlapsRange(0, start - 1));
            }
            prevEnd = end;
        }
        assertFalse(ix.overlapsRange(ix.lastKey() + 1, ix.lastKey() + 10000 * BLOCK_SIZE));
    }

    public void testOneRangeIndexMinus() {
        final Index ix = Index.FACTORY.getIndexByRange(1000, 5000);
        final Index r = ix.minus(Index.FACTORY.getIndexByRange(1001, 4999));
        final Index c = ix.clone();
        c.removeRange(1001, 4999);
    }

    private void releaseReleasedRegression0case0(final Index ix) {
        ix.insertRange(1, 2);
        ix.insertRange(5, 6);
        final Index ix2 = ix.clone();
        assertEquals(2, ix.refCount());
        assertEquals(2, ix2.refCount());
        ix.removeRange(1, 6);
        assertEquals(0, ix.size());
        assertEquals(1, ix2.refCount());
    }

    public void testReleaseReleasedRegression0case0Rsp() {
        releaseReleasedRegression0case0(TreeIndex.makeEmptyRsp());
    }

    public void testReleaseReleasedRegression0case0SR() {
        releaseReleasedRegression0case0(TreeIndex.makeEmptySr());
    }

    private void releaseReleasedRegression0case1(final Index ix) {
        ix.insertRange(1, 2);
        ix.insertRange(5, 6);
        final Index ix2 = ix.clone();
        assertEquals(2, ix.refCount());
        assertEquals(2, ix2.refCount());
        ix.remove(ix2);
        assertEquals(0, ix.size());
        assertEquals(1, ix2.refCount());
    }

    public void testReleaseReleasedRegression0case1Rsp() {
        releaseReleasedRegression0case1(TreeIndex.makeEmptyRsp());
    }

    public void testReleaseReleasedRegression0case1SR() {
        releaseReleasedRegression0case1(TreeIndex.makeEmptySr());
    }

    private void releaseReleasedRegression0case2(final Index ix) {
        ix.insertRange(1, 2);
        ix.insertRange(5, 6);
        final Index ix2 = ix.clone();
        assertEquals(2, ix.refCount());
        assertEquals(2, ix2.refCount());
        ix.update(Index.FACTORY.getEmptyIndex(), ix2);
        assertEquals(0, ix.size());
        assertEquals(1, ix2.refCount());
    }

    public void testReleaseReleasedRegression0case2Rsp() {
        releaseReleasedRegression0case2(TreeIndex.makeEmptyRsp());
    }

    public void testReleaseReleasedRegression0case2SR() {
        releaseReleasedRegression0case2(TreeIndex.makeEmptySr());
    }

    public void testSearchIteratorRegression0() {
        final Index ix0 = Index.FACTORY.getEmptyIndex();
        // force a bitmap container.
        for (int i = 0; i < 5000; ++i) {
            ix0.insert(2 * i);
        }
        ix0.insertRange(10069, 10070);
        final Index.SearchIterator it = ix0.searchIterator();
        final long target0 = 10070;
        final MutableLong target = new MutableLong(target0);
        final Index.TargetComparator comp = (long key, int dir) -> Long.compare(target.longValue(), key) * dir;
        final long r = it.binarySearchValue(comp, 1);
        assertEquals(target0, r);
        assertEquals(target0, it.currentValue());
        assertFalse(it.hasNext());
    }

    public void testAsKeyRangesChunk() {
        final Index index = Index.FACTORY.getEmptyIndex();
        index.insertRange(130972, 131071);
        index.insert(262144);

        final LongChunk<Attributes.OrderedKeyRanges> ranges = index.asKeyRangesChunk();
        assertEquals(4, ranges.size());
        assertEquals(130972, ranges.get(0));
        assertEquals(131071, ranges.get(1));
        assertEquals(262144, ranges.get(2));
        assertEquals(262144, ranges.get(3));
    }

    public void testReverseIteratorAdvanceRegression0() {
        Index.SequentialBuilder builder = Index.FACTORY.getSequentialBuilder();
        final int key = 1073741822;
        builder.appendKey(key);
        builder.appendKey(1073741824);
        Index index = builder.getIndex();
        final Index.SearchIterator reverseIter = index.reverseIterator();
        final boolean valid = reverseIter.advance(key);
        assertTrue(valid);
    }

    public void testReverseIteratorAdvanceRegression1() {
        Index.SequentialBuilder builder = Index.FACTORY.getSequentialBuilder();
        int key = 1073741822;
        builder.appendKey(1073741820);
        builder.appendKey(key);
        builder.appendKey(1073741824);
        Index index = builder.getIndex();
        Index.SearchIterator iter = index.reverseIterator();
        assertTrue(iter.advance(key));
        assertEquals(key, iter.currentValue());
    }

    public void testReverseIteratorAdvanceRegression2() {
        long key = 1073741824;
        Index.RandomBuilder builder = Index.FACTORY.getRandomBuilder();
        builder.addKey(key);
        builder.addKey(key + 10);
        Index index = builder.getIndex();
        index.remove(key + 10);
        Index.SearchIterator iter = index.reverseIterator();
        assertFalse(iter.advance(key - 1));
    }

    public void testReverseIteratorAdvanceRegression3() {
        long key = 1073741824;
        Index.RandomBuilder builder = Index.FACTORY.getRandomBuilder();
        builder.addKey(key);
        builder.addKey(key + 10);
        Index index = builder.getIndex();
        index.remove(key + 10);
        assertFalse(index.reverseIterator().advance(key - 1));
        assertTrue(index.reverseIterator().advance(key));
        assertTrue(index.reverseIterator().advance(key + 1));
    }

    public void testReverseIteratorAdvanceRegression4() {
        long key = 1073741822;
        Index.SequentialBuilder builder = Index.FACTORY.getSequentialBuilder();
        builder.appendKey(key);
        Index index = builder.getIndex();
        Index.SearchIterator iter = index.reverseIterator();
        assertTrue(iter.advance(key + 10));
        assertEquals(key, iter.currentValue());
    }

    public void testReverseIteratorAdvanceRegression5() {
        long key = 1073741823;
        Index.SequentialBuilder builder = Index.FACTORY.getSequentialBuilder();
        builder.appendKey(key - 1);
        builder.appendKey(key + 1);
        Index index = builder.getIndex();
        Index.SearchIterator iter = index.reverseIterator();
        assertTrue(iter.advance(key - 1));
        assertEquals(key - 1, iter.currentValue());
        assertTrue(iter.advance(key - 1));
        assertEquals(key - 1, iter.currentValue());
    }

    public void testReverseIteratorAdvanceRegression6() {
        Index.SequentialBuilder builder = Index.FACTORY.getSequentialBuilder();
        final int key = BLOCK_SIZE;
        builder.appendKey(key - 1);
        builder.appendKey(key + 1);
        Index index = builder.getIndex();
        final Index.SearchIterator reverseIter = index.reverseIterator();
        final boolean valid = reverseIter.advance(key);
        assertTrue(valid);
        assertEquals(key - 1, reverseIter.currentValue());
    }

    public void testIteratorAdvanceRegression1() {
        final Index ix = Index.FACTORY.getIndexByValues(3, 5, 7, 9, 11, 21);
        final Index.RangeIterator rit = ix.rangeIterator();
        assertTrue(rit.advance(21));
        assertEquals(21, rit.currentRangeStart());
        assertEquals(21, rit.currentRangeEnd());
        assertFalse(rit.hasNext());
    }

    public void testSubsetOf() {
        final Index ix1 = TreeIndex.makeEmptySr();
        ix1.insertRange(0, 1);
        final Index ix2 = TreeIndex.makeSingleRange(0, 2);
        assertTrue(ix1.subsetOf(ix2));
    }

    private static void rvs2ix(final Index ix, final long[] vs) {
        long pendingStart = -1;
        for (long v : vs) {
            if (v < 0) {
                ix.insertRange(pendingStart, -v);
                pendingStart = -1;
            } else {
                if (pendingStart != -1) {
                    ix.insert(pendingStart);
                }
                pendingStart = v;
            }
        }
        if (pendingStart != -1) {
            ix.insert(pendingStart);
        }
    }

    public void testMinusRegression0() {
        final Index ix1 = new TreeIndex(RspBitmap.makeSingleRange(1073741843L, 1073741860L));
        final Index ix2 = new TreeIndex(SortedRanges.makeSingleElement(1073741843L));
        final Index ix3 = ix1.minus(ix2);
        assertEquals(ix1.lastKey() - ix1.firstKey(), ix3.size());
        assertTrue(ix3.containsRange(ix1.firstKey() + 1, ix1.lastKey()));
    }

    public void testSubsetOfRegression0() {
        SortedRanges sr0 = new SortedRangesShort(64, 10);
        final Index ix0 = new TreeIndex(sr0);
        rvs2ix(ix0, new long[] {
                85, 88, 103, 121, 201, 204, 220, 258, 275, 296, 366, 370, 386, 409, 411, 453, 584, 587, 602, 631, 661,
                683, 714, -715,
                744, 750, 786, 791, 830, 836, 841, 885, 911, 941, 981, 993, 1024, 1052, 1054, 1089, 1151, 1220, 1232,
                1243, 1266, -1267,
                1296, 1355, 1403, 1429, 1431, 1442, 1466, 1477, 1533, 1556, 1559, 1577, 1579, 1589, 1610, 1622, 1645,
                1654, 1661, 1683,
                1695, 1784, 1790, 1830, 1876, 1914, 1926, 1972, 1980, 2016, 2075, 2082, 2150, 2154, 2166, -2167, 2171,
                2177, 2188, 2202,
                2217, -2218, 2246, 2257, 2266, 2278, -2279, 2282, 2288, 2294, 2300, 2302, -2303, 2331, 2346, 2355, 2364,
                2368, 2377,
                2412, 2421, 2442, 2465, 2472, -2473, 2488, 2497, 2500, 2504, 2511, 2529, -2530, 2532, 2536, -2537, 2540,
                2560, 2563, -2564,
                2573, -2574, 2588, 2595, -2596, 2598, 2601, 2612, 2627, -2628, 2647, 2664, 2670, 2672, -2673, 2696,
                -2697, 2702, 2723,
                2741, 2751, 2757, 2784, 2792, 2799, 2818, 2824, 2838, 2853, 2857, 2863
        });
        final TreeIndex ix1 = TreeIndex.makeEmptyRsp();
        rvs2ix(ix1, new long[] {296, 366, 370, 386, 409, 411, 453});
        assertTrue(ix1.subsetOf(ix0));
    }

    public void testIntersectRegression0() {
        Index ix0 = TreeIndex.makeEmptySr();
        Index ix1 = TreeIndex.makeEmptyRsp();
        ix0 = indexFromString(
                "87,236,275,324,329,468,505,673,705,779,834,848,917,1017,1019,1062,1366,1405,1453,1575,1599,1757," +
                        "1834,1853,1856,1895,1960,2098,2167,2218,2411,2606,2686,2842,2958,3225,3451,3509,3587,3601," +
                        "3614,3722,3747,3807,3907,4061,4158,4371,4558,4590-4591,4732,4739,4757,4801,4894,5000,5312," +
                        "5601,5755,5854,5901,6006,6029,6080,6117,6126,6176,6339-6340,6384,6431,6627,6903,6916,7159",
                ix0);
        ix1 = indexFromString(
                "6849,6851,6856,6859,6863,6866,6875,6884,6888,6895,6900,6902-6903,6921,6923,6941,6949,6968," +
                        "6974,6978-6979,6997,7007-7020,7022-7023,7025-7028,7030-7032,7034-7056,7058-7066,7068-7075," +
                        "7077-7091,7094-7117,7119-7125,7127-7129,7131-7142,7144,7146-7168,7170-7172,7174-7180," +
                        "7182-7187,7189-7211,7213-7224",
                ix1);
        final Index ix2 = ix0.intersect(ix1);
        ix2.validate();
    }

    public void testConversionRemovalRefCount0() {
        Index ix0 = TreeIndex.makeEmptySr();
        final int n = 4;
        for (int i = 0; i < SortedRanges.MAX_CAPACITY - n; ++i) {
            ix0.insert(i * 6 + 2);
        }
        final Index ix1 = TreeIndex.makeEmptySr();
        for (int i = 0; i < n + 1; ++i) {
            ix1.insert(i * 6 + 4);
        }
        final int initialRefCount = ix0.refCount();
        final Index ix2 = ix0.clone();
        assertEquals(initialRefCount + 1, ix2.refCount());
        assertEquals(initialRefCount + 1, ix0.refCount());
        ix0.insert(ix1);
        assertEquals(SortedRanges.MAX_CAPACITY + 1, ix0.size());
        assertEquals(1, ix0.refCount());
        assertEquals(initialRefCount, ix2.refCount());
    }

    public void testSubsetOfMixed() {
        final Index ix0 = TreeIndex.makeEmptySr();
        final long bkSz = BLOCK_SIZE;
        final long off0 = bkSz;
        ix0.insertRange(off0 + 7, off0 + 9);
        final long off1 = off0 + 2 * bkSz;
        ix0.insertRange(off1 + 13, off1 + 15);
        final long off2 = off1 + 4 * bkSz;
        ix0.insert(off2 + 20);
        final long off3 = off2 + 3 * bkSz;
        ix0.insertRange(off2 + 33, off3 + 45);
        final Index ix1 = TreeIndex.makeEmptyRsp();
        ix1.insertRange(off0 + 7, off0 + 9);
        ix1.insertRange(off1 + 13, off1 + 15);
        ix1.insert(off2 + 20);
        ix1.insertRange(off2 + 33, off3 + 45);
        assertTrue(ix1.subsetOf(ix0));
        long pendingStart = 0;
        try (final Index.RangeIterator it1 = ix0.rangeIterator()) {
            while (it1.hasNext()) {
                it1.next();
                final long start = it1.currentRangeStart();
                final long end = it1.currentRangeEnd();
                for (long v : new long[] {pendingStart, start - bkSz, start - 1}) {
                    final Index ix2 = ix1.clone();
                    ix2.insert(v);
                    assertFalse("v==" + v, ix2.subsetOf(ix0));
                }
                pendingStart = end + 1;
            }
            for (long v : new long[] {pendingStart, pendingStart + 3}) {
                final Index ix2 = ix1.clone();
                ix2.insert(v);
                assertFalse("v==" + v, ix2.subsetOf(ix0));
            }
        }
    }

    public void testRemoveRegression0() {
        final String ix0Str = "201609,201631-201632,201671,201674,201705,201715-201716,201719,201724,201749,201782," +
                "201789,201842,201865,201888,201892,201908,201918,201927,201935,201954,201961,201971,202012,202014," +
                "202022,202034,202082,202092,202137,202140,202142,202148,205512,205519,205539,205557,205561,205579," +
                "205613,205667,205724,205759,205799-205800,205818,205822,205841,205853,205866,205893,205934,209611," +
                "209618,209621,209636,209673,209702,209729,209731,209736,209748,209772,209823,209835,209886,209891," +
                "209894,209897,209926,209943,209980,209989,209995,210058,210079,210109,210149,210158,221440,221474," +
                "221486,221492,221527,221537,221555,221608,221612,221614,221644,221669,221680,221958,222006,222018," +
                "227097,227110,230227,230229,232133-233244,233246-235291";
        final String ix1Str = "201719,201888,202012,202014,202156,202159,202179,202199,202212,202227,202279,202287," +
                "202289,202301,202347,202451,202467,202480,202488,202537,202541,202571,202643,202681,202756,202784," +
                "202826,202844,202847,202911,203019,203116,203130,203163,203178,203182,203194-203195,203216-203217," +
                "203247,203308,203312,203326,203337,203368,203427-203428,203466,203470,203513,203524,203595,203625," +
                "203655,203681,203711,203719,203738,203758,203768,203776,203778,203785,203801,203875,203893,203940," +
                "203951,203987,204000,204025,204043,204085,204148,204171,204261,204289,204353,204383,204399,204474," +
                "204524,204529,204541,204556-204557,204559,204570,204611,204620,204641,204802,204871,204931-204932," +
                "204956,205035,205107,205114,205155,205167,205175,205183,205204,205226,205316,205357,205370,205401," +
                "205422-205423,205822,205934,209835,209886,210079,210158,219810,219815,219824,219871,219892,219916," +
                "219937,220009,220022,220025,220040,220052,220081,220117,220144,220182,220189,220192,220226,220232," +
                "220297,220306,220406,220415,220429,220435,220445,220477,220629,220800,220809,220901,220918,220922," +
                "220961,220967,220969,220976,220981,220989,220992,221182,221316,221346,221357,221403,221414,221420," +
                "221439-221440,221492,232136,232159,232169,232182,232186,232212-232213,232219,232252,232281,232306," +
                "232310,232314,232320,232338,232369,232374,232397,232402,232406,232424,232432,232436,232441,232444," +
                "232459,232490,232494,232499,232516-232518,232525,232543,232558,232580,232591,232598,232603,232640," +
                "232649,232660,232673,232677,232686,232709,232715,232721,232723,232739,232743,232757,232771,232773," +
                "232776,232778,232783,232796,232810,232823";
        Index ix0 = TreeIndex.makeEmptySr();
        ix0 = indexFromString(ix0Str, ix0);
        Index ix1 = TreeIndex.makeEmptyRsp();
        ix1 = indexFromString(ix1Str, ix1);
        Index ix0Rsp = TreeIndex.makeEmptyRsp();
        ix0Rsp = indexFromString(ix0Str, ix0Rsp);
        ix0.remove(ix1);
        ix0Rsp.remove(ix1);
        assertEquals(ix0Rsp.size(), ix0.size());
        assertTrue(ix0Rsp.subsetOf(ix0));
    }

    public void testMinusRegression1() {
        final Index ix0 = TreeIndex.makeEmptySr();
        rvs2ix(ix0, new long[] {
                10, -12, 14, 16, 18, 20, 22, 24, 26, 28, -29, 31, 33, -34, 36, 38, -39, 41, 43, 45, -47, 49, 51, 53, 55,
                57, 59, 61, 63, -64,
                66, -67, 69, 71, -76, 78, -80, 82, -83, 85, -88, 90, -91, 93, -94, 96, 98, 100, 102, -110, 112, -113
        });
        final Index ix1 = TreeIndex.makeEmptySr();
        rvs2ix(ix1, new long[] {24, 28, 31, 38, 51, 57, 59, 61, 74, 90, 93, 107, -108, 110});
        final Index ix3 = ix0.minus(ix1);
        final Index intersect = ix0.intersect(ix1);
        final Index ix4 = ix0.clone();
        ix4.remove(intersect);
        assertEquals(ix4.size(), ix3.size());
        assertTrue(ix4.subsetOf(ix3));
    }

    public void testMinusRegression2() {
        final String ix0Str = "88,103,121,258,275,366,370,409,411,584,587,602,683,714-715,744,750,791,836,981,1024," +
                "1052,1054,1089,1151,1220,1243,1267,1296,1403,1429,1533,1556,1589,1661,1784,1790,1914,2150,2167,2171," +
                "2202,2288,2294,2300,2303,2331,2346,2355,2364,2368,2412,2421,2504,2511,2529-2530,2532,2563,2574,2595," +
                "2628,2670,2673,2696,2741,2751,2757,2792,2799,2818";

        final String ix1Str = "4,22,60,64,75,78,106,109,129,135,161,196,217,279,285,312,339,363,366,373,379,420,454," +
                "528,592,629,646,651,706,725,731,735,787,813,902,947,1002,1048,1064,1070,1099,1133,1135,1154,1159," +
                "1175,1183,1211,1223,1270,1301,1308,1323,1347,1357,1365,1367,1382,1430,1538,1558,1564,1587,1638,1641," +
                "1677-1678,1732,1761,1774,1804,1811,1813,1843,1856,1890,1903,1918,1927,1933,1942,2009,2068,2155,2184," +
                "2189,2206,2210,2221,2223,2230,2303,2310,2316,2329,2347,2352,2354,2380,2458,2474,2482,2489,2494,2503," +
                "2622,2625,2631,2635,2640,2643-2644,2684,2686,2688,2690,2704,2725,2811,2829,2839";
        Index ix0 = TreeIndex.makeEmptySr();
        ix0 = indexFromString(ix0Str, ix0);
        Index ix1 = TreeIndex.makeEmptyRsp();
        ix1 = indexFromString(ix1Str, ix1);
        Index ix0Rsp = TreeIndex.makeEmptyRsp();
        ix0Rsp = indexFromString(ix0Str, ix0Rsp);
        Index result = ix0.minus(ix1);
        Index rspResult = ix0Rsp.minus(ix1);
        assertEquals(rspResult.size(), result.size());
        assertTrue(rspResult.subsetOf(result));
    }

    public void testRemoveRegression1() {
        Index ix0 = TreeIndex.makeEmptyRsp();
        ix0 = indexFromString("0-65536,131071-393215", ix0);
        Index ix1 = TreeIndex.makeEmptySr();
        ix1 = indexFromString("195608,196607", ix1);
        ix0.remove(ix1);
        Index expected = TreeIndex.makeEmptySr();
        expected = indexFromString("0-65536,131071-195607,195609-196606,196608-393215", expected);
        assertEquals(expected.size(), ix0.size());
        assertTrue(expected.subsetOf(ix0));
    }

    public void testRemoveRegression2() {
        Index ix0 = TreeIndex.makeEmptyRsp();
        ix0.insert(0);
        ix0.insert(65535);
        for (int i = 2; i <= 65534; i += 2) {
            ix0.insert(i);
        }
        Index ix1 = TreeIndex.makeEmptySr();
        ix1.insert(0);
        ix1.insert(65535);
        ix0.remove(ix1);
        Index expected = ix0.subindexByKey(1, 65534);
        assertEquals(expected.size(), ix0.size());
        assertTrue(expected.subsetOf(ix0));
    }

    public void testSequentialBuilderCompactness() {
        if (!ImmutableContainer.ENABLED) {
            return;
        }
        final TreeIndexImpl.SequentialBuilder b = new TreeIndexImplSequentialBuilder();
        final int nblocks = SortedRanges.INT_SPARSE_MAX_CAPACITY + 1;
        for (long block = 0; block < nblocks; ++block) {
            final long blockKey = block * BLOCK_SIZE;
            b.appendKey(blockKey + 10);
            if (block % 2 == 0) {
                b.appendRange(blockKey + 11, blockKey + 20);
            } else {
                b.appendKey(blockKey + 12);
                b.appendKey(blockKey + 14);
                b.appendKey(blockKey + 16);
                b.appendKey(blockKey + 18);
                b.appendKey(blockKey + 20);
            }
        }
        final TreeIndexImpl impl = b.getTreeIndexImpl();
        assertTrue(impl instanceof RspBitmap);
        final RspBitmap rsp = (RspBitmap) impl;
        assertEquals(0.0, rsp.containerOverhead());
    }

    public void testRetainMixed() {
        final Index ix1 = new TreeIndex(new RspBitmap(3, 4));
        SortedRanges sr = new SortedRangesLong();
        sr = sr.add(20);
        final Index ix2 = new TreeIndex(sr);
        ix1.retain(ix2);
        assertTrue(ix1.empty());
    }

    public void testRetainRefCountRegress() {
        final Index ix1 = new TreeIndex(new RspBitmap(20, 24));
        SortedRanges sr = new SortedRangesLong();
        sr = sr.add(18);
        sr = sr.add(20);
        final Index ix2 = new CurrentOnlyIndex(sr);
        final Index clone = ix2.clone();
        ix2.retain(ix1);
        assertEquals(1, ix2.refCount());
        assertEquals(1, clone.refCount());
        assertEquals(Index.FACTORY.getIndexByValues(20), ix2);
        assertEquals(Index.FACTORY.getIndexByValues(18, 20), clone);
    }


    public void testRetainRangeRefCountRegress() {
        SortedRanges sr = new SortedRangesLong();
        sr = sr.add(18);
        sr = sr.add(20);
        final Index ix2 = new CurrentOnlyIndex(sr);
        final Index clone = ix2.clone();
        ix2.retainRange(20, 24);
        assertEquals(1, ix2.refCount());
        assertEquals(1, clone.refCount());
        assertEquals(Index.FACTORY.getIndexByValues(20), ix2);
        assertEquals(Index.FACTORY.getIndexByValues(18, 20), clone);
    }

    public void testRetainSrPrefix() {
        SortedRanges sr0 = new SortedRangesLong();
        sr0 = sr0.add(20);
        final Index ix0 = new TreeIndex(sr0);
        SortedRanges sr1 = new SortedRangesLong();
        sr1 = sr1.addRange(5, 10);
        sr1 = sr1.addRange(25, 30);
        final Index ix1 = new TreeIndex(sr1);
        ix0.insert(21); // force prev.
        ix0.retain(ix1);
        ix0.validate();
        assertTrue(ix0.empty());
    }

    public void testInsertWithShift() {
        final long start0 = 2 * BLOCK_SIZE + BLOCK_SIZE / 2;
        final long end0 = 3 * BLOCK_SIZE + BLOCK_SIZE / 2;
        int i0 = 0;
        for (Index ix0 : new Index[] {
                TreeIndex.makeSingleRange(start0, end0),
                new TreeIndex(new RspBitmap(start0, end0)),
                new TreeIndex(SortedRanges.makeSingleRange(start0, end0))}) {
            final long start1 = 4 * BLOCK_SIZE + BLOCK_SIZE / 2;
            final long end1 = 5 * BLOCK_SIZE + BLOCK_SIZE / 2;
            int i1 = 0;
            for (Index ix1 : new Index[] {
                    TreeIndex.makeSingleRange(start1, end1),
                    new TreeIndex(new RspBitmap(start1, end1)),
                    new TreeIndex(SortedRanges.makeSingleRange(start1, end1))}) {
                int ia = 0;
                for (long shiftAmount : new long[] {2 * BLOCK_SIZE, 2 * BLOCK_SIZE + 1}) {
                    final Index ix2 = ix0.clone();
                    ix2.insertWithShift(shiftAmount, ix1);
                    final String m = "i0==" + i0 + " && i1==" + i1 + " && ia==" + ia;
                    assertEquals(m, end1 - start1 + 1 + end0 - start0 + 1, ix2.size());
                    assertTrue(m, ix2.containsRange(start0, end0));
                    assertTrue(m, ix2.containsRange(start1 + shiftAmount, end1 + shiftAmount));
                    ++ia;
                }
                ++i1;
            }
            ++i0;
        }
    }

    public void testSearchIteratorBinarySearchFirstCallNotFoundNoNext() {
        final Index[] ixs = new Index[] {
                new TreeIndex(SingleRange.make(11, 11)),
                new TreeIndex(SortedRanges.makeSingleRange(11, 11)),
                new TreeIndex(RspBitmap.makeSingleRange(11, 11))
        };
        for (int i = 0; i < ixs.length; ++i) {
            final String m = "i==" + i;
            final Index ix = ixs[i];
            final Index.SearchIterator sit = ix.searchIterator();
            final MutableLong target = new MutableLong(10);
            final Index.TargetComparator comp = new Index.TargetComparator() {
                @Override
                public int compareTargetTo(final long rKey, final int direction) {
                    final long d = (target.longValue() - rKey) * direction;
                    return (d > 0) ? 1 : (d < 0) ? -1 : 0;
                }
            };
            long v = sit.binarySearchValue(comp, 1);
            assertEquals(-1, v);
            assertTrue(sit.hasNext());
            target.setValue(11);
            v = sit.binarySearchValue(comp, 1);
            assertEquals(11, v);
            assertFalse(sit.hasNext());
        }
    }

    public void testRemoveSrFromRspFullBlockRegression() {
        final long offset = 3 * BLOCK_SIZE + 52546; // 249154
        SortedRanges sr = new SortedRangesInt(8, offset);
        sr = sr.addRange(offset + 0, offset + 1);
        sr = sr.addRange(offset + 12, offset + 13);
        sr = sr.add(offset + 16);
        sr = sr.add(offset + 22);
        sr = sr.addRange(offset + 25, offset + 27);
        final long card = 5 * BLOCK_SIZE;
        final RspBitmap rb = new RspBitmap(0, card - 1);
        assertEquals(card, rb.getCardinality());
        final TreeIndexImpl result = rb.ixRemove(sr);
        assertEquals(card - sr.getCardinality(), result.ixCardinality());
    }

    public void testInsertWithSrTypeChange() {
        final int cap = SortedRanges.INT_SPARSE_MAX_CAPACITY;
        SortedRanges sr0 = new SortedRangesInt(cap, 0);
        for (int i = 0; i < cap; ++i) {
            sr0 = sr0.add(i * BLOCK_SIZE + 1); // ensure we create a sparse SR.
            assertNotNull(sr0);
        }
        assertEquals(cap, sr0.getCardinality());
        final long offset = Short.MAX_VALUE - 1;
        SortedRanges sr1 = new SortedRangesInt(6, offset);
        sr1 = sr1.add(offset);
        sr1 = sr1.add(offset + 10000);
        final TreeIndexImpl result = sr0.ixInsert(sr1);
        result.ixValidate();
        assertEquals(cap + sr1.getCardinality(), result.ixCardinality());
    }

    public void testRemoveTime() {
        final Random r = new Random();
        final Index.SequentialBuilder outer = Index.FACTORY.getSequentialBuilder();
        final Index.SequentialBuilder inner = Index.FACTORY.getSequentialBuilder();
        for (long i = 0; i < 1500000; ++i) {
            long ii = i << 16;
            outer.appendRange(ii, ii + 5);
            if (r.nextInt(10) == 0) {
                inner.appendRange(ii, ((i + 1) << 16) - 1);
            }
        }
        try (final Index result = outer.getIndex();
                final Index toRemove = inner.getIndex()) {
            final long t0 = System.currentTimeMillis();
            result.remove(toRemove);
            final long t1 = System.currentTimeMillis();
            final double removeTimeSeconds = (t1 - t0) / 1000.0;
            // With the O(n^2) implementation this took ~47 seconds on an Intel Core i9-8950HK 2.90 GHz.
            // The O(n) implementation took 0.1 seconds on the same machine.
            final double reasonableThresholdIfWePauseSeconds = 8.0;
            assertTrue("removeTimeSeconds=" + removeTimeSeconds,
                    removeTimeSeconds < reasonableThresholdIfWePauseSeconds);
        }
    }

    public void testSequentialBuilderMakesSortedRanges() {
        final TreeIndexImplSequentialBuilder builder = new TreeIndexImplSequentialBuilder();
        builder.appendRange(10, 20);
        builder.appendRange(30, 40);
        final TreeIndexImpl tix = builder.getTreeIndexImpl();
        assertTrue(tix instanceof SortedRanges);
    }

    public void testSequentialBuilderMakesSparseSortedRangesLong() {
        for (int j = 0; j < 2; ++j) {
            final int max = SortedRanges.LONG_SPARSE_MAX_CAPACITY + ((j == 0) ? 0 : 1);
            final TreeIndexImplSequentialBuilder builder = new TreeIndexImplSequentialBuilder();
            for (int i = 0; i < max; ++i) {
                builder.appendKey(Integer.MAX_VALUE * (long) i);
            }
            TreeIndexImpl tix = builder.getTreeIndexImpl();
            assertTrue((j == 0) ? tix instanceof SortedRanges : tix instanceof RspBitmap);
        }
    }

    public void testSequentialBuilderMakesSparseSortedRangesInt() {
        for (int j = 0; j < 2; ++j) {
            final int max = SortedRanges.INT_SPARSE_MAX_CAPACITY + ((j == 0) ? 0 : 1);
            final TreeIndexImplSequentialBuilder builder = new TreeIndexImplSequentialBuilder();
            final int step = BLOCK_SIZE / (SortedRanges.ELEMENTS_PER_BLOCK_DENSE_THRESHOLD - 2);
            for (int i = 0; i < max; ++i) {
                builder.appendKey(i * (long) step);
            }
            TreeIndexImpl tix = builder.getTreeIndexImpl();
            assertTrue((j == 0) ? tix instanceof SortedRanges : tix instanceof RspBitmap);
        }
    }

    public void testSequentialBuilderMakesDenseSortedRangesInt() {
        for (int j = 0; j < 2; ++j) {
            final int max = SortedRanges.INT_DENSE_MAX_CAPACITY + ((j == 0) ? 0 : 1);
            final TreeIndexImplSequentialBuilder builder = new TreeIndexImplSequentialBuilder();
            final int step = BLOCK_SIZE / (SortedRanges.ELEMENTS_PER_BLOCK_DENSE_THRESHOLD + 2);
            for (int i = 0; i < max; ++i) {
                builder.appendKey(i * (long) step);
            }
            TreeIndexImpl tix = builder.getTreeIndexImpl();
            assertTrue((j == 0) ? tix instanceof SortedRanges : tix instanceof RspBitmap);
        }
    }

    public void testRemoveSrFromRspFullBlockRegression2() {
        // try to prime the work data to be only a single thing
        final RspBitmap rb1 = new RspBitmap(0, 1 << 17 - 1);
        rb1.ixCompact();
        final RspBitmap rb2 = new RspBitmap(0, 1 << 16 - 1);
        rb2.ixCompact();
        rb2.andEqualsUnsafe(rb1);
        final long offset = 3 * BLOCK_SIZE + 52546; // 249154
        SortedRanges sr = new SortedRangesInt(8, offset);
        sr = sr.addRange(offset + 0, offset + 1);
        sr = sr.addRange(offset + 12, offset + 13);
        sr = sr.add(offset + 16);
        sr = sr.add(offset + 22);
        sr = sr.addRange(offset + 25, offset + 27);
        final long card = 5 * BLOCK_SIZE;
        final RspBitmap rb = new RspBitmap(0, card - 1);
        assertEquals(card, rb.getCardinality());
        final TreeIndexImpl result = rb.ixRemove(sr);
        assertEquals(card - sr.getCardinality(), result.ixCardinality());
    }
}