package usage.configuration;

import cases.configuration.sequences.Printer;
import org.junit.jupiter.api.Test;
import utils.TestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNull;

public class SequenceTest {

    private final String packageDir = "cases.configuration.sequences";
    private final String jsonConfig = "";

    @Test
    public void testSequenceInject() throws IOException {
        var app = TestUtils.initContainer(packageDir, jsonConfig);

        Printer printer = app.getBean("printer");
        printer.print();

        assertNull(printer.getFileInfo());
    }
}