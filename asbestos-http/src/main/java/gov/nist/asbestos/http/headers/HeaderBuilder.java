package gov.nist.asbestos.http.headers;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

public class HeaderBuilder {

    // does not include URI line
    static String headersAsString(RawHeaders headers) throws Exception {
        Headers heads = parseHeaders(headers);
        return heads.toString();
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
                    .map(String::trim)
                    .collect(Collectors.toList());
            lines.put(name, values);
        }

        RawHeaders rawHeaders = new RawHeaders(lines);
        rawHeaders.uriLine = uriLine;


        return rawHeaders;
    }

    static public Headers parseHeaders(String headers) throws Exception {
        return parseHeaders(rawHeadersFromString(headers));
    }

    static public Headers parseHeaders(RawHeaders rawHeaders) throws Exception {
        Headers headers = new Headers();

        String[] lineParts = rawHeaders.uriLine.split(" ");

        int size = lineParts.length;
        if (!(size == 2 || size == 3))
            throw new Exception(String.format("HeaderBuilder : URI line should have two or three elements, has %s", size));

        headers.verb = lineParts[0];
        String x = (size == 2) ? lineParts[1] : lineParts[1] + '?' + lineParts[2];
        headers.pathInfo = new URI(x);

        rawHeaders.names.forEach(name -> {
            List<String> values = rawHeaders.headers.get(name);
            values.forEach(value -> headers.nameValueList.add(new NameValue(name, value)));
        });

        return headers;
    }

//    static public Headers parseHeaders(Map<String, ?> theHeaders) {
//        Headers headers = new Headers();
//        if (theHeaders == null || theHeaders.isEmpty())
//            return headers;
//        String aKey = theHeaders.keySet().iterator().next();
//        Object aValue = theHeaders.get(aKey);
//        if (aValue instanceof String) {
//            ((Map<String, String>) theHeaders).forEach((name, values) -> {
//                if (values != null && !values.equals("")) {
//                    List<String> subValues = Arrays.asList(values.split(";"));
//                    subValues.forEach(value -> headers.nameValueList.add(new NameValue(name.trim(), value.trim())));
//                }
//            });
//        } else {
//            ((Map<String, List<String>>) theHeaders).forEach((name, values) -> {
//                if (values != null) {
//                    values.forEach(value -> {
//                        List<String> subValues = Arrays.asList(value.split(";"));
//                        subValues.forEach(val -> headers.nameValueList.add(new NameValue(name.trim(), val.trim())));
//                    });
//
//                }
//            });
//
//        }
//
//        return headers;
//    }
}
