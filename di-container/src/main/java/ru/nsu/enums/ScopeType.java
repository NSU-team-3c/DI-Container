package ru.nsu.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ScopeType {
    @JsonProperty("singleton")
    SINGLETON,
    @JsonProperty("prototype")
    PROTOTYPE,
    @JsonProperty("thread")
    THREAD;
}
