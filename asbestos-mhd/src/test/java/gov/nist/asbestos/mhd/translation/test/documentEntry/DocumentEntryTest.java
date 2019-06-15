package gov.nist.asbestos.mhd.translation.test.documentEntry;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.asbestos.asbestosProxySupport.Base.Base;
import gov.nist.asbestos.mhd.transactionSupport.ResourceWrapper;
import gov.nist.asbestos.mhd.translation.BundleToRegistryObjectList;
import gov.nist.asbestos.mhd.translation.DocumentEntryToDocumentReference;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValErrors;
import gov.nist.asbestos.simapi.validation.ValFactory;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class DocumentEntryTest {
    private static Val val;
    private static FhirContext fhirContext;
    private static ObjectMapper objectMapper;
    private static JsonFactory jsonFactory;

    @BeforeAll
    static void beforeAll() {
        fhirContext = Base.getFhirContext();
        objectMapper = new  ObjectMapper();
        jsonFactory = objectMapper.getFactory();
    }

    @BeforeEach
    void beforeEach() {
        val = new Val();
    }

    void run(DocumentReference documentReference) throws IOException {
        ResourceWrapper resource = new ResourceWrapper(documentReference);

        String json = fhirContext.newJsonParser().encodeResourceToString(documentReference);
        JsonParser jsonParser1 = jsonFactory.createParser(json);
        JsonNode node1 = objectMapper.readTree(jsonParser1);

        BundleToRegistryObjectList bundleToRegistryObjectList = new BundleToRegistryObjectList();
        bundleToRegistryObjectList.setVal(val);
        ExtrinsicObjectType extrinsicObjectType = bundleToRegistryObjectList.createExtrinsicObject(resource);

        DocumentEntryToDocumentReference documentEntryToDocumentReference = new DocumentEntryToDocumentReference();
        documentEntryToDocumentReference.setVal(val);
        DocumentReference documentReference1 = documentEntryToDocumentReference.getDocumentReference(extrinsicObjectType);

        String json2 = fhirContext.newJsonParser().encodeResourceToString(documentReference1);
        JsonParser jsonParser2 = jsonFactory.createParser(json2);
        JsonNode node2 = objectMapper.readTree(jsonParser2);

        assertEquals(node1, node2);
    }

    private DocumentReference getEmptyDR() {
        DocumentReference documentReference = new DocumentReference();
        Identifier idr = new Identifier();
        idr.setSystem("urn:ietf:rfc:3986");
        idr.setValue("ID1");
        documentReference.getIdentifier().add(idr);
        Attachment attachment = new Attachment();
        attachment.setContentType("application/octet-stream");
        documentReference.getContent().add(new DocumentReference.DocumentReferenceContentComponent());
        documentReference.getContent().get(0).setAttachment(attachment);
        return documentReference;
    }

    @Test
    void emptyRT() throws IOException {
        DocumentReference documentReference = getEmptyDR();

        run(documentReference);

        val.ignore("DocumentReference.masterIdentifier not present - declared by IHE to be [1..1]");

        if (val.hasErrors())
            fail(ValFactory.toJson(new ValErrors(val)));
    }

}
