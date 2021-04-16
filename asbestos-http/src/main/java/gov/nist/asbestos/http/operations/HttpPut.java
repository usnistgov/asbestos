package gov.nist.asbestos.http.operations;


import java.io.IOException;
import java.net.URI;

public class HttpPut extends HttpMethod {

    public HttpPut() {
        super("PUT");
    }

    public HttpPut put() {
        submit();
        return this;
    }

    public HttpPut putJson(URI uri, String json) throws IOException {
        submit(uri, json);
        return this;
    }

    public HttpPut run() throws IOException {
        super.run();
        return this;
    }

}
