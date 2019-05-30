package gov.nist.asbestos.asbestosProxy.channels.mhd.transactionSupport;

import org.hl7.fhir.r4.model.Identifier;

public class MhdIdentifier {
    private String system;
    private String value;
    private Identifier identifier;

    MhdIdentifier(Identifier identifier) {
        this.identifier = identifier;
        system = identifier.getSystem();
        value = identifier.getValue();
    }

    public String getSystem() {
        return system;
    }

    public String getValue() {
        return value;
    }

    public Identifier getIdentifier() {
        return identifier;
    }
}
