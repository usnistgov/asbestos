package gov.nist.asbestos.http.support;

import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.http.operations.Verb;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.*;

public class Common {

    public static URI buildURI(HttpServletRequest req) {
        Map<String, List<String>> parms = req.getParameterMap();
        parms = fixParmMap(parms);
        return HttpBase.buildURI(req.getRequestURI(), parms);
    }

    private static Map<String, List<String>> fixParmMap(Map<String, List<String>> parms) {
        Map<String, List<String>> map = new HashMap<>();
        for (String key : parms.keySet()) {
            if (key.contains("=")) {
                String[] parts = key.split("=");
                String theKey = parts[0];
                String[] values = parts[1].split(",");
                List<String> theValues = new ArrayList<>(Arrays.asList(values));
                map.put(theKey, theValues);
            } else {
                map.put(key, parms.get(key));
            }
        }
        return map;
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
