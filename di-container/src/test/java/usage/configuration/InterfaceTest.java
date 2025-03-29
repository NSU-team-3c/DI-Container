package usage.configuration;

import cases.configuration.interfaces.Car;
import org.junit.jupiter.api.Test;
import utils.TestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class InterfaceTest {

    private final String packageDir = "cases.configuration.interfaces";
    private final String jsonConfig = "";

    @Test
    public void interfaceBindingTest() throws IOException {
        var app = TestUtils.initContainer(packageDir, jsonConfig);

        Car car = app.getBean("Car");

        car.drive();
    }

    @Test
    public void checkEngineTest() throws IOException {
        var app = TestUtils.initContainer(packageDir, jsonConfig);

        Car car = app.getBean("Car");
        var engine = car.getEngine();
        var gasEngine = car.getGasEngine();
        var electricEngine = car.getElectricEngine();

        assertTrue(engine.equals(gasEngine) || engine.equals(electricEngine));
    }
}