package gov.nist.asbestos.http.headers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// TODO test response
class HeadersTest {
    private Headers requestHeaders = null;

    @Test
    void getContentType() {
        System.out.println("Hello");
    }

    @Test
    void getAccept() {
        List<String> expected = Arrays.asList( "application/fhir+xml", "application/fhir+json", "application/xml+fhir", "application/json+fhir" );
        String accepts = requestHeaders.getAccept().getAllValuesAndParmsAsString();
        expected.forEach(expect -> {
            assertTrue(accepts.contains(expect), "value " + expect + " missing");
        });
    }

    @Test
    void getNames() {
        List<String> names = Arrays.asList("user-agent", "accept-charset", "accept-encoding", "accept", "host", "connection");
        List<String> returns = requestHeaders.getNames();
        names.forEach(returns::contains);
    }

    @Test
    void firstLine() {
        assertEquals("GET", requestHeaders.verb);
        assertEquals("/default__patient1/metadata", requestHeaders.pathInfo.toString());
    }

    @Test
    void getAccepts() {

    }

    @Test
    void getStatus() {

    }

    @Test
    void getContentEncoding() {
    }

    @Test
    void getAll() {
    }

    @Test
    void getAll1() {
    }

    @BeforeEach
    void setUp() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/http/headers/getRequestHeaders1.txt");
        String in = new String(Files.readAllBytes(Paths.get(getClass().getResource("/http/headers/getRequestHeaders1.txt").toURI())));
        requestHeaders = new Headers(in);
    }
}
