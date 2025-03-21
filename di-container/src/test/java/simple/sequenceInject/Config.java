package simple.sequenceInject;

import ru.nsu.annotations.Bean;
import ru.nsu.annotations.Configure;
import ru.nsu.enums.ScopeType;


@Configure
public class Config {

    @Bean(name = "printer", scope = ScopeType.THREAD)
    public Printer printer() {
        return new Printer();
    }

    @Bean(name="file", scope = ScopeType.THREAD)
    public File file() {
        return new File();
    }

    @Bean(name="reader", scope = ScopeType.THREAD)
    public Reader reader() {
        return new Reader();
    }
}
