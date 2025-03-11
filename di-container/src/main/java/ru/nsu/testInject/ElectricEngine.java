package ru.nsu.testInject;

public class ElectricEngine implements Engine {

    public ElectricEngine() {}

    @Override
    public void start() {
        System.out.println("Electric Engine is starting...");
    }
}
