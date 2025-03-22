package cases.json.medium;

import lombok.Data;

import javax.inject.Named;
import javax.inject.Provider;

@Data
@Named("airController")
public class AirController {

    private Provider<AirService> airService;

    public AirController(Provider<AirService> airService) {
        this.airService = airService;
    }

    public AirController() {}
}