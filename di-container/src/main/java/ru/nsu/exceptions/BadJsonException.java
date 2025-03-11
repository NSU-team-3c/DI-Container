package ru.nsu.exceptions;

public class BadJsonException extends RuntimeException {
    public BadJsonException(String beanName, String description) {
        super("Error with json for bean " + beanName + description);
    }
}