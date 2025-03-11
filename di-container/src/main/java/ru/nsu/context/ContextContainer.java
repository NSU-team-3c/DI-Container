package ru.nsu.context;

import lombok.NonNull;
import ru.nsu.bean.Bean;
import ru.nsu.enums.ScopeType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ContextContainer {

    private Map<String, Bean> beans = new HashMap<>();

    private Map<String, Object> singletonInstances = new HashMap<>();

    private List<String> orderedByDependenciesBeans = new ArrayList<>();

    private Map<String, ThreadLocal<Object>> threadInstances = new HashMap<>();

    private Map<String, Object> customBean = new HashMap<>();

    //private DependencyScanningConfig dependencyScanningConfig;


    public ContextContainer() {
//        this.dependencyScanningConfig = dependencyScanningConfig;
//        this.beanDefinitions = dependencyScanningConfig.getNameToBeanDefinitionMap();
//        DependencyResolver resolver = new DependencyResolver(beanDefinitions);
//
//        this.orderedByDependenciesBeans = resolver.resolveDependencies();
//
//
//        new ShutdownHookService(this);
    }

    public Bean findPrototypeBean(String beanName) {
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

    public void registerSingletonBeanInstance(@NonNull Bean bean, Object beanInstance) {
        singletonInstances.put((bean.getName() != null ? bean.getName() : bean.getClassName()), beanInstance);
    }


    public void registerThreadBeanInstance(@NonNull Bean bean, Supplier<?> beanSupplier) {
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
}