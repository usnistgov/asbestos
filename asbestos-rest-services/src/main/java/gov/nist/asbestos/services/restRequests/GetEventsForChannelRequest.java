package gov.nist.asbestos.services.restRequests;

import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.Verb;
import gov.nist.asbestos.http.support.Common;
import java.util.logging.Logger;

import java.io.IOException;

// 0 - empty
// 1 - app context  (asbestos)
// 2 - "logList"
// 3 - testSession
// 4 - channelId
// 5 - eventId (optional, only this event summary is fully loaded)
// 6 - "getSingleEvent" only applies if eventId is present, return only the eventId full summary

public class GetEventsForChannelRequest {
    private static Logger log = Logger.getLogger(GetEventsForChannelRequest.class.getName());

    private Request request;
    private boolean hasFilterEventId;

    public static boolean isRequest(Request request) {
        Headers headers = Common.getRequestHeaders(request.req, Verb.GET);
        Header acceptHeader = headers.getAccept();
        boolean jsonOk = acceptHeader.getValue().contains("json");

        final int uriPartsSize = request.uriParts.size();
        return (uriPartsSize == 5 || hasFilterEventParam(request) || isSingleEventRequest(request)) && "logList".equalsIgnoreCase(request.uriParts.get(2)) && jsonOk;
    }

    private static boolean isSingleEventRequest(Request request) {
        final int uriParts = request.uriParts.size();
        return uriParts == 7 && "getSingleEvent".equals(request.uriParts.get(6));
    }

    private static boolean hasFilterEventParam(Request request) {
        final int uriParts = request.uriParts.size();
        return uriParts > 5;
    }

    public GetEventsForChannelRequest(Request request) {
        request.setType(this.getClass().getSimpleName());

        this.request = request;
        if (hasFilterEventParam(request))
            this.hasFilterEventId = true;
    }

    public void run() throws IOException {
        request.announce("GetEventsForChannelRequest");
        String query = request.req.getQueryString();
        if (query != null && query.contains("summaries=true")) {
            String filterEventId = (this.hasFilterEventId) ? request.uriParts.get(5): null;
            request.ec.buildJsonListingOfEventSummaries(request.resp, request.uriParts.get(3), request.uriParts.get(4), filterEventId, isSingleEventRequest(request));
        } else
            request.ec.buildJsonListingOfResourceTypes(request.resp, request.uriParts.get(3), request.uriParts.get(4));
        request.ok();
    }

}
