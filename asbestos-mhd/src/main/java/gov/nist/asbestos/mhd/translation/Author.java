package gov.nist.asbestos.mhd.translation;

import gov.nist.asbestos.asbestosProxySupport.Base.IVal;
import gov.nist.asbestos.simapi.validation.Val;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ClassificationType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Practitioner;

public class Author implements IVal {
    private Val val = null;
    private AuthorPerson authorPerson = null;
    private AuthorInstitution authorInstitution = null;
    private AuthorRole authorRole = null;
    private AuthorSpecialty authorSpecialty = null;
    private AuthorTelecommunication authorTelecommunication = null;

    public ClassificationType fromPractitioner(Practitioner practitioner) {
        ClassificationType c = new ClassificationType();



        return c;
    }

    public Practitioner fromClassification(ClassificationType c) {
        for (SlotType1 slot : c.getSlot()) {
            String name = slot.getName();
            if (!slot.getValueList().getValue().isEmpty()) {
                String value = slot.getValueList().getValue().get(0);
                if ("authorPerson".equals(name))
                    authorPerson = (AuthorPerson) new AuthorPerson().setValue(value);
                else if ("authorInstitution".equals(name))
                    authorInstitution = (AuthorInstitution) new AuthorInstitution().setValue(value);
                else if ("authorRole".equals(name))
                    authorRole = (AuthorRole) new AuthorRole().setValue(value);
                else if ("authorSpecialty".equals(name))
                    authorSpecialty = (AuthorSpecialty) new AuthorSpecialty().setValue(value);
                else if ("authorTelecommunication".equals(name))
                    authorTelecommunication = (AuthorTelecommunication) new AuthorTelecommunication().setValue(value);
            }
            Practitioner practitioner = new Practitioner();
            if (authorPerson != null && authorPerson.id != null) {
                Identifier identifier = new Identifier();
                identifier.setValue(authorPerson.id);
                practitioner.addIdentifier(identifier);
            }
            if (authorPerson != null && authorPerson.familyName != null) {
                HumanName humanName = new HumanName();
                humanName.setFamily(authorPerson.familyName);
                if (authorPerson.givenName != null)
                    humanName.addGiven(authorPerson.givenName);
            }
            if (authorTelecommunication != null && authorTelecommunication.telecomAddr != null) {
                ContactPoint contactPoint = new ContactPoint();
                contactPoint.setSystem(ContactPoint.ContactPointSystem.EMAIL);
                contactPoint.setValue(authorTelecommunication.telecomAddr);
            }
             return practitioner;
        }
        return null;
    }

    @Override
    public void setVal(Val val) {
        this.val = val;
    }
}
