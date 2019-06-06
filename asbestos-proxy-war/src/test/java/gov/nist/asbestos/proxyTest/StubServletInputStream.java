package gov.nist.asbestos.proxyTest;

import javax.servlet.ServletInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class StubServletInputStream extends ServletInputStream {
    private ByteArrayInputStream byteArrayInputStream;

    StubServletInputStream(String inputString) {
        byteArrayInputStream = new ByteArrayInputStream(inputString.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public int read() throws IOException {
        return byteArrayInputStream.read();
    }
}
