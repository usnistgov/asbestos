package gov.nist.asbestos.utilities;

import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

class ResourceHasMethodsTest {

    @BeforeAll
    static protected void setUp()  {
    }

    private void testAtts(DocumentReference dr) {
        if (dr.hasStatus()) {
            String status = dr.getStatus().toCode();
            assert status != null;
            System.out.println("Status: " + status);
        }

        if (dr.hasAuthor()) {
            List<Reference> authors = dr.getAuthor();
            for (Reference ref : authors) {
                assert ref != null;
                System.out.println("Author: " + ref.getReference());
            }
        }

    }

    private void checkKeys(List<String> keyList, String jsonString) {
        System.out.println(jsonString);
        keyList.forEach(s -> {assert jsonString.contains(String.format("\"%s\"", s));});
    }

    @Test
    void parseTestDocumentReference() {

        DocumentReference dr;
        dr = new DocumentReference();
        dr.setStatus(Enumerations.DocumentReferenceStatus.fromCode("current"));
        dr.addAuthor(new Reference("http://example.com/fhir/Author/1"));

        testAtts(dr);

        String jsonString = ResourceHasMethods.toJson(dr);
        List<String> keyList = Arrays.asList("status", "author");
        checkKeys(keyList, jsonString);
    }

    @Test
    void parseTestPatient() {
        Patient patient1;
        patient1 = new Patient();
        patient1.setGender(Enumerations.AdministrativeGender.UNKNOWN);
        HumanName humanName1 = new HumanName();
        humanName1.addGiven("Mickey");
        humanName1.setFamily("Mouse");
        patient1.setName(Arrays.asList(humanName1));
        patient1.setBirthDate(new Date(28, 0, 1));
        patient1.setId("PATIENT1_1928_MM");

        String jsonString = ResourceHasMethods.toJson(patient1);
        List<String> keyList = Arrays.asList("id","name","given","family", "birthDate");
        checkKeys(keyList, jsonString);
    }
}