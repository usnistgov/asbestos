package gov.nist.asbestos.asbestosProxy.requests;

import com.google.gson.Gson;
import gov.nist.asbestos.asbestosProxy.channel.ChannelControl;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.log.SimStore;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
// 0 - empty
// 1 - app context
// 2 - "channel"
// 3 - "channels"
// 4 - "all"
// return channelIds and their URLs

public class GetChannelIdAndURLRequest {
    private static Logger log = Logger.getLogger(GetChannelIdAndURLRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 5
                && "channels".equalsIgnoreCase(request.uriParts.get(3))
                && "all".equalsIgnoreCase(request.uriParts.get(4));
    }

    public GetChannelIdAndURLRequest(Request request) {
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
                idu.id = channelConfig.getChannelId();
                idu.url = channelConfig.getFhirBase();
                idu.site = channelConfig.getXdsSiteName();
                idsAndUrls.add(idu);
            } catch (Throwable e) {
                request.notFound();
                return;
            }
        }
        String json = new Gson().toJson(idsAndUrls);

        request.resp.setContentType("application/json");
        request.resp.getOutputStream().print(json);
        request.ok();
    }
}
