package gov.nist.asbestos.asbestosProxy.requests;

import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.Verb;
import gov.nist.asbestos.http.support.Common;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.List;

// 0 - empty
// 1 - app context  (asbestos)
// 2 - "log"
// 3 - "marker"
// 4 - testSession
// 5 - channelId
// get current marker value

public class GetChannelMarkerRequest {
    private static Logger log = Logger.getLogger(GetChannelMarkerRequest.class);

    static final String MarkerType = "Marker";

    private Request request;

    public static boolean isRequest(Request request) {
        Headers headers = Common.getRequestHeaders(request.req, Verb.GET);
        Header acceptHeader = headers.getAccept();
        boolean jsonOk = acceptHeader.getValue().contains("json");

        return request.uriParts.size() == 6 &&
                "log".equalsIgnoreCase(request.uriParts.get(2)) &&
                "marker".equalsIgnoreCase(request.uriParts.get(3)) &&
                jsonOk;
    }

    public GetChannelMarkerRequest(Request request) {
        this.request = request;
    }

    public void run() {
        log.info("GetChannelMarkerRequest");
        File markerDir = request.ec.getResourceType(request.uriParts.get(4), request.uriParts.get(5), MarkerType);
        List<String> markers = Dirs.dirListingAsStringList(markerDir);
        if (markers.size() == 0) {
            request.resp.setStatus(request.resp.SC_NO_CONTENT);
        } else if (markers.size() == 1) {
            Returns.returnString(request.resp, markers.get(0));
        } else {
            markers.sort(String.CASE_INSENSITIVE_ORDER);
            String last = markers.get(markers.size() - 1);
            Returns.returnString(request.resp, last);
        }
    }

}
