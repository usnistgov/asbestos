package gov.nist.asbestos.http.headers;

import java.util.*;

public class RawHeaders {
    String uriLine;  // GET|POST path [queryString]
    Map<String, List<String>> headers = new HashMap<>();
    List<String> names = new ArrayList<>();

//    public RawHeaders(Enumeration namesEnum, Map<String, Enumeration> headers) {
//        while (namesEnum.hasMoreElements()) {
//            String name = (String) namesEnum.nextElement();
//            names.add(name);
//            generateNamesAsList(name, headers.get(name))
//        }
//    }

//    RawHeaders() {
//
//    }

    public void addNames(Enumeration namesEnum) {
        while (namesEnum.hasMoreElements()) {
            String name = (String) namesEnum.nextElement();
            names.add(name);
        }
    }

    public void addHeaders(String name, Enumeration headersEnum) {
        List<String> values = new ArrayList<>();
        while(headersEnum.hasMoreElements()) {
            String val = (String) headersEnum.nextElement();
            values.add(val);
        }
        headers.put(name, values);
    }

    public RawHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
        names = new ArrayList<>(headers.keySet());
    }

//    static private Map<String, List<String>> generateHeadersAsList(String name, Enumeration<String> e) {
//        List<String> lst = new ArrayList<>();
//        while (e.hasMoreElements())
//            lst.add(e.nextElement());
//        return lst;
//    }

//    List<String> headersAsList(String name) {
//        if (!headers) {
//            headers = generateHeadersAsList(name)
//            names = generateNamesAsList()
//        }
//        headers
//    }

//    List<String> namesAsList() {
//        if (!headers) {
//            headers = generateHeadersAsList(name)
//            names = generateNamesAsList()
//        }
//        names
//    }

//    private List<String> generateNamesAsList(String name) {
//        List<String> lst = new ArrayList<>();
//
//        while(names.hasMoreElements())
//            lst << names.nextElement()
//
//        lst
//    }
}
