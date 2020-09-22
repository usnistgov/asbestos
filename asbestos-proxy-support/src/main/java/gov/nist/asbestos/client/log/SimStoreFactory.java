package gov.nist.asbestos.client.log;


import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.channel.ChannelConfigFactory;
import gov.nist.asbestos.simapi.simCommon.SimId;
import gov.nist.asbestos.simapi.simCommon.TestSession;

import java.io.File;

public class SimStoreFactory {

    public static SimStore load(File externalCache, ChannelConfig channelConfig) {
        SimStore simStore = new SimStore(externalCache);
        simStore.channelConfig = channelConfig;
        simStore.setChannelId(getSimId(channelConfig));
        simStore.getStore(true); // create
        File configFile = new File(simStore.getChannelDir(), "channelConfig.json");
        simStore.setNewlyCreated(!configFile.exists());
        ChannelConfigFactory.store(channelConfig, configFile);
        return simStore;
    }

    public static SimStore load(File externalCache, TestSession testSession, String id) {
        SimStore simStore = new SimStore(externalCache);
        simStore.setSimIdForLoader(new SimId(testSession, id));  // doesn't do Id validation
        ChannelConfig channelConfig = ChannelConfigFactory.load(new File(simStore.getChannelDir(), "channelConfig.json"));
        simStore.setChannelId(getSimId(channelConfig));
        simStore.channelConfig = channelConfig;
        return simStore;
    }

    // return (exists) or exception (doesn't)
    public static SimStore sense(File externalCache, TestSession testSession, String id) {
        SimStore simStore = new SimStore(externalCache);
        simStore.setSimIdForLoader(new SimId(testSession, id));  // doesn't do Id validation
        assert simStore.exists();
        return simStore;
    }

    public static boolean exists(File externalCache, SimId channelId) {
        SimStore simStore = new SimStore(externalCache);
        simStore.setSimIdForLoader(channelId);  // doesn't do Id validation
        return simStore.exists();
    }

    private static SimId getSimId(ChannelConfig channelConfig) {
        return new SimId(new TestSession(channelConfig.getTestSession()), channelConfig.getChannelId(), channelConfig.getActorType(), channelConfig.getEnvironment());
    }

}
