package cases.configuration.interfaces;

import lombok.NoArgsConstructor;

import javax.inject.Named;


@NoArgsConstructor
@Named("ElectricEngine")
public class ElectricEngine implements Engine {

    @Override
    public void start() {
        System.out.println("Electric Engine is starting...");
    }
}
