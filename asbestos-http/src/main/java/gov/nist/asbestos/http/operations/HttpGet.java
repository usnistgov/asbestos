package gov.nist.asbestos.http.operations;


import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class HttpGet extends HttpBase {
    // TODO GET parameters in the body
    void get(URI uri, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            if (headers != null)
                addHeaders(connection, headers);
            requestHeadersList = connection.getRequestProperties();
            status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                setResponseHeadersList(connection.getHeaderFields());
            }
            try {
                setResponse(IOUtils.toByteArray(connection.getInputStream()));
            } catch (Throwable t) {
            }
        } finally {
            if (connection != null)
                connection.disconnect();
        }
    }

    public void get(String url) throws URISyntaxException, IOException {
        get(new URI(url), (Map<String, String>) null);
    }

    public HttpGet get(URI uri, String contentType) throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", contentType);
        headers.put("accept-charset", "utf-8");
        get(uri, headers);
        if (getResponse() != null)
            setResponseText(new String(getResponse()));
        return this;
    }

    public HttpGet getJson(String url) throws URISyntaxException, IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        headers.put("accept-charset", "utf-8");
        get(new URI(url), headers);
        if (getResponse() != null)
            setResponseText(new String(getResponse()));
        return this;
    }

    public HttpGet getJson(URI uri) throws IOException {
//        Map<String, String> headers = new HashMap<>();
//        headers.put("accept", "application/json");
//        headers.put("accept-charset", "utf-8");
        get(uri, "application/json");
//        setResponseText(new String(getResponse()));
        return this;
    }

    public HttpGet run() throws IOException {
        Objects.requireNonNull(uri);
        get(uri, getRequestHeaders().getAll());
        return this;
    }

}
