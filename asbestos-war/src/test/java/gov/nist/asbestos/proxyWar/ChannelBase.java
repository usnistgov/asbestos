package gov.nist.asbestos.proxyWar;

import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import gov.nist.asbestos.client.channel.ChannelConfig;

import java.net.URI;
import java.net.URISyntaxException;

public class ChannelBase {

    public static URI getChannelBase(ChannelConfig channelConfig) {
        String fhirToolkitBase = ServiceProperties.getInstance().getPropertyOrStop(ServicePropertiesEnum.FHIR_TOOLKIT_BASE);
        try {
            return new URI(fhirToolkitBase + "/proxy/" + channelConfig.asFullId());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
