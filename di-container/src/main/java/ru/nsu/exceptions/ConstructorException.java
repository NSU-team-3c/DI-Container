package ru.nsu.exceptions;

public class ConstructorException extends RuntimeException {
    public ConstructorException(String beanName, String description) {
        super("Something wrong with bean: " + beanName + "\n" + description);
    }
}