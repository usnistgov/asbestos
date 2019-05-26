package gov.nist.asbestos.fproxy.log


import gov.nist.asbestos.asbestosProxy.channel.ChannelConfig;
import gov.nist.asbestos.asbestosProxy.channel.SimConfigMapper;
import gov.nist.asbestos.asbestosProxy.log.SimStore;

import java.io.File;
import java.util.Map;

class SimStoreBuilder {
    static SimStore builder(File externalCache, ChannelConfig simConfig) {
        SimStore simStore = new SimStore(externalCache)
        simStore.config = simConfig
        String json = JsonOutput.toJson(simConfig)
        simStore.channelId = getSimId(simConfig)
        simStore.getStore(true) // create
        File configFile = new File(simStore.simDir, 'config.json')
        simStore.newlyCreated = !configFile.exists()
        configFile.text = JsonOutput.prettyPrint(json)
        simStore
    }

    static SimStore loader(File externalCache, TestSession testSession, String id) {
        SimStore simStore = new SimStore(externalCache)
        simStore.setSimIdForLoader(new SimId(testSession, id))  // doesn't do Id validation
        Map rawConfig = (Map) new JsonSlurper().parse(new File(simStore.simDir, 'config.json'))
        ChannelConfig simConfig = new SimConfigMapper(rawConfig).build()
        simStore.channelId = getSimId(simConfig)
        simStore.config = simConfig
        simStore
    }

    // return (exists) or exception (doesn't)
    static SimStore sense(File externalCache, TestSession testSession, String id) {
        SimStore simStore = new SimStore(externalCache)
        simStore.setSimIdForLoader(new SimId(testSession, id))  // doesn't do Id validation
        assert simStore.exists()
        simStore
 //       Map rawConfig = (Map) new JsonSlurper().parse(new File(simStore.simDir, 'config.json'))
    }

    static boolean exists(File externalCache, SimId channelId) {
        SimStore simStore = new SimStore(externalCache)
        simStore.setSimIdForLoader(channelId)  // doesn't do Id validation

    }

    static SimId getSimId(ChannelConfig channelConfig) {
        new SimId(new TestSession(channelConfig.testSession), channelConfig.channelId, channelConfig.actorType, channelConfig.environment)
    }

    static ChannelConfig buildSimConfig(String json) {
        Map rawConfig = (Map) new JsonSlurper().parseText(json)
        new SimConfigMapper(rawConfig).build()
    }

}
