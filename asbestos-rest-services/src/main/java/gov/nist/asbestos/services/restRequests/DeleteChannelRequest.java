package gov.nist.asbestos.services.restRequests;

import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.log.SimStore;
import gov.nist.asbestos.simapi.simCommon.SimId;
import java.util.logging.Logger;

import java.io.IOException;
// 0 - empty
// 1 - app context
// 2  "rw" or "accessGuard"
// 3 - "channel"
// 4 - channelID  (testSession__id)

public class DeleteChannelRequest {
    private static Logger log = Logger.getLogger(DeleteChannelRequest.class.getName());

    private Request request;

    public static boolean isRequest(Request request) {
        if (request.uriParts.size() == 5) {
            String uriPart3 = request.uriParts.get(3);
            return "channel".equals(uriPart3);
        }
        return false;
    }

    public DeleteChannelRequest(Request request) {
        request.setType(this.getClass().getSimpleName());
        this.request = request;
    }

    public void run() throws IOException {
        request.announce("DeleteChannel");
        String channelId = request.uriParts.get(4);

        SimId simId = SimId.buildFromRawId(channelId);
        SimStore simStore = new SimStore(request.externalCache, simId);
        simStore.deleteSim();
        request.ok();
    }
}
