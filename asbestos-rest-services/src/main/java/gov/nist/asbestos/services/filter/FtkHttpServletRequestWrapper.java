package gov.nist.asbestos.services.filter;

import org.apache.commons.io.IOUtils;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;

class FtkHttpServletRequestWrapper extends HttpServletRequestWrapper {

    private byte[] body;

    public FtkHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
        try {
            body = IOUtils.toByteArray(request.getInputStream());
        } catch (IOException ex) {
            body = new byte[0];
        }
    }

    @Override
    public ServletInputStream getInputStream() {
        return new ServletInputStream() {
            ByteArrayInputStream bais = new ByteArrayInputStream(body);

            @Override
            public int read()  {
                return bais.read();
            }

            @Override
            public long skip(long n) throws IOException {
                return bais.skip(n);
            }

            @Override
            public int available() throws IOException {
                return bais.available();
            }

            @Override
            public void close() throws IOException {
                bais.close();
            }

            @Override
            public synchronized void mark(int readlimit) {
                bais.mark(readlimit);
            }

            @Override
            public boolean markSupported() {
                return bais.markSupported();
            }

            @Override
            public synchronized void reset() throws IOException {
                bais.reset();
            }
        };
    }


}
