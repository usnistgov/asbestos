package gov.nist.asbestos.services.restRequests;

// 0 - empty
// 1 - app context  (asbestos)
// 2 - "log"
// 3 - "ec"

import gov.nist.asbestos.client.Base.Request;

import java.io.IOException;

public class GetEcRequest {
    private Request request;

    public static boolean isRequest(gov.nist.asbestos.client.Base.Request request) {
        return (request.uriParts.size() == 4
                && "log".equalsIgnoreCase(request.uriParts.get(2))
                && "ec".equalsIgnoreCase(request.uriParts.get(3))
        );
    }

    public GetEcRequest(Request request) {
        this.request = request;
    }

    public void run() throws IOException {
        request.resp.setContentType("text/plain");
        request.resp.getOutputStream().print(request.externalCache.toString());
    }
}
