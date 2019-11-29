package gov.nist.asbestos.asbestosProxy.requests;

import gov.nist.asbestos.asbestosProxy.servlet.ChannelConnector;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.http.operations.HttpGet;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

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

    public void run() {
        log.info("hapiheartbeat");
        String channelId = "default__default";

        ChannelConfig channelConfig = ChannelConnector.getChannelConfig(request.resp, request.externalCache, channelId);
        if (channelConfig == null) throw new Error("Channel does not exist");

        try {
            request.resp.getOutputStream().write(("BaseAddress: " + channelConfig.getFhirBase()).getBytes());
            URI uri = new URI(channelConfig.getFhirBase() + "/metadata");
            HttpGet getter = new HttpGet();
            getter.get(uri, Format.JSON.getContentType());
            if (getter.isSuccess()) {
                request.resp.setStatus(request.resp.SC_OK);
            } else
                request.resp.setStatus(request.resp.SC_SERVICE_UNAVAILABLE);
        } catch (Throwable e) {
            request.resp.setStatus(request.resp.SC_SERVICE_UNAVAILABLE);
        }
    }
}
