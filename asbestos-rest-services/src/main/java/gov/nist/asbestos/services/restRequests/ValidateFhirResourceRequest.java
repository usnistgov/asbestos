package gov.nist.asbestos.services.restRequests;

import ca.uhn.fhir.model.base.resource.BaseOperationOutcome;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;
import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.channel.FtkChannelTypeEnum;
import gov.nist.asbestos.client.channel.IgNameConstants;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.client.Op;
import gov.nist.asbestos.testEngine.engine.FtkInternalRequestCode;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

// 0 - empty
// 1 - web server application context
// 2 - "ftkValidate"
// 3 - resourceType
// 4 - $validate
// Following URL is the same pattern as the HL7 FHIR $validate operation, the ?... query string is optional
// ${igName} is a test collection property
// from http://hl7.org/fhir/valueset-testscript-operation-codes.html
// Example: https://fhirtoolkit.test:9743/asbestos/ftkValidate/${resourceType}/$validate?profile=${igResourceProfile}#${igName}

public class ValidateFhirResourceRequest {
    private static Logger logger = Logger.getLogger(ValidateFhirResourceRequest.class.getName());

    private Request request;
    private String rawRequest;


    public static boolean isRequest(Request request) {
        if (request.uriParts.size() == 5) {
            String uriPart2 = request.uriParts.get(2);
            return "ftkValidate".equals(uriPart2);
        }
        return false;

        // split profile query string parameter with #, limit 2 ?
    }

    public ValidateFhirResourceRequest(Request request) throws IOException {
        request.setType(this.getClass().getSimpleName());
        this.request = request;
        this.rawRequest = IOUtils.toString(request.req.getInputStream(), Charset.defaultCharset());   // json
    }

    public void run() throws IOException {
        request.announce(this.getClass().getName());
        Map<String, String> paramsMap = request.getParametersMap();

        final String profile = paramsMap.get("profile");
        final String igName = paramsMap.get("igName");


        IgNameConstants igNameConstant = IgNameConstants.find(igName);
        if (igNameConstant == null) {
            String error = String.format("%s not found.", igName);
            logger.severe(error);
            request.badRequest(error);
            return;
        }

        FhirValidator fhirValidator = FtkChannelTypeEnum.findValidator(igNameConstant);
        if (fhirValidator == null) {
            String error = String.format("Null validator for %s.", igNameConstant.getIgName());
            logger.severe(error);
            request.serverError();
            return;
        }

        ValidationOptions validationOptions = new ValidationOptions();
        if (profile != null) {
            validationOptions.addProfile(profile);
        }

        ValidationResult validationResult = fhirValidator.validateWithResult(rawRequest, validationOptions);
        OperationOutcome oo = (OperationOutcome) validationResult.toOperationOutcome();

        String jsonStr = ParserBase.encode(oo, Format.JSON);

        request.resp.setContentType("application/json");
        request.resp.getOutputStream().print(jsonStr);
        request.ok();


    }


}
