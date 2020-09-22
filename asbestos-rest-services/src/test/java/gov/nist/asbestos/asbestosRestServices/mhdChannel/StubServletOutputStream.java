package gov.nist.asbestos.asbestosRestServices.mhdChannel;

import javax.servlet.ServletOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

class StubServletOutputStream extends ServletOutputStream {
    private ByteArrayOutputStream baos = new ByteArrayOutputStream();
    public void write(int i) {
        baos.write(i);
    }

    public String toString() {
        return baos.toString();
    }
}
