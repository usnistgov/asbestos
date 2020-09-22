package gov.nist.asbestos.services.restRequests;

import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.Verb;
import gov.nist.asbestos.http.support.Common;
import org.apache.log4j.Logger;

import java.io.IOException;

// 0 - empty
// 1 - app context  (asbestos)
// 2 - "log"
// 3 - testSession
// 4 - channelId

public class GetEventsForChannelRequest {
    private static Logger log = Logger.getLogger(GetEventsForChannelRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        Headers headers = Common.getRequestHeaders(request.req, Verb.GET);
        Header acceptHeader = headers.getAccept();
        boolean jsonOk = acceptHeader.getValue().contains("json");

        return request.uriParts.size() == 5 && "log".equalsIgnoreCase(request.uriParts.get(2)) && jsonOk;
    }

    public GetEventsForChannelRequest(Request request) {
        this.request = request;
    }

    public void run() throws IOException {
        request.announce("GetEventsForChannelRequest");
        String query = request.req.getQueryString();
        if (query != null && query.contains("summaries=true"))
            request.ec.buildJsonListingOfEventSummaries(request.resp, request.uriParts.get(3), request.uriParts.get(4));
        else
            request.ec.buildJsonListingOfResourceTypes(request.resp, request.uriParts.get(3), request.uriParts.get(4));
        request.ok();
    }

}
