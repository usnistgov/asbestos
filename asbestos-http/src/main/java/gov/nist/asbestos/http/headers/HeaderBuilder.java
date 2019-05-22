package gov.nist.asbestos.http.headers

import java.net.URI;
import java.util.List;
import java.util.Map;

class HeaderBuilder {

    // does not include URI line
    static String headersAsString(RawHeaders headers) {
        Headers heads = parseHeaders(headers)
        heads.toString()
    }

    static RawHeaders rawHeadersFromString(String input) {
        Map<String, List<String>> lines = [:]
        String uriLine = null

        input.eachLine {
            if (!it.contains(':')) {
                uriLine = it
                return
            }
            String[] nameValue = it.split(':', 2)
            assert nameValue.size() == 2
            String name = nameValue[0].trim()
            String value = nameValue[1].trim()
            List<String> values = value.split(';')
            assert values.size() > 0
            (0..<values.size()).each { int i ->
                values[i] = values[i].trim()
            }
            lines.put(name, values)
        }

        RawHeaders rawHeaders = new RawHeaders(lines)
        rawHeaders.uriLine = uriLine


        rawHeaders
    }

    static Headers parseHeaders(String headers) {
        parseHeaders(rawHeadersFromString(headers))
    }

    static Headers parseHeaders(RawHeaders rawHeaders) {
        Headers headers = new Headers()

        String[] lineParts = rawHeaders.uriLine.split()
        assert [2, 3].contains(lineParts.size()) : "HeaderBuilder : URI line should have two or three elements, has ${lineParts.size()}"
        headers.verb = lineParts[0]
        String x = (lineParts.size() == 2) ? lineParts[1] : lineParts[1] + '?' + lineParts[2]
        headers.pathInfo = new URI(x)

        List<String> names = rawHeaders.names
        names.each { String name ->
            List<String> values = rawHeaders.headers.get(name)
            values.each { String value ->
                headers.nameValueList << new NameValue([name: name, value: value])
            }
        }

        headers
    }

    // TODO  needs test
    static Headers parseHeaders(Map<String, ?> theHeaders) {
        Headers headers = new Headers()

        if (!theHeaders)
            return headers

        String firstName = theHeaders.keySet().first()
        Object firstValue = theHeaders.get(firstName)
        if (firstValue instanceof String) {
            theHeaders.each { String name, String value ->
                if (value) {
                    List<String> subValues = value.split(';')
                    if (subValues) {
                        subValues.each { String subValue ->
                            headers.nameValueList << new NameValue([name: name?.trim(), value: subValue?.trim()])
                        }
                    }
                }
            }
        } else  {
            List<String> names = theHeaders.keySet() as List
            names.each {String name ->
                if (!name) return
                List<String> values = theHeaders.get(name)
                if (values) {
                    values.each { String value ->
                        if (value) {
                            List<String> subValues = value.split(';')
                            if (subValues) {
                                subValues.each { String subValue ->
                                    headers.nameValueList << new NameValue([name: name?.trim(), value: subValue?.trim()])
                                }
                            }
                        }
                    }
                }
            }
        }

        headers
    }
}
