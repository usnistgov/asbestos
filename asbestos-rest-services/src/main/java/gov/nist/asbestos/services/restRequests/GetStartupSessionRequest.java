package gov.nist.asbestos.services.restRequests;

// 0 - empty
// 1 - app context
// 2 - "log"
// 3 - "startupSession"


import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import java.util.logging.Logger;

import java.io.IOException;

public class GetStartupSessionRequest {
    private static Logger log = Logger.getLogger(GetStartupSessionRequest.class.getName());

    private Request request;

    public static boolean isRequest(gov.nist.asbestos.client.Base.Request request) {
        return (request.uriParts.size() == 4
                && "log".equalsIgnoreCase(request.uriParts.get(2))
                && "startupSession".equalsIgnoreCase(request.uriParts.get(3))
        );
    }

    public GetStartupSessionRequest(Request request) {
        this.request = request;
    }

    public void run() throws IOException {
        request.announce("GetStartupSession");
        String session = ServiceProperties.getInstance().getPropertyOrThrow(ServicePropertiesEnum.STARTUP_SESSION_ID);
        log.info("initial session is " + session);
        request.resp.setContentType("text/plain");
        request.resp.getOutputStream().print(session);
    }
}
