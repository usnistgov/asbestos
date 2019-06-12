package gov.nist.asbestos.mhd.transactionSupport;

import java.util.ArrayList;
import java.util.List;

public class AssigningAuthorities {
    private List<String> values = new ArrayList<>();
    private boolean any = false;
    private static String oidPrefix = "urn:oid:";

    public static AssigningAuthorities allowAny() {
        AssigningAuthorities a = new AssigningAuthorities();
        a.any = true;
        return a;
    }

    public AssigningAuthorities addAuthority(String value) {
        values.add(stripPrefix(value));
        return this;
    }

    private static String stripPrefix(String aa) {
        if (aa.startsWith(oidPrefix))
            aa = aa.substring(oidPrefix.length());
        return aa;
    }

    public boolean check(String aa) {
        aa = stripPrefix(aa);
        return any || values.contains(aa);
    }
}
