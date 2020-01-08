package gov.nist.asbestos.asbestosProxy.requests;

import org.apache.log4j.Logger;

import java.io.IOException;
// 0 - empty
// 1 - app context
// 2 - "signIn"

public class GetSignInRequest {
    private static Logger log = Logger.getLogger(GetSignInRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 3 && "signIn".equalsIgnoreCase(request.uriParts.get(2));
    }

    public GetSignInRequest(Request request) {
        this.request = request;
    }

    public void run() throws IOException {
        log.info("GetSignIn");

        request.resp.setContentType("application/json");
        request.resp.getOutputStream().print("{\"status\":\"ok\"}");

        request.resp.setStatus(request.resp.SC_OK);
    }
}
