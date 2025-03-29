package cases.json.medium;

import lombok.Data;

import javax.inject.Inject;
import javax.inject.Named;

@Data
@Named("airService")
public class AirService {

    @Inject
    private AirRepo airRepo;

    private String property1;

    private Integer property2;

    public AirService(String property1) {
        this.property1 = property1;
    }

    public AirService() {
    }
}