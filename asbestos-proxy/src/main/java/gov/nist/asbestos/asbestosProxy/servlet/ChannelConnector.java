package gov.nist.asbestos.asbestosProxy.servlet;

import gov.nist.asbestos.client.log.SimStore;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.simapi.simCommon.SimId;

import javax.servlet.http.HttpServletResponse;
import java.io.File;

// Connect to a channel
public class ChannelConnector {

    // channelId is testSession__channelName
    public static SimStore connect(HttpServletResponse resp, File externalCache, String channelId) {
        SimId simId = SimId.buildFromRawId(channelId);
        SimStore simStore = new SimStore(externalCache, simId);
        if (!simStore.exists()) {
            resp.setStatus(resp.SC_NOT_FOUND);
            return null;
        }
        simStore.open();
        if (!simStore.exists()) {
            resp.setStatus(resp.SC_NOT_FOUND);
            return null;
        }
        return simStore;
    }

    public static ChannelConfig getChannelConfig(HttpServletResponse resp, File externalCache, String channelId) {
        SimStore simStore = connect(resp, externalCache, channelId);
        if (simStore == null) return null;
        return simStore.getChannelConfig();
    }
}
