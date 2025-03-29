package usage.hybrid;

import cases.hybrid.simple.UserService;
import org.junit.jupiter.api.Test;
import utils.TestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertSame;

public class SimpleTest {

    private final String packageDir = "cases.hybrid.simple";
    private final String jsonConfig = "beans.json";

    @Test
    public void singletonJsonTest() throws IOException {
        var app = TestUtils.initContainer(packageDir, jsonConfig);

        UserService firstInstance = app.getBean("userService");
        UserService secondInstance = app.getBean("userService");

        assertSame(firstInstance, secondInstance, "must be same");
    }
}