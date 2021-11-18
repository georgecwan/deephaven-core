package io.deephaven.engine.tables.lang;

public enum LanguageParserDummyEnum {
    ONE("One"), TWO("Two"), THREE("Red"), FOUR("Blue");

    private final String attribute;

    LanguageParserDummyEnum(String attribute) {
        this.attribute = attribute;
    }

    public String getAttribute() {
        return attribute;
    }
}