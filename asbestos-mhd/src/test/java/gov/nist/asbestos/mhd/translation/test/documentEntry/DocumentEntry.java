package gov.nist.asbestos.mhd.translation.test.documentEntry;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.events.Event;
import gov.nist.asbestos.client.events.ITask;
import gov.nist.asbestos.client.log.SimStore;
import gov.nist.asbestos.client.resolver.ResourceCacheMgr;
import gov.nist.asbestos.client.resolver.ResourceMgr;
import gov.nist.asbestos.mhd.transactionSupport.AssigningAuthorities;
import gov.nist.asbestos.mhd.transactionSupport.CodeTranslator;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.mhd.transforms.BundleToRegistryObjectList;
import gov.nist.asbestos.mhd.translation.ContainedIdAllocator;
import gov.nist.asbestos.mhd.transforms.DocumentEntryToDocumentReference;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.simapi.simCommon.SimId;
import gov.nist.asbestos.simapi.tk.installation.Installation;
import gov.nist.asbestos.simapi.validation.*;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

// TODO relatesTo (no tests, no impl)
// TODO content.attachment.data
// TODO referenceIdList
@Disabled("The Test suffix from the class name was removed so that it would be ignored until the errors are fixed.")
class DocumentEntry {
    private static Val val;
    private static FhirContext fhirContext;
    private static ObjectMapper objectMapper;
    private static JsonFactory jsonFactory;
    private static File externalCache;
    private static ResourceMgr rMgr;
    private static ResourceCacheMgr resourceCacheMgr;
    private static FhirClient fhirClient;

    @BeforeAll
    static void beforeAll() throws URISyntaxException {
        externalCache = Paths.get(DocumentEntry.class.getResource("/external_cache/findme.txt").toURI()).getParent().toFile();
        resourceCacheMgr = new ResourceCacheMgr(externalCache);
        fhirContext = ProxyBase.getFhirContext();
        objectMapper = new  ObjectMapper();
        jsonFactory = objectMapper.getFactory();
        Installation.instance().setExternalCache(externalCache);
    }

    @BeforeEach
    void beforeEach() {
        val = new Val();
        fhirClient = new FhirClient()
            .setResourceCacheMgr(new ResourceCacheMgr(externalCache));
    }

    private static File getCodesFile() {
        return Installation.instance().getCodesFile("default");
    }

