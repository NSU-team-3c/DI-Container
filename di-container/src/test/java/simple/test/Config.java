package simple.test;

import ru.nsu.annotations.Configure;
import ru.nsu.enums.ScopeType;
import ru.nsu.annotations.Bean;

@Configure
public class Config {

    @Bean(name = "userService", scope = ScopeType.THREAD)
    public UserService userService() {
        return new UserService();
    }

}
