package utils;

import ru.nsu.app.Application;
import ru.nsu.context.ContextContainer;
import ru.nsu.scanner.BeanScanner;

import java.io.IOException;

public class TestUtils {
    public static Application initContainer(String scanningDir, String jsonConfig) throws IOException {
        BeanScanner scanner = new BeanScanner();
        scanner.scanAnnotatedClasses(scanningDir, jsonConfig);

        ContextContainer context = new ContextContainer(scanner);
        Application app = new Application(context);
        app.instantiateAndRegisterBeans();

        return app;
    }
}