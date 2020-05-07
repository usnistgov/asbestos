package gov.nist.asbestos.http.support;

import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.http.operations.Verb;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Common {

    // params come in three formats - try each
    public static URI buildURI(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String queryString = req.getQueryString();
        if (queryString != null)
            uri = uri + "?" + queryString;
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new Error(e);
        }


//        List<NameValuePair> nameValues;
//        try {
//            nameValues = new URIBuilder(req.getRequestURI()).getQueryParams();
//        } catch (URISyntaxException e) {
//            throw new Error(e);
//        }
//
//
//        Enumeration<String> namesEnumeration = req.getParameterNames(); //.hasMoreElements();
//        List<String> values = new ArrayList<>();
//        while (namesEnumeration.hasMoreElements()) {
//            String name = namesEnumeration.nextElement();
//            values.add(name);
//        }
//        if (!values.isEmpty() && !values.get(0).contains("=")) {
//            // second format - values is parm names
//            Map<String, List<String>> parms = req.getParameterMap();
//            parms = fixParmMap(parms);
//            return HttpBase.buildURI(req.getRequestURI(), parms);
//        }
//        if (values.size() == 1 && values.get(0).contains("=")) {
//            // entire query string is in values[0] -
//            String[] parts1 = values.get(0).split("&");
//        }
//        // values will finish with one entry - url=xxxx
//        if (values.isEmpty()) {
//            return HttpBase.buildURI(req.getRequestURI(), (Map<String, List<String>>) null);
//        }
//        String value = values.get(0);
//        String[] parts = value.split("=", 2);
//        String url = parts[1];
//        try {
//            url = URLDecoder.decode(url, StandardCharsets.UTF_8.toString());
//            return new URI(url);
//        } catch (URISyntaxException | UnsupportedEncodingException e) {
//            throw new Error(e);
//        }

//        Map<String, List<String>> parms = req.getParameterMap();
//        parms = fixParmMap(parms);
//        return HttpBase.buildURI(req.getRequestURI(), parms);
    }

    private static Map<String, List<String>> fixParmMap(Map<String, List<String>> parms) {
        Map<String, List<String>> map = new HashMap<>();
        for (String key : parms.keySet()) {
            if (key.contains("&")) {
                for (String parm : key.split("&")) {
                    fixAParm(map, parm);
                }
            } else {
                if (key.contains("=")) {
                    fixAParm(map, key);
//                    String[] parts = key.split("=");
//                    String theKey = parts[0];
//                    String[] values = parts[1].split(",");
//                    List<String> theValues = new ArrayList<>(Arrays.asList(values));
//                    map.put(theKey, theValues);
                } else {
                    map.put(key, parms.get(key));
                }
            }
        }
        return map;
    }

    private static void fixAParm(Map<String, List<String>> map, String parm) {
            String[] parts = parm.split("=");
            String theKey = parts[0];
            String[] values = parts[1].split(",");
            List<String> theValues = new ArrayList<>(Arrays.asList(values));
            map.put(theKey, theValues);
    }

    public static Headers getRequestHeaders(HttpServletRequest req, Verb verb) {
        List<String> names = Collections.list(req.getHeaderNames());
        Map<String, List<String>> hdrs = new HashMap<>();
        for (String name : names) {
            List<String> values = Collections.list(req.getHeaders(name));
            hdrs.put(name, values);
        }
        Headers headers = new Headers(hdrs);
        headers.setVerb(verb.toString());
        try {
            headers.setPathInfo(Common.buildURI(req));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return headers;
    }
}
