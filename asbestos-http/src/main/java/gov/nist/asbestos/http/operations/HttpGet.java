package gov.nist.asbestos.http.operations;


import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class HttpGet extends HttpBase {
    // TODO GET parameters in the body
    void get(URI uri, Map<String, String> headers) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            if (headers != null)
                addHeaders(connection, headers);
            requestHeadersList = connection.getRequestProperties();
            status = connection.getResponseCode();
//            if (status == HttpURLConnection.HTTP_OK || status == HttpURLConnection.HTTP_CREATED) {
//                setResponseHeadersList(connection.getHeaderFields());
//                setResponse(IOUtils.toByteArray(connection.getInputStream()));
//            }
            try {
                InputStream is = connection.getInputStream();
                setResponseHeadersList(connection.getHeaderFields());
                setResponse(IOUtils.toByteArray(is));
            } catch (Throwable t) {
                    // ok - won't always be available
            }
        } catch (Throwable t) {
            throw new Error("GET " + uri, t);
        } finally {
            if (connection != null)
                connection.disconnect();
        }
    }

    public void get(String url) throws URISyntaxException {
        get(new URI(url), (Map<String, String>) null);
    }

    public void get() {
        try {
            get(getUri(), getRequestHeaders().getAll());
        } catch (Throwable e) {
            status = 404;
            String msg = e.getMessage() + "\n" + ExceptionUtils.getStackTrace(e);
            setResponseText(msg);
            setResponse(msg.getBytes());
        }
    }

    public HttpGet get(URI uri, String contentType)  {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", contentType);
        headers.put("accept-charset", "utf-8");
        get(uri, headers);
        if (getResponse() != null) {
            setResponseText(new String(getResponse()));
        }
        return this;
    }

    public HttpGet getJson(String url) throws URISyntaxException {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        headers.put("accept-charset", "utf-8");
        get(new URI(url), headers);
        if (getResponse() != null)
            setResponseText(new String(getResponse()));
        return this;
    }

    public HttpGet getJson(URI uri)  {
        get(uri, "application/json");
        return this;
    }

    public HttpGet run()  {
        Objects.requireNonNull(uri);
        get(uri, getRequestHeaders().getAll());
        return this;
    }

    public String getVerb() {
        return "GET";
    }

}
