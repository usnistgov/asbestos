package gov.nist.asbestos.http.operations;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public class HttpDelete  extends HttpBase {
    public HttpDelete run(URI uri)  {
        Objects.requireNonNull(uri);
        HttpURLConnection connection = null;
        try {
            if (!uri.isAbsolute())
                throw new URISyntaxException("URI is not absolute", uri.toString());
            connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setDoOutput(true);
            connection.setRequestProperty(
                    "Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestMethod("DELETE");
            status = connection.getResponseCode();
            try {
                InputStream is = connection.getInputStream();
                setResponseHeadersList(connection.getHeaderFields());
                setResponse(IOUtils.toByteArray(is));
            } catch (Throwable t) {
                // ok - won't always be available
            }
        }
        catch (Exception e) {
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
            throw new RuntimeException(url, e);
        }
    }

    public HttpDelete run()  {
        Objects.requireNonNull(uri);
        run(uri);
        return this;
    }

    public String getVerb() {
        return "DELETE";
    }

}
