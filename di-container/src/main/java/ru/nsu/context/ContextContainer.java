package ru.nsu.context;

import lombok.Data;
import lombok.NonNull;
import ru.nsu.bean.BeanObject;
import ru.nsu.enums.ScopeType;
import ru.nsu.exceptions.SomethingBadException;
import ru.nsu.scanner.BeanScanner;

import javax.inject.Provider;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Supplier;

@Data
public class ContextContainer {
    private Map<String, BeanObject> beans;
    private Map<String, Object> singletonInstances = new HashMap<>();
    private List<String> orderedByDependenciesBeans = new ArrayList<>();
    private Map<String, ThreadLocal<Object>> threadInstances = new HashMap<>();
    private Map<String, Object> customBean = new HashMap<>();
    private BeanScanner beanScanner;
    private Map<String, String> interfaceBindings;

    public ContextContainer(BeanScanner beanScanner) {
        this.beanScanner = beanScanner;
        this.beans = beanScanner.getNameToBeansMap();
        this.interfaceBindings = beanScanner.getInterfaceBindings();
        DependenciesManager resolver = new DependenciesManager(this.beans);

        this.orderedByDependenciesBeans = resolver.resolveDependencies();


        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanupBeans));
    }

    public BeanObject findPrototypeBean(String beanName) {
        for (var currentBean : beans.values()) {
            if (currentBean.getName().equals(beanName) || currentBean.getClassName().equals(beanName)) {
                if (currentBean.getScope().equals(ScopeType.PROTOTYPE)) {
                    return currentBean;
                }
            }
        }
        return null;
    }


    public <T> T getThreadLocalBean(String beanName) {
        return threadInstances.get(beanName) != null ? (T) threadInstances.get(beanName).get() : null;
    }

    public boolean containsBean(String beanName) {
        return singletonInstances.containsKey(beanName) || threadInstances.containsKey(beanName);
    }

    public void registerSingletonBeanInstance(@NonNull BeanObject bean, Object beanInstance) {
        singletonInstances.put((bean.getName() != null ? bean.getName() : bean.getClassName()), beanInstance);
    }


    public void registerThreadBeanInstance(@NonNull BeanObject bean, Supplier<?> beanSupplier) {
        threadInstances.put((bean.getName() != null ? bean.getName() : bean.getClassName()), ThreadLocal.withInitial(beanSupplier));
    }

    public String getContainerState(String beanClassName) {
        StringBuilder s = new StringBuilder();
        s.append("Context:{\n");

        s.append("singleton instances: \n");
        for (String key : singletonInstances.keySet()) {
            Object bean = singletonInstances.get(key);
            s.append(key + " : " + singletonInstances.get(key).hashCode() + "=[");
            Field[] beanFields = bean.getClass().getDeclaredFields();
            for (Field beanField : beanFields) {
                try {
                    beanField.setAccessible(true);
                    Object beanFieldValue = beanField.get(bean);
                    if (beanFieldValue != null) {
                        s.append(beanField.getName() + " : " + beanFieldValue.hashCode());
                    } else {
                        s.append(beanField.getName() + " : null");
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            s.append("]\n");
        }


        s.append("}");
        return s.toString();
    }

    //  CLEANING
    private void cleanupBeans() {
        var beanDefinitions = this.getOrderedByDependenciesBeans();
        var singletonInstances = this.getSingletonInstances();
        Collections.reverse(beanDefinitions);

        beanDefinitions.forEach((currentBeanName) -> {
            BeanObject bean = this.getBeans().get(currentBeanName);
            Object beanInstance = null;
            if (bean == null) {
                throw new RuntimeException("bean not found");
            }

            switch (bean.getScope()) {
                case SINGLETON -> beanInstance = singletonInstances.get(currentBeanName);
                case THREAD -> beanInstance = this.getThreadLocalBean(currentBeanName);
                case PROTOTYPE -> beanInstance = null;
                default -> {
                    throw new RuntimeException("unknown scope");
                }
            }

            if (beanInstance != null) {
                checkForPrototypeBeans(beanInstance);
                invokePreDestroy(beanInstance, bean);
            }
        });
    }


    // CLEANING
    public void checkForPrototypeBeans(Object beanInstance) {
        for (Field field : beanInstance.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);

                var prototypeDep = field.get(beanInstance);

                if (prototypeDep instanceof Provider) {
                    prototypeDep = ((Provider<?>) prototypeDep).get();
                }
                if (prototypeDep != null) {
                    BeanObject prototypeBean = this.findPrototypeBean(prototypeDep.getClass().getName());
                    if (prototypeBean != null && prototypeBean.getPreDestroyMethod() != null) {
                        invokePreDestroy(prototypeDep, prototypeBean);
                    }
                }
            } catch (IllegalAccessException e) {
                throw new SomethingBadException(field.getName() + " predestroy failed");
            }
        }
    }

    private void invokePreDestroy(Object beanInstance, BeanObject bean) {
        if (bean.getPreDestroyMethod() != null) {
            try {
                Method preDestroyMethod = bean.getPreDestroyMethod();
                preDestroyMethod.setAccessible(true);
                preDestroyMethod.invoke(beanInstance);
            } catch (Exception e) {
                throw new SomethingBadException(bean.getName() + "can't invoke predestroy method");
            }
        }
    }
}
