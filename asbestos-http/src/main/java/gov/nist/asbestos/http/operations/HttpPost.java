package gov.nist.asbestos.http.operations;

import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.util.Gzip;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class HttpPost  extends HttpBase {
    private Header locationHeader = null;

    // content is unzipped
    private void post(URI uri, Map<String, String> headers, byte[] content) throws IOException {
        String stringContent = new String(content);
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) uri.toURL().openConnection();
            if (_requestHeaders != null)
                addHeaders(connection, _requestHeaders);
            if (headers != null)
                addHeaders(connection, headers);
            requestHeadersList = connection.getRequestProperties();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            // TODO use proper charset (from input)
            if (content != null) {
                if (isRequestGzipEncoded())
                    connection.getOutputStream().write(Gzip.compressGZIP(content));
                else
                    connection.getOutputStream().write(content);
            }
            status = connection.getResponseCode();
            try {
                setResponseHeadersList(connection.getHeaderFields());
                InputStream is;
                if (status >= 400)
                    is = connection.getErrorStream();
                else
                    is = connection.getInputStream();
                byte[] bytes = IOUtils.toByteArray(is);
                if (isResponseGzipEncoded())
                    setResponse(Gzip.decompressGZIP(bytes));
                else
                    setResponse(bytes);
            } catch (Throwable t) {
                // ok - won't always be available
            }
        } finally {
            if (connection != null)
                connection.disconnect();
        }
        if (getResponseHeaders() != null)
            locationHeader = getResponseHeaders().get("Location");
    }

    public HttpPost post() {
        try {
            post(uri, getRequestHeaders().getAll(), getRequest());
        } catch (IOException e) {
            status = 400;
            String msg = uri + "\n" + e.getMessage() + "\n" + ExceptionUtils.getStackTrace(e);
            setResponseText(msg);
            setResponse(msg.getBytes());
        }
        return this;
    }

    public HttpPost postJson(URI uri, String json) throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "application/json");
        post(uri, headers, json.getBytes());
        return this;
    }

    public HttpPost run() throws IOException {
        Objects.requireNonNull(uri);
        post(uri, getRequestHeaders().getAll(),  getRequest());
        return this;
    }

    public String getVerb() {
        return "POST";
    }

    public Header getLocationHeader() {
        return locationHeader;
    }

    public HttpPost setLocation(String location) {
        this.locationHeader = new Header("Location", location);
        return this;
    }
}
