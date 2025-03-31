package ru.nsu.app;

import lombok.Data;
import ru.nsu.bean.BeanObject;
import ru.nsu.context.ContextContainer;
import ru.nsu.enums.ScopeType;
import ru.nsu.exceptions.BadJsonException;
import ru.nsu.exceptions.ConstructorException;
import ru.nsu.exceptions.SomethingBadException;
import ru.nsu.utils.Utils;

import javax.inject.Named;
import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
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
     * Если найти не получается пытается достать объект из мапы биндинга интерфейсов на бины
     *
     * @param name bean name
     * @param <T>
     * @return bean
     */
    public <T> T getBean(String name) {
        BeanObject bean = null;
        var allBeans = context.getBeans();

        String beanName = name;
        bean = allBeans.get(name);
        if (bean == null) {
            var foundBean = allBeans.values().stream().filter((currBean) -> currBean.getClassName().equals(name)
                    || currBean.getName().equals(name)).findFirst();
            if (foundBean.isPresent()) {
                bean = foundBean.get();
                beanName = bean.getName();
            } else {
                bean = allBeans.get(context.getInterfaceBindings().get(name));
                beanName = bean.getName();
            }
        }

        T result = switch (bean.getScope()) {
            case SINGLETON -> getSingleton(beanName, bean);
            case PROTOTYPE -> (T) createBeanInstance(bean);
            case THREAD -> getThreadLocal(beanName, bean);
        };

        if (bean.getScope().equals(ScopeType.PROTOTYPE)) {
            invokePostConstruct(result, bean);
        }

        return result;
    }

    public <T> T getSingleton(String name, BeanObject bean) {
        if (context.getSingletonInstances().containsKey(name)) {
            return (T) context.getSingletonInstances().get(name);
        } else {
            context.getSingletonInstances().put(name, createBeanInstance(bean));
            return (T) context.getSingletonInstances().get(name);
        }
    }

    public <T> T getThreadLocal(String name, BeanObject bean) {
        var result = context.getThreadLocalBean(name);

        if (result == null) {
            var threadLocal = new ThreadLocal<>();
            threadLocal.set(createBeanInstance(bean));
            context.getThreadInstances().put(name, threadLocal);
            result = context.getThreadLocalBean(name);
        }

        return (T) result;
    }

    private void  registerBean(ScopeType beanScope, BeanObject bean, Object beanInstance) {
        switch (beanScope) {
            case THREAD -> context.registerThreadBeanInstance(bean, () -> createBeanInstance(bean));
            case SINGLETON -> context.registerSingletonBeanInstance(bean, beanInstance);
        }
    }

    public void initAndRegisterBeans() {
        var beans = context.getBeans();
        var orderedBeanNames = context.getOrderedByDependenciesBeans();

        Collections.reverse(orderedBeanNames);

        orderedBeanNames.forEach(beanName -> {
            var bean = beans.get(beanName);
            initAndRegisterBean(bean);
        });
    }

    private void initAndRegisterBean(BeanObject bean) {
        var beanName = (bean.getName() != null) ? bean.getName() : bean.getClassName();
        var beanScope = bean.getScope();
        if (beanScope.equals(ScopeType.PROTOTYPE)) {
            return;
        }
        if (!context.containsBean(beanName)) {
            var beanInstance = createBeanInstance(bean);
            invokePostConstruct(beanInstance, bean);

            registerBean(beanScope, bean, beanInstance);
        }
    }

    private void invokePostConstruct(Object beanInstance, BeanObject bean) {
        Method postConstructMethod = bean.getPostConstructMethod();
        if (postConstructMethod != null) {
            try {
                postConstructMethod.setAccessible(true);
                postConstructMethod.invoke(beanInstance);
            } catch (Exception e) {
                throw new SomethingBadException(bean.getName() + ": failed on post constructor method");
            }
        }
    }


    public Object createBeanInstance(BeanObject bean) {
        var beanName = (bean.getName() != null) ? bean.getName() : bean.getClassName();

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
            throw new ConstructorException(beanName, "failed on instance creation " + e.getMessage());
        }
    }


    private Object injectField(Field field) {
        field.setAccessible(true);
        Named namedAnnotation = field.getAnnotation(Named.class);
        var actualName = (namedAnnotation != null ? namedAnnotation.value() : field.getType().getName());
        var fieldInstance = getBean(actualName);
        BeanObject newFieldBean;
        if (fieldInstance == null) {
            newFieldBean = context.getBeans().get(actualName);
            fieldInstance = createAndRegisterBeanDependency(newFieldBean);
        }
        return fieldInstance;
    }


    private Object createAndRegisterBeanDependency(BeanObject bean) {
        var beanInstance = createBeanInstance(bean);
        invokePostConstruct(beanInstance, bean);
        registerBean(bean.getScope(), bean, beanInstance);
        if (beanInstance == null) {
            throw new SomethingBadException(bean.getName() + " dependency error");
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

                actualName = (namedValue != null) ? namedValue : paramTypes[i].getName();

                paramsResult = getBean(actualName);

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
        initParams.forEach((key, value) -> {
            try {
                String methodName = Utils.createSetMethodName(key);
                Method setterMethod = findMethodByNameAndParameterType(instance.getClass(), methodName, value);
                setterMethod.invoke(instance, value);
            } catch (Exception e) {
                throw new SomethingBadException(key + instance.getClass().getName());
            }
        });
    }

    private Method findMethodByNameAndParameterType(Class<?> clazz, String methodName, Object value) throws NoSuchMethodException {
        var foundMethod = Arrays.stream(clazz.getMethods()).filter((method) -> (method.getName().equals(methodName) &&
                method.getParameterTypes().length == 1 &&
                method.getParameterTypes()[0].isAssignableFrom(value.getClass()))).findFirst();

        if (foundMethod.isPresent()) {
            return foundMethod.get();
        } else {
            throw new NoSuchMethodException(clazz.getName() + ":" + methodName);
        }
    }
}