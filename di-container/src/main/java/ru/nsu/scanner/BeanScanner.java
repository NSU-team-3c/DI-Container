package ru.nsu.scanner;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import ru.nsu.annotations.Bean;
import ru.nsu.annotations.Configure;
import ru.nsu.bean.BeanObject;
import ru.nsu.bean.BeanDTO;
import ru.nsu.bean.BeanDTOWrapper;
import ru.nsu.enums.ScopeType;
import ru.nsu.exceptions.BadJsonException;
import ru.nsu.exceptions.ClazzException;
import ru.nsu.exceptions.ConstructorException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
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

    private Map<String, BeanObject> nameToBeansMap = new HashMap<>();

    private Map<String, BeanObject> singletonScopes = new HashMap<>();

    private Map<String, BeanObject> threadScopes = new HashMap<>();

    private Map<String, BeanObject> unknownScopes = new HashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private List<BeanDTO> beansFromJson = new ArrayList<>();
    private List<BeanDTO> beansFromAnnotations;

    private Map<String, Class<?>> interfaceBindings = new HashMap<>();

    private void bind(Class<?> interfaceClass, Class<?> implementationClass) {
        interfaceBindings.put(interfaceClass.getName(), implementationClass);
    }

    /**
     * Биндит классы и интерфейсы из-за свойства hashmap 1-1
     *
     * @param allClasses
     */
    private void autobind(Set<Class<?>> allClasses) {
        for (Class<?> clazz : allClasses) {
            if (clazz.isInterface()) continue;
            Class<?>[] interfaces = clazz.getInterfaces();
            for (Class<?> iface : interfaces) {
                bind(iface, clazz);
            }
        }
    }

    public void scanAnnotatedClasses(String scanningDirectory, String jsonConfig) throws IOException {
        Reflections reflections = new Reflections(scanningDirectory,
                new SubTypesScanner(false),
                new TypeAnnotationsScanner());

        if (!Objects.equals(jsonConfig, "")) {
            this.beansFromJson = readBeans(jsonConfig).getBeans();
        }
        this.beansFromAnnotations = parseBeanConfig(reflections.getTypesAnnotatedWith(Configure.class));

        Set<Class<?>> allClasses = reflections.getSubTypesOf(Object.class);

        autobind(allClasses);

        for (Class<?> clazz : allClasses) {
            if (!clazz.isInterface() && isAvailableForInjection(clazz)) {
                BeanObject bean = new BeanObject();
                String namedAnnotationValue = Optional.ofNullable(clazz.getAnnotation(Named.class))
                        .map(Named::value)
                        .orElseThrow(() -> new ClazzException(clazz.getCanonicalName()));

                BeanDTO beanDTO = Optional.ofNullable(findBean(namedAnnotationValue))
                        .orElseThrow(() -> new BadJsonException(namedAnnotationValue, ". No configuration for bean with name."));

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
                bean.setScope(beanDTO.getScope());
                bean.setInjectedFields(injectedFields.isEmpty() ? null : injectedFields);
                bean.setInjectedProviderFields(injectedProviderFields.isEmpty() ? null : injectedProviderFields);
                bean.setConstructor(selectedConstructor);
                bean.setInitParams(beanDTO.getInitParams());
                findConstructMethods(clazz, bean);

                this.nameToBeansMap.put(namedAnnotationValue, bean);
                switch (beanDTO.getScope()) {
                    case PROTOTYPE -> {}
                    case SINGLETON -> singletonScopes.put(namedAnnotationValue, bean);
                    case THREAD -> threadScopes.put(namedAnnotationValue, bean);
                    default -> throw new BadJsonException(namedAnnotationValue, "Unknown bean scope " + bean.getScope());
                }
            }
        }

    }


    private void findConstructMethods(Class<?> clazz, BeanObject bean) {
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

        bean.setPostConstructMethod(postConstructMethod);
        bean.setPreDestroyMethod(preDestroyMethod);
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


    private BeanDTOWrapper readBeans(String jsonConfigPath) throws IOException {
        InputStream jsonInput = this.getClass().getClassLoader().getResourceAsStream(jsonConfigPath);
        return objectMapper.readValue(jsonInput, BeanDTOWrapper.class);
    }

    public List<BeanDTO> parseBeanConfig(Set<Class<?>> configClasses) {

        List<BeanDTO> beans = new ArrayList<>();

        for (Class<?> configClass : configClasses) {
            if (configClass.isAnnotationPresent(Configure.class)) {
                for (Method method : configClass.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(Bean.class)) {
                        Bean beanAnnotation = method.getAnnotation(Bean.class);

                        String beanName = beanAnnotation.name().isEmpty() ? method.getName() : beanAnnotation.name();

                        BeanDTO beanDTO = getBeanDTO(configClass, beanAnnotation, beanName);

                        beans.add(beanDTO);
                    }
                }
            }
        }

        return beans;
    }

    private static BeanDTO getBeanDTO(Class<?> configClass, Bean beanAnnotation, String beanName) {
        ScopeType scope = beanAnnotation.scope();

        // Создаем объект BeanDTO
        BeanDTO beanDTO = new BeanDTO();
        beanDTO.setClassName(configClass.getCanonicalName());
        beanDTO.setName(beanName);
        beanDTO.setScope(scope);
        beanDTO.setInitParams(new HashMap<>());  // Здесь можно добавить логику для инициализации параметров
        beanDTO.setConstructorParams(new ArrayList<>());  // Также добавляем логику для параметров конструктора
        return beanDTO;
    }


    public void scanJsonConfig(String jsonConfigPath) throws ClassNotFoundException, IOException {
        this.beansFromJson = readBeans(jsonConfigPath).getBeans();
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
            ScopeType scope = currentBean.getScope();
            String beanName = currentBean.getName();
            BeanObject bean = new BeanObject();
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
                case SINGLETON -> singletonScopes.put(beanName, bean);
                case PROTOTYPE -> {
                }
                case THREAD -> threadScopes.put(beanName, bean);
                default -> throw new BadJsonException(beanName, "Unknown scope.");
            }
            nameToBeansMap.put(beanName, bean);
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


    private BeanDTO findBean(String namedAnnotationValue) {
        for (BeanDTO currentBean : beansFromJson) {
            if (namedAnnotationValue.equals(currentBean.getName())) {
                return currentBean;
            }
        }

        for (BeanDTO currentBean : beansFromAnnotations) {
            if (namedAnnotationValue.equals(currentBean.getName())) {
                return currentBean;
            }
        }

        return null;
    }
}