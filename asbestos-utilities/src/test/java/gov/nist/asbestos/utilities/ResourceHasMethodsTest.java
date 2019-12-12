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

    static DocumentReference dr;
    static Patient patient1;

    @BeforeAll
    static protected void setUp()  {
        dr = new DocumentReference();
        dr.setStatus(Enumerations.DocumentReferenceStatus.fromCode("current"));
        dr.addAuthor(new Reference("http://example.com/fhir/Author/1"));


        patient1 = new Patient();
        patient1.setGender(Enumerations.AdministrativeGender.UNKNOWN);
        HumanName humanName1 = new HumanName();
        humanName1.addGiven("Mickey");
        humanName1.setFamily("Mouse");
        patient1.setName(Arrays.asList(humanName1));
        patient1.setBirthDate(new Date(28, 0, 1));
        patient1.setId("PATIENT1_1928_MM");
    }

    @Test
    public void testAtts() {
        if (dr.hasStatus()) {
            String status = dr.getStatus().toCode();
            System.out.println("Status: " + status);
        }

        if (dr.hasAuthor()) {
            List<Reference> authors = dr.getAuthor();
            for (Reference ref : authors) {
                System.out.println("Author: " + ref.getReference());
            }
        }

    }

    @Test
    void parse() {
        String jsonString = ResourceHasMethods.toJson(dr);
        System.out.println(jsonString);

        jsonString = ResourceHasMethods.toJson(patient1);
        assert jsonString.contains("\"id\"");
        assert jsonString.contains("\"name\"");
        assert jsonString.contains("\"given\"");
        assert jsonString.contains("\"family\"");
        assert jsonString.contains("\"birthDate\"");




        System.out.println(jsonString);


    }
}