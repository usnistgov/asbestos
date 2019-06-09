package gov.nist.asbestos.asbestosProxySupport.Base;

import ca.uhn.fhir.context.FhirContext;


class Base {
    static private FhirContext ourCtx;

    public static FhirContext getFhirContext() {
        if (ourCtx == null)
            ourCtx = FhirContext.forR4();
        return ourCtx;
    }

//    private static hexChars = ('0'..'9') + ('a'..'f')
//    static boolean isUUID(String u) {
//        if (u.startsWith('urn:uuid:')) return true
//        try {
//            int total = 0
//
//            total += (0..7).sum { (hexChars.contains(u[it])) ? 0 : 1 }
//            total += (u[8] == '-') ? 0 : 1
//            total += (9..12).sum { (hexChars.contains(u[it])) ? 0 : 1 }
//            total += (u[13] == '-') ? 0 : 1
//            total += (14..17).sum { (hexChars.contains(u[it])) ? 0 : 1 }
//            total += (u[18] == '-') ? 0 : 1
//            total += (19..22).sum { (hexChars.contains(u[it])) ? 0 : 1 }
//            total += (u[23] == '-') ? 0 : 1
//            total += (24..35).sum { (hexChars.contains(u[it])) ? 0 : 1 }
//            return total == 0
//        } catch (Exception e) {
//            return false
//        }
//    }
}
