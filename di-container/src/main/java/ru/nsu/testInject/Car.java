package ru.nsu.testInject;

import ru.nsu.Inject;

public class Car {
    private final Engine engine;

    @Inject
    public Car(Engine engine) {
        this.engine = engine;
    }

    public void drive() {
        engine.start();
        System.out.println("Car is driving...");
    }
}
