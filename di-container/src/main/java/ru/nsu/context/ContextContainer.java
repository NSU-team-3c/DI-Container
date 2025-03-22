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
    private Map<String, Class<?>> interfaceBindings;

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
        ThreadLocal<?> threadLocal = threadInstances.get(beanName);
        if (threadLocal != null) {
            return (T) threadLocal.get();
        }
        return null;
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
        for(String key:singletonInstances.keySet()){
            Object bean = singletonInstances.get(key);
            s.append(key + " : " + singletonInstances.get(key).hashCode() + "=[");
            Field[] beanFields = bean.getClass().getDeclaredFields();
            for(Field beanField:beanFields){
                try {
                    beanField.setAccessible(true);
                    Object beanFieldValue = beanField.get(bean);
                    if(beanFieldValue !=null){
                        s.append(beanField.getName() + " : " + beanFieldValue.hashCode());
                    }else{
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

    //  MONITORING
    private void cleanupBeans() {
        var beanDefinitions = this.getOrderedByDependenciesBeans();
        var singletonInstances = this.getSingletonInstances();
        Collections.reverse(beanDefinitions);
        for (var currentBeanName : beanDefinitions) {
            BeanObject bean = this.getBeans().get(currentBeanName);
            Object beanInstance = null;
            if (bean == null) {
                throw new RuntimeException("Почему-то такого бина нет");
            }
            if (bean.getScope().equals(ScopeType.SINGLETON)) {
                beanInstance = singletonInstances.get(currentBeanName);
            } else if (bean.getScope().equals(ScopeType.THREAD)) {
                beanInstance = this.getThreadLocalBean(currentBeanName);
            } else if (bean.getScope().equals(ScopeType.PROTOTYPE)) {
                continue;
            }
            if (beanInstance == null) {
                throw new SomethingBadException(currentBeanName + "Error in shutdownHookService, can't found bean instance with this name.");
            }
            checkForPrototypeBeans(beanInstance);
            invokePreDestroy(beanInstance, bean);

        }
    }


    // CLEANING
    public void checkForPrototypeBeans(Object beanInstance) {
        for (Field field : beanInstance.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);

                Object potentialPrototypeDependency = field.get(beanInstance);

                if (potentialPrototypeDependency instanceof Provider) {
                    potentialPrototypeDependency = ((Provider<?>) potentialPrototypeDependency).get();
                }
                if (potentialPrototypeDependency != null) {
                    BeanObject prototypeBean = this.findPrototypeBean(potentialPrototypeDependency.getClass().getName());
                    if (prototypeBean != null && prototypeBean.getPreDestroyMethod() != null) {
                        invokePreDestroy(potentialPrototypeDependency, prototypeBean);
                    }
                }
            } catch (IllegalAccessException e) {
                throw new SomethingBadException(field.getName() + "Exception with PreDestroy method " +
                        "of the prototype field of the singleton bean");
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
                throw new SomethingBadException(bean.getName() + "Exception with invoking of PreDestroy method");
            }
        }
    }
}