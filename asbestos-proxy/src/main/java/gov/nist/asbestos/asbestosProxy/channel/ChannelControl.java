package gov.nist.asbestos.asbestosProxy.channel;

import gov.nist.asbestos.client.log.SimStore;
import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.simapi.simCommon.SimId;

import java.io.File;

public class ChannelControl {
    static public ChannelConfig channelConfigFromChannelId(File externalCache, String channelId) {
        SimId simId = SimId.buildFromRawId(channelId);
        SimStore simStore = new SimStore(externalCache, simId);
        simStore.open();
        return simStore.getChannelConfig();
    }

}
