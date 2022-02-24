package gov.nist.asbestos.services.restRequests;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.Verb;
import gov.nist.asbestos.http.support.Common;
import org.apache.commons.io.IOUtils;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import java.io.IOException;

// 0 - empty
// 1 - app context  (asbestos)
// 2 - "logList"
// 3 - testSession
// 4 - channelId
// 5 - eventId (optional, only this event summary is fully loaded)
// 6 - "getSingleEvent" only applies if eventId is present, return only the eventId full summary

public class EventsForChannelRequest {
    private static final String ITEMS_PER_PAGE = "itemsPerPage";
    private static final String PAGE_NUM = "pageNum";
    private static Logger log = Logger.getLogger(EventsForChannelRequest.class.getName());

    private Request request;
    private boolean hasFilterEventId;

    public static boolean isRequest(Request request) {
        Headers headers = Common.getRequestHeaders(request.req, Verb.GET);
        Header acceptHeader = headers.getAccept();
        boolean jsonOk = acceptHeader.getValue().contains("json");

        final int uriPartsSize = request.uriParts.size();
        return (uriPartsSize == 5 || hasFilterEventParam(request) || isSingleEventRequest(request)) && "logList".equalsIgnoreCase(request.uriParts.get(2)) && jsonOk;
    }

    public static boolean isPostRequest(Request request) {
        Headers headers = Common.getRequestHeaders(request.req, Verb.POST);
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

    public EventsForChannelRequest(Request request) {
        request.setType(this.getClass().getSimpleName());

        this.request = request;
        if (hasFilterEventParam(request))
            this.hasFilterEventId = true;
    }

    public void doGet() throws IOException {
        request.announce("EventsForChannelRequest:doGet");
        String query = request.req.getQueryString();
        if (query != null && query.contains("summaries=true")) {
            String filterEventId = (this.hasFilterEventId) ? request.uriParts.get(5): null;
            List<String> ids = filterEventId != null ? Collections.singletonList(filterEventId) : null;
            Map<String,String> qparms = Ref.parseParameters(query);
            int itemsPerPage = qparms.containsKey(ITEMS_PER_PAGE) ? Integer.parseInt(qparms.get(ITEMS_PER_PAGE)) : -1;
            int pageNum = qparms.containsKey(PAGE_NUM) ? Integer.parseInt(qparms.get(PAGE_NUM)) : -1;
            request.ec.buildJsonListingOfEventSummaries(request.resp, request.uriParts.get(3), request.uriParts.get(4), ids, isSingleEventRequest(request), itemsPerPage, pageNum);
        } else
            request.ec.buildJsonListingOfResourceTypes(request.resp, request.uriParts.get(3), request.uriParts.get(4));
        request.ok();
    }

    public void doPost() throws IOException {
        request.announce("EventsForChannelRequest:doPost");
        String string = IOUtils.toString(request.req.getInputStream(), Charset.defaultCharset());   // json
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        CollectionType type = objectMapper.getTypeFactory()
                .constructCollectionType(ArrayList.class, String.class);
        ArrayList<String> eventIds = objectMapper.readValue(string, type);

        // get only these properties below
        // `${summary.verb} ${summary.resourceType} from ${summary.ipAddr}`
        request.ec.buildJsonListingOfEventSummaries(request.resp, request.uriParts.get(3), request.uriParts.get(4), eventIds, isSingleEventRequest(request), -1, -1);

        request.ok();
    }

}
