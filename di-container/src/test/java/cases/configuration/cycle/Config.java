package cases.configuration.cycle;

import ru.nsu.annotations.Bean;
import ru.nsu.annotations.Configure;
import ru.nsu.enums.ScopeType;

@Configure
public class Config {

    @Bean(name = "cycleRepo", scope = ScopeType.SINGLETON)
    public CycleRepo repo() {
        return new CycleRepo();
    }

    @Bean(name = "cycleService", scope = ScopeType.SINGLETON)
    public CycleService service() {
        return new CycleService(repo());
    }
}
