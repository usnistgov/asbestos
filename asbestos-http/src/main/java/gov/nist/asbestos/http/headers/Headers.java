package gov.nist.asbestos.http.headers;


import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

public class Headers {
    String verb = null;
    URI pathInfo = null;
    int status = 0;
    List<NameValue> nameValueList = new ArrayList<>();

    private Optional<String> getSimpleValue(String headerName) {
        Optional<NameValue> nameValue = nameValueList.stream()
                .filter(nv -> nv.name.equalsIgnoreCase(headerName))
                .findFirst();
        if (nameValue.isPresent()) {
            String value = nameValue.get().value;
            if (value.contains(";")) {
                return Optional.of(value.split(";", 2)[0].trim());
            }
        }
        return Optional.empty();
    }

    public Optional<String> getContentType() {
        return getSimpleValue("content-type");
    }

    public Optional<String> getAccept() {
        return getSimpleValue("accept");
    }

    public Optional<String> getContentEncoding() {
        return getSimpleValue("content-encoding");
    }

    public Optional<String> getAll(String type) {
        List<String> list = nameValueList.stream()
                .filter(nv -> nv.name.equalsIgnoreCase(type))
                .map(NameValue::getName)
                .collect(Collectors.toList());
        if (list.isEmpty()) return Optional.empty();
        return Optional.of(String.join("; ", list));
    }

    public Map<String, String> getAll() {
        Map<String, String> result = new HashMap<>();

        nameValueList.forEach(nv ->
                result.put(nv.name, getAll(nv.name).get()));

        return result;
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
                    result.put(name, getAll(name).get());
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
                .collect(Collectors.toMap(nv -> nv.name, nv -> getAll(nv.name).get()));

        map.forEach((name, value) -> {
            buf.append(String.join(": ", name, value));
                });

        return buf.toString();
    }


}
