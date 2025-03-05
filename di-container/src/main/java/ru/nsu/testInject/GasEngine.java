package ru.nsu.testInject;


public class GasEngine implements Engine {

    public GasEngine() {}

    @Override
    public void start() {
        System.out.println("Gas Engine is starting...");
    }
}