    void run(DocumentReference documentReference, DocumentReference expected, boolean checkForErrors) throws IOException, JAXBException {
        ResourceWrapper resource = new ResourceWrapper(documentReference);
        if (expected == null)
            expected = documentReference;

        CodeTranslator codeTranslator = new CodeTranslator(getCodesFile());

        String json = fhirContext.newJsonParser().encodeResourceToString(expected);
        JsonParser jsonParser1 = jsonFactory.createParser(json);
        JsonNode node1 = objectMapper.readTree(jsonParser1);

        ChannelConfig channelConfig = new ChannelConfig()
                .setChannelId("test")
                .setChannelType("mhd")
                .setActorType("fhir")
                .setEnvironment("default")
                .setFhirBase("http://localhost:8877/fhir/fhir")
                .setTestSession("default");

        SimId simId = SimId.buildFromRawId("default" + "__" + "test").withActorType("fhir").withEnvironment("default");
        SimStore simStore = new SimStore(externalCache, simId);
        simStore.create(channelConfig);
        simStore.setResource("Bundle");
        Event event = simStore.newEvent();

        rMgr = new ResourceMgr();
        rMgr.setVal(val);
        rMgr.setFhirClient(fhirClient);
        ITask task = event.newTask();
        rMgr.setTask(task);

        BundleToRegistryObjectList bundleToRegistryObjectList = new BundleToRegistryObjectList();
        bundleToRegistryObjectList.setVal(val);
        bundleToRegistryObjectList.setCodeTranslator(codeTranslator);
        bundleToRegistryObjectList.setResourceMgr(rMgr);
        bundleToRegistryObjectList.setAssigningAuthorities(AssigningAuthorities.allowAny());

        ContainedIdAllocator containedIdAllocator = new ContainedIdAllocator();

        rMgr = new ResourceMgr();
        rMgr.setVal(val);
        rMgr.setFhirClient(fhirClient);

        ExtrinsicObjectType extrinsicObjectType = bundleToRegistryObjectList.createExtrinsicObject(resource, new ValE(val));

        DocumentEntryToDocumentReference documentEntryToDocumentReference = new DocumentEntryToDocumentReference();
        documentEntryToDocumentReference.setVal(val);
        documentEntryToDocumentReference.setResourceCacheMgr(resourceCacheMgr);
        documentEntryToDocumentReference.setCodeTranslator(codeTranslator);

        containedIdAllocator = new ContainedIdAllocator();
        documentEntryToDocumentReference.setContainedIdAllocator(containedIdAllocator);
        DocumentReference documentReference1 = documentEntryToDocumentReference.getDocumentReference(extrinsicObjectType);

        String json2 = fhirContext.newJsonParser().encodeResourceToString(documentReference1);
        JsonParser jsonParser2 = jsonFactory.createParser(json2);
        JsonNode node2 = objectMapper.readTree(jsonParser2);

        if (checkForErrors && val.hasErrors())
            fail(ValFactory.toJson(new ValErrors(val)));
        if (checkForErrors && val.hasWarnings())
            fail(ValFactory.toJson(new ValWarnings(val)));

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
    void emptyDR() throws IOException, JAXBException {
        DocumentReference documentReference = getEmptyDR();

        run(documentReference, null, false);

        if (!val.ignore("masterIdentifier not present"))
            fail("Error did not occur");
        if ( !val.ignore("subject not present or has no reference"))
            fail("Error did not occur");

        if (val.hasErrors())
            fail(ValFactory.toJson(new ValErrors(val)));
    }

    private DocumentReference withMasterIdentifier() {
        DocumentReference documentReference = getEmptyDR();

        Identifier idr = new Identifier();
        idr.setSystem("urn:ietf:rfc:3986");
        idr.setValue("1.2.3.4");
        documentReference.setMasterIdentifier(idr);

        return documentReference;
    }

    @Test
    void masterIdentifier() throws IOException, JAXBException {
        DocumentReference documentReference = withMasterIdentifier();

        run(documentReference, null, false);

        if (!val.ignore("subject not present or has no reference"))
            fail("Error did not occur");

        if (val.hasErrors())
            fail(ValFactory.toJson(new ValErrors(val)));
    }

    private DocumentReference withMasterIdentifierAndExtension() {
        DocumentReference documentReference = getEmptyDR();

        Identifier idr = new Identifier();
        idr.setSystem("urn:ietf:rfc:3986");
        idr.setValue("1.2.3.4^123");
        documentReference.setMasterIdentifier(idr);

        return documentReference;
    }

    @Test
    void masterIdentifierAndExtension() throws IOException, JAXBException {
        DocumentReference documentReference = withMasterIdentifierAndExtension();

        run(documentReference, null, false);

        if (!val.ignore("subject not present or has no reference"))
            fail("Error did not occur");

        if (val.hasErrors())
            fail(ValFactory.toJson(new ValErrors(val)));
    }

    private DocumentReference withUnOfficialEntryUUID() {
        DocumentReference documentReference = withMasterIdentifier();

        Identifier idr;
        // Unofficial entryUUID - should be marked Official
        idr = new Identifier();
        idr.setSystem("urn:ietf:rfc:3986");
        idr.setValue("58a6f841-87b3-4a3e-92fd-a8ffeff98590");
        documentReference.getIdentifier().add(idr);

        return documentReference;
    }

    @Test
    void unOfficialEntryUUID() throws IOException, JAXBException {
        DocumentReference documentReference = withUnOfficialEntryUUID();
        documentReference.getIdentifier().remove(0);  // ID1 not appropriate (was placeholder for ID)
        DocumentReference expected = withUnOfficialEntryUUID();
        expected.getIdentifier().remove(1);  // second (almost official) will not be processed

        run(documentReference, expected, false);

        if (!val.ignore("DocumentReference.identifier is UUID but not labeled as official"))
            fail("Error did not occur");
        if (!val.ignore("subject not present or has no reference"))
            fail("Error did not occur");

        if (val.hasErrors())
            fail(ValFactory.toJson(new ValErrors(val)));
    }

    private DocumentReference withOfficialEntryUUID() {
        DocumentReference documentReference = withMasterIdentifier();

        Identifier idr;
        // Unofficial entryUUID - should be marked Official
        idr = new Identifier();
        idr.setSystem("urn:ietf:rfc:3986");
        idr.setValue("58a6f841-87b3-4a3e-92fd-a8ffeff98590");
        idr.setUse(Identifier.IdentifierUse.OFFICIAL);
        documentReference.getIdentifier().add(idr);

        return documentReference;
    }

    @Test
    void officialEntryUUID() throws IOException, JAXBException {
        DocumentReference documentReference = withOfficialEntryUUID();
        DocumentReference expected = withOfficialEntryUUID();
        expected.getIdentifier().remove(0);  // ID1 not appropriate (was placeholder for ID)

        run(documentReference, expected, false);

        if (!val.ignore("subject not present or has no reference"))
            fail("Error did not occur");

        if (val.hasErrors())
            fail(ValFactory.toJson(new ValErrors(val)));

        System.out.println(ValFactory.toJson(val));
    }


    private DocumentReference withStatus() {
        DocumentReference documentReference = withOfficialEntryUUID();

        documentReference.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);

        return documentReference;
    }

