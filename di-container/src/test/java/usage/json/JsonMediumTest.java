package usage.json;

import org.junit.jupiter.api.Test;
import utils.TestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class JsonMediumTest {

    private final String jsonConfig = "beansMedium.json";
    private final String scanningDir = "cases.json.medium";

    @Test
    public void mediumSingletonTest() throws IOException {
        var app = TestUtils.initContainer(scanningDir, jsonConfig);

        var firstInstanceService = app.getBean("airService");
        var secondInstanceService = app.getBean("airService");

        var firstInstanceController = app.getBean("airController");
        var secondInstanceController = app.getBean("airController");

        assertSame(firstInstanceService, secondInstanceService, "must be same");
        assertSame(firstInstanceController, secondInstanceController, "must be same");
    }

    @Test
    public void mediumPrototypeTest() throws IOException {
        var app = TestUtils.initContainer(scanningDir, jsonConfig);

        var firstInstance = app.getBean("airRepo");
        var secondInstance = app.getBean("airRepo");

        assertNotSame(firstInstance, secondInstance, "must be different");
    }
}