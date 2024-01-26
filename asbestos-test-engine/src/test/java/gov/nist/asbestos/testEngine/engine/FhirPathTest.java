package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.Base.ParserBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FhirPathTest {
    private static FHIRPathEngine fhirPathEngine;

    @BeforeAll
    static void beforeAll() {
        fhirPathEngine = FhirPathEngineBuilder.build();
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

    private Bundle loadBundle(String path) {
        InputStream is = FhirPathTest.class.getResourceAsStream(path);
        IBaseResource resource = ParserBase.getFhirContext().newXmlParser().parseResource(is);
        assertTrue(resource instanceof Bundle);
        return (Bundle) resource;
    }

    @Test
    void manifestInBundle() {
        Bundle bundle = loadBundle("/pdb/request.xml");
        String match = FhirPathEngineBuilder.evalForString(bundle, "Bundle.entry.resource.where(is(FHIR.DocumentReference)).count() = 1");
        assertEquals("true", match);
    }

    @Test
    void manifestInBundle2() {
        Bundle bundle = loadBundle("/pdb/request.xml");
        String match = FhirPathEngineBuilder.evalForString(bundle, "Bundle.entry.where(fullUrl = 'http://localhost:9556/svc/fhir/DocumentReference/45').request.first().method.value = 'POST'");
        assertEquals("true", match);
    }

    @Test
    void manifestInBundle3() {
        Bundle bundle = loadBundle("/pdb/request.xml");
        String match = FhirPathEngineBuilder.evalForString(bundle, "Bundle.entry.resource.where(status = 'current').exists()");
        assertEquals("true", match);
    }


}
