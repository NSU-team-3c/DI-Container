package usage.json;

import cases.json.easy.UserRepo;
import cases.json.easy.UserService;
import org.junit.jupiter.api.Test;
import utils.TestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class JsonEasyTest {

    private final String jsonConfig = "beansEasy.json";
    private final String scanningDir = "cases.json.easy";

    @Test
    public void easySingletonTest() throws IOException {
        var app = TestUtils.initContainer(scanningDir, jsonConfig);

        UserService firstInstance = app.getBean("userService");
        UserService secondInstance = app.getBean("userService");

        assertSame(firstInstance, secondInstance, "must be same");
    }

    @Test
    public void easyPrototypeTest() throws IOException {
        var app = TestUtils.initContainer(scanningDir, jsonConfig);

        UserRepo firstInstance = app.getBean("userRepo");
        UserRepo secondInstance = app.getBean("userRepo");

        assertNotSame(firstInstance, secondInstance, "must be different");
    }
    @Test
    public void easySettersTest() throws IOException {
        var app = TestUtils.initContainer(scanningDir, jsonConfig);

        UserService service = app.getBean("userService");
        UserRepo repo = app.getBean("userRepo");

        assertEquals(service.getProperty1(), "hello");
        assertEquals(service.getProperty2(), 5);
        assertEquals(repo.getData(), "some info");
    }

}