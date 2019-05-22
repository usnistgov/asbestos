package gov.nist.asbestos.http.headers

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

class HeaderBuilder {

    // does not include URI line
    static String headersAsString(RawHeaders headers) {
        Headers heads = parseHeaders(headers)
        heads.toString()
    }

    static RawHeaders rawHeadersFromString(String input) {
        Map<String, List<String>> lines = new HashMap<>();
        String uriLine = null;
        StringTokenizer st = new StringTokenizer(input, "\n");

        while (st.hasMoreElements()) {
            String it = st.nextToken();

            if (!it.contains(":")) {
                uriLine = it;
                continue;
            }
            String[] nameValue = it.split(":", 2);
            String name = nameValue[0].trim();
            String value = nameValue[1].trim();
            List<String> values = Arrays.asList(value.split(";"));
            values = values.stream()
                    .map(x -> x.trim())
                    .collect(Collectors.toList());
            lines.put(name, values);
        }

        RawHeaders rawHeaders = new RawHeaders(lines);
        rawHeaders.uriLine = uriLine;


        return rawHeaders;
    }

    static Headers parseHeaders(String headers) {
        parseHeaders(rawHeadersFromString(headers));
    }

    static Headers parseHeaders(RawHeaders rawHeaders) {
        Headers headers = new Headers();

        String[] lineParts = rawHeaders.uriLine.split(" ");

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
