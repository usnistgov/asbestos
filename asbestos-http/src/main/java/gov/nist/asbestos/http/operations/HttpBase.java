package gov.nist.asbestos.http.operations;

import gov.nist.asbestos.http.headers.HeaderBuilder;
import gov.nist.asbestos.http.headers.Headers;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

abstract class HttpBase {
    Map<String, List<String>> requestHeadersList = null;
    Headers _requestHeaders = null;
    Headers _responseHeaders = null;
    int status;
    String _responseText = null;
    byte[] _response;
    String _requestText = null;
    byte[] _request;
    URI uri;

    abstract HttpBase run() throws IOException

    static String parameterMapToString(Map<String, List<String>> parameterMap) {
        if (!parameterMap || parameterMap.isEmpty())
            return ''
        StringBuilder buf = new StringBuilder()

        boolean isFirst = true
//        buf.append('?')
        parameterMap.each { String name, Object ovalues ->
            List<String> values = ovalues as List<String>
            values.each { String value ->
                if (isFirst) {
                    isFirst = false
                } else {
                    buf.append('&')
                }
                buf.append(name).append('=').append(value)
            }
        }

        buf
    }

    void setResponseHeadersList(Map<String, List<String>> responseHeadersList) {
        _responseHeaders = HeaderBuilder.parseHeaders(responseHeadersList)
    }

    void setResponse(byte[] bytes) {
        _response = bytes
    }

    void setResponseText(String txt) {
        _responseText = txt
    }

    byte[] getResponse() {
        _response
    }

    void setRequest(byte[] bytes) {
        _request = bytes
    }

    void setRequestText(String txt) {
        _requestText = txt
    }

    byte[] getRequest() {
        _request
    }

    Headers setRequestHeaders(Headers hdrs) {
        _requestHeaders = hdrs
        hdrs
    }

    Headers setResponseHeaders(Headers hdrs) {
        _responseHeaders = hdrs
        hdrs
    }

    String getResponseText() {
        _responseText
    }

    Headers getRequestHeaders() {
        if (!_requestHeaders)
            _requestHeaders = HeaderBuilder.parseHeaders(requestHeadersList)
        _requestHeaders
    }

    Headers getResponseHeaders() {
        _responseHeaders
    }

    String getResponseContentType() {
        responseHeaders.contentType
    }

    String getRequestContentType() {
        requestHeaders.contentType
    }

    static addHeaders(HttpURLConnection connection, Map<String, String> headers) {
        headers.each { String name, String value ->
            connection.setRequestProperty(name, value)
        }
    }

    static addHeaders(HttpURLConnection connection, Headers headers) {
        headers.all.each { String name, String value ->
            connection.setRequestProperty(name, value)
        }
    }

    static URI buildURI(String url, ParameterBuilder parameterBuilder) {
        buildURI(url, parameterBuilder.parameterMap)
    }

    static URI buildURI(String url, Map<String, List<String>> parameterMap) {
        String params = parameterMapToString(parameterMap)
        if (params)
            new URI(url + '?' + params)
        else
            new URI(url)
    }

    static Map<String, List<String>> mapFromQuery(String query) {
        ParameterBuilder pb = new ParameterBuilder()

        String[] parts = query.split('&')
        parts.each { String part ->
            String[] nameValue = part.split('=')
            String name = nameValue[0]
            String value = nameValue[1]
            pb.add(name, value)
        }

        pb.parameterMap
    }

    static flattenQueryMap(Map<String, List<String>> queryMap) {
        Map<String, String> map = [:]

        queryMap.each { String name, List<String> values ->
            assert values.size() == 1
            map[name] = values[0]
        }
        map
    }

}
