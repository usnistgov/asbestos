import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r4.hapi.validation.PrePopulatedValidationSupport;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;

class FhirPathBugTest {

    @Test
    void test1() {
        FHIRPathEngine engine = new FHIRPathEngine(new HapiWorkerContext(FhirContext.forR4(), new PrePopulatedValidationSupport()));

        Bundle bundle = new Bundle();
        Bundle.BundleEntryComponent patient = bundle.addEntry();
        patient.setResource(new Patient());
        Bundle.BundleEntryComponent list = bundle.addEntry();
        list.setResource(new ListResource());

        boolean useUUID = false;
        String listUrl;
        String patientUrl;
        if (useUUID) {
            patientUrl = "urn:uuid:1.2.129.6.58.922.88336.1";
            listUrl = "urn:uuid:1.2.129.6.58.922.88336.2";
        } else {
            patientUrl = "http://example.com/fhir/Patient/1";
            listUrl = "http://example.com/fhir/List/1";
        }

        patient.setFullUrl(patientUrl);
        list.setFullUrl(listUrl);

        String quotedPatientUrl = "'" + patientUrl + "'";
        String path = "Bundle.entry.where(resource is Patient).fullUrl = " + quotedPatientUrl;

        Object o = engine.parse(path);

        List<Base> results = engine.evaluate(bundle, path);
        assertFalse(results.isEmpty());
    }
}
