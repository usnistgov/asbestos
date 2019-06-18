package gov.nist.asbestos.mhd.translation.test.documentEntry;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.asbestos.asbestosProxySupport.Base.Base;
import gov.nist.asbestos.mhd.translation.Author;
import gov.nist.asbestos.simapi.validation.Val;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ClassificationType;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Practitioner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AuthorTest {
    private static Val val;
    private static FhirContext fhirContext;

    @BeforeAll
    static void beforeAll() {
        fhirContext = Base.getFhirContext();
    }

    @BeforeEach
    void beforeEach() {
        val = new Val();
    }

    @Test
    void practitionerSoloWithName() throws IOException {
        Practitioner practitioner1 = new Practitioner();
        HumanName humanName1 = new HumanName();
        humanName1.setFamily("Smith");
        practitioner1.addName(humanName1);


        Practitioner practitioner2 = new Practitioner();
        HumanName humanName2 = new HumanName();
        humanName2.setFamily("Jones");
        practitioner2.addName(humanName2);

        Practitioner practitioner3 = new Practitioner();
        HumanName humanName3 = new HumanName();
        humanName3.setFamily("Jones");
        practitioner3.addName(humanName3);

        String json1 = fhirContext.newJsonParser().encodeResourceToString(practitioner1);
        String json2 = fhirContext.newJsonParser().encodeResourceToString(practitioner2);
        String json3 = fhirContext.newJsonParser().encodeResourceToString(practitioner3);

        ObjectMapper objectMapper = new  ObjectMapper();
        JsonFactory jsonFactory = objectMapper.getFactory();

        JsonParser jsonParser1 = jsonFactory.createParser(json1);
        JsonNode node1 = objectMapper.readTree(jsonParser1);
        JsonParser jsonParser2 = jsonFactory.createParser(json2);
        JsonNode node2 = objectMapper.readTree(jsonParser2);
        JsonParser jsonParser3 = jsonFactory.createParser(json3);
        JsonNode node3 = objectMapper.readTree(jsonParser3);

        assertNotEquals(node1, node2);
        assertEquals(node2, node3);
    }

    @Test
    void withNameAndId() throws IOException {
        Practitioner practitioner1 = new Practitioner();
        HumanName humanName1 = new HumanName();
        humanName1.setFamily("Smith");
        humanName1.addGiven("George");
        practitioner1.addName(humanName1);
        Identifier identifier = new Identifier();
        identifier.setValue("43");
        practitioner1.addIdentifier(identifier);
        ContactPoint contactPoint = new ContactPoint();
        contactPoint.setValue("bob@go.com");
        contactPoint.setSystem(ContactPoint.ContactPointSystem.EMAIL);
        practitioner1.addTelecom(contactPoint);

        Author author = new Author();
        author.setVal(val);

        ClassificationType c = author.practitionerToClassification(practitioner1);
        Author author2 = new Author();
        author2.setVal(val);
        Practitioner practitioner2 = (Practitioner) author2.authorClassificationToContained(c).get(0);

        String json1 = fhirContext.newJsonParser().encodeResourceToString(practitioner1);
        String json2 = fhirContext.newJsonParser().encodeResourceToString(practitioner2);

        ObjectMapper objectMapper = new  ObjectMapper();
        JsonFactory jsonFactory = objectMapper.getFactory();
        JsonParser jsonParser1 = jsonFactory.createParser(json1);
        JsonNode node1 = objectMapper.readTree(jsonParser1);
        JsonParser jsonParser2 = jsonFactory.createParser(json2);
        JsonNode node2 = objectMapper.readTree(jsonParser2);

        assertEquals(node1, node2);
    }
}
