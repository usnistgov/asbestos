package gov.nist.asbestos.proxyWar;

import gov.nist.asbestos.http.operations.CustomUriBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class HttpGetTestxx {
    String url1 = "http://localhost:8081/asbestos/proxy/default__limited/DocumentManifest/1.2.10.242.128.49.2020.09.02.14.30.50.502.2";
    String url2 = "http://localhost:8080/fhir/fhir/Patient?identifier=urn:oid:1.3.6.1.4.1.21367.13.20.1000|TEST-1000-1";
    String url3 = "http://localhost:8080/fhir/fhir/Patient?identifier=urn:oid:1.3.6.1.4.1.21367.13.20.1000|TEST-1000-1#foo";
    String url4 = "http://localhost:8080/fhir/fhir/Patient#foo";
    String url5 = "/foo/bar";
    String url6 = "foo/bar";
    String url7 = "";

    @Test
    void get1() throws URISyntaxException, IOException {
        doget(url1);
    }

//    @Test
//    void get2() throws IOException, URISyntaxException {
//        doget(url2);
//    }

    @Test
    void get3() throws URISyntaxException, IOException {
        String scheme = "http";
        String authority = "localhost:8081";
        String path = "/asbestos/proxy/default__limited/DocumentManifest/1.2.10.242.128.49.2020.09.02.14.30.50.502.2";
        String query = null;
        String fragment = null;
        URI uri = new URI(scheme, authority, path, query, fragment);
        System.out.println(uri.toString());
        doget(uri);
    }

    @Test
    void get4() throws URISyntaxException, IOException {
        String scheme = "http";
        String authority = "localhost:8080";
        String path = "/fhir/fhir/Patient";
        String query = "identifier=urn:oid:1.3.6.1.4.1.21367.13.20.1000|TEST-1000-1";
        String fragment = null;
        URI uri = new URI(scheme, authority, path, query, fragment);
        doget(uri);
    }

    @Test
    void get5() throws URISyntaxException, IOException {
        CustomUriBuilder builder = new CustomUriBuilder(url1);
        assertEquals(url1, builder.build().toString());
        doget(builder.build());
    }

    @Test
    void get6() throws URISyntaxException, IOException {
        CustomUriBuilder builder = new CustomUriBuilder(url2);
        assertEquals(url2, URLDecoder.decode(builder.build().toString(), "UTF-8"));
        doget(builder.build());
    }

    @Test
    void parse1() throws URISyntaxException {
        CustomUriBuilder builder = new CustomUriBuilder(url3);
        assertEquals("http", builder.getScheme());
        assertEquals("localhost:8080", builder.getAuthority());
        assertEquals("/fhir/fhir/Patient", builder.getPath());
        assertEquals("identifier=urn:oid:1.3.6.1.4.1.21367.13.20.1000|TEST-1000-1", builder.getQuery());
        assertEquals("foo", builder.getFragment());
    }

    @Test
    void parse2() throws URISyntaxException {
        CustomUriBuilder builder = new CustomUriBuilder(url4);
        assertEquals("http", builder.getScheme());
        assertEquals("localhost:8080", builder.getAuthority());
        assertEquals("/fhir/fhir/Patient", builder.getPath());
        assertNull(builder.getQuery());
        assertEquals("foo", builder.getFragment());
    }

    @Test
    void parse3() throws URISyntaxException {
        CustomUriBuilder builder = new CustomUriBuilder(url5);
        assertEquals(url5, builder.build().toString());
    }

    @Test
    void parse4() throws URISyntaxException {
        CustomUriBuilder builder = new CustomUriBuilder(url6);
        assertEquals(url6, builder.build().toString());
    }

    @Test
    void parse5() throws URISyntaxException {
        CustomUriBuilder builder = new CustomUriBuilder(url7);
        assertEquals(url7, builder.build().toString());
    }

    void doget(String theUrl) throws URISyntaxException, IOException {
        URI uri = new URI(theUrl);
        doget(uri);
    }

    void doget(URI uri) throws IOException {
        System.out.println(uri.toString());
        URL url = uri.toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        int status = connection.getResponseCode();
        assertEquals(200, status);
    }

}
