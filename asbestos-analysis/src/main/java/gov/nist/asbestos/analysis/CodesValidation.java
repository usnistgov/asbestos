package gov.nist.asbestos.analysis;

import gov.nist.asbestos.asbestorCodesJaxb.Code;
import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.mhd.transactionSupport.CodeTranslator;
import org.hl7.fhir.r4.model.*;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CodesValidation {
    EC ec;
    File codes;
    CodeTranslator codeTranslator;
    List<String> baseErrors = new ArrayList<>();

    public CodesValidation(EC ec) {
        this.ec = ec;
        codes = ec.getCodesFile("default");
        try {
            codeTranslator = new CodeTranslator(codes);
        } catch (FileNotFoundException e) {
            baseErrors.add(e.getMessage());
        } catch (JAXBException e) {
            baseErrors.add(e.getMessage());
        }
    }

    public List<String> validate(BaseResource baseResource) {
        if (baseResource instanceof DocumentManifest)
            return validate((DocumentManifest) baseResource);
        if (baseResource instanceof DocumentReference)
            return validate((DocumentReference) baseResource);
        return new ArrayList<>();
    }

    public List<String> validate(DocumentManifest documentManifest) {
        List<String> errors = new ArrayList<>(baseErrors);

        check(documentManifest.getType(), "type", "contentTypeCode", errors);

        return errors;
    }

    public List<String> validate(DocumentReference documentReference) {
        List<String> errors = new ArrayList<>(baseErrors);

        check(documentReference.getType(), "type", "typeCode", errors);
        check(documentReference.getCategory(), "category", "classCode", errors);
        for (DocumentReference.DocumentReferenceContentComponent contentComponent : documentReference.getContent()) {
            check(contentComponent.getFormat(), "format", "formatCode", errors);
        }
        if (documentReference.hasContext()) {
            check(documentReference.getContext().getEvent(), "event", "eventCodeList", errors);
            check(documentReference.getContext().getFacilityType(), "facilityType", "healthcareFacilityTypeCode", errors);
            check(documentReference.getContext().getPracticeSetting(), "practiceSetting", "practiceSettingCode", errors);
        }

        return errors;
    }

    public void check(Coding coding, String fhirCodeName, String xdsCodeName, List<String> errors) {
        if (coding == null)
            return;
        if (coding.getSystem() == null && coding.getCode() == null)
            return;
        boolean error = false;
        if (coding.getSystem() == null) {
            error = true;
            errors.add(fhirCodeName + ": " + " has no system component");
        }
        if (coding.getCode() == null) {
            error = true;
            errors.add(fhirCodeName + ": " + " has no code component");
        }
        if (error)
            return;
        Optional<Code> code = codeTranslator.findCodeBySystem(xdsCodeName, coding.getSystem(), coding.getCode());
        if (!code.isPresent())
            errors.add(fhirCodeName + ": " + coding.getSystem() + "|" + coding.getCode() + " is not registered");
    }

    private void check(List<CodeableConcept> codeableConcepts, String fhirCodeName, String xdsCodeName, List<String> errors) {
        if (codeableConcepts == null)
            return;
        for (CodeableConcept codeableConcept : codeableConcepts) {
            check(codeableConcept, fhirCodeName, xdsCodeName, errors);
        }
    }

    private void check(CodeableConcept codeableConcept, String fhirCodeName, String xdsCodeName, List<String> errors) {
        if (codeableConcept == null)
            return;
        for (Coding coding : codeableConcept.getCoding()) {
            check(coding, fhirCodeName, xdsCodeName, errors);
        }
    }
}
