package simple.interfaceTest;

import lombok.NoArgsConstructor;

import javax.inject.Named;

@NoArgsConstructor
@Named("GasEngine")
public class GasEngine implements Engine {

    @Override
    public void start() {
        System.out.println("Gas Engine is starting...");
    }
}