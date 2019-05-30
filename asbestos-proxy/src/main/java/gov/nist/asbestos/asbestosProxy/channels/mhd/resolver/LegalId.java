package gov.nist.asbestos.asbestosProxy.channels.mhd.resolver;

public class LegalId {

    static boolean isLegal(String id) {
        boolean legal = id.length() <= 64;

        for (char c : id.toCharArray()) {
            if (!legal(c))
                legal = false;
        }

        return legal;
    }

    private static boolean legal(char c) {
        if ('a' <= c && c <= 'z') return true;
        if ('A' <= c && c <= 'Z') return true;
        if ('0' <= c && c <= '9') return true;
        if (c == '-') return true;
        if (c == '.') return true;
        return false;
    }

}
