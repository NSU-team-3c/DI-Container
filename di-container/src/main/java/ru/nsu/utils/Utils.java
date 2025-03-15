package ru.nsu.utils;

import ru.nsu.annotations.Bean;
import ru.nsu.exceptions.IncorrectFieldAnnotationsException;

import java.lang.reflect.Field;

public class Utils {
    public static void isAnnotationValid(Field field) throws IncorrectFieldAnnotationsException {

        // TODO change Wired to Inject
        if (field.isAnnotationPresent(Wired.class) && field.isAnnotationPresent(Bean.class)) {
            throw new IncorrectFieldAnnotationsException(field.getType().toString(), field.getName());
        }
    }

    // TODO change Wired to Inject
    public static String getInjectableFieldName(Field field) {
        var name = field.getAnnotation(Wired.class).name();
        if (name.isEmpty() || name.isBlank()) {
            return getDefaultName(field.getType());
        }

        return name;
    }

    public static String getDefaultName(Class<?> item) {
        var a = item.getTypeName();
        return a.substring(0, 1).toLowerCase() +
                a.substring(1);
    }
}