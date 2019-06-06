package gov.nist.asbestos.http.operations;

import java.io.IOException;

public class HttpAny extends HttpBase {
    @Override
    public HttpBase run() throws IOException {
        throw new RuntimeException("HttpAny is not runnable");
    }
}