    private List<DocumentReference> withStatusAndExpected() {
        List<DocumentReference> x = new ArrayList<>();
        DocumentReference original = withStatus();
        x.add(original);

        DocumentReference expected = withStatus();
        expected.getIdentifier().remove(0);  // ID1 not appropriate (was placeholder for ID)
        x.add(expected);
        return x;
    }

    @Test
    void status() throws IOException, JAXBException {
        List<DocumentReference> x = withStatusAndExpected();
        DocumentReference documentReference = x.get(0);
        DocumentReference expected = x.get(1);

        run(documentReference, expected, false);

        if (!val.ignore("subject not present or has no reference"))
            fail("Error did not occur");

        if (val.hasErrors())
            fail(ValFactory.toJson(new ValErrors(val)));

    }

    private DocumentReference withTypeCode() {
        DocumentReference documentReference = withOfficialEntryUUID();

        CodeableConcept typeCode = new CodeableConcept().addCoding(new Coding()
                .setCode("11506-3")
                .setSystem("http://loinc.org")
                .setDisplay("Custom Progress note"));
        documentReference.setType(typeCode);

        return documentReference;
    }

    private List<DocumentReference> withTypeCodeAndExpected() {
        List<DocumentReference> x = new ArrayList<>();
        DocumentReference original = withTypeCode();
        x.add(original);

        DocumentReference expected = withTypeCode();
        expected.getIdentifier().remove(0);  // ID1 not appropriate (was placeholder for ID)
        x.add(expected);
        return x;
    }

    @Test
    void typeCode() throws IOException, JAXBException {
        List<DocumentReference> x = withTypeCodeAndExpected();
        DocumentReference documentReference = x.get(0);
        DocumentReference expected = x.get(1);

        run(documentReference, expected, false);

        if (!val.ignore("subject not present or has no reference"))
            fail("Error did not occur");

        if (val.hasErrors())
            fail(ValFactory.toJson(new ValErrors(val)));
    }

    private DocumentReference withTypeCodeNoDisplay() {
        DocumentReference documentReference = withOfficialEntryUUID();

        CodeableConcept typeCode = new CodeableConcept().addCoding(new Coding()
                .setCode("11506-3")
                .setSystem("http://loinc.org"));
        documentReference.setType(typeCode);

        return documentReference;
    }

