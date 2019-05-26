package gov.nist.asbestos.asbestosProxy.channels.mhd.transactionSupport

class AssigningAuthorities {
    List<String> values = []
    boolean any = false
    private static oidPrefix = 'urn:oid:'

    AssigningAuthorities allowAny() {
        any = true
        this
    }

    AssigningAuthorities addAuthority(String value) {
        values << stripPrefix(value)
        this
    }

    private static String stripPrefix(String aa) {
        if (aa.startsWith(oidPrefix))
            aa = aa.substring(oidPrefix.size())
        aa
    }

    boolean check(String aa) {
        aa = stripPrefix(aa)
        any || values.contains(aa)
    }
}
