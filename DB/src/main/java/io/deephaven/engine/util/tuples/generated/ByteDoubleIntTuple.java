package io.deephaven.engine.util.tuples.generated;

import io.deephaven.engine.tables.lang.DBLanguageFunctionUtil;
import io.deephaven.engine.util.serialization.SerializationUtils;
import io.deephaven.engine.util.serialization.StreamingExternalizable;
import io.deephaven.engine.util.tuples.CanonicalizableTuple;
import gnu.trove.map.TIntObjectMap;
import org.jetbrains.annotations.NotNull;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.function.UnaryOperator;

/**
 * <p>3-Tuple (triple) key class composed of byte, double, and int elements.
 * <p>Generated by {@link io.deephaven.engine.util.tuples.TupleCodeGenerator}.
 */
public class ByteDoubleIntTuple implements Comparable<ByteDoubleIntTuple>, Externalizable, StreamingExternalizable, CanonicalizableTuple<ByteDoubleIntTuple> {

    private static final long serialVersionUID = 1L;

    private byte element1;
    private double element2;
    private int element3;

    private transient int cachedHashCode;

    public ByteDoubleIntTuple(
            final byte element1,
            final double element2,
            final int element3
    ) {
        initialize(
                element1,
                element2,
                element3
        );
    }

    /** Public no-arg constructor for {@link Externalizable} support only. <em>Application code should not use this!</em> **/
    public ByteDoubleIntTuple() {
    }

    private void initialize(
            final byte element1,
            final double element2,
            final int element3
    ) {
        this.element1 = element1;
        this.element2 = element2;
        this.element3 = element3;
        cachedHashCode = ((31 +
                Byte.hashCode(element1)) * 31 +
                Double.hashCode(element2)) * 31 +
                Integer.hashCode(element3);
    }

    public final byte getFirstElement() {
        return element1;
    }

    public final double getSecondElement() {
        return element2;
    }

    public final int getThirdElement() {
        return element3;
    }

    @Override
    public final int hashCode() {
        return cachedHashCode;
    }

    @Override
    public final boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        final ByteDoubleIntTuple typedOther = (ByteDoubleIntTuple) other;
        // @formatter:off
        return element1 == typedOther.element1 &&
               element2 == typedOther.element2 &&
               element3 == typedOther.element3;
        // @formatter:on
    }

    @Override
    public final int compareTo(@NotNull final ByteDoubleIntTuple other) {
        if (this == other) {
            return 0;
        }
        int comparison;
        // @formatter:off
        return 0 != (comparison = DBLanguageFunctionUtil.compareTo(element1, other.element1)) ? comparison :
               0 != (comparison = DBLanguageFunctionUtil.compareTo(element2, other.element2)) ? comparison :
               DBLanguageFunctionUtil.compareTo(element3, other.element3);
        // @formatter:on
    }

    @Override
    public void writeExternal(@NotNull final ObjectOutput out) throws IOException {
        out.writeByte(element1);
        out.writeDouble(element2);
        out.writeInt(element3);
    }

    @Override
    public void readExternal(@NotNull final ObjectInput in) throws IOException, ClassNotFoundException {
        initialize(
                in.readByte(),
                in.readDouble(),
                in.readInt()
        );
    }

    @Override
    public void writeExternalStreaming(@NotNull final ObjectOutput out, @NotNull final TIntObjectMap<SerializationUtils.Writer> cachedWriters) throws IOException {
        out.writeByte(element1);
        out.writeDouble(element2);
        out.writeInt(element3);
    }

    @Override
    public void readExternalStreaming(@NotNull final ObjectInput in, @NotNull final TIntObjectMap<SerializationUtils.Reader> cachedReaders) throws Exception {
        initialize(
                in.readByte(),
                in.readDouble(),
                in.readInt()
        );
    }

    @Override
    public String toString() {
        return "ByteDoubleIntTuple{" +
                element1 + ", " +
                element2 + ", " +
                element3 + '}';
    }

    @Override
    public ByteDoubleIntTuple canonicalize(@NotNull final UnaryOperator<Object> canonicalizer) {
        return this;
    }
}