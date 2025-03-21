package ru.nsu.app;

import lombok.Data;
import ru.nsu.bean.BeanObject;
import ru.nsu.context.ContextContainer;
import ru.nsu.enums.ScopeType;
import ru.nsu.exceptions.*;


import javax.inject.Named;
import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.Map;

@Data
public class Application {
    private final ContextContainer context;

    public Application(ContextContainer context) {
        this.context = context;
    }

    /**
     * Получение бина по имени.
     * Если найти не получается пытается достать объект из мапы биндинга интерфейсов на класс
     *
     * @param name bean name
     *
     * @return bean
     *
     * @param <T>
     */
    public <T> T getBean(String name) {
        BeanObject bean = null;
        var allBeans = context.getBeans();
        bean = allBeans.get(name);
        if (bean == null) {
            for (var singleBean : allBeans.values()) {
                if (singleBean.getClassName().equals(name) || singleBean.getName().equals(name)) {
                    bean = singleBean;
                    break;
                }
            }
        }

        /*
        * Если ничего не нашли по имени, возможно тогда надо искать по интерфейсам.
        * Названия классов хранятся в виде: package.ClassName
        * Для того чтобы достать имя класса разбиваем строку названия класса по точкам и забираем последний элемент
        * Какой класс будет у объекта после такого биндинга зависит от удачи, но он точно будет реализовывать указанный интерфейс
        */
        if (bean == null) {
            var tmp = context.getInterfaceBindings().get(name).getName().split("\\.");
            bean = allBeans.get(tmp[tmp.length - 1]);
        }
        T result = switch (bean.getScope()) {
            case SINGLETON -> (T) context.getSingletonInstances().get(name);
            case PROTOTYPE -> (T) createBeanInstance(bean);
            case THREAD -> context.getThreadLocalBean(name);
            default -> {
                throw new BadJsonException(bean.getName(), ".No such bean scope: " + bean.getScope());
            }
        };
        if (bean.getScope().equals(ScopeType.PROTOTYPE)) {
            invokePostConstruct(result, bean);
        }

        return result;
    }

    public void instantiateAndRegisterBeans() {
        var beans = context.getBeans();
        var orderedBeanNames = context.getOrderedByDependenciesBeans();

        Collections.reverse(orderedBeanNames);

        orderedBeanNames.forEach(beanName -> {
            BeanObject bean = beans.get(beanName);
            System.out.println(bean);

            instantiateAndRegisterBean(bean);
        });
    }

    private void instantiateAndRegisterBean(BeanObject bean) {
        String beanName = (bean.getName() != null) ? bean.getName() : bean.getClassName();
        ScopeType beanScope = bean.getScope();
        if (beanScope.equals(ScopeType.PROTOTYPE)){
            return;
        }
        if (!context.containsBean(beanName)) {
            Object beanInstance = createBeanInstance(bean);
            invokePostConstruct(beanInstance, bean);
            if (beanScope.equals(ScopeType.THREAD)) {
                context.registerThreadBeanInstance(bean, () -> createBeanInstance(bean));
            } else if (beanScope.equals(ScopeType.SINGLETON)) {
                context.registerSingletonBeanInstance(bean, beanInstance);
            }
        }
    }

    private void invokePostConstruct(Object beanInstance, BeanObject bean) {
        Method postConstructMethod = bean.getPostConstructMethod();
        if (postConstructMethod != null) {
            try {
                postConstructMethod.setAccessible(true);
                postConstructMethod.invoke(beanInstance);
            } catch (Exception e) {
                throw new SomethingBadException(bean.getName() + ": Failed to invoke PostConstruct method");
            }
        }
    }


