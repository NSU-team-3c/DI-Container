package ru.nsu.annotations;

import ru.nsu.enums.ScopeType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD})
public @interface Bean {
    String name() default ""; // Имя бина, по умолчанию пустое.

    ScopeType scope() default ScopeType.SINGLETON;
}