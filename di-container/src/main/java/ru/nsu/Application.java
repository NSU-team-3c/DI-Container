package ru.nsu;

import org.reflections.Reflections;
import org.reflections.scanners.*;

import java.io.IOException;

import java.lang.reflect.*;

import java.util.Set;

public class Application {
    private static ApplicationContext context = new ApplicationContext();
    private static Object createFromFactoryMethod(Method factoryMethod) throws InvocationTargetException, IllegalAccessException, InstantiationException, NoSuchMethodException {
        Object createdObject = null;
        if (Modifier.isStatic(factoryMethod.getModifiers())){
            createdObject = factoryMethod.invoke(null);
        } else{
            Constructor factoryConstructor = factoryMethod.getDeclaringClass().getDeclaredConstructor();
            factoryConstructor.setAccessible(true);
            Object factoryObject = factoryConstructor.newInstance();
            createdObject = factoryMethod.invoke(factoryObject);
        }
        return createdObject;
    }

    private static Object createFromConstructor(Constructor<?> constructor) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        Object[] params = new Object[constructor.getParameterCount()];
        for (int i = 0; i < constructor.getParameterCount(); i++) {
            Class<?> paramType = constructor.getParameterTypes()[i];
            params[i] = context.getBean(paramType);
        }
        return constructor.newInstance(params);
    }

    public static void autoBind(String packageToScan) {
        Reflections reflections = new Reflections(packageToScan, new SubTypesScanner(false));
        Set<Class<?>> allClasses = reflections.getSubTypesOf(Object.class);
        var c = reflections.getAllTypes();

        for (Class<?> clazz : allClasses) {
            if (clazz.isInterface()) continue; // Пропускаем интерфейсы
            Class<?>[] interfaces = clazz.getInterfaces();
            for (Class<?> iface : interfaces) {
                context.bind(iface, clazz); // Автоматическая регистрация
            }
        }
    }

    public static void run (Class clazz) throws IOException {
        Scan scan = (Scan)clazz.getAnnotation(Scan.class);
        String packageToScan = scan.packageToScan();
        autoBind(packageToScan);
        Reflections reflections = new Reflections(packageToScan,
                new MethodAnnotationsScanner(),
                new TypeAnnotationsScanner(),
                new SubTypesScanner(),
                new FieldAnnotationsScanner(),
                new MethodParameterScanner());

        Set<Method> beanMethods = reflections.getMethodsAnnotatedWith(Bean.class);
        for (Method beanMethod:beanMethods){
            try {
                Object createdObject = createFromFactoryMethod(beanMethod);
                Class createdObjectClass = createdObject.getClass();
                Field [] fields = createdObjectClass.getDeclaredFields();
                for(Field field:fields){
                    if(field.isAnnotationPresent(Wired.class)){
                        Class fieldType = field.getType();
                        Set<Method> factoryMethods = reflections.getMethodsReturn(fieldType);
                        boolean hasFactoryMethod = false;
                        for(Method method:factoryMethods){
                            if(method.isAnnotationPresent(Bean.class)){
                                hasFactoryMethod = true;
                                // create object
                                Object object = createFromFactoryMethod(method);
                                field.setAccessible(true);
                                field.set(createdObject, object);
                                context.setBean(method.getReturnType().getCanonicalName(), object);
                                break;
                            }
                        }
                        if(!hasFactoryMethod){
                            Object object  = new BeanFactory().createBean(fieldType);
                            context.setBean(fieldType.getCanonicalName(), object);
                            field.setAccessible(true);
                            field.set(createdObject, object);
                        }
                    }
                }
                context.setBean(beanMethod.getReturnType().getCanonicalName(), createdObject);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        Set<Field> fieldsWired = reflections.getFieldsAnnotatedWith(Wired.class);
        for(Field fieldWired:fieldsWired){
            fieldWired.setAccessible(true);
            Class declaringClass = fieldWired.getDeclaringClass();
            if(context.containsBean(declaringClass.getSuperclass())){
                continue;
            }
            Class[]interfaces = declaringClass.getInterfaces();
            if(interfaces.length > 0 && context.containsBean(interfaces[0])){
                continue;
            }

            Object object = context.getBean(declaringClass);
            Object beanToInject = context.getBean(fieldWired.getType());

            try {
                fieldWired.set(object, beanToInject);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        Set<Constructor> injectConstructors = reflections.getConstructorsAnnotatedWith(Inject.class);
        for (Constructor<?> constructor : injectConstructors) {
            try {
                Object bean = createFromConstructor(constructor);
                context.setBean(bean.getClass().getCanonicalName(), bean);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        System.out.println(context);
    }

    public static ApplicationContext getContext() {
        return context;
    }

    public static void setContext(ApplicationContext context) {
        Application.context = context;
    }

}
