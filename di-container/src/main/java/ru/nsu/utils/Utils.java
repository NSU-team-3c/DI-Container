package ru.nsu.utils;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.stream.Stream;

public class Utils {
    public static boolean isAvailableForInjection(Class<?> clazz) {
        if (clazz.isAnnotationPresent(Named.class)) {
            return true;
        }

        return (Stream.of(clazz.getDeclaredFields(), clazz.getDeclaredConstructors(), clazz.getDeclaredMethods())
                .flatMap(Arrays::stream)
                .anyMatch(member -> member.isAnnotationPresent(Inject.class) || member.isAnnotationPresent(Named.class)));
    }

    // TODO change
//    public static String getInjectableFieldName(Field field) {
//        var name = field.getAnnotation(Wired.class).name();
//        if (name.isEmpty() || name.isBlank()) {
//            return getDefaultName(field.getType());
//        }
//
//        return name;
//    }

//    public static String getDefaultName(Class<?> item) {
//        var a = item.getTypeName();
//        return a.substring(0, 1).toLowerCase() +
//                a.substring(1);
//    }
}