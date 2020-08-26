package gov.nist.asbestos.asbestosProxy.requests;

import gov.nist.asbestos.asbestosProxy.channel.ChannelControl;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.sharedObjects.ChannelConfigFactory;
import org.apache.log4j.Logger;

import java.io.IOException;
// 0 - empty
// 1 - app context
// 2 - "channel"
// 3 - channelID

public class GetChannelConfigRequest {
    private static Logger log = Logger.getLogger(GetChannelConfigRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 4 && "channel".equalsIgnoreCase(request.uriParts.get(2));
    }

    public GetChannelConfigRequest(Request request) {
        this.request = request;
    }

    public void run() throws IOException {
        request.announce("GetChannelConfig");
        String channelId = request.uriParts.get(3);
        ChannelConfig channelConfig;

        try {
            channelConfig = ChannelControl.channelConfigFromChannelId(request.externalCache, channelId);
        } catch (Throwable e) {
            request.notFound();
            return;
        }
        String configString = ChannelConfigFactory.convert(channelConfig);

        request.resp.setContentType("application/json");
        request.resp.getOutputStream().print(configString);
        request.ok();
    }
}
