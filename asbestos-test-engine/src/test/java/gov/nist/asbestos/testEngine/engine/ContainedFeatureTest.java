package gov.nist.asbestos.testEngine.engine;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;

import javax.lang.model.type.ReferenceType;

class ContainedFeatureTest {

    @Test
    void theTest() {
        Organization org = new Organization();
        org.setId("#localOrganization");
        org.getNameElement().setValue("Contained Test Organization");

// Create a patient
        Patient patient = new Patient();
        patient.setId("Patient/1333");
        patient.addIdentifier().setSystem("urn:mrns").setValue("253345");

// Set the reference, and manually add the contained resource
        patient.getManagingOrganization().setReference("#localOrganization");
        patient.addContained(org);

        String encoded = FhirContext.forR4().newXmlParser().setPrettyPrint(true).encodeResourceToString(patient);
        System.out.println(encoded);
    }

    @Test
    void theExtension() {
        Organization org = new Organization();
        org.getNameElement().setValue("Contained Test Organization");

        Patient patient = new Patient();
        patient.setId("Patient/1333");
        patient.addIdentifier().setSystem("urn:mrns").setValue("253345");

        Extension extension = new Extension("http://example.com", new Reference(org));
        patient.addModifierExtension(extension);




        String encoded = FhirContext.forR4().newXmlParser().setPrettyPrint(true).encodeResourceToString(patient);
        System.out.println(encoded);
    }
}
