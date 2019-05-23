package gov.nist.asbestos.http.headers;

import org.junit.jupiter.api.Test;

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

}