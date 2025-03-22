package ru.nsu.exceptions;

public class SomethingBadException extends RuntimeException {
    public SomethingBadException(String description) {
        super(description);
    }
}