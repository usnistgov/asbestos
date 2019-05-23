package gov.nist.asbestos.http.headers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HeadersTest {
    Headers headers = null;

    @Test
    void getContentType() {
        System.out.println("Hello");
    }

    @Test
    void getAccept() {
        List<String> expected = Arrays.asList( "application/fhir+xml", "application/fhir+json", "application/xml+fhir", "application/json+fhir" );
        String accepts = headers.getAccept();
        expected.forEach(expect -> {
            assertTrue(accepts.contains(expect), "value " + expect + " missing");
        });
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

    @Test
    void removeHeader() {
    }

    @Test
    void getMultiple() {
    }

    @BeforeEach
    void setUp() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/http/headers/getHeaders1.txt");
        String in = new String(Files.readAllBytes(Paths.get(getClass().getResource("/http/headers/getHeaders1.txt").toURI())));
        headers = HeaderBuilder.parseHeaders(in);
    }
}
