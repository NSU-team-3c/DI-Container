import org.junit.jupiter.api.Test;
import ru.nsu.app.Application;
import ru.nsu.bean.Bean;
import ru.nsu.context.ContextContainer;
import ru.nsu.scanner.BeanScanner;
import simple.test.UserService;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertSame;

public class SimpleTest {

    @Test
    public void singletonTest() throws IOException {
        BeanScanner scanner = new BeanScanner();
        scanner.scanAnnotatedClasses("simple.test", "beans.json");

        ContextContainer context = new ContextContainer(scanner);
        Application app = new Application(context);
        app.instantiateAndRegisterBeans();

        UserService firstInstance = app.getBean("userService");
        UserService secondInstance = app.getBean("userService");

        assertSame(firstInstance, secondInstance, "Это синглетон бины и они должны быть всегда одинаковые");
    }
}