    private List<DocumentReference> withTypeCodeAndExpectedNoDisplay() {
        List<DocumentReference> x = new ArrayList<>();
        DocumentReference original = withTypeCodeNoDisplay();
        x.add(original);

        DocumentReference expected = withTypeCodeNoDisplay();
        expected.getType().getCoding().get(0).setDisplay("Progress note");
        expected.getIdentifier().remove(0);  // ID1 not appropriate (was placeholder for ID)
        x.add(expected);
        return x;
    }

    @Test
    void typeCodeNoDisplay() throws IOException, JAXBException {
        List<DocumentReference> x = withTypeCodeAndExpectedNoDisplay();
        DocumentReference documentReference = x.get(0);
        DocumentReference expected = x.get(1);

        run(documentReference, expected, false);

        if (!val.ignore("subject not present or has no reference"))
            fail("Error did not occur");

        if (val.hasErrors())
            fail(ValFactory.toJson(new ValErrors(val)));
    }

    private DocumentReference withClassCode() {
        DocumentReference documentReference = withOfficialEntryUUID();

        CodeableConcept classCode = new CodeableConcept().addCoding(new Coding()
                .setCode("REPORTS")
                .setSystem("urn:oid:1.3.6.1.4.1.19376.1.2.6.1")
                .setDisplay("Reports"));
        documentReference.setCategory(Collections.singletonList(classCode));

        return documentReference;
    }

    private List<DocumentReference> withClassCodeAndExpected() {
        List<DocumentReference> x = new ArrayList<>();
        DocumentReference original = withClassCode();
        x.add(original);

        DocumentReference expected = withClassCode();
        expected.getIdentifier().remove(0);  // ID1 not appropriate (was placeholder for ID)
        x.add(expected);
        return x;
    }

    @Test
    void classCode() throws IOException, JAXBException {
        List<DocumentReference> x = withClassCodeAndExpected();
        DocumentReference documentReference = x.get(0);
        DocumentReference expected = x.get(1);

        run(documentReference, expected, false);

        if (!val.ignore("subject not present or has no reference"))
            fail("Error did not occur");

        if (val.hasErrors())
            fail(ValFactory.toJson(new ValErrors(val)));
    }

    private DocumentReference withPatient() {
        DocumentReference documentReference = withOfficialEntryUUID();

        documentReference.setSubject(new Reference().setReference("http://localhost:8080/fhir/Patient/a2"));

        return documentReference;
    }

    private List<DocumentReference> withPatientAndExpected() {
        List<DocumentReference> x = new ArrayList<>();
        DocumentReference original = withPatient();
        x.add(original);

        DocumentReference expected = withPatient();
        expected.getIdentifier().remove(0);  // ID1 not appropriate (was placeholder for ID)
        x.add(expected);
        return x;
    }

    @Test
    void patient() throws IOException, JAXBException {
        List<DocumentReference> x = withPatientAndExpected();
        DocumentReference documentReference = x.get(0);
        DocumentReference expected = x.get(1);

        run(documentReference, expected, true);
    }

    private DocumentReference withLegalAuthenticator() {
        DocumentReference documentReference = withOfficialEntryUUID();

        Practitioner practitioner = new Practitioner();
        practitioner.addName().setFamily("Flintstone").addGiven("Fred");
        practitioner.setId("#practitioner1");

        documentReference.addContained(practitioner);
        documentReference.setAuthenticator(new Reference().setReference("#practitioner1"));

        return documentReference;
    }

    private List<DocumentReference> withLegalAuthenticatorAndExpected() {
        List<DocumentReference> x = new ArrayList<>();
        DocumentReference original = withLegalAuthenticator();
        x.add(original);

        DocumentReference expected = withLegalAuthenticator();
        expected.getIdentifier().remove(0);  // ID1 not appropriate (was placeholder for ID)
        x.add(expected);
        return x;
    }

