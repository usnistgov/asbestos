package gov.nist.asbestos.mhd.util;

import java.util.Arrays;
import java.util.List;

public class Utils {
    public static String URN_OID = "urn:oid:";
            public static String URN_UUID = "urn:uuid:";

    public static String stripUrnPrefixes(String id) {
        if (id == null) return id;
        List<String> prefixes = Arrays.asList(URN_UUID, URN_OID);
        for (String p : prefixes) {
           if (id.startsWith(p)) return id.substring(p.length());
        }
        return id;
    }

    public static String addUrnOidPrefix(String id) {
        return URN_OID + stripUrnPrefixes(id);
    }

}
