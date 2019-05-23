package gov.nist.asbestos.http.headers;


import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

public class Headers {
    String verb = null;
    URI pathInfo = null;
    int status = 0;
    private List<Header> headers = new ArrayList<>();

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

    public List<String> getNames() {
        Set<String> nameSet = new HashSet<>();

        for (Header header : headers) {
            nameSet.add(header.getName());
        }
        return new ArrayList<>(nameSet);
    }

    public Header getContentType() {
        return headers.stream()
                .filter(header -> header.getName().equalsIgnoreCase("content-type"))
                .findFirst()
                .get();
    }

    public Header getAccept() {
        return headers.stream()
                .filter(header -> header.getName().equalsIgnoreCase("accept"))
                .findFirst()
                .get();
    }

    public Header getContentEncoding() {
        return headers.stream()
                .filter(header -> header.getName().equalsIgnoreCase("content-encoding"))
                .findFirst()
                .get();
    }

    private List<String> getAll(String theName) {
        Objects.requireNonNull(theName);
        return headers.stream()
                .filter(header -> theName.equals(header.getName()))
                .map(header -> header.getValues().toString())
                .collect(Collectors.toList());
    }

    public Map<String, String> getAll() {
        return headers.stream()
                .collect(Collectors.toMap(Header::getName, Header::getAllValuesAndParmsAsString));
    }

//    // this is for collecting headers accept* for example
//    public Map<String, String> getMultiple(List<String> namePrefixs) {
//        Map<String, String> result = new HashMap<>();
//
//        namePrefixs.forEach(pre -> {
//            nameValueList.forEach(nv -> {
//                String name = nv.name;
//                if (name.startsWith(pre) && !result.keySet().contains(name)) {
//                    result.put(name, getAll(name));
//                }
//            });
//        });
//
//
//        return result;
//    }

    public String toString() {
        return headers.stream()
                .map(Header::toString)
                .collect(Collectors.joining("\r\n"));
    }


}
