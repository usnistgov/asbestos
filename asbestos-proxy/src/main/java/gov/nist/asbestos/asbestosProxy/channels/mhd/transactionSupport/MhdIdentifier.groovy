package gov.nist.asbestos.asbestosProxy.channels.mhd.transactionSupport

import org.hl7.fhir.r4.model.Identifier

class MhdIdentifier {
    String system
    String value
    Identifier identifier

    MhdIdentifier(Identifier identifier) {
        this.identifier = identifier
        system = identifier.system
        value = identifier.value
    }
}
