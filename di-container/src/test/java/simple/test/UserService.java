package simple.test;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.inject.Inject;
import javax.inject.Named;

@Data
@NoArgsConstructor
@Named("userService")
public class UserService {

    @Inject
    private UserRepository userRepositoryField;

    public void getUserInfo() {
        System.out.println("User Info: " + userRepositoryField.getUserName());
    }

}
