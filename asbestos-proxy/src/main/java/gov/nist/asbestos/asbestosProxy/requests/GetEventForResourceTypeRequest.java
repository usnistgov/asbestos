package gov.nist.asbestos.asbestosProxy.requests;

import gov.nist.asbestos.asbestosProxy.servlet.Task;
import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.Verb;
import gov.nist.asbestos.http.support.Common;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.List;

// 0 - empty
// 1 - app context  (asbestos)
// 2 - "log"
// 3 - testSession
// 4 - channelId
// 5 - resourceType

public class GetEventForResourceTypeRequest {
    private static Logger log = Logger.getLogger(GetEventForResourceTypeRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        Headers headers = Common.getRequestHeaders(request.req, Verb.GET);
        Header acceptHeader = headers.getAccept();
        boolean jsonOk = acceptHeader.getValue().contains("json");

        return request.uriParts.size() == 6 && "log".equalsIgnoreCase(request.uriParts.get(2)) && jsonOk;
    }

    public GetEventForResourceTypeRequest(Request request) {
        this.request = request;
    }

    public void run() {
        log.info("GetEventForResourceTypeRequest");
        request.ec.buildJsonListingOfEvents(
                request.resp,
                request.uriParts.get(3),
                request.uriParts.get(4),
                request.uriParts.get(5));
    }

}
