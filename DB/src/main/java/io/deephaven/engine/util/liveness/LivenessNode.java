package io.deephaven.engine.util.liveness;

/**
 * A {@link LivenessReferent} that is also a {@link LivenessManager}, transitively enforcing liveness on its referents.
 */
public interface LivenessNode extends LivenessReferent, LivenessManager {
}