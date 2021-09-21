package gov.nist.asbestos.services.restRequests;

import com.google.gson.Gson;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.log.SimStore;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.List;
// 0 - empty
// 1 - app context
// 2 - "rw"
// 3 - "channel"
// Return list of channel IDs

public class GetChannelIdsRequest {
    private static Logger log = Logger.getLogger(GetChannelIdsRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 4 && "channel".equalsIgnoreCase(request.uriParts.get(3));
    }

    public GetChannelIdsRequest(Request request) {
        this.request = request;
    }

    public void run() throws IOException {
        request.announce("GetChannelIds");
        SimStore simStore = new SimStore(request.externalCache);
        List<String> ids = simStore.getChannelIds();

        String json = new Gson().toJson(ids);

        request.resp.setContentType("application/json");
        request.resp.getOutputStream().print(json);
        request.ok();
    }
}