    @Test
    void legalAuthenticator() throws IOException, JAXBException {
        List<DocumentReference> x = withLegalAuthenticatorAndExpected();
        DocumentReference documentReference = x.get(0);
        DocumentReference expected = x.get(1);

        run(documentReference, expected, false);

        if (!val.ignore("subject not present or has no reference"))
            fail("Error did not occur");

        if (val.hasErrors())
            fail(ValFactory.toJson(new ValErrors(val)));
    }

    private DocumentReference withAuthorPractitioner() {
        DocumentReference documentReference = withOfficialEntryUUID();

        Practitioner practitioner = new Practitioner();
        practitioner.addName().setFamily("Jones").addGiven("Fred");
        practitioner.setId("#practitioner1");

        documentReference.addContained(practitioner);
        documentReference.addAuthor(new Reference().setReference("#practitioner1"));

        return documentReference;
    }

    private List<DocumentReference> withAuthorPractitionerAndExpected() {
        List<DocumentReference> x = new ArrayList<>();
        DocumentReference original = withAuthorPractitioner();
        x.add(original);

        DocumentReference expected = withAuthorPractitioner();
        expected.getIdentifier().remove(0);  // ID1 not appropriate (was placeholder for ID)
        x.add(expected);
        return x;
    }

    @Test
    void authorPractitioner() throws IOException, JAXBException {
        List<DocumentReference> x = withAuthorPractitionerAndExpected();
        DocumentReference documentReference = x.get(0);
        DocumentReference expected = x.get(1);

        run(documentReference, expected, false);

        if (!val.ignore("subject not present or has no reference"))
            fail("Error did not occur");

        if (val.hasErrors())
            fail(ValFactory.toJson(new ValErrors(val)));
    }

    private DocumentReference withAuthorOrganization() {
        DocumentReference documentReference = withOfficialEntryUUID();

        Organization organization = new Organization();
        organization.setName("Wall Street");
        ContactPoint contactPoint = new ContactPoint();
        contactPoint.setSystem(ContactPoint.ContactPointSystem.EMAIL);
        contactPoint.setValue("444-789-6543");
        organization.setId("#organization1");

        documentReference.addContained(organization);
        documentReference.addAuthor(new Reference().setReference("#organization1"));

        return documentReference;
    }

    private List<DocumentReference> withAuthorOrganizationAndExpected() {
        List<DocumentReference> x = new ArrayList<>();
        DocumentReference original = withAuthorOrganization();
        x.add(original);

        DocumentReference expected = withAuthorOrganization();
        expected.getIdentifier().remove(0);  // ID1 not appropriate (was placeholder for ID)
        x.add(expected);
        return x;
    }

    @Test
    void authorOrganization() throws IOException, JAXBException {
        List<DocumentReference> x = withAuthorOrganizationAndExpected();
        DocumentReference documentReference = x.get(0);
        DocumentReference expected = x.get(1);

        run(documentReference, expected, false);

        if (!val.ignore("subject not present or has no reference"))
            fail("Error did not occur");

        if (val.hasErrors())
            fail(ValFactory.toJson(new ValErrors(val)));
    }



    private DocumentReference with2AuthorPractitioner() {
        DocumentReference documentReference = withOfficialEntryUUID();

        Practitioner practitioner = new Practitioner();
        practitioner.addName().setFamily("Flintstone").addGiven("Fred");
        practitioner.setId("#practitioner1");

        Practitioner practitioner2 = new Practitioner();
        practitioner2.addName().setFamily("Rubble").addGiven("Barney");
        practitioner2.setId("#practitioner2");

        documentReference.addContained(practitioner);
        documentReference.addContained(practitioner2);
        documentReference.addAuthor(new Reference().setReference("#practitioner1"));
        documentReference.addAuthor(new Reference().setReference("#practitioner2"));

        return documentReference;
    }

