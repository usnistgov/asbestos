package gov.nist.asbestos.http.operations;


import gov.nist.asbestos.http.util.Gzip;
import org.apache.commons.io.IOUtils;
import java.util.logging.Level;

import java.io.InputStream;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public class HttpGetter extends HttpBase {
    private static Logger logger = Logger.getLogger(HttpGetter.class.getName());
    public static String GET_VERB = "GET";

    // TODO GET parameters in the body
    private void get(URI theUri, Map<String, String> headers) {
        this.uri = theUri;
        URL url = null;
        try {
            url = theUri.toURL();
        } catch (Exception e) {
            String message = "If this is a GET request, then it might not have a resource associated with its body. ";
            if (url != null) {
                if (url.toString().isEmpty()) {
                    message += "URI is an empty string.";
                } else {
                    message = "";
                }
            } else {
                message += "URI is null.";
            }
            throw new RuntimeException("Cannot decode URI " + theUri + " into a URL. " + message + " Exception: " + e.toString());
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            if (_requestHeaders != null)
                addHeaders(connection, _requestHeaders);
            if (headers != null)
                addHeaders(connection, headers);
            requestHeadersList = connection.getRequestProperties();
            status = connection.getResponseCode();
//            try {
            InputStream is = status < 400 ? connection.getInputStream() : connection.getErrorStream();
            setResponseHeadersList(connection.getHeaderFields());
            byte[] bytes = null;
            if (status >= 400) {
                try {
                    bytes = IOUtils.toByteArray(is);
                } catch (Throwable t) {
                    // ignore
                }
            } else {
                bytes = IOUtils.toByteArray(is);
            }
            if (isResponseGzipEncoded())
                setResponse(Gzip.decompressGZIP(bytes));
            else
                setResponse(bytes);
//            } catch (Throwable t) {
            // ok - won't always be available
            //System.out.println(t.getMessage());
//            }
        } catch (Throwable t) {
            String errorMsg = "GET " + uri + "\n" + t.getMessage();
            logger.log(Level.SEVERE, errorMsg, t);
            throw new Error(errorMsg, t);
        } finally {
            if (connection != null)
                connection.disconnect();
        }
    }

    public boolean isSearch() {
        String query = uri.getQuery();
        return query != null && !query.equals("");
    }

    public void get(String url) throws URISyntaxException {
        get(new CustomUriBuilder(url).build(), (Map<String, String>) null);
    }

    public void get() {
        try {
            get(getUri(), getRequestHeaders().getAll());
        } catch (Throwable e) {
            status = 404;
            String msg = e.getMessage() + "\n" + "HttpGetter#get Error. Check server log for details.";
            logger.log(Level.SEVERE, msg, e);
            setResponseText(msg);
            setResponse(msg.getBytes());
        }
    }

    public HttpGetter get(URI uri, String contentType)  {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", contentType);
        headers.put("accept-charset", "utf-8");
        get(uri, headers);
        return this;
    }

    public HttpGetter getJson(String url) throws URISyntaxException {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        headers.put("accept-charset", "utf-8");
        get(new CustomUriBuilder(url).build(), headers);
        if (getResponse() != null)
            setResponseText(new String(getResponse()));
        return this;
    }

    public HttpGetter getJson(URI uri)  {
        get(uri, "application/json");
        return this;
    }

    public HttpGetter run()  {
        Objects.requireNonNull(uri);
        get(uri, getRequestHeaders().getAll());
        return this;
    }

    public String getVerb() {
        return GET_VERB;
    }

}
