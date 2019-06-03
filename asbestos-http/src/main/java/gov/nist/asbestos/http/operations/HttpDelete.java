package gov.nist.asbestos.http.operations;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public class HttpDelete  extends HttpBase {
    public HttpDelete run(URI uri)  {
        Objects.requireNonNull(uri);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setDoOutput(true);
            connection.setRequestProperty(
                    "Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestMethod("DELETE");
            status = connection.getResponseCode();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (connection != null)
                connection.disconnect();
        }
        return this;
    }

    public HttpDelete run(String url) {
        try {
            run(new URI(url));
            return this;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public HttpDelete run()  {
        Objects.requireNonNull(uri);
        run(uri);
        return this;
    }
}
