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

    public Headers() {}

    public Headers(List<Header> headerList) {
        this.headers = headerList;
    }

    public void removeHeader(String name) {
        for (Header header : headers) {
            if (header.getName().equalsIgnoreCase(name)) {
                headers.remove(header);
                return;
            }
        }
    }

    public Headers addAll(Headers theHeaders) {
        this.headers.addAll(theHeaders.headers);
        return this;
    }

    public Headers(String headerString) {
        StringTokenizer st = new StringTokenizer(headerString, "\n");

        while(st.hasMoreTokens()) {
            String it = WhiteSpace.removeTrailing(st.nextToken());
            if (!it.contains(":")) {
                String[] parts = it.split(" ", 2);
                if (parts.length == 2) {
                    verb = parts[0];
                    try {
                        pathInfo = new URI(parts[1]);
                    } catch (Exception e) {
                        throw new RuntimeException(parts[1], e);
                    }
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
                if (name != null && values != null) {
                    headers.add(new Header(name, values));
                }
            });

        }
    }

    public List<Header> getHeaders() {
        return headers;
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
        Optional<Header> hdr = headers.stream()
                .filter(header -> header.getName().equalsIgnoreCase("content-encoding"))
                .findFirst();
        return hdr.orElse(new Header("content-encoding"));
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

    // this is for collecting headers accept* for example
    public Headers select(List<String> namePrefixs) {
        Headers headers = new Headers();
        for (String namePrefix : namePrefixs) {
            headers.addAll(getAllWithPrefix(namePrefix));
        }
        return headers;
    }

    private Headers getAllWithPrefix(String prefix) {
        List<Header> headerList = headers.stream()
                .filter(header -> header.getName().toLowerCase().startsWith(prefix))
                .collect(Collectors.toList());
        return new Headers(headerList);
    }

    public String getVerb() {
        return verb;
    }

    public void setVerb(String verb) {
        this.verb = verb;
    }

    public URI getPathInfo() {
        return pathInfo;
    }

    public void setPathInfo(URI pathInfo) {
        this.pathInfo = pathInfo;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String toString() {
        return headers.stream()
                .map(Header::toString)
                .collect(Collectors.joining("\r\n"));
    }


}
