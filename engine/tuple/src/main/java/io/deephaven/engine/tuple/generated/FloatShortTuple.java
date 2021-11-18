package io.deephaven.engine.tuple.generated;

import gnu.trove.map.TIntObjectMap;
import io.deephaven.engine.tuple.CanonicalizableTuple;
import io.deephaven.engine.tuple.serialization.SerializationUtils;
import io.deephaven.engine.tuple.serialization.StreamingExternalizable;
import io.deephaven.util.compare.FloatComparisons;
import io.deephaven.util.compare.ShortComparisons;
import org.jetbrains.annotations.NotNull;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.function.UnaryOperator;

/**
 * <p>2-Tuple (double) key class composed of float and short elements.
 * <p>Generated by io.deephaven.replicators.TupleCodeGenerator.
 */
public class FloatShortTuple implements Comparable<FloatShortTuple>, Externalizable, StreamingExternalizable, CanonicalizableTuple<FloatShortTuple> {

    private static final long serialVersionUID = 1L;

    private float element1;
    private short element2;

    private transient int cachedHashCode;

    public FloatShortTuple(
            final float element1,
            final short element2
    ) {
        initialize(
                element1,
                element2
        );
    }

    /** Public no-arg constructor for {@link Externalizable} support only. <em>Application code should not use this!</em> **/
    public FloatShortTuple() {
    }

    private void initialize(
            final float element1,
            final short element2
    ) {
        this.element1 = element1;
        this.element2 = element2;
        cachedHashCode = (31 +
                Float.hashCode(element1)) * 31 +
                Short.hashCode(element2);
    }

    public final float getFirstElement() {
        return element1;
    }

    public final short getSecondElement() {
        return element2;
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
        final FloatShortTuple typedOther = (FloatShortTuple) other;
        // @formatter:off
        return element1 == typedOther.element1 &&
               element2 == typedOther.element2;
        // @formatter:on
    }

    @Override
    public final int compareTo(@NotNull final FloatShortTuple other) {
        if (this == other) {
            return 0;
        }
        int comparison;
        // @formatter:off
        return 0 != (comparison = FloatComparisons.compare(element1, other.element1)) ? comparison :
               ShortComparisons.compare(element2, other.element2);
        // @formatter:on
    }

    @Override
    public void writeExternal(@NotNull final ObjectOutput out) throws IOException {
        out.writeFloat(element1);
        out.writeShort(element2);
    }

    @Override
    public void readExternal(@NotNull final ObjectInput in) throws IOException, ClassNotFoundException {
        initialize(
                in.readFloat(),
                in.readShort()
        );
    }

    @Override
    public void writeExternalStreaming(@NotNull final ObjectOutput out, @NotNull final TIntObjectMap<SerializationUtils.Writer> cachedWriters) throws IOException {
        out.writeFloat(element1);
        out.writeShort(element2);
    }

    @Override
    public void readExternalStreaming(@NotNull final ObjectInput in, @NotNull final TIntObjectMap<SerializationUtils.Reader> cachedReaders) throws Exception {
        initialize(
                in.readFloat(),
                in.readShort()
        );
    }

    @Override
    public String toString() {
        return "FloatShortTuple{" +
                element1 + ", " +
                element2 + '}';
    }

    @Override
    public FloatShortTuple canonicalize(@NotNull final UnaryOperator<Object> canonicalizer) {
        return this;
    }
}