    private List<DocumentReference> with2AuthorPractitionerAndExpected() {
        List<DocumentReference> x = new ArrayList<>();
        DocumentReference original = with2AuthorPractitioner();
        x.add(original);

        DocumentReference expected = with2AuthorPractitioner();
        expected.getIdentifier().remove(0);  // ID1 not appropriate (was placeholder for ID)
        x.add(expected);
        return x;
    }

    @Test
    void author2Practitioner() throws IOException, JAXBException {
        List<DocumentReference> x = with2AuthorPractitionerAndExpected();
        DocumentReference documentReference = x.get(0);
        DocumentReference expected = x.get(1);

        run(documentReference, expected, false);

        if (!val.ignore("subject not present or has no reference"))
            fail("Error did not occur");

        if (val.hasErrors())
            fail(ValFactory.toJson(new ValErrors(val)));
    }

    private DocumentReference withAuthorPractitionerAndTelecom() {
        DocumentReference documentReference = withOfficialEntryUUID();

        Practitioner practitioner = new Practitioner();
        practitioner.addName().setFamily("Jones").addGiven("Fred");
        practitioner.setId("#practitioner1");
        ContactPoint contactPoint = new ContactPoint();
        contactPoint.setSystem(ContactPoint.ContactPointSystem.EMAIL);
        contactPoint.setValue("301-777-7766");
        practitioner.addTelecom(contactPoint);

        documentReference.addContained(practitioner);
        documentReference.addAuthor(new Reference().setReference("#practitioner1"));

        return documentReference;
    }

    private List<DocumentReference> withAuthorPractitionerAndTelecomAndExpected() {
        List<DocumentReference> x = new ArrayList<>();
        DocumentReference original = withAuthorPractitionerAndTelecom();
        x.add(original);

        DocumentReference expected = withAuthorPractitionerAndTelecom();
        expected.getIdentifier().remove(0);  // ID1 not appropriate (was placeholder for ID)
        x.add(expected);
        return x;
    }

    @Test
    void authorPractitionerAndTelecom() throws IOException, JAXBException {
        List<DocumentReference> x = withAuthorPractitionerAndTelecomAndExpected();
        DocumentReference documentReference = x.get(0);
        DocumentReference expected = x.get(1);

        run(documentReference, expected, false);

        if (!val.ignore("subject not present or has no reference"))
            fail("Error did not occur");

        if (val.hasErrors())
            fail(ValFactory.toJson(new ValErrors(val)));
    }


    private DocumentReference withAuthorPractitionerRole() {
        DocumentReference documentReference = withOfficialEntryUUID();

        Practitioner practitioner = new Practitioner();
        practitioner.addName().setFamily("Jones").addGiven("Fred");
        practitioner.setId("#practitioner1");
        documentReference.addContained(practitioner);

        PractitionerRole practitionerRole = new PractitionerRole();
        practitionerRole.getCode().add(new CodeableConcept().addCoding(new Coding().setSystem("http://snomed.info/sct").setCode("8724009")));
        practitionerRole.setId("#practitionerrole1");
        practitionerRole.setPractitioner(new Reference().setReference("#practitioner1"));
        documentReference.addContained(practitionerRole);

        documentReference.addAuthor(new Reference().setReference("#practitionerrole1"));

        return documentReference;
    }

    private List<DocumentReference> withAuthorPractitionerRoleAndExpected() {
        List<DocumentReference> x = new ArrayList<>();
        DocumentReference original = withAuthorPractitionerRole();
        x.add(original);

        DocumentReference expected = withAuthorPractitionerRole();
        expected.getIdentifier().remove(0);  // ID1 not appropriate (was placeholder for ID)
        x.add(expected);
        return x;
    }

    @Test
    void authorPractitionerRole() throws IOException, JAXBException {
        List<DocumentReference> x = withAuthorPractitionerRoleAndExpected();
        DocumentReference documentReference = x.get(0);
        DocumentReference expected = x.get(1);

        run(documentReference, expected, false);

        if (!val.ignore("subject not present or has no reference"))
            fail("Error did not occur");

        if (val.hasErrors())
            fail(ValFactory.toJson(new ValErrors(val)));
    }

