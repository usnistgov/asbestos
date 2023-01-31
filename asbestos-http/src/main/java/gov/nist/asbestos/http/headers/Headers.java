package gov.nist.asbestos.http.headers;


import java.util.logging.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

public class Headers {
    public static final String X_FTK_VALIDATION_EVENT = "x-ftkValidation-event";
    String verb = null;
    URI pathInfo = null;
    int status = 0;
    private List<Header> headers = new ArrayList<>();
    private static Logger log = Logger.getLogger(Headers.class.getName());
    public Headers() {}

    public Headers(List<Header> headerList) {
        this.headers = headerList;
    }

    public Map<String, String> asMap() {
        Map<String, String> map = new HashMap<>();
        for (Header header : headers) {
            map.put(header.getName(), header.getAllValuesAsString());
        }
        return map;
    }

    public boolean isZipped() {
        Header contentEncodingHeader = get("Content-Encoding");
        return contentEncodingHeader != null && contentEncodingHeader.getValue().contains("gzip");
    }

    public boolean requestsZip() {
        Header contentEncodingHeader = get("Accept-Encoding");
        return contentEncodingHeader != null && contentEncodingHeader.getValue().contains("gzip");
    }

    public Headers withContentType(String type) {
        Header ct = new Header("Content-Type", type);
        headers.add(ct);
        return this;
    }

    public Headers withAccept(String type) {
        Header acc = new Header("Accept", type);
        headers.add(acc);
        return this;
    }

    public Headers withAcceptEncoding(String encoding) {
        Header hdr = new Header("Accept-Encoding", Collections.singletonList(encoding));
        headers.add(hdr);
        return this;
    }

    public Headers withVerb(String verb) {
        this.verb = verb;
        return this;
    }

    public Headers withPathInfo(URI pathInfo) {
        this.pathInfo = pathInfo;
        return this;
    }

    public void removeHeader(String name) {
        for (Header header : headers) {
            if (header.getName().equalsIgnoreCase(name)) {
                headers.remove(header);
                return;
            }
        }
    }

    public Headers set(Header header) {
        removeHeader(header.getName());
        add(header);
        return this;
    }

    public Headers add(Header header) {
        if (!hasHeader(header.getName()))
            headers.add(header);
        return this;
    }

    public Headers addAll(Headers theHeaders) {
        for (Header header : theHeaders.getHeaders())
            add(header);
        return this;
    }

    public Headers(String headerString) {
        StringTokenizer st = new StringTokenizer(headerString, "\n");

        boolean isFirst = true;
        while(st.hasMoreTokens()) {
            String it = WhiteSpace.removeTrailing(st.nextToken());
            if (isFirst) {
                String[] parts = it.split(" ", 3);
                if (parts.length >= 1) {
                    String first = parts[0];
                    try {
                        int status = Integer.parseInt(first);
                        this.status = status;
                    } catch (Throwable t) {
                        this.verb =first;
                        try {
                            if (parts.length >= 2)
                                this.pathInfo = new URI(parts[1]);
                        } catch (Throwable t1) {
                            log.severe("Cannot parse " + parts[1]);
                        }
                    }
                }
                isFirst = false;
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

    public String getHeaderValue(String name) {
        for (Header header : headers) {
            if (name.equalsIgnoreCase(header.getName()))
                return header.getValue();
        }
        return null;
    }

    public List<String> getNames() {
        Set<String> nameSet = new HashSet<>();

        for (Header header : headers) {
            nameSet.add(header.getName());
        }
        return new ArrayList<>(nameSet);
    }

    public boolean hasContentType() {
        Optional<Header> theHeader = headers.stream()
                .filter(header -> header.getName().equalsIgnoreCase("content-type"))
                .findFirst();
        return theHeader.isPresent();
    }

    public boolean hasHeader(String name) {
        for (Header header : headers) {
            if (header.getName().equalsIgnoreCase(name))
                return true;
        }
        return false;
    }

    public void deleteContentType() {
        for (Header header : headers) {
            if (header.getName().equalsIgnoreCase("Content-Type")) {
                headers.remove(header);
                return;
            }
        }
        return;
    }

    public Header getContentType() {
        Optional<Header> theHeader = headers.stream()
                .filter(header -> header.getName().equalsIgnoreCase("content-type"))
                .findFirst();
        if (theHeader.isPresent())
            return theHeader.get();
        else
            return new Header("content-type", "");
    }

    public Header get(String name) {
        Optional<Header> theHeader = headers.stream()
                .filter(header -> header.getName().equalsIgnoreCase(name))
                .findFirst();
        if (theHeader.isPresent())
            return theHeader.get();
        else
            return null;
    }

    public String getValue(String name) {
        Header header = get(name);
        return header.getValue();
    }

    public String getProxyEvent() {
        return getFtkEvent("x-proxy-event");
    }

    private String getFtkEvent(String s) {
        Header pe = get(s);
        if (pe == null)
            return null;
        return pe.getValue();
    }


    public Header getAccept() {
        Optional<Header> accepts = headers.stream()
                .filter(header -> header.getName().equalsIgnoreCase("accept"))
                .findFirst();
        return accepts.orElseGet(() -> new Header("accept", "*/*"));
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

    public Headers setVerb(String verb) {
        this.verb = verb;
        return this;
    }

    public URI getPathInfo() {
        return pathInfo;
    }

    public Headers setPathInfo(URI pathInfo) {
        this.pathInfo = pathInfo;
        return this;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String toString() {
        return
                (verb == null ? "" : verb + " ")  + (status == 0 ? "" : status + " ") + (pathInfo == null ? "" : pathInfo) + "\n" +
                        headers.stream()
                                .map(Header::toString)
                                .collect(Collectors.joining("\r\n"));
    }


    public String getValidationEvent() {
        return getFtkEvent(X_FTK_VALIDATION_EVENT);
    }
}
