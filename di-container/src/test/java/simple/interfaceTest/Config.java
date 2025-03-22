package simple.interfaceTest;

import ru.nsu.annotations.Bean;
import ru.nsu.annotations.Configure;
import ru.nsu.enums.ScopeType;

@Configure
public class Config {

    @Bean(name = "Car", scope = ScopeType.THREAD)
    public Car car() {
        return new Car();
    }

    @Bean(name = "GasEngine", scope = ScopeType.THREAD)
    public Engine gasEngine() {
        return new GasEngine();
    }

    @Bean(name = "ElectricEngine", scope = ScopeType.THREAD)
    public Engine electricEngine() {
        return new ElectricEngine();
    }

}
