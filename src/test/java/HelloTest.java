import com.amazonaws.services.lambda.runtime.Context;
import example.Hello;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class HelloTest {

    @Test
    void testHandleRequest(){
        Hello hello = new Hello();

        Context context = null;

        Map<String, Object> response = hello.handleRequest(null, context);

        assertNotNull(response);
        assertEquals(200, response.get("statusCode"));

        Map<String, String> headers = (Map<String, String>) response.get("headers");

        assertNotNull(headers);
        assertEquals("application/json", headers.get("Content-Type"));

        assertEquals("Hello World!", response.get("body"));

    }
}