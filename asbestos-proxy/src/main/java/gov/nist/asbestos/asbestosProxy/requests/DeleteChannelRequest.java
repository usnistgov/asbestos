package gov.nist.asbestos.asbestosProxy.requests;

import gov.nist.asbestos.client.log.SimStore;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.sharedObjects.ChannelConfigFactory;
import gov.nist.asbestos.simapi.simCommon.SimId;
import gov.nist.asbestos.simapi.simCommon.TestSession;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.charset.Charset;
// 0 - empty
// 1 - app context
// 2 - "channel"
// 3 - channelID  (testSession__id)

public class DeleteChannelRequest {
    private static Logger log = Logger.getLogger(DeleteChannelRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 4 && request.uriParts.get(2).equals("channel");
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
