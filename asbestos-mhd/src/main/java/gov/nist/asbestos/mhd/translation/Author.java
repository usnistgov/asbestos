package gov.nist.asbestos.mhd.translation;

import gov.nist.asbestos.asbestosProxySupport.Base.IVal;
import gov.nist.asbestos.simapi.validation.Val;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ClassificationType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;

public class Author implements IVal {
    private Val val = null;
    private List<AuthorPerson> authorPersons = new ArrayList<>();  // zero or one
    private List<AuthorInstitution> authorInstitutions = new ArrayList<>(); // zero or more
    private List<AuthorRole> authorRoles = new ArrayList<>(); // zero or more
    private List<AuthorSpecialty> authorSpecialtys = new ArrayList<>(); // zero or more
    private List<AuthorTelecommunication> authorTelecommunications = new ArrayList<>(); // zero or more

    public ClassificationType practitionerToClassification(Practitioner practitioner) {
        if (practitioner.hasName()) {
            for (HumanName humanName : practitioner.getName()) {
                AuthorPerson authorPerson = new AuthorPerson();
                if (humanName.hasFamily())
                    authorPerson.familyName = humanName.getFamily();
                if (humanName.hasGiven())
                    authorPerson.givenName = humanName.getGiven().get(0).getValue();
                authorPersons.add(authorPerson);
            }
        }
        for (Identifier identifier : practitioner.getIdentifier()) {
            if (identifier.hasValue()) {
                AuthorPerson authorPerson = authorPersons.isEmpty() ? new AuthorPerson() : authorPersons.get(0);
                authorPerson.id = identifier.getValue();
            }
        }
        if (practitioner.hasTelecom()) {
            for (ContactPoint contactPoint : practitioner.getTelecom()) {
                AuthorTelecommunication authorTelecommunication = new AuthorTelecommunication();
                authorTelecommunication.telecomAddr = contactPoint.getValue();
                if (contactPoint.getSystem() == ContactPoint.ContactPointSystem.EMAIL)
                    authorTelecommunication.setInternet();
                authorTelecommunications.add(authorTelecommunication);
            }
        }

        boolean hasValues = !(authorPersons.isEmpty() && authorInstitutions.isEmpty() && authorRoles.isEmpty() && authorSpecialtys.isEmpty() && authorTelecommunications.isEmpty());
        if (hasValues) {
            ClassificationType c = new ClassificationType();
            c.setNodeRepresentation("");

            if (!authorPersons.isEmpty()) {
                AuthorPerson authorPerson = authorPersons.get(0);
                SlotType1 slot = Slot.makeSlot("authorPerson", authorPerson.toString());
                c.getSlot().add(slot);
            }
            for (AuthorTelecommunication authorTelecommunication : authorTelecommunications) {
                SlotType1 slot = Slot.makeSlot("authorTelecommunication", authorTelecommunication.toString());
                c.getSlot().add(slot);
            }
            for (AuthorRole authorRole : authorRoles) {
                if (authorRole.hasCodeAndSystem()) {
                    SlotType1 slot = Slot.makeSlot("authorRole", authorRole.getCodeAndSystem());
                    c.getSlot().add(slot);
                }
            }
            c.setNodeRepresentation("");
            c.setObjectType("urn:oasis:names:tc:ebxml-regrep:ObjectType:RegistryObject:Classification");
            return c;
        }
        return null;
    }

    public List<Resource> authorClassificationToContained(ClassificationType c) {
        Practitioner practitioner = new Practitioner();
        practitioner.setId(ContainedIdAllocator.newId(Practitioner.class));
        List<Resource> contained = new ArrayList<>();
        contained.add(practitioner);
        for (SlotType1 slot : c.getSlot()) {
            String name = slot.getName();
            for (String value  : slot.getValueList().getValue()) {
                if ("authorPerson".equals(name)) {
                    authorPersons.add((AuthorPerson) new AuthorPerson().setValue(value, val));
                }
                else if ("authorInstitution".equals(name)) {
                    authorInstitutions.add((AuthorInstitution) new AuthorInstitution().setValue(value, val));
                }
                else if ("authorRole".equals(name)) {
                    AuthorRole authorRole = new AuthorRole();
                    authorRole.setCodeAndSystem(value);
                    authorRoles.add(authorRole);
                }
                else if ("authorSpecialty".equals(name)) {
                    authorSpecialtys.add((AuthorSpecialty) new AuthorSpecialty().setValue(value, val));
                }
                else if ("authorTelecommunication".equals(name)) {
                    authorTelecommunications.add((AuthorTelecommunication) new AuthorTelecommunication().setValue(value, val));
                }
            }
        }
        if (!authorRoles.isEmpty()) {
            PractitionerRole practitionerRole = new PractitionerRole();
            practitionerRole.setId(ContainedIdAllocator.newId(PractitionerRole.class));
            for (AuthorRole authorRole : authorRoles) {
                if (authorRole.hasCodeableConcept() || authorRole.hasCodeAndSystem()) {
                    CodeableConcept cc = authorRole.getCodeableConcept();
                    practitionerRole.getCode().add(cc);
                }
            }
            practitionerRole.setPractitioner(new Reference().setReference(practitioner.getId()));
            contained.add(practitionerRole);
        }
        for (AuthorPerson authorPerson : authorPersons) {
            if (!authorPerson.id.equals("")) {
                Identifier identifier = new Identifier();
                identifier.setValue(authorPerson.id);
                practitioner.addIdentifier(identifier);
            }
        }
        for (AuthorPerson authorPerson : authorPersons) {
            if (!authorPerson.familyName.equals("")) {
                HumanName humanName = new HumanName();
                humanName.setFamily(authorPerson.familyName);
                if (authorPerson.givenName != null)
                    humanName.addGiven(authorPerson.givenName);
                practitioner.addName(humanName);
            }
        }
        for (AuthorTelecommunication authorTelecommunication : authorTelecommunications) {
            if (!authorTelecommunication.telecomAddr.equals("")) {
                ContactPoint contactPoint = new ContactPoint();
                contactPoint.setSystem(ContactPoint.ContactPointSystem.EMAIL);
                contactPoint.setValue(authorTelecommunication.telecomAddr);
                practitioner.addTelecom(contactPoint);
            }
        }
        assert !contained.isEmpty() && contained.get(0) instanceof Practitioner;
        return contained;
    }

    public List<AuthorRole> getAuthorRoles() {
        return authorRoles;
    }

    @Override
    public void setVal(Val val) {
        this.val = val;
    }
}
