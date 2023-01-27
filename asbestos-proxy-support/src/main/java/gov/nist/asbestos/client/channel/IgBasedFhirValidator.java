package gov.nist.asbestos.client.channel;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import gov.nist.asbestos.client.Base.ParserBase;
import org.hl7.fhir.common.hapi.validation.support.CachingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.NpmPackageValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.SnapshotGeneratingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;

import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IgBasedFhirValidator {
    private static Logger logger = Logger.getLogger(IgBasedFhirValidator.class.getName());
    private static final ConcurrentHashMap<String, FhirValidator> validatorMap = new ConcurrentHashMap<>();

    private IgBasedFhirValidator() {
    }

    public static FhirValidator getInstance(String igPackage) {
        if (validatorMap.contains(igPackage)) {
            return validatorMap.get(igPackage);
        }
        try {
            URL url = new IgBasedFhirValidator().getClass().getResource(igPackage);
            if (url == null) {
                return null;
            }
        } catch (Exception ex) {
            return null;
        }
        NpmPackageValidationSupport npmPackageValidationSupport;
        FhirContext ctx = ParserBase.getFhirContext();
        npmPackageValidationSupport = new NpmPackageValidationSupport(ctx);
        try {
            npmPackageValidationSupport.loadPackageFromClasspath(igPackage);
            // Create a support chain including the NPM Package Support
            ValidationSupportChain validationSupportChain = new ValidationSupportChain(
                    npmPackageValidationSupport,
                    new DefaultProfileValidationSupport(ctx),
                    new CommonCodeSystemsTerminologyService(ctx),
                    new InMemoryTerminologyServerValidationSupport(ctx),
                    new SnapshotGeneratingValidationSupport(ctx)
            );
            CachingValidationSupport validationSupport = new CachingValidationSupport(validationSupportChain);

            // Create a validator. Note that for good performance you can create as many validator objects
            // as you like, but you should reuse the same validation support object in all of the,.
            FhirValidator validator = ctx.newValidator();
            FhirInstanceValidator instanceValidator = new FhirInstanceValidator(validationSupport);
            validator.registerValidatorModule(instanceValidator);
            FhirValidator previousValue = validatorMap.put(igPackage, validator);
            if (previousValue != null) {
                logger.severe("Unexpected non-null value already exists in map for key " + igPackage);
            }
            return validator;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error loading " + igPackage, e);
            return null;
        }


    }
}
