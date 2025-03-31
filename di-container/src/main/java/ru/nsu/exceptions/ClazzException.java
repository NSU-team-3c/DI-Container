package ru.nsu.exceptions;

public class ClazzException extends RuntimeException {
    public ClazzException(String clazzName) {
        super("clazz error: " + clazzName);
    }
}