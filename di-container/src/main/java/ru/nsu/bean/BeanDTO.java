package ru.nsu.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import ru.nsu.enums.ScopeType;

import java.util.List;
import java.util.Map;

@Data
public class BeanDTO {
    @JsonProperty("class")
    private String className;

    @JsonProperty("name")
    private String name;

    @JsonProperty("scope")
    private ScopeType scope;

    @JsonProperty("initParams")
    private Map<String, Object> initParams;

    @JsonProperty("constructorParams")
    private List<Object> constructorParams;
}