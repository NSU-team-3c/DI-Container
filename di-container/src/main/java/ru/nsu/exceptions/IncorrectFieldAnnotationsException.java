package ru.nsu.exceptions;

public class IncorrectFieldAnnotationsException extends Exception {
    public IncorrectFieldAnnotationsException(String inType, String inField) {
        super("Inject and Bean annotation in one field!\n" + "Type: " + inType + "\nField: " + inField);
    }
}