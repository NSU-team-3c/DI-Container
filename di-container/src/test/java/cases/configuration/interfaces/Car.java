package cases.configuration.interfaces;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.inject.Inject;
import javax.inject.Named;

@Data
@NoArgsConstructor
@Named("Car")
public class Car {

    @Inject
    private Engine engine;

    @Inject
    private GasEngine gasEngine;

    @Inject
    private ElectricEngine electricEngine;

    public void drive() {
        engine.start();
        gasEngine.start();
        electricEngine.start();
        System.out.println("Car is driving...");
    }
}