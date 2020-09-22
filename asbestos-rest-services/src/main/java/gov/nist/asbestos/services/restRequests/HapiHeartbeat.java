package gov.nist.asbestos.services.restRequests;

import gov.nist.asbestos.services.servlet.ChannelConnector;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.http.operations.HttpGetter;
import gov.nist.asbestos.client.channel.ChannelConfig;
import org.apache.log4j.Logger;

import java.net.URI;

// 0 - empty
// 1 - appContext
// 2 - "engine"
// 3 - "hapiheartbeat"
// issues a simple GET to default channel

public class HapiHeartbeat {
    private static Logger log = Logger.getLogger(HapiHeartbeat.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 4 && request.uriParts.get(3).equals("hapiheartbeat");
    }

    public HapiHeartbeat(Request request) {
        this.request = request;
    }

    class HeartBeat {
        String addr;
        boolean responding;
    }

    public void run() {
        log.info("hapiheartbeat");
        String channelId = "default__default";

        ChannelConfig channelConfig = ChannelConnector.getChannelConfig(request.resp, request.externalCache, channelId);
        if (channelConfig == null) throw new Error("Channel does not exist");

        HeartBeat heartBeat = new HeartBeat();
        try {
            request.resp.setStatus(request.resp.SC_OK);
            heartBeat.addr = channelConfig.getFhirBase();
            URI uri = new URI(channelConfig.getFhirBase() + "/metadata");
            HttpGetter getter = new HttpGetter();
            getter.get(uri, Format.JSON.getContentType());
            heartBeat.responding = getter.isSuccess();
        } catch (Throwable e) {
            heartBeat.responding = false;
        }
        request.returnObject(heartBeat);
    }
}
