package gov.nist.asbestos.services.restRequests;

import com.google.gson.Gson;
import gov.nist.asbestos.asbestosProxy.channel.ChannelControl;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.log.SimStore;
import gov.nist.asbestos.client.channel.ChannelConfig;
import java.util.logging.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
// 0 - empty
// 1 - app context
// 2 - "rw" or "accessGuard"
// 3 - "channel"
// 4 - "channels"
// 5 - "all"
// return channelIds and their URLs

public class GetChannelIdAndURLRequest {
    private static Logger log = Logger.getLogger(GetChannelIdAndURLRequest.class.getName());

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 6
                && "channels".equalsIgnoreCase(request.uriParts.get(4))
                && "all".equalsIgnoreCase(request.uriParts.get(5));
    }

    public GetChannelIdAndURLRequest(Request request) {
        request.setType(this.getClass().getSimpleName());
        this.request = request;
    }

    private class IdandURL {
        String id;
        String url;
        String site;
    }

    public void run() throws IOException {
        request.announce("GetChannelIdAndURLRequest");
        SimStore simStore = new SimStore(request.externalCache);
        List<String> ids = simStore.getChannelIds();
        List<IdandURL> idsAndUrls = new ArrayList<>();

        for (String id : ids) {
            try {
                ChannelConfig channelConfig = ChannelControl.channelConfigFromChannelId(request.externalCache, id);
                IdandURL idu = new IdandURL();
                idu.id = channelConfig.asChannelId();
                idu.url = channelConfig.getFhirBase();
                idu.site = channelConfig.getXdsSiteName();
                idsAndUrls.add(idu);
            } catch (Throwable e) {
                request.announce("Skipping " + id + " due to an exception: " + e.toString());
            }
        }
        String json = new Gson().toJson(idsAndUrls);

        request.resp.setContentType("application/json");
        request.resp.getOutputStream().print(json);
        request.ok();
    }
}
