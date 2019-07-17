package gov.nist.asbestos.http.operations;

import gov.nist.asbestos.http.headers.Header;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class HttpPost  extends HttpBase {
    private Header locationHeader = null;

    private void post(URI uri, Map<String, String> headers, byte[] content) throws IOException {
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) uri.toURL().openConnection();
            if (headers != null)
                addHeaders(connection, headers);
            requestHeadersList = connection.getRequestProperties();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            // TODO use proper charset (from input)
            if (content != null)
                connection.getOutputStream().write(content);
            status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK || status == HttpURLConnection.HTTP_CREATED) {
                //connection.getHeaderFields()
                setResponseHeadersList(connection.getHeaderFields());
            }
            if (status >= 400)
                return;
                byte[] bb = IOUtils.toByteArray(connection.getInputStream());
                setResponse(bb);
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
