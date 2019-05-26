package gov.nist.asbestos.asbestosProxy.channels.mhd.resolver

class LegalId {

    static legalChars = ('a'..'z') + ('A'..'Z') + ('0'..'9') + '-' + '.'

    static boolean isLegal(String id) {
        boolean legal = id.size() <= 64

        id.toCharArray().each {
            if (!legalChars.contains(new String(it)))
                legal = false
        }

        legal
    }
}
