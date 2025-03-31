package ru.nsu.exceptions;

public class BadJsonException extends RuntimeException {
    public BadJsonException(String beanName, String description) {
        super("Bad json format: " + beanName + description);
    }
}