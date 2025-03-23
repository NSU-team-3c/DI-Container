package cases.hybrid.simple;

import lombok.Data;

import javax.inject.Named;
import java.util.Random;

@Data
@Named("userRepo")
public class UserRepository {

    private final String userName;

    public UserRepository() {
        Random random = new Random();
        this.userName = "User" + random.nextInt(1000);
    }
}