    private DocumentReference withDescription() {
        DocumentReference documentReference = withOfficialEntryUUID();

        documentReference.setDescription("Hello World!");

        return documentReference;
    }

    private List<DocumentReference> withDescriptionAndExpected() {
        List<DocumentReference> x = new ArrayList<>();
        DocumentReference original = withDescription();
        x.add(original);

        DocumentReference expected = withDescription();
        expected.getIdentifier().remove(0);  // ID1 not appropriate (was placeholder for ID)
        x.add(expected);
        return x;
    }

    @Test
    void description() throws IOException, JAXBException {
        List<DocumentReference> x = withDescriptionAndExpected();
        DocumentReference documentReference = x.get(0);
        DocumentReference expected = x.get(1);

        run(documentReference, expected, false);

        if (!val.ignore("subject not present or has no reference"))
            fail("Error did not occur");

        if (val.hasErrors())
            fail(ValFactory.toJson(new ValErrors(val)));
    }

    private DocumentReference withSecurityLabel() {
        DocumentReference documentReference = withOfficialEntryUUID();

        documentReference.addSecurityLabel(new CodeableConcept()
                .addCoding(new Coding()
                        .setCode("L")
                        .setSystem("http://terminology.hl7.org/CodeSystem/v3-Confidentiality")
                        .setDisplay("low")));

        return documentReference;
    }

    private List<DocumentReference> withSecurityLabelAndExpected() {
        List<DocumentReference> x = new ArrayList<>();
        DocumentReference original = withSecurityLabel();
        x.add(original);

        DocumentReference expected = withSecurityLabel();
        expected.getIdentifier().remove(0);  // ID1 not appropriate (was placeholder for ID)
        x.add(expected);
        return x;
    }

    @Test
    void securityLabel() throws IOException, JAXBException {
        List<DocumentReference> x = withSecurityLabelAndExpected();
        DocumentReference documentReference = x.get(0);
        DocumentReference expected = x.get(1);

        run(documentReference, expected, false);

        if ( !val.ignore("subject not present or has no reference"))
            fail("Error did not occur");

        if (val.hasErrors())
            fail(ValFactory.toJson(new ValErrors(val)));
    }

    private DocumentReference withContentType() {
        DocumentReference documentReference = withOfficialEntryUUID();

        DocumentReference.DocumentReferenceContentComponent content = documentReference.getContent().get(0);
        Attachment attachment = content.getAttachment();
        attachment.setContentType("text/plain");
        attachment.setLanguage("en-us");
        attachment.setTitle("comments");
        attachment.setCreation(new Date());
        content.setFormat(new Coding()
                .setCode("1.2.840.10008.5.1.4.1.1.88.59")
                .setSystem("http://dicom.nema.org/resources/ontology/DCM")
                .setDisplay("Key Object Selection Document"));

        return documentReference;
    }

    private List<DocumentReference> withContentTypeAndExpected() {
        List<DocumentReference> x = new ArrayList<>();
        DocumentReference original = withContentType();
        x.add(original);

        DocumentReference expected = withContentType();
        expected.getIdentifier().remove(0);  // ID1 not appropriate (was placeholder for ID)
        x.add(expected);
        expected.getContent().get(0).getAttachment().setCreation(original.getContent().get(0).getAttachment().getCreation());
        return x;
    }

    @Test
    void contentType() throws IOException, JAXBException {
        List<DocumentReference> x = withContentTypeAndExpected();
        DocumentReference documentReference = x.get(0);
        DocumentReference expected = x.get(1);

        run(documentReference, expected, false);

        if ( !val.ignore("subject not present or has no reference"))
            fail("Error did not occur");

        if (val.hasErrors())
            fail(ValFactory.toJson(new ValErrors(val)));
    }
}
