package gov.nist.asbestos.http.headers;


import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

public class Headers {
    String verb = null;
    URI pathInfo = null;
    int status = 0;
    List<Header> headers = new ArrayList<>();

    Headers() {}

    public Headers(String headerString) throws URISyntaxException {
        StringTokenizer st = new StringTokenizer(headerString, "\n");

        while(st.hasMoreTokens()) {
            String it = WhiteSpace.removeTrailing(st.nextToken());
            if (!it.contains(":")) {
                String[] parts = it.split(" ", 2);
                if (parts.length == 2) {
                    verb = parts[0];
                    pathInfo = new URI(parts[1]);
                }
                continue;
            }
            headers.add(new Header(it));
        }
    }

    public Headers(Map<String, ?> theHeaders) {
        if (theHeaders == null || theHeaders.isEmpty())
            return;
        String aKey = theHeaders.keySet().iterator().next();
        Object aValue = theHeaders.get(aKey);
        if (aValue instanceof String) {
            ((Map<String, String>) theHeaders).forEach((name, values) -> {
                headers.add(new Header(name, values));
            });
        } else {
            ((Map<String, List<String>>) theHeaders).forEach((name, values) -> {
                if (values != null) {
                    headers.add(new Header(name, values));
                }
            });

        }
    }

    public String getContentType() {
        return getAll("content-type");
    }

    public String getAccept() {
        return getAll("accept");
    }

    public String getContentEncoding() {
        return getAll("content-encoding");
    }

    public List<String> getAll(String theName) {
        Objects.requireNonNull(theName);
        return headers.stream()
                .filter(header -> theName.equals(header.getName()))
                .map(header -> header.getValues().toString())
                .collect(Collectors.toList());
    }

    public Map<String, String> getAll() {
        Map<String, String> result = new HashMap<>();

        for

        return headers.stream()
                .map(header -> header.getName())
                .collect(Collectors.toMap(name -> name, name -> getAll(name)));
    }

    public void removeHeader(String name) {
        nameValueList.removeIf(nv -> nv.name.equals(name));
    }

    // this is for collecting headers accept* for example
    public Map<String, String> getMultiple(List<String> namePrefixs) {
        Map<String, String> result = new HashMap<>();

        namePrefixs.forEach(pre -> {
            nameValueList.forEach(nv -> {
                String name = nv.name;
                if (name.startsWith(pre) && !result.keySet().contains(name)) {
                    result.put(name, getAll(name));
                }
            });
        });


        return result;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();

        if (StringUtils.isEmpty(verb) && pathInfo != null)
            buf.append(verb).append(' ').append(pathInfo).append("\r\n");
        if (status != 0)
            buf.append("1.1 ${status} unknown");
        Map<String, String> hdrs = new HashMap<>();

        Map<String, String> map = nameValueList.stream()
                .collect(Collectors.toMap(nv -> nv.name, nv -> getAll(nv.name)));

        map.forEach((name, value) -> {
            buf.append(String.join(": ", name, value));
                });

        return buf.toString();
    }


}
