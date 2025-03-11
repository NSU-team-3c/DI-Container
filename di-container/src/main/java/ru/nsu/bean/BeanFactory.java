package ru.nsu.bean;

//import com.sun.org.apache.xpath.internal.Arg;

import ru.nsu.enums.ScopeType;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class BeanFactory {

    public Bean createBean(Class clazz, ScopeType scope, Object...args) {
        int constructorArgsLength = args.length;
        Class[] constructorArgsTypes = new Class[constructorArgsLength];
        for(int i = 0; i < constructorArgsLength; i++){
            constructorArgsTypes[i] = args[i].getClass();
        }
        Object bean = null;
        try {
            Constructor constructor = clazz.getDeclaredConstructor(constructorArgsTypes);
            constructor.setAccessible(true);
            bean = constructor.newInstance(args);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return new Bean(clazz, "", scope, bean);
    }

    public Bean createBean(Class clazz, ScopeType scope) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        Constructor constructor = null;
        try {
            constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        Object bean = constructor.newInstance();

        return new Bean(clazz, "", scope, bean);
    }

    public Bean createBean(Class clazz) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        Constructor constructor = null;
        try {
            constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        Object bean = constructor.newInstance();

        return new Bean(clazz, "", ScopeType.SINGLETON, bean);
    }
}
