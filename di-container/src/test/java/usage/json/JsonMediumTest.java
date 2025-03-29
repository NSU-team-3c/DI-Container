package usage.json;

import cases.json.medium.AirController;
import cases.json.medium.AirRepo;
import cases.json.medium.AirService;
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
    public void testProvider() throws IOException {
        var app = TestUtils.initContainer(scanningDir, jsonConfig);

        AirController controller = app.getBean("airController");
        AirService service = app.getBean("airService");
        AirRepo repo = app.getBean("airRepo");

        assertNotSame(repo, service.getAirRepo());
        assertSame(service, controller.getAirService().get());
        assertEquals(controller.getAirService().get().getAirRepo().getData(), "some info");
    }

    @Test
    public void mediumPrototypeTest() throws IOException {
        var app = TestUtils.initContainer(scanningDir, jsonConfig);

        var firstInstance = app.getBean("airRepo");
        var secondInstance = app.getBean("airRepo");

        assertNotSame(firstInstance, secondInstance, "must be different");
    }
}