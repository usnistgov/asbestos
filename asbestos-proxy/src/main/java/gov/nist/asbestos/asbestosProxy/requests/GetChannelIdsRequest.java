package gov.nist.asbestos.asbestosProxy.requests;

import com.google.gson.Gson;
import gov.nist.asbestos.client.log.SimStore;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.sharedObjects.ChannelConfigFactory;
import gov.nist.asbestos.simapi.simCommon.SimId;
import gov.nist.asbestos.simapi.simCommon.TestSession;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
// 0 - empty
// 1 - app context
// 2 - "channel"
// Return list of channel IDs

public class GetChannelIdsRequest {
    private static Logger log = Logger.getLogger(GetChannelIdsRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 3 && "channel".equalsIgnoreCase(request.uriParts.get(2));
    }

    public GetChannelIdsRequest(Request request) {
        this.request = request;
    }

    public void run() throws IOException {
        SimStore simStore = new SimStore(request.externalCache);
        List<String> ids = simStore.getChannelIds();

        String json = new Gson().toJson(ids);

        request.resp.setContentType("application/json");
        request.resp.getOutputStream().print(json);

        request.resp.setStatus(request.resp.SC_OK);
        log.info("OK");
    }
}
