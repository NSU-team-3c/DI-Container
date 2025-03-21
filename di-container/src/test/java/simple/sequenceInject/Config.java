package simple.sequenceInject;

import ru.nsu.annotations.Bean;
import ru.nsu.annotations.Configure;
import ru.nsu.enums.ScopeType;


@Configure
public class Config {

    @Bean(name = "printer", scope = ScopeType.PROTOTYPE)
    public Printer printer() {
        return new Printer();
    }

    @Bean(name="file", scope = ScopeType.SINGLETON)
    public File file() {
        return new File();
    }

    @Bean(name="defaultFile", scope =  ScopeType.SINGLETON)
    public File defaultFile() {
        var file = new File();

        file.setFile("default");

        return file;
    }

    @Bean(name="reader", scope = ScopeType.SINGLETON)
    public Reader reader() {
        return new Reader();
    }
}
