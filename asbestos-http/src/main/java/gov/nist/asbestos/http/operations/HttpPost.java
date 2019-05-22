package gov.nist.asbestos.http.operations

import groovy.transform.TypeChecked
import org.apache.log4j.Logger

@TypeChecked
class HttpPost  extends HttpBase {
    static Logger log = Logger.getLogger(HttpPost);

    void post(URI uri, Map<String, String> headers, byte[] content) {
        HttpURLConnection connection

        try {
            connection = (HttpURLConnection) uri.toURL().openConnection()
            if (headers)
                addHeaders(connection, headers)
            requestHeadersList = connection.getRequestProperties()
            connection.setRequestMethod('POST')
            connection.setDoOutput(true)
            connection.setDoInput(true)
            // TODO use proper charset (from input)
            if (content)
                connection.getOutputStream().write(content)
            status = connection.getResponseCode()
            if (status == HttpURLConnection.HTTP_OK || HttpURLConnection.HTTP_CREATED) {
                //connection.getHeaderFields()
                responseHeadersList = connection.getHeaderFields()
            }
            if (status >= 400)
                return
            try {
                byte[] bb = connection.inputStream.bytes
                setResponse(bb)
            } catch (Throwable t) {
                log.info(t.getMessage());
                throw t
            }
        } finally {
            if (connection)
                connection.disconnect()
            //requestHeadersList = connection.getRequestProperties()
        }
    }

    HttpPost postJson(URI uri, String json) {
        Map<String, String> headers = ['content-type':'application/json']
        post(uri, headers, json.bytes)
        this
    }

    HttpPost run() {
        assert uri
        post(uri, requestHeaders.all,  request)
        this
    }

}
