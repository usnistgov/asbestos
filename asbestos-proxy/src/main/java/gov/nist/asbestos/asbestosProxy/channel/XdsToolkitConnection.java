package gov.nist.asbestos.asbestosProxy.channel;

import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.log.SimStore;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.sharedObjects.ChannelConfigFactory;
import gov.nist.toolkit.toolkitApi.SimulatorBuilder;
import gov.nist.toolkit.toolkitApi.ToolkitServiceException;
import gov.nist.toolkit.toolkitServicesCommon.SimConfig;
import gov.nist.toolkit.toolkitServicesCommon.resource.SimIdResource;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class XdsToolkitConnection {
    private File externalChannels;
    private String xdsToolkitBase = null;

    public XdsToolkitConnection(File externalCache, String testSession) {
        externalChannels = new File(new File(externalCache, EC.CHANNELS_DIR), testSession);
    }

    public Optional<SimConfig> get(String channelName) throws IOException {
        File target = new File(externalChannels, channelName);
        if (!target.exists())
            throw new IOException("Channel " + channelName + " does not exist");

        Optional<String> xdsToolkitBase = ServiceProperties.getInstance().getProperty(ServicePropertiesEnum.XDS_TOOLKIT_BASE);
        if (xdsToolkitBase.isPresent()) {
            this.xdsToolkitBase = xdsToolkitBase.get();
            File configFile = getChannelConfigFile(externalChannels, channelName);
            ChannelConfig channelConfig = ChannelConfigFactory.load(configFile);
            String xdsSiteName = channelConfig.getXdsSiteName();
            if (xdsSiteName == null)
                throw new IOException("xdsSiteName not defined in channel " + channelName);
            SimIdResource simIdResource = new SimIdResource();
            simIdResource.setFullId(xdsSiteName);
            simIdResource.setActorType("rr");

            SimulatorBuilder xdsSimApi = new SimulatorBuilder(xdsToolkitBase.get());

            try {
                SimConfig simConfig = xdsSimApi.get(simIdResource);
                return Optional.ofNullable(simConfig);
            } catch (ToolkitServiceException e) {
                return Optional.empty();
            }
        } else {
            throw new IOException("xdsToolkitBase not defined in Service Properties");
        }
    }

    private File getChannelConfigFile(File externalChannels, String name) {
        return new File(new File(externalChannels, name), SimStore.CHANNEL_CONFIG_FILE);
    }

    public String getXdsToolkitBase() {
        return xdsToolkitBase;
    }
}
