package gov.nist.asbestos.http.operations;

import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.util.Gzip;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.w3c.dom.events.UIEvent;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
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
    private OperationOutcome operationOutcome = null;  // used for efficiency between components
    protected boolean sendZip = false;
    protected boolean acceptZip = false;

    public abstract HttpBase run() throws IOException;
    public abstract String getVerb();

    public HttpBase sendGzip() {
        if (_requestHeaders == null)
            _requestHeaders = new Headers();
        _requestHeaders.add(new Header("Content-Encoding", "gzip"));
        this.sendZip = true;
        return this;
    }

    public HttpBase acceptGzip() {
        if (_requestHeaders == null)
            _requestHeaders = new Headers();
        _requestHeaders.add(new Header("Accept-Encoding", "gzip"));
        this.acceptZip = true;
        return this;
    }

    public boolean isResponseGzipEncoded() {
        if (_responseHeaders == null) return false;
        return _responseHeaders.isZipped();
    }

    public boolean isRequestGzipEncoded() {
        if (_requestHeaders == null) return false;
        return _requestHeaders.isZipped();
    }

    public static String parameterMapToString(Map<String, List<String>> parameterMap) {
        if (parameterMap == null || parameterMap.isEmpty())
            return "";
        StringBuilder buf = new StringBuilder();

        boolean isFirst = true;

        for (String name : parameterMap.keySet()) {
            Object o = parameterMap.get(name);
            // sometimes it is an array
            if (o instanceof List) {
                List<String> values = (List) o;
                for (String value : values) {
                    if (isFirst) {
                        isFirst = false;
                    } else {
                        buf.append('&');
                    }
                    buf.append(name).append('=').append(value);
                }
            }
            if (o.getClass().isArray()) {
                String[] values = (String[]) o;
                for (String value : values) {
                    if (isFirst) {
                        isFirst = false;
                    } else {
                        buf.append('&');
                    }
                    buf.append(name).append('=').append(value);
                }
            }
        }

        return buf.toString();
    }

    public void setResponseHeadersList(Map<String, List<String>> responseHeadersList) {
        _responseHeaders = new Headers(responseHeadersList);
    }

    public void setResponse(byte[] bytes) {
        _response = bytes;
//        if (isResponseGzipEncoded())
//            _responseText = Gzip.decompressGZIPToString(bytes);
//        else
        if (bytes != null)
            _responseText = new String(bytes);
    }

    public void setResponseText(String txt) {
        _responseText = txt;
        if (txt != null)
            _response = txt.getBytes();
    }

    public byte[] getResponse() {
        return _response;
    }

    public void setRequest(byte[] bytes) {
//        if (sendZip) {
//            bytes = Gzip.compressGZIP(bytes);
//        }
        _request = bytes;
    }

    public void setRequestText(String txt) {
        _requestText = txt;
    }

    public byte[] getRequest() {
        if (_request == null && _requestText != null)
            return _requestText.getBytes();
//        if (isRequestGzipEncoded())
//            return Gzip.decompressGZIP(_request);
        return _request;
    }

    public Headers setRequestHeaders(Headers hdrs) {
        String verb = getVerb();
        if (_requestHeaders == null)
            _requestHeaders = new Headers();
        _requestHeaders.addAll(hdrs);
        _requestHeaders.setVerb(verb);
        _requestHeaders.setPathInfo(hdrs.getPathInfo());
        return _requestHeaders;
    }

    public Headers setResponseHeaders(Headers hdrs) {
        _responseHeaders = hdrs;
        return hdrs;
    }

    public String getResponseText() {
        //Header contentEncodingHeader = getResponseHeaders().get("Content-Encoding");
        //boolean zipped = getResponseHeaders().isZipped();
                //contentEncodingHeader != null && contentEncodingHeader.getValue().contains("gzip");

        if (_responseText == null && _response != null) {
//            if (zipped) {
//                    _responseText = Gzip.decompressGZIPToString(_response);
//            } else
                _responseText = new String(_response);
        }
        return _responseText;
    }

    public Headers getRequestHeaders() {
        if (_requestHeaders == null)
            _requestHeaders = new Headers(requestHeadersList);
        return _requestHeaders;
    }

    public Headers getResponseHeaders() {
        if (_responseHeaders == null)
            _responseHeaders = new Headers();
        if (status != 0)
            _responseHeaders.setStatus(status);
        return _responseHeaders;
    }

    public String getResponseContentType() {
        return getResponseHeaders().getContentType().getAllValuesAndParmsAsString();
    }

    public String getRequestContentType() {
        return getRequestHeaders().getContentType().getAllValuesAndParmsAsString();
    }

    public void setResponseContentType(String contentType) {
        if (_responseHeaders == null)
            _responseHeaders = new Headers();
        _responseHeaders.set(new Header("Content-Type", contentType));
    }

    public void setRequestContentType(String contentType) {
        if (_requestHeaders == null)
            _requestHeaders = new Headers();
        if (_requestHeaders.hasContentType())
            _requestHeaders.deleteContentType();
        _requestHeaders.add(new Header("Content-Type", contentType));
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
        if (parameterMap == null) {
            try {
                return new URI(url);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
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

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isSuccess() {
        return status == 200;
    }

    public OperationOutcome getOperationOutcome() {
        return operationOutcome;
    }

    public void setOperationOutcome(OperationOutcome operationOutcome) {
        this.operationOutcome = operationOutcome;
    }

    public String getContentLocation() {
        return getResponseHeaders().getHeaderValue("Content-Location");
    }

    public String getResponseContentEncoding() {
        Header header = getResponseHeaders().get("content-encoding");
        if (header != null)
            return header.getValue();
        return null;
    }
}
