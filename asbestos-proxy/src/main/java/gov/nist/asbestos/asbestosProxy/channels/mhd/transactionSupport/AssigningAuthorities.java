package gov.nist.asbestos.asbestosProxy.channels.mhd.transactionSupport;

import java.util.ArrayList;
import java.util.List;

public class AssigningAuthorities {
    private List<String> values = new ArrayList<>();
    private boolean any = false;
    private static String oidPrefix = "urn:oid:";

    public AssigningAuthorities allowAny() {
        any = true;
        return this;
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
