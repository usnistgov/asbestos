package gov.nist.asbestos.asbestosProxy.wrapper;

import gov.nist.asbestos.http.operations.HttpBase;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.*;

public class Common {

    static URI buildURI(HttpServletRequest req) {
        Map<String, List<String>> parms = req.getParameterMap();
        parms = fixParmMap(parms);
        return HttpBase.buildURI(req.getRequestURI(), parms);
    }

    static Map<String, List<String>> fixParmMap(Map<String, List<String>> parms) {
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


}
