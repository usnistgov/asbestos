package gov.nist.asbestos.asbestosProxy.util;

import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import org.apache.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;

public class XdsActorMapper {
    private static Logger logger = Logger.getLogger(XdsActorMapper.class);

    public URI getEndpoint(String siteName, String actorType, String transactionType, boolean isTls) {
        try {
            ServicePropertiesEnum key = (isTls ? ServicePropertiesEnum.TLS_XDS_TOOLKIT_BASE : ServicePropertiesEnum.XDS_TOOLKIT_BASE);
            String xdsToolkitBase = ServiceProperties.getInstance().getPropertyOrStop(key);
            return new URI(
                    xdsToolkitBase +
                    "/sim/" +
                    siteName + "/" +
                    actorType + "/" +
                    transactionType);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
