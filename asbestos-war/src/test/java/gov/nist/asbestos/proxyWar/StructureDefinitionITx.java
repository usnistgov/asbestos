package gov.nist.asbestos.proxyWar;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.ConceptValidationOptions;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.validation.*;
import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.Base.MhdValueSets;
import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.Base.CodesToValueSets;
import org.hl7.fhir.common.hapi.validation.support.*;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.utils.IResourceValidator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;



/*
    This fails.

    The main test is fromRecipe() which tracks the referenced example in the HAPI documentation.
    Even the FHIR basics, like narrative-status do not validate because of a ValueSet loading
    problem.

    The test fetchNarrativeStatusValueSet() shows the details.

    In the supplied HAPI jars, there is a Bundle at
            /org/hl7/fhir/r4/model/valueset/valuesets.xml
    which is the default reference set.  Each item in the Bundle is either a CodeSystem or
    ValueSet.

    When running fromRecipe() the error (one of them) references
    http://hl7.org/fhir/ValueSet/narrative-status
    which exists in the Bundle.  In its compose section is an include to
    http://hl7.org/fhir/narrative-status
    which actually does not exist in the Bundle.  But the entry
    http://hl7.org/fhir/CodeSystem/narrative-status (fullUrl) does exist which carries the
    url http://hl7.org/fhir/narrative-status.

    My first question is this a bug?  Should it reference
    http://hl7.org/fhir/CodeSystem/narrative-status
    instead?
    This change does not change the answer - no codes.
 */

class StructureDefinitionITx {
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

        assertTrue(result.isSuccessful());
    }

    // Default typeCodes list http://hl7.org/fhir/ValueSet/c80-doc-typecodes
    // Default formatCodes list http://hl7.org/fhir/ValueSet/formatcodes

    ValidationSupportChain validationSupportChain;
