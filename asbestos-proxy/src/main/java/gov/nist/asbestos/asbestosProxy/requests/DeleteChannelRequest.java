package gov.nist.asbestos.asbestosProxy.requests;

import gov.nist.asbestos.client.log.SimStore;
import gov.nist.asbestos.simapi.simCommon.SimId;
import org.apache.log4j.Logger;

import java.io.IOException;
// 0 - empty
// 1 - app context
// 2 - "channel"
// 3 - channelID  (testSession__id)

public class DeleteChannelRequest {
    private static Logger log = Logger.getLogger(DeleteChannelRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        if (request.uriParts.size() == 4) {
            String uriPart2 = request.uriParts.get(2);
            return "channel".equals(uriPart2) || "channelGuard".equals(uriPart2);
        }
        return false;
    }

    public DeleteChannelRequest(Request request) {
        this.request = request;
    }

    public void run() throws IOException {
        log.info("DeleteChannel");
        String channelId = request.uriParts.get(3);

        SimId simId = SimId.buildFromRawId(channelId);
        SimStore simStore = new SimStore(request.externalCache, simId);
        simStore.deleteSim();
    }
}
