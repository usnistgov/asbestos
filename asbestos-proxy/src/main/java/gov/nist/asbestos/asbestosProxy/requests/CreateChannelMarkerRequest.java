package gov.nist.asbestos.asbestosProxy.requests;

import gov.nist.asbestos.client.events.Event;
import gov.nist.asbestos.client.log.SimStore;
import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.Verb;
import gov.nist.asbestos.http.support.Common;
import gov.nist.asbestos.simapi.simCommon.SimId;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.List;

import static gov.nist.asbestos.asbestosProxy.requests.GetChannelMarkerRequest.MarkerType;

// 0 - empty
// 1 - app context  (asbestos)
// 2 - "log"
// 3 - "marker"
// 4 - testSession
// 5 - channelId
// create new channel marker

public class CreateChannelMarkerRequest {
    private static Logger log = Logger.getLogger(CreateChannelMarkerRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        Headers headers = Common.getRequestHeaders(request.req, Verb.POST);
        Header acceptHeader = headers.getAccept();
        boolean jsonOk = acceptHeader.getValue().contains("json");

        return request.uriParts.size() == 6 &&
                "log".equalsIgnoreCase(request.uriParts.get(2)) &&
                "marker".equalsIgnoreCase(request.uriParts.get(3)) &&
                jsonOk;
    }

    public CreateChannelMarkerRequest(Request request) {
        this.request = request;
    }

    public void run() {
        log.info("CreateChannelMarkerRequest");
        SimId simId = SimId.buildFromRawId(request.uriParts.get(4) + "__" + request.uriParts.get(5));
        SimStore simStore = new SimStore(request.ec.externalCache, simId);
        simStore.setResource(MarkerType);
        Event event = simStore.newEvent();
        String eventId = event.getEventId();
        Returns.returnString(request.resp, eventId);
    }

}
