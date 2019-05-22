package gov.nist.asbestos.http.headers;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HeadersTests {


    @Test
    private void testfileReadTest() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/http/headers/getHeaders1.txt");
        String in = new String(Files.readAllBytes(Paths.get(getClass().getResource("/http/headers/getHeaders1.txt").toURI())));
        assertEquals("foo", in);
    }

}
