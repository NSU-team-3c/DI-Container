package cases.json.easy;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.inject.Named;

@Data
@Named("userRepo")
@NoArgsConstructor
public class UserRepo {
    private String data;
}