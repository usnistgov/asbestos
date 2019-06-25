package gov.nist.asbestos.mhd.translation.test.documentEntry;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Base64BinaryTest {

    @Test
    void foo() {
        String foobar = "foobar";
        String base64 = Base64.getEncoder().encodeToString(foobar.getBytes());
        assertEquals("Zm9vYmFy", base64);
    }
}
