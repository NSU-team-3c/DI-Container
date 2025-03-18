package ru.nsu.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.nsu.enums.ScopeType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BeanObject {
    private String className;
    private String name;
    private ScopeType scope;
    private List<Field> injectedFields;
    private List<Field> injectedProviderFields;
    private Constructor<?> constructor;
    private Map<String, Object> initParams;
    private Method postConstructMethod;
    private Method preDestroyMethod;
}