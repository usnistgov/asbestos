package gov.nist.asbestos.http.operations;

import gov.nist.asbestos.http.headers.Headers;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract public class HttpBase {
    Map<String, List<String>> requestHeadersList = null;
    Headers _requestHeaders = null;
    Headers _responseHeaders = null;
    int status;
    String _responseText = null;
    byte[] _response;
    String _requestText = null;
    byte[] _request;
    URI uri;

    public abstract HttpBase run() throws IOException;

    public static String parameterMapToString(Map<String, List<String>> parameterMap) {
        if (parameterMap == null || parameterMap.isEmpty())
            return "";
        StringBuilder buf = new StringBuilder();

        boolean isFirst = true;

        for (String name : parameterMap.keySet()) {
            List<String> values = parameterMap.get(name);
            for (String value : values) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    buf.append('&');
                }
                buf.append(name).append('=').append(value);
            }
        }

        return buf.toString();
    }

    public void setResponseHeadersList(Map<String, List<String>> responseHeadersList) {
        _responseHeaders = new Headers(responseHeadersList);
    }

    public void setResponse(byte[] bytes) {
        _response = bytes;
    }

    public void setResponseText(String txt) {
        _responseText = txt;
    }

    public byte[] getResponse() {
        return _response;
    }

    public void setRequest(byte[] bytes) {
        _request = bytes;
    }

    public void setRequestText(String txt) {
        _requestText = txt;
    }

    public byte[] getRequest() {
        return _request;
    }

    public Headers setRequestHeaders(Headers hdrs) {
        _requestHeaders = hdrs;
        return hdrs;
    }

    public Headers setResponseHeaders(Headers hdrs) {
        _responseHeaders = hdrs;
        return hdrs;
    }

    public String getResponseText() {
        return _responseText;
    }

    public Headers getRequestHeaders() {
        if (_requestHeaders == null)
            _requestHeaders = new Headers(requestHeadersList);
        return _requestHeaders;
    }

    public Headers getResponseHeaders() {
        return _responseHeaders;
    }

    public String getResponseContentType() {
        return getResponseHeaders().getContentType().getAllValuesAndParmsAsString();
    }

    public String getRequestContentType() {
        return getRequestHeaders().getContentType().getAllValuesAndParmsAsString();
    }

    public static void addHeaders(HttpURLConnection connection, Map<String, String> headers) {
        headers.forEach(connection::setRequestProperty);
    }

    public static void addHeaders(HttpURLConnection connection, Headers headers) {
        headers.getAll().forEach(connection::setRequestProperty);
    }

    public static URI buildURI(String url, ParameterBuilder parameterBuilder) {
        return buildURI(url, parameterBuilder.parameterMap);
    }

    public static URI buildURI(String url, Map<String, List<String>> parameterMap)  {
        String params = parameterMapToString(parameterMap);
        try {
            if (params.length() > 0)
                return new URI(url + '?' + params);
            return new URI(url);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, List<String>> mapFromQuery(String query) {
        ParameterBuilder pb = new ParameterBuilder();

        String[] parts = query.split("&");

        Arrays.asList(parts).forEach(string -> {
            String[] strings = string.split("=", 2);
            pb.add(strings[0], strings[1]);
        });

        return pb.parameterMap;
    }

    public static Map<String, String> flattenQueryMap(Map<String, List<String>> queryMap) {
        Map<String, String> map = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : queryMap.entrySet()) {
            String name = entry.getKey();
            List<String> values = entry.getValue();
            if (values.size() == 0)
                throw new RuntimeException("HttpBase#flttenQueryMap: called with empty value list for parameter " + name);
            if (values.size() > 1)
                throw new RuntimeException("HttpBase#flttenQueryMap: called with value list for parameter " + name + " containing multiple values");
            map.put(name, values.get(0));
        }

        return map;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public URI getUri() {
        return uri;
    }

    public int getStatus() {
        return status;
    }
}
