package io.deephaven.engine.v2.utils;

public interface SingleValueRedirectionIndex extends RedirectionIndex {
    void setValue(long value);

    long getValue();
}