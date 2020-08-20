package gov.nist.asbestos.asbestosProxy.requests;

import gov.nist.asbestos.asbestosProxy.channel.ChannelControl;
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
// 2 - "channelLock"
// Create a channel based on JSON configuration in request

public class LockChannelRequest {
    private static Logger log = Logger.getLogger(LockChannelRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        if (request.uriParts.size() == 3) {
            String uriPart2 = request.uriParts.get(2);
            return "channelLock".equals(uriPart2);
        }
        return false;
    }

    public LockChannelRequest(Request request) {
        this.request = request;
    }

    public void run() throws IOException {
        request.announce("LockChannel");
        String string = IOUtils.toString(request.req.getInputStream(), Charset.defaultCharset());   // json
        ChannelConfig channelConfigInRequest = ChannelConfigFactory.convert(string);

        ChannelConfig channelConfig = ChannelControl.channelConfigFromChannelId(request.externalCache, channelConfigInRequest.asFullId());
        if (channelConfig.isWriteLocked() != channelConfigInRequest.isWriteLocked()) {
            channelConfig.setWriteLocked(channelConfigInRequest.isWriteLocked());
            SimStore simStore = new SimStore(request.externalCache,
                    new SimId(new TestSession(channelConfig.getTestSession()),
                            channelConfig.getChannelId(),
                            channelConfig.getActorType(),
                            channelConfig.getEnvironment(),
                            true));
            simStore.create(channelConfig);
            log.info("Channel " + simStore.getChannelId().toString() + " write protect updated to: '" + channelConfig.isWriteLocked() + "'" );
        } else {
            log.info("Write protection was not modified because the configuration value is already the same.");
        }
        request.ok();
    }
}
