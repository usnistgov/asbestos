package gov.nist.asbestos.asbestosProxy.requests;


// 0 - empty
// 1 - app context  (asbestos)
// 2 - "log"
// 3 - "ValidationServer"


import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import org.apache.log4j.Logger;

public class GetValidationServerRequest {
    private static Logger log = Logger.getLogger(GetEventRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 4 && "ValidationServer".equalsIgnoreCase(request.uriParts.get(3));
    }

    public GetValidationServerRequest(Request request) {
        this.request = request;
    }

    public void run() {
        request.announce("GetValidationServerRequest");
        String base = ServiceProperties.getInstance().getPropertyOrStop(ServicePropertiesEnum.FHIR_VALIDATION_SERVER);
        request.returnValue(base);
        request.ok();
    }

}
