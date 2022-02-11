package gov.nist.asbestos.services.restRequests;

import gov.nist.asbestos.client.Base.Request;
import java.util.logging.Logger;

import java.io.IOException;
// 0 - empty
// 1 - app context
// 2 - "signIn"

public class GetSignInRequest {
    private static Logger log = Logger.getLogger(GetSignInRequest.class.getName());

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 3 && "signIn".equalsIgnoreCase(request.uriParts.get(2));
    }

    public GetSignInRequest(Request request) {
        request.setType(this.getClass().getSimpleName());
        this.request = request;
    }

    public void run() throws IOException {
        request.announce("GetSignIn");

        request.resp.setContentType("application/json");
        request.resp.getOutputStream().print("{\"status\":\"ok\"}");
        request.ok();
    }
}
