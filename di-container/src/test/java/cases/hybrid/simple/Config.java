package cases.hybrid.simple;

import ru.nsu.annotations.Bean;
import ru.nsu.annotations.Configure;
import ru.nsu.enums.ScopeType;

@Configure
public class Config {

    @Bean(name = "userService", scope = ScopeType.THREAD)
    public UserService userService() {
        return new UserService();
    }

}
