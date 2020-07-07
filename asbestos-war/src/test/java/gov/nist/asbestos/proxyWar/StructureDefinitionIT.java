package gov.nist.asbestos.proxyWar;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.IValidatorModule;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;
import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.Base.MhdValueSets;
import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.Base.CodesToValueSets;
import org.hl7.fhir.common.hapi.validation.support.CachingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StructureDefinitionIT {
    static File externalCache = ExternalCache.getExternalCache("structureDefinition");
    static EC ec = new EC(externalCache);
    static FhirContext ctx = FhirContext.forR4();

    @Test
    void runSimpleValidator() {
        FhirValidator validator = ctx.newValidator();

        IValidatorModule module = new FhirInstanceValidator(ProxyBase.getFhirContext());
        validator.registerValidatorModule(module);

        Patient resource = new Patient();
        resource.addName().setFamily("Simpson").addGiven("Homer");
        ValidationResult result = validator.validateWithResult(resource);

        for (SingleValidationMessage next : result.getMessages()) {
            System.out.println(next.getLocationString() + " " + next.getMessage());
        }
    }

    // Default typeCodes list http://hl7.org/fhir/ValueSet/c80-doc-typecodes
    // Default formatCodes list http://hl7.org/fhir/ValueSet/formatcodes

    @Test
    void runValidatorWithLoadedStructureDefinition() {
        ValidationSupportChain supportChain = new ValidationSupportChain();

        DefaultProfileValidationSupport defaultSupport = new DefaultProfileValidationSupport(ProxyBase.getFhirContext());
        supportChain.addValidationSupport(defaultSupport);

        PrePopulatedValidationSupport prePopulatedSupport = new PrePopulatedValidationSupport(ProxyBase.getFhirContext());
        prePopulatedSupport.addStructureDefinition(loadStructureDefinition("structuredefinition-IHE_MHD_Provide_Minimal_DocumentReference.xml"));
        supportChain.addValidationSupport(prePopulatedSupport);

        CachingValidationSupport cache = new CachingValidationSupport(supportChain);

        FhirInstanceValidator validatorModule = new FhirInstanceValidator(cache);
        FhirValidator validator = ctx.newValidator();
        validator.registerValidatorModule(validatorModule);

        DocumentReference docRef = (DocumentReference) ProxyBase.parse(new File(externalCache, "docRef1.json"));
        assertNotNull(docRef);

        ValidationResult result = validator.validateWithResult(docRef);

        List<String> issues = new ArrayList<>();
        for (SingleValidationMessage next : result.getMessages()) {
            issues.add(next.getMessage());
            System.out.println(next.getLocationString() + " " + next.getMessage());
        }
        assertEquals(2,issues.size());
    }

    @Test
    void runValidationWithMhdValueSets() {
        ValidationSupportChain supportChain = new ValidationSupportChain();

        DefaultProfileValidationSupport defaultSupport = new DefaultProfileValidationSupport(ProxyBase.getFhirContext());
        //supportChain.addValidationSupport(defaultSupport);

        PrePopulatedValidationSupport prePopulatedSupport = new PrePopulatedValidationSupport(ProxyBase.getFhirContext());
        prePopulatedSupport.addStructureDefinition(loadStructureDefinition("structuredefinition-IHE_MHD_Provide_Minimal_DocumentReference.xml"));

        Map<String, ValueSet> valueSets = ec.getMhdValueSetsAsMap("default");
        assertEquals(11, valueSets.size());
        for (ValueSet valueSet : valueSets.values()) {
            prePopulatedSupport.addValueSet(valueSet);
        }

        supportChain.addValidationSupport(prePopulatedSupport);

//        ca.uhn.fhir.context.support.ValidationSupportContext c;
//
//        IContextValidationSupport.CodeValidationResult result  = supportChain.validateCodeInValueSet(
//                "http://loinc.org",
//                "11369-6",
//                "Immunization",
//                valueSets.get("http:/localhost:8877/asbestos/valueset/default/typeCode"));




        CachingValidationSupport cache = new CachingValidationSupport(supportChain);

        FhirInstanceValidator validatorModule = new FhirInstanceValidator(cache);
        FhirValidator validator = ctx.newValidator();
        validator.registerValidatorModule(validatorModule);

        DocumentReference docRef = (DocumentReference) ProxyBase.parse(new File(externalCache, "docRef1.json"));
        assertNotNull(docRef);

        ValidationResult validationResult = validator.validateWithResult(docRef);

        List<String> issues = new ArrayList<>();
        for (SingleValidationMessage next : validationResult.getMessages()) {
            issues.add(next.getMessage());
            System.out.println(next.getLocationString() + " " + next.getMessage());
        }
        System.out.println(issues);
        assertEquals(0, issues.size());
    }

    @Test
    void getUrl() {
        System.out.println(new MhdValueSets(externalCache, "default").getUrl("formatCode"));
    }

    @Test
    void translateCodesXmlToValueSets() throws FileNotFoundException, JAXBException {
        File codesFile = ec.getCodesFile("default");
        assertTrue(codesFile.exists());

        CodesToValueSets cvs = new CodesToValueSets(codesFile);
        Map<String, ValueSet> valueSetMap = cvs.run();
        assertEquals(11, valueSetMap.size());
    }

    @BeforeAll
    static void buildValueSets() {
        if (ec.mhdValueSetsNeedBuilding("default"))
            ec.buildMhdValueSets("default");
    }

    private StructureDefinition loadStructureDefinition(String name) {
        File fsdFile = new File(externalCache, "FhirStructDefs");
        assertTrue(fsdFile.exists());
        File sdFile = new File(fsdFile, name);
        assertTrue(sdFile.exists());
        StructureDefinition sd = (StructureDefinition) ProxyBase.parse(sdFile);
        return sd;
    }
}
