package ru.nsu.scanner;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import ru.nsu.annotations.Inject;
import ru.nsu.annotations.Named;
import ru.nsu.bean.Bean;
import ru.nsu.bean.BeanDTO;
import ru.nsu.bean.BeanDTOWrapper;
import ru.nsu.exceptions.BadJsonException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
public class BeanScanner {

    private Map<String, Bean> nameToBeanDefinitionMap = new HashMap<>();

    private Map<String, Bean> singletonScopes = new HashMap<>();

    private Map<String, Bean> threadScopes = new HashMap<>();

    private Map<String, Bean> unknownScopes = new HashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private List<BeanDTO> beansFromJson;

    public void scanAnnotatedClasses(String scanningDirectory, String jsonConfig) throws IOException {
        Reflections reflections = new Reflections(scanningDirectory,
                new SubTypesScanner(false),
                new TypeAnnotationsScanner());


        this.beansFromJson = readBeanDefinitions(jsonConfig).getBeans();

        Set<Class<?>> allClasses = reflections.getSubTypesOf(Object.class);

        for (Class<?> clazz : allClasses) {
            if (!clazz.isInterface() && isAvailableForInjection(clazz)) {
                Bean bean = new Bean();
                String namedAnnotationValue = Optional.ofNullable(clazz.getAnnotation(Named.class))
                        .map(Named::value)
                        .orElseThrow(() -> new ClazzException(clazz.getCanonicalName()));

                BeanDTO beanDefinitionReader = Optional.ofNullable(findBeanInJson(namedAnnotationValue))
                        .orElseThrow(() -> new WrongJsonException(namedAnnotationValue, ". No configuration for bean with name."));

                List<Field> injectedFields = new ArrayList<>();
                List<Field> injectedProviderFields = new ArrayList<>();
                analyzeClassFields(clazz, injectedFields, injectedProviderFields);

                Constructor<?> selectedConstructor = null;
                boolean isConstructorFound = false;

                for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                    if (constructor.isAnnotationPresent(Inject.class)) {
                        selectedConstructor = constructor;
                        isConstructorFound = true;
                        break;
                    }
                }

                if (!isConstructorFound) {
                    try {
                        selectedConstructor = clazz.getDeclaredConstructor();
                    } catch (NoSuchMethodException e) {
                        throw new ConstructorException(clazz.getCanonicalName(), "No constructor at all");
                    }
                }

                if ((!injectedFields.isEmpty() && isConstructorFound) || (!injectedProviderFields.isEmpty() && isConstructorFound)) {
                    throw new ConstructorException(clazz.getCanonicalName(), "Only one type of injection is available: fields or constructors");
                }
                bean.setClassName(clazz.getCanonicalName());
                bean.setName(namedAnnotationValue);
                bean.setScope(beanDefinitionReader.getScope());
                bean.setInjectedFields(injectedFields.isEmpty() ? null : injectedFields);
                bean.setInjectedProviderFields(injectedProviderFields.isEmpty() ? null : injectedProviderFields);
                bean.setConstructor(selectedConstructor);
                bean.setInitParams(beanDefinitionReader.getInitParams());
                findConstructMethods(clazz, bean);

                this.nameToBeanDefinitionMap.put(namedAnnotationValue, bean);
                switch (beanDefinitionReader.getScope()) {
                    case "prototype" -> {
                    }
                    case "singleton" -> singletonScopes.put(namedAnnotationValue, bean);
                    case "thread" -> threadScopes.put(namedAnnotationValue, bean);
                    default -> throw new BadJsonException(namedAnnotationValue, "Unknown bean scope " + bean.getScope());
                }
            }
        }
    }


    private void findConstructMethods(Class<?> clazz, BeanDefinition beanDefinition) {
        Method postConstructMethod = null;
        Method preDestroyMethod = null;

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(PostConstruct.class) && method.getParameterCount() == 0) {
                if (postConstructMethod == null) {
                    postConstructMethod = method;
                } else {
                    throw new IllegalStateException("@PostConstruct annotation found on multiple methods in " + clazz.getName());
                }
            }

            if (method.isAnnotationPresent(PreDestroy.class) && method.getParameterCount() == 0) {
                if (preDestroyMethod == null) {
                    preDestroyMethod = method;
                } else {
                    throw new IllegalStateException("@PreDestroy annotation found on multiple methods in " + clazz.getName());
                }
            }
        }

        beanDefinition.setPostConstructMethod(postConstructMethod);
        beanDefinition.setPreDestroyMethod(preDestroyMethod);
    }

    private void analyzeClassFields(Class<?> clazz, List<Field> injectedFields, List<Field> injectedProviderFields) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Inject.class)) {
                if (Provider.class.isAssignableFrom(field.getType())) {
                    injectedProviderFields.add(field);
                } else {
                    injectedFields.add(field);
                }
            }
        }
    }


    private BeanDTOWrapper readBeanDefinitions(String jsonConfigPath) throws IOException {
        String fullPath = "beans/" + jsonConfigPath;
        InputStream jsonInput = this.getClass().getClassLoader().getResourceAsStream(fullPath);
        return objectMapper.readValue(jsonInput, BeanDTOWrapper.class);
    }


    public void scanJsonConfig(String jsonConfigPath) throws ClassNotFoundException, IOException {
        this.beansFromJson = readBeanDefinitions(jsonConfigPath).getBeans();
        for (BeanDTO currentBean : beansFromJson) {
            if (currentBean.getName() == null) {
                throw new BadJsonException("unknown", "No name field for this json");
            }
            if (currentBean.getScope() == null) {
                throw new BadJsonException(currentBean.getName(), "No scope field for this bean in json");
            }
            if (currentBean.getClassName() == null) {
                throw new BadJsonException(currentBean.getName(), "No className field for this bean in json");
            }
            String scope = currentBean.getScope();
            String beanName = currentBean.getName();
            Bean bean = new Bean();
            bean.setScope(scope);
            bean.setName(beanName);
            bean.setClassName(currentBean.getClassName());
            bean.setInitParams(currentBean.getInitParams());

            if (currentBean.getConstructorParams() != null && !currentBean.getConstructorParams().isEmpty()) {
                List<String> paramTypeNames = currentBean.getConstructorParams().stream()
                        .map(Object::toString)
                        .collect(Collectors.toList());
                Constructor<?> constructor = findAndSetConstructor(currentBean.getClassName(), paramTypeNames);
                bean.setConstructor(constructor);
            }
            switch (scope) {
                case "singleton" -> singletonScopes.put(beanName, bean);
                case "prototype" -> {
                }
                case "thread" -> threadScopes.put(beanName, bean);
                default -> throw new BadJsonException(beanName, "Unknown scope.");
            }
            nameToBeanDefinitionMap.put(beanName, bean);
        }
    }


    private Constructor<?> findAndSetConstructor(String className, List<String> paramTypeNames) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(className);
        List<Class<?>> paramClasses = new ArrayList<>();

        for (String paramName : paramTypeNames) {
            if (paramName.startsWith("Provider<") && paramName.endsWith(">")) {
                paramClasses.add(javax.inject.Provider.class);
            } else {
                paramClasses.add(Class.forName(paramName));
            }
        }

        Class<?>[] paramTypes = paramClasses.toArray(new Class[0]);
        for (Constructor<?> constructor : clazz.getConstructors()) {
            if (Arrays.equals(constructor.getParameterTypes(), paramTypes)) {
                return constructor;
            }
        }

        throw new ConstructorException(className, "Can't make suitable construct for json config.");
    }

    private boolean isAvailableForInjection(Class<?> clazz) {
        if (clazz.isAnnotationPresent(Named.class)) {
            return true;
        }

        return (Stream.of(clazz.getDeclaredFields(), clazz.getDeclaredConstructors(), clazz.getDeclaredMethods())
                .flatMap(Arrays::stream)
                .anyMatch(member -> member.isAnnotationPresent(Inject.class) || member.isAnnotationPresent(Named.class)));
    }


    private BeanDTO findBeanInJson(String namedAnnotationValue) {
        for (BeanDTO currentBeanJson : beansFromJson) {
            if (namedAnnotationValue.equals(currentBeanJson.getName())) {
                return currentBeanJson;
            }
        }
        return null;
    }
}