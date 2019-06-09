package gov.nist.asbestos.http.operations;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class HttpPost  extends HttpBase {

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

}