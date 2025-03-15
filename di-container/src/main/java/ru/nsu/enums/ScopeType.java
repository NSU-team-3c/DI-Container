package ru.nsu.enums;

public enum ScopeType {
    SINGLETON("singletone"),
    PROTOTYPE("prototype"),
    THREAD("thread");

    private final String text;

    ScopeType(final String text) {
        this.text = text;
    }

    public String toString() {
        return text;
    }
}
