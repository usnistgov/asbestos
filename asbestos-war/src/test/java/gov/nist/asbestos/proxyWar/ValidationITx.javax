package gov.nist.asbestos.proxyWar;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;
import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.Format;
import org.hl7.fhir.common.hapi.validation.support.*;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ValidationITx {
    static FhirContext ctx = FhirContext.forR4();
    ValidationSupportContext validationSupportContext;
    DefaultProfileValidationSupport def;

    @BeforeEach
    void buildSupportChain() {
        def = new DefaultProfileValidationSupport(ctx);
        ValidationSupportChain validationSupportChain = new ValidationSupportChain(
                def,
                new PrePopulatedValidationSupport(ctx),
                new InMemoryTerminologyServerValidationSupport(ctx),
                new CommonCodeSystemsTerminologyService(ctx)
        );
        validationSupportContext = new ValidationSupportContext(validationSupportChain);
    }

    // fails - conceptSet has no concepts but passes assertFalse(conceptSet.isEmpty())
    @Test
    void fetchAndValidateNarrativeStatusValueSet() {
        IBaseResource valueset = def.fetchValueSet("http://hl7.org/fhir/ValueSet/narrative-status");
        assertNotNull(valueset);
        ValueSet vs = (ValueSet) valueset;
        assertTrue(vs.hasCompose());
        ValueSet.ValueSetComposeComponent compose = vs.getCompose();
        assertTrue(compose.hasInclude());
        List<ValueSet.ConceptSetComponent> conceptSet = compose.getInclude();
        assertFalse(conceptSet.isEmpty());
        boolean found = false;
        for (ValueSet.ConceptSetComponent comp : conceptSet) {
            assertFalse(comp.isEmpty());
            for (ValueSet.ConceptReferenceComponent rcomp: comp.getConcept()) {
                System.out.println("Code: " + rcomp.getCode());
                found = true;
            }
            if (comp.hasSystem()) {
                String system = comp.getSystem();
                int conceptCount = comp.getConcept().size();
                System.out.println("System: " + system + "\nConcept count: " + conceptCount);;
            }
        }
        assertTrue(found);       // contains at least one concept?
    }

    String resourceString = "{\n" +
            "  \"resourceType\": \"DocumentReference\",\n" +
            "  \"text\": {\n" +
            "    \"status\": \"empty\",\n" +
            "    \"div\": \"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">Comment</div>\"\n" +
            "  }\n" +
            "}\n";

    // 10.3.11 Recipe from https://hapifhir.io/hapi-fhir/docs/validation/validation_support_modules.html

    /*
ERROR: DocumentReference Profile http://hl7.org/fhir/StructureDefinition/DocumentReference, Element 'DocumentReference.status': minimum required = 1, but only found 0
ERROR: DocumentReference Profile http://hl7.org/fhir/StructureDefinition/DocumentReference, Element 'DocumentReference.content': minimum required = 1, but only found 0
ERROR: DocumentReference.text.status The value provided ("empty") is not in the value set http://hl7.org/fhir/ValueSet/narrative-status|4.0.1 (http://hl7.org/fhir/ValueSet/narrative-status), and a code is required from this value set) (error message = Validation failed)
     */
    @Test
    void fromRecipe() {
        ValidationSupportChain supportChain = new ValidationSupportChain();

        DefaultProfileValidationSupport defaultSupport = new DefaultProfileValidationSupport(ctx);
        supportChain.addValidationSupport(defaultSupport);

        PrePopulatedValidationSupport prePopulatedSupport = new PrePopulatedValidationSupport(ctx);
        // prePopulatedSupport.addStructureDefinition(someStructureDefnition);
        //prePopulatedSupport.addValueSet(someValueSet);
        supportChain.addValidationSupport(prePopulatedSupport);

        CachingValidationSupport cache = new CachingValidationSupport(supportChain);

        FhirInstanceValidator validatorModule = new FhirInstanceValidator(cache);
        FhirValidator validator = ctx.newValidator().registerValidatorModule(validatorModule);

        DocumentReference input = (DocumentReference) ProxyBase.parse(resourceString, Format.JSON);

        ValidationResult result = validator.validateWithResult(input);

        for (SingleValidationMessage next : result.getMessages()) {
            ResultSeverityEnum severity = next.getSeverity();
            System.out.println(severity.toString() + ": " + next.getLocationString() + " " + next.getMessage());
        }
        assertTrue(result.isSuccessful());    // FAILS
    }

}
