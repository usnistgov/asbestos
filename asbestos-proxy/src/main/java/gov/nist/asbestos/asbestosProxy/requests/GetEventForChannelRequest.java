package gov.nist.asbestos.asbestosProxy.requests;

import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.Verb;
import gov.nist.asbestos.http.support.Common;
import org.apache.log4j.Logger;

// 0 - empty
// 1 - app context  (asbestos)
// 2 - "log"
// 3 - testSession
// 4 - channelId

public class GetEventForChannelRequest {
    private static Logger log = Logger.getLogger(GetEventForChannelRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        Headers headers = Common.getRequestHeaders(request.req, Verb.GET);
        Header acceptHeader = headers.getAccept();
        boolean jsonOk = acceptHeader.getValue().contains("json");

        return request.uriParts.size() == 5 && "log".equalsIgnoreCase(request.uriParts.get(2)) && jsonOk;
    }

    public GetEventForChannelRequest(Request request) {
        this.request = request;
    }

    public void run() {
        log.info("GetEventForChannelRequest");
        String query = req.getQueryString();
        if (query != null && query.contains("summaries=true")) {
            request.ec.buildJsonListingOfEventSummaries(request.resp, request.uriParts[3], request.uriParts[4]);
            return;
        }
        // JSON listing of resourceTypes in channelId
        buildJsonListingOfResourceTypes(resp, uriParts[3], uriParts[4]);
        return;
    }

}
