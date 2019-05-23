package gov.nist.asbestos.http.headers;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HeaderTest {

    @Test
    void single() {
        String hdr = "content-type: text/plain";
        assertEquals(hdr, new Header(hdr).toString());
    }

    @Test
    void multiple() {
        String hdr = "content-type: text/plain, text/pdf";
        assertEquals(hdr, new Header(hdr).toString());
    }

    @Test
    void singleWithParam() {
        String hdr = "content-type: text/plain;q=1.0";
        assertEquals(hdr, new Header(hdr).toString());
    }

    @Test
    void multipleWithParam() {
        String hdr = "content-type: text/plain;q=1.0, text/pdf;q=0.5";
        assertEquals(hdr, new Header(hdr).toString());
    }

    @Test
    void nameAndSeparateValue() {
        String name = "content-type";
        String value = "text/plain;q=1.0, text/pdf;q=0.5";
        assertEquals(String.join(":", name, value), new Header(name, value).toString());
    }

    @Test
    void nameAndSeparateValueList() {
        String name = "content-type";
        List<String> values = Arrays.asList("text/plain;q=1.0","text/pdf;q=0.5");
        assertEquals(String.join(":", name, String.join(",", values)), new Header(name, values).toString());
    }

    @Test
    void getAllValuesAndParms() {
        String hdr = "content-type: text/plain;q=1.0, text/pdf;q=0.5";
        assertEquals(" text/plain;q=1.0, text/pdf;q=0.5", new Header(hdr).getAllValuesAndParmsAsString());
    }

    @Test
    void getAllValuesAsString() {
        String hdr = "content-type: text/plain;q=1.0, text/pdf;q=0.5";
        assertEquals(" text/plain, text/pdf", new Header(hdr).getAllValuesAsString());
    }
}
