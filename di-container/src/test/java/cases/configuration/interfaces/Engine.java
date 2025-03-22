package cases.configuration.interfaces;

import javax.inject.Named;

@Named("Engine")
public interface Engine {
    void start();
}