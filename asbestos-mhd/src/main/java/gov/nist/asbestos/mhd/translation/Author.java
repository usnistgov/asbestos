package gov.nist.asbestos.mhd.translation;

import gov.nist.asbestos.client.Base.IVal;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ClassificationType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Author implements IVal {
    private Val val = null;
    private ContainedIdAllocator containedIdAllocator = null;
    private List<AuthorPerson> authorPersons = new ArrayList<>();  // zero or one
    private List<AuthorInstitution> authorInstitutions = new ArrayList<>(); // zero or more
    private List<AuthorRole> authorRoles = new ArrayList<>(); // zero or more
    private List<AuthorSpecialty> authorSpecialtys = new ArrayList<>(); // zero or more
    private List<AuthorTelecommunication> authorTelecommunications = new ArrayList<>(); // zero or more

    public ClassificationType organizationToClassification(Organization organization) {
        Objects.requireNonNull(val);
        Objects.requireNonNull(organization);
        if (organization.hasName()) {
            AuthorInstitution authorInstitution = new AuthorInstitution();
            authorInstitution.setValue(organization.getName(), val);
            authorInstitutions.add(authorInstitution);
            ClassificationType c = new ClassificationType();
            c.setNodeRepresentation("");
            c.setObjectType("urn:oasis:names:tc:ebxml-regrep:ObjectType:RegistryObject:Classification");
            SlotType1 slot = Slot.makeSlot("authorInstitution", authorInstitution.toString());
            c.getSlot().add(slot);
            return c;
        } else {
            val.add(new ValE("Cannot translate Organization - no name component").asWarning());
        }
        return null;
    }

    public ClassificationType practitionerToClassification(Practitioner practitioner) {
        Objects.requireNonNull(practitioner);
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
        Objects.requireNonNull(containedIdAllocator);
        Objects.requireNonNull(c);
        Practitioner practitioner = null; //new Practitioner();
        Organization organization = null;
        List<Resource> contained = new ArrayList<>();
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
        for (AuthorPerson authorPerson : authorPersons) {  // can only be singular
            if (!authorPerson.id.equals("")) {
                if (practitioner == null) {
                    practitioner = new Practitioner();
                    practitioner.setId(containedIdAllocator.newId(Practitioner.class));
                    contained.add(practitioner);
                }
                Identifier identifier = new Identifier();
                identifier.setValue(authorPerson.id);
                practitioner.addIdentifier(identifier);
            }
            if (!authorPerson.familyName.equals("")) {
                if (practitioner == null) {
                    practitioner = new Practitioner();
                    practitioner.setId(containedIdAllocator.newId(Practitioner.class));
                    contained.add(practitioner);
                }
                HumanName humanName = new HumanName();
                humanName.setFamily(authorPerson.familyName);
                if (authorPerson.givenName != null)
                    humanName.addGiven(authorPerson.givenName);
                practitioner.addName(humanName);
            }
        }
        if (!authorInstitutions.isEmpty()) {
            organization = new Organization();
            String name = authorInstitutions.get(0).getOrgName();
            organization.setName(name);
            organization.setId(containedIdAllocator.newId(Organization.class));
            contained.add(organization);
        }
        if (!authorRoles.isEmpty()) {
            if (practitioner == null) {
                practitioner = new Practitioner();
                practitioner.setId(containedIdAllocator.newId(Practitioner.class));
                contained.add(practitioner);
            }
            PractitionerRole practitionerRole = new PractitionerRole();
            practitionerRole.setId(containedIdAllocator.newId(PractitionerRole.class));
            for (AuthorRole authorRole : authorRoles) {
                if (authorRole.hasCodeableConcept() || authorRole.hasCodeAndSystem()) {
                    CodeableConcept cc = authorRole.getCodeableConcept();
                    practitionerRole.getCode().add(cc);
                }
            }
            practitionerRole.setPractitioner(new Reference().setReference(practitioner.getId()));
            contained.add(practitionerRole);
        }
        for (AuthorTelecommunication authorTelecommunication : authorTelecommunications) {
            if (!authorTelecommunication.telecomAddr.equals("")) {
                ContactPoint contactPoint = new ContactPoint();
                contactPoint.setSystem(ContactPoint.ContactPointSystem.EMAIL);
                contactPoint.setValue(authorTelecommunication.telecomAddr);
                if (practitioner != null)
                    practitioner.addTelecom(contactPoint);
                else if (organization != null)
                    organization.addTelecom(contactPoint);
            }
        }
        return contained;
    }

    public List<AuthorRole> getAuthorRoles() {
        return authorRoles;
    }

    public void setContainedIdAllocator(ContainedIdAllocator containedIdAllocator) {
        this.containedIdAllocator = containedIdAllocator;
    }

    @Override
    public void setVal(Val val) {
        this.val = val;
    }
}
