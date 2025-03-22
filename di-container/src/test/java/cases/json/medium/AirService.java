package cases.json.medium;

import cases.json.easy.UserRepo;
import lombok.Data;

import javax.inject.Inject;
import javax.inject.Named;

@Data
@Named("airService")
public class AirService {

    @Inject
    private UserRepo userRepo;

    private String property1;

    private Integer property2;

    public AirService(String property1) {
        this.property1 = property1;
    }

    public AirService() {}
}