//    ValidationSupportContext validationSupportContext;
    static DefaultProfileValidationSupport def;
    PrePopulatedValidationSupport prepop;

    @BeforeEach
    void buildSupportChain() {
        InMemoryTerminologyServerValidationSupport inMem = new InMemoryTerminologyServerValidationSupport(ctx);
        CommonCodeSystemsTerminologyService common = new CommonCodeSystemsTerminologyService(ctx);
        prepop = new PrePopulatedValidationSupport(ctx);
        validationSupportChain = new ValidationSupportChain(
                def,
                prepop,
                inMem,
                common
        );
//        validationSupportContext = new ValidationSupportContext(validationSupportChain);
    }

    @BeforeAll
    static void beforeAll() {
        def = new DefaultProfileValidationSupport(ctx);
    }

    // As defined in DefaultProfileValidationSupport for R4, the classpath
    // /org/hl7/fhir/r4/model/valueset/valuesets.xml is opened looking for
    // ValueSets.  valuesets.xml is either a ValueSet resource or a Bundle
    // of ValueSet resources.  ValueSet to be returned is identified by the
    // url attribute of the ValueSet
    @Test
    void valueSetFromClassPathInDefaultProfileValidation() {
        IBaseResource valueset = def.fetchValueSet("http://hl7.org/fhir/ValueSet/c80-doc-typecodes");
        assertNotNull(valueset);
    }

    // fails - no concepts
    @Test
    void fetchNarrativeStatusValueSet() {
//        IBaseResource valueset = def.fetchValueSet("http://hl7.org/fhir/ValueSet/narrative-status|4.0.1");
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
                System.out.println("System: " + system);
            }
        }
        assertTrue(found);
    }

    @Test
    void fetchNarrativeStatusValueSetNoVersion() {
        IBaseResource valueset = def.fetchValueSet("http://hl7.org/fhir/ValueSet/narrative-status");
        assertNotNull(valueset);
    }

    @Test
    void fetchNarrativeStatusCodeSystem() {
        IBaseResource valueset = def.fetchCodeSystem("http://hl7.org/fhir/CodeSystem/narrative-status");
        assertNotNull(valueset);
    }

    // to see loading of first ValueSet, DefaultProfileValidationSupport#fetchValueSet
    // 10.3.11 Recipe from https://hapifhir.io/hapi-fhir/docs/validation/validation_support_modules.html
    @Test
    void fromRecipe() {
        // Create a chain that will hold our modules
        ValidationSupportChain supportChain = new ValidationSupportChain();

// DefaultProfileValidationSupport supplies base FHIR definitions. This is generally required
// even if you are using custom profiles, since those profiles will derive from the base
// definitions.
        DefaultProfileValidationSupport defaultSupport = new DefaultProfileValidationSupport(ctx);
        supportChain.addValidationSupport(defaultSupport);

// Create a PrePopulatedValidationSupport which can be used to load custom definitions.
// In this example we're loading two things, but in a real scenario we might
// load many StructureDefinitions, ValueSets, CodeSystems, etc.
        PrePopulatedValidationSupport prePopulatedSupport = new PrePopulatedValidationSupport(ctx);
        // prePopulatedSupport.addStructureDefinition(someStructureDefnition);
        //prePopulatedSupport.addValueSet(someValueSet);
        supportChain.addValidationSupport(prePopulatedSupport);

// Wrap the chain in a cache to improve performance
        CachingValidationSupport cache = new CachingValidationSupport(supportChain);

// Create a validator using the FhirInstanceValidator module. We can use this
// validator to perform validation
        FhirInstanceValidator validatorModule = new FhirInstanceValidator(cache);
        FhirValidator validator = ctx.newValidator().registerValidatorModule(validatorModule);

        DocumentReference input = (DocumentReference) ProxyBase.parse(new File(externalCache, "docRef1.json"));
        assertNotNull(input);

        ValidationResult result = validator.validateWithResult(input);

        for (SingleValidationMessage next : result.getMessages()) {
            ResultSeverityEnum severity = next.getSeverity();
            System.out.println(severity.toString() + ": " + next.getLocationString() + " " + next.getMessage());
        }
        assertTrue(result.isSuccessful());
        assertEquals(0, result.getMessages().size());
    }

    // This doesn't work because DefaultProfileValidationSupport does not implement
    // validateCodeInValueSet and support chain relies on DefaultProfileValidationSupport.
    // InMemoryTerminologyServerValidationSupport implements it
    // PrePopulatedValidationSupport does not
    // CommonCodeSystemsTerminologyService focuses only on "common" CodeSystems
    // CachingValidationSupport implements it
    @Test
    void valueSetFromClassPathInSupportChain() {
        IBaseResource baseResource = def.fetchValueSet("http://hl7.org/fhir/ValueSet/c80-doc-typecodes");
        assertNotNull(baseResource);
        ValueSet valueSet = (ValueSet) baseResource;

        CachingValidationSupport cache = new CachingValidationSupport(validationSupportChain);

        IValidationSupport.CodeValidationResult result = cache.validateCode(
                validationSupportChain,
                new ConceptValidationOptions().setInferSystem(true),
                "http://loinc.org",
                "34895-3",
                "Education Note",
                "http://hl7.org/fhir/ValueSet/c80-doc-typecodes"
                );
        assertNotNull(result);
        assertTrue(result.isOk());
    }

    // DefaultProfileValidationSupport, used in valueSetFromClassPath does not define validateCode()
    @Test
    void validateCode() {
        IValidationSupport.CodeValidationResult result = validationSupportChain.validateCode(
                validationSupportChain,
                new ConceptValidationOptions().setInferSystem(true),          // ConceptValidationOptions
                "http://loinc.org",                              // theCodeSystem
                "34895-3",                                            // theCode
                "Education Note",                                   // theDisplay
                "http://hl7.org/fhir/ValueSet/c80-doc-typecodes" // valueSetURL
        );

        assertNotNull(result);
        assertTrue(result.isOk());
    }

    @Test
    void runValidatorWithDefaults() {
        ValidationSupportChain validationSupportChain = new ValidationSupportChain(
                new DefaultProfileValidationSupport(ctx),
                new InMemoryTerminologyServerValidationSupport(ctx),
                new CommonCodeSystemsTerminologyService(ctx)
        );

        FhirValidator validator = ctx.newValidator();

        FhirInstanceValidator instanceValidator = new FhirInstanceValidator(validationSupportChain);
        //instanceValidator.setBestPracticeWarningLevel(IResourceValidator.BestPracticeWarningLevel.Error);

        validator.registerValidatorModule(instanceValidator);

        DocumentReference docRef = (DocumentReference) ProxyBase.parse(new File(externalCache, "docRef1.json"));
        assertNotNull(docRef);

        ValidationResult result = validator.validateWithResult(docRef);

        for (SingleValidationMessage next : result.getMessages()) {
            ResultSeverityEnum severity = next.getSeverity();
            System.out.println(severity.toString() + ": " + next.getLocationString() + " " + next.getMessage());
        }
        assertTrue(result.isSuccessful());
        assertEquals(0, result.getMessages().size());
    }

    @Test
    void runValidatorWithLoadedStructureDefinition() {
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
