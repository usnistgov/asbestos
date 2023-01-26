package gov.nist.asbestos.services.restRequests;

import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.testEngine.engine.FtkInternalRequestCode;

import java.io.IOException;
import java.util.logging.Logger;

// 0 - empty
// 1 - web server application context
// 2 - "validate"
// 3 - resourceType
// 4 - $validate
// Following URL is the same pattern as the HL7 FHIR $validate operation
// from http://hl7.org/fhir/valueset-testscript-operation-codes.html
// Example: https://fhirtoolkit.test:9743/asbestos/validate/${resourceType}/$validate?profile=${pdbBundleProfile}#${igVersion}

public class ValidateFhirResourceRequest {
    private static Logger log = Logger.getLogger(ValidateFhirResourceRequest.class.getName());

    private Request request;

    public static boolean isRequest(Request request) {
        if (request.uriParts.size() == 7) {
            String uriPart3 = request.uriParts.get(3);
            return FtkInternalRequestCode.FTK_FUNCTION_CODE.getCode().equals(uriPart3);
        }
        return false;

    }

    public ValidateFhirResourceRequest(Request request) {
        request.setType(this.getClass().getSimpleName());
        this.request = request;
    }

    public void run() throws IOException {
    }


}
