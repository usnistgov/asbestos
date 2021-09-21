package gov.nist.asbestos.http.operations;


import java.io.IOException;
import java.net.URI;

public class HttpPost extends HttpMethod {

    public HttpPost() {
        super("POST");
    }

    public HttpPost post() {
        submit();
        return this;
    }

    public HttpPost postJson(URI uri, String json) throws IOException {
        submit(uri, json);
        return this;
    }

    public HttpPost run() throws IOException {
        super.run();
        return this;
    }

}
