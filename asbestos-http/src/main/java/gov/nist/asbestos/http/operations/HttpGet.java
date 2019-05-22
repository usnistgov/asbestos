package gov.nist.asbestos.http.operations


class HttpGet extends HttpBase {
    // TODO GET parameters in the body
    void get(URI uri, Map<String, String> headers) {
        HttpURLConnection connection
        try {
            connection = (HttpURLConnection) uri.toURL().openConnection()
            connection.setRequestMethod('GET')
            if (headers)
                addHeaders(connection, headers)
            requestHeadersList = connection.getRequestProperties()
            status = connection.getResponseCode()
            if (status == HttpURLConnection.HTTP_OK) {
                responseHeadersList = connection.getHeaderFields()
            }
            try {
                setResponse(connection.inputStream.bytes)
            } catch (Throwable t) {
            }
        } finally {
            if (connection)
                connection.disconnect()
        }
    }

    void get(String url) {
        get(new URI(url), (Map<String, String>) null)
    }

    HttpGet get(URI uri, String contentType) {
        Map<String, String> headers = [ accept: "${contentType}", 'accept-charset': 'utf-8']
        get(uri, headers)
        if (response)
            setResponseText(new String(response))
        this
    }

    HttpGet getJson(String url) {
        Map<String, String> headers = [ accept: 'application/json', 'accept-charset': 'utf-8']
        get(new URI(url), headers)
        if (response)
            setResponseText(new String(response))
        this
    }

    void getJson(URI uri) {
        Map<String, String> headers = [ accept: 'application/json', 'accept-charset': 'utf-8']
        get(uri, headers)
        setResponseText(new String(response))
    }

    HttpGet run() {
        assert uri
        get(uri, requestHeaders.all)
        this
    }

}
