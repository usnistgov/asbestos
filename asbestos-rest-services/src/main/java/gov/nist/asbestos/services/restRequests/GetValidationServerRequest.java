package gov.nist.asbestos.services.restRequests;


// 0 - empty
// 1 - app context  (asbestos)
// 2 - "log"
// 3 - "ValidationServer"


import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import java.util.logging.Logger;

public class GetValidationServerRequest {
    private static Logger log = Logger.getLogger(GetEventRequest.class.getName());

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 4 && "ValidationServer".equalsIgnoreCase(request.uriParts.get(3));
    }

    public GetValidationServerRequest(Request request) {
        request.setType(this.getClass().getSimpleName());
        this.request = request;
    }

    public void run() {
        request.announce("GetValidationServerRequest");
        String base = ServiceProperties.getInstance().getPropertyOrThrow(ServicePropertiesEnum.FHIR_VALIDATION_SERVER);
        request.returnValue(base);
        request.ok();
    }

}
