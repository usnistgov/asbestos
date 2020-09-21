package gov.nist.asbestos.utilities;

import gov.nist.asbestos.client.Base.ParserBase;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

class ResourceHasMethodsFilterTest {

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
        // Enable this to see the jsonString in the Console
//         System.out.println(jsonString);
        keyList.forEach(s -> {
//            System.out.println(String.format("Checking %s", s));
            assert jsonString.contains(String.format("\"%s\"", s));
        });
//        System.out.println("ok.");
    }

    @Test
    void parseTestDocumentReference1() {

        DocumentReference dr;
        dr = new DocumentReference();
        dr.setStatus(Enumerations.DocumentReferenceStatus.fromCode("current"));
        dr.addAuthor(new Reference("http://example.com/fhir/Author/1"));

        testAtts(dr);

        String jsonString = ResourceHasMethodsFilter.toJson(dr);
        List<String> keyList = Arrays.asList("status", "author");
        checkKeys(keyList, jsonString);
    }


    @Test
    void parseTestDocumentReferenceExample() throws Exception {
        // Check keys
        // https://www.ihe.net/uploadedFiles/Documents/ITI/IHE_ITI_Suppl_MHD.pdf
        // Table 4.5.1.1-1: FHIR DocumentReference mapping to DocumentEntry
        List<String> keyList = Arrays.asList("masterIdentifier","identifier","status","docStatus","type","category","subject","date","author","authenticator","relatesTo","description","securityLabel","content","context");
        String jsonString = parseTestFile("documentReferenceExample.json");
        checkKeys(keyList, jsonString);

    }

    @Test
    void parseTestDocumentReferenceXdsExample() throws Exception {

        // Check keys
        // https://www.ihe.net/uploadedFiles/Documents/ITI/IHE_ITI_Suppl_MHD.pdf
        // Table 4.5.1.1-1: FHIR DocumentReference mapping to DocumentEntry
        List<String> keyList = Arrays.asList(/* not found: authenticator */ "masterIdentifier","identifier","status","type","category","subject","date","author","description","securityLabel","content","context");
        String jsonString = parseTestFile("documentReferenceXdsExample.json");
        checkKeys(keyList, jsonString);
    }

    @Test
    void parseTestDocumentManifestXdsExample() throws Exception {
        // Check keys
        // https://www.ihe.net/uploadedFiles/Documents/ITI/IHE_ITI_Suppl_MHD.pdf
        // Table 4.5.1.2-1: FHIR DocumentManifest mapping to SubmissionSet
        List<String> keyList = Arrays.asList(/* not found: "meta",*/ "masterIdentifier","identifier","status","type","subject","created","author","recipient","source","description");
        String jsonString = parseTestFile("documentManifestXdsExample.json");
        checkKeys(keyList, jsonString);
    }

    @Test
    void parseTestDocumentManifestFmAttachmentExample() throws Exception {
        // Check keys
        List<String> keyList = Arrays.asList("contained","identifier","status","created","recipient","content","related");
        String jsonString = parseTestFile("documentManifestFinancialManagementAttachmentExample.json");
        checkKeys(keyList, jsonString);
    }

    private String parseTestFile(String fileName) throws Exception {

       // Load resource
        BaseResource baseResource = null;
       File file = Paths.get(ResourceHasMethodsFilterTest.class.getResource("/").toURI()).resolve(fileName).toFile();
       if (file != null && file.exists()) {
           // Parse resource to model using FHIR Parser
            baseResource = ParserBase.parse(file);
       }

       assert  baseResource != null;

        // Parse using HasResourceMethods
        String jsonString = ResourceHasMethodsFilter.toJson(baseResource);

        assert  jsonString != null;

        return jsonString;
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

        String jsonString = ResourceHasMethodsFilter.toJson(patient1);
        List<String> keyList = Arrays.asList("id","name","given","family", "birthDate");
        checkKeys(keyList, jsonString);
    }
}