    public Object createBeanInstance(BeanObject bean) {
        String beanName = (bean.getName() != null) ? bean.getName() : bean.getClassName();

        try {
            Class<?> beanClass = Class.forName(bean.getClassName());
            Constructor<?> constructor = bean.getConstructor();
            if (constructor == null || constructor.getParameters().length == 0) {
                constructor = beanClass.getDeclaredConstructor();
            }
            Object[] constructorParams = resolveConstructorParameters(constructor);
            Object instance = constructor.newInstance(constructorParams);

            var injectedFields = bean.getInjectedFields();
            var injectedProviderFields = bean.getInjectedProviderFields();

            if (injectedFields != null && !injectedFields.isEmpty()) {
                for (Field field : injectedFields) {
                    field.setAccessible(true);
                    Object fieldInstance = injectField(field);
                    field.set(instance, fieldInstance);
                }
            }


            if (injectedProviderFields != null && !injectedProviderFields.isEmpty()) {
                for (Field field : injectedProviderFields) {
                    field.setAccessible(true);
                    Object fieldInstance = injectField(field);
                    Provider<?> providerField = () -> fieldInstance;
                    field.set(instance, providerField);
                }
            }

            applyInitParams(instance, bean.getInitParams());
            return instance;
        } catch (Exception e) {
            throw new ConstructorException(beanName, "Failed to create instance. " + e.getMessage());
        }
    }


    private Object injectField(Field field) {
        field.setAccessible(true);
        Named namedAnnotation = field.getAnnotation(Named.class);
        String actualName = (namedAnnotation != null ? namedAnnotation.value() : field.getType().getName());
        Object fieldInstance = getBean(actualName);
        BeanObject newFieldBean;
        if (fieldInstance == null) {
            newFieldBean = context.getBeans().get(actualName);
            fieldInstance = createAndRegisterBeanDependency(newFieldBean);
        }
        return fieldInstance;
    }


    private Object createAndRegisterBeanDependency(BeanObject bean) {
        Object beanInstance = createBeanInstance(bean);
        invokePostConstruct(beanInstance, bean);
        switch (bean.getScope()) {
            case THREAD ->
                    context.registerThreadBeanInstance(bean, () -> createBeanInstance(bean));
            case SINGLETON -> context.registerSingletonBeanInstance(bean, beanInstance);
            case PROTOTYPE -> {
            }
        }
        if (beanInstance == null) {
            throw new SomethingBadException(bean.getName() + " Error in creating of dependency, can't create instance for this name.");
        }

        return beanInstance;
    }

    private Object[] resolveConstructorParameters(Constructor<?> constructor) {
        Class<?>[] paramTypes = constructor.getParameterTypes();
        Object[] params = new Object[paramTypes.length];

        for (int i = 0; i < paramTypes.length; i++) {
            if (Provider.class.isAssignableFrom(paramTypes[i])) {
                Class<?> actualType = (Class<?>) ((ParameterizedType) constructor.getGenericParameterTypes()[i]).getActualTypeArguments()[0];
                Provider<?> provider = () -> getBean(actualType.getName());
                params[i] = provider;
            } else {
                // Пытаемся получить имя из аннотации @Named для параметра, если оно есть
                Annotation[] annotations = constructor.getParameterAnnotations()[i];
                String namedValue = null;
                for (Annotation annotation : annotations) {
                    if (annotation instanceof Named) {
                        namedValue = ((Named) annotation).value();
                        break;
                    }
                }
                Object paramsResult;
                String actualName;

                // Определяем actualName в зависимости от того, задано ли namedValue.
                actualName = (namedValue != null) ? namedValue : paramTypes[i].getName();

                // Пытаемся получить bean с использованием actualName.
                paramsResult = getBean(actualName);

                // Если bean не найден, создаем его инстанс.
                if (paramsResult == null) {
                    BeanObject beanDefinition = context.getBeans().get(actualName);
                    paramsResult = createAndRegisterBeanDependency(beanDefinition);
                }

                params[i] = paramsResult;
            }
        }
        return params;
    }


    private void applyInitParams(Object instance, Map<String, Object> initParams) {
        if (initParams == null || initParams.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : initParams.entrySet()) {
            try {
                String methodName = entry.getKey();
                Object value = entry.getValue();
                Method setterMethod = findMethodByNameAndParameterType(instance.getClass(), methodName, value);
                setterMethod.invoke(instance, value);
            } catch (Exception e) {
                throw new SomethingBadException(entry.getKey() + instance.getClass().getName());
            }
        }
    }

    private Method findMethodByNameAndParameterType(Class<?> clazz, String methodName, Object value) throws NoSuchMethodException {
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterTypes().length == 1 && method.getParameterTypes()[0].isAssignableFrom(value.getClass())) {
                return method;
            }
        }
        throw new NoSuchMethodException(clazz.getName() + "." + methodName + "(...)");
    }
}