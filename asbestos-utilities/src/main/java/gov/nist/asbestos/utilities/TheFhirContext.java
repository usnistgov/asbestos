package gov.nist.asbestos.utilities;

import ca.uhn.fhir.context.FhirContext;

public class TheFhirContext {
    static private FhirContext ourCtx = null;

    public static FhirContext get() {
        if (ourCtx == null)
            ourCtx = FhirContext.forR4();
        return ourCtx;
    }
}
