package cases.json.easy;

import lombok.Data;

import javax.inject.Inject;
import javax.inject.Named;

@Data
@Named("userService")
public class UserService {

    @Inject
    private UserRepo userRepo;

    private String property1;

    private Integer property2;

    public UserService(String property1) {
        this.property1 = property1;
    }

    public UserService() {}
}