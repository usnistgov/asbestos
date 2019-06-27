package gov.nist.asbestos.testEngine;

import ca.uhn.fhir.context.FhirContext;
import gov.nist.asbestos.client.Base.ProxyBase;
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r4.hapi.ctx.IValidationSupport;
import org.hl7.fhir.r4.hapi.validation.PrePopulatedValidationSupport;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FhirPathTest {
    private static FhirContext fhirContext = ProxyBase.getFhirContext();
    private static IValidationSupport iValidationSupport;
    private static HapiWorkerContext hapiWorkerContext;
    private static FHIRPathEngine fhirPathEngine;

    @BeforeAll
    static void beforeAll() {
        iValidationSupport = new PrePopulatedValidationSupport();
        hapiWorkerContext = new HapiWorkerContext(fhirContext, iValidationSupport);
        fhirPathEngine = new FHIRPathEngine(hapiWorkerContext);
    }

    @Test
    void humanName() {
        HumanName humanName1 = new HumanName().setFamily("Smith").addGiven("George");
        List<Base> results = fhirPathEngine.evaluate(humanName1, "family = 'Smith' and given = 'George'");

        assertFalse(results.isEmpty());
        assertTrue(results.get(0) instanceof BooleanType);
        BooleanType bool = (BooleanType) results.get(0);
        assertTrue(bool.booleanValue());
    }

    @Test
    void patient() {
        HumanName humanName1 = new HumanName().setFamily("Smith").addGiven("George");
        Patient patient = new Patient();
        patient.addName(humanName1);

        List<Base> results = fhirPathEngine.evaluate(patient, "Patient.name.family = 'Smith' and name.given = 'George'");

        assertFalse(results.isEmpty());
        assertTrue(results.get(0) instanceof BooleanType);
        BooleanType bool = (BooleanType) results.get(0);
        assertTrue(bool.booleanValue());
    }

    @Test
    void bundle() {
        HumanName humanName1 = new HumanName().setFamily("Smith").addGiven("George");
        Patient patient1 = new Patient();
        patient1.addName(humanName1);

        HumanName humanName2 = new HumanName().setFamily("Smith").addGiven("Jay");
        Patient patient2 = new Patient();
        patient2.addName(humanName2);

        Bundle bundle = new Bundle();
        bundle.getEntry().add(new Bundle.BundleEntryComponent().setResource(patient1));
       // bundle.getEntry().add(new Bundle.BundleEntryComponent().setResource(patient2));

        assertTrue(asBoolean(fhirPathEngine.evaluate(bundle, "Patient.all(name.family = 'Smith')")));
    }

    private static boolean asBoolean(List<Base> results) {
        assertFalse(results.isEmpty());
        assertTrue(results.get(0) instanceof BooleanType);
        BooleanType bool = (BooleanType) results.get(0);
        return bool.booleanValue();
    }
}
