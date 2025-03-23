package usage.configuration;

import org.junit.jupiter.api.Test;
import ru.nsu.context.ContextContainer;
import ru.nsu.scanner.BeanScanner;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class cycleTest {

    private final String packageDir = "cases.configuration.cycle";
    private final String jsonConfig = "";

    @Test
    public void cycleTest() throws IOException {
        BeanScanner scanner = new BeanScanner();
        scanner.scanAnnotatedClasses(packageDir, jsonConfig);

        assertThrows(RuntimeException.class, () -> new ContextContainer(scanner));
    }

}