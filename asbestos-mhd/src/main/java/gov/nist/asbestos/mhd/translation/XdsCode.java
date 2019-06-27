package gov.nist.asbestos.mhd.translation;

import gov.nist.asbestos.asbestorCodesJaxb.Code;
import gov.nist.asbestos.client.Base.IVal;
import gov.nist.asbestos.mhd.transactionSupport.CodeTranslator;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ClassificationType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;

import java.util.Objects;
import java.util.Optional;

public class XdsCode implements IVal {
    private CodeTranslator codeTranslator = null;
    private String scheme = null;
    private String code = null;
    private String codingScheme = null;
    private String name = null;
    private Val val = null;

    Coding asCoding() {
        Objects.requireNonNull(codeTranslator);
        Objects.requireNonNull(scheme);
        Objects.requireNonNull(code);
        Objects.requireNonNull(codingScheme);
        Objects.requireNonNull(name);
        Objects.requireNonNull(val);
        Optional<Code> theCode = codeTranslator.findCodeForCodeAndScheme(scheme, code, codingScheme);
        String fhirCode = code;
        String fhirSystem = theCode.map(Code::getSystem).orElse(null);
        if (fhirSystem == null)
            val.add(new ValE("Cannot translate XDS code " + scheme + "|" + codingScheme + "|" + code + " into FHIR through codes.xml").asError());
        Coding coding = new Coding();
        coding.setDisplay(name);
        coding.setCode(fhirCode);
        coding.setSystem(fhirSystem);
        return coding;
    }

    CodeableConcept asCodeableConcept() {
        return new CodeableConcept(asCoding());
    }

    XdsCode setCodeTranslator(CodeTranslator codeTranslator) {
        this.codeTranslator = codeTranslator;
        return this;
    }

    XdsCode setClassificationType(ClassificationType c) {
        scheme = c.getClassificationScheme();
        code = c.getNodeRepresentation();
        codingScheme = Slot.getSlotValue1(c.getSlot(), "codingScheme");
        name = Slot.getValue(c.getName());
        return this;
    }

    @Override
    public void setVal(Val val) {
        this.val = val;
    }
}
