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
    private List<String> orderedByDependenciesBeans;
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

    private Object getBeanByScopeAndName(ScopeType scope, String beanName) {
        var singletonInstances = this.getSingletonInstances();
        Object beanInstance = null;
        switch (scope) {
            case SINGLETON -> beanInstance = singletonInstances.get(beanName);
            case THREAD -> beanInstance = this.getThreadLocalBean(beanName);
            case PROTOTYPE -> {}
            default -> {
                throw new RuntimeException("unknown scope");
            }
        }

        return beanInstance;
    }

    //  CLEANING
    private void cleanupBeans() {
        var beanDefinitions = this.getOrderedByDependenciesBeans();
        Collections.reverse(beanDefinitions);

        beanDefinitions.forEach((currentBeanName) -> {
            BeanObject bean = this.getBeans().get(currentBeanName);
            Object beanInstance = null;
            if (bean == null) {
                throw new RuntimeException("bean not found");
            }
            beanInstance = getBeanByScopeAndName(bean.getScope(), currentBeanName);
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
                var prototypeDependency = field.get(beanInstance);
                if (prototypeDependency instanceof Provider) {
                    prototypeDependency = ((Provider<?>) prototypeDependency).get();
                }
                if (prototypeDependency != null) {
                    BeanObject prototypeBean = this.findPrototypeBean(prototypeDependency.getClass().getName());
                    if (prototypeBean != null && prototypeBean.getPreDestroyMethod() != null) {
                        invokePreDestroy(prototypeDependency, prototypeBean);
                    }
                }
            } catch (IllegalAccessException e) {
                throw new SomethingBadException(field.getName() + " pre destroy failed");
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
                throw new SomethingBadException(bean.getName() + "can't invoke pre destroy method");
            }
        }
    }

    public String getContainerState(String beanClassName) {
        StringBuilder s = new StringBuilder();
        s.append("Context:{\n");

        s.append("singleton instances: \n");
        for (String key : singletonInstances.keySet()) {
            Object bean = singletonInstances.get(key);
            s.append(key).append(" : ").append(singletonInstances.get(key).hashCode()).append("=[");
            Field[] beanFields = bean.getClass().getDeclaredFields();
            for (Field beanField : beanFields) {
                try {
                    beanField.setAccessible(true);
                    Object beanFieldValue = beanField.get(bean);
                    if (beanFieldValue != null) {
                        s.append(beanField.getName()).append(" : ").append(beanFieldValue.hashCode());
                    } else {
                        s.append(beanField.getName()).append(" : null");
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
}
