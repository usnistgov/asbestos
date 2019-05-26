package gov.nist.asbestos.asbestosProxy.channel;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class ChannelConfig {
    String environment;
    String testSession;
    String channelId;
    String actorType;
    String channelType;
    String fhirBase;
    Map extensions;

    // TODO test needed
    public URI translateEndpointToFhirBase(URI req) throws URISyntaxException {
        String path  = req.getPath();
        int channelI = path.indexOf("/Channel");
        if (channelI != -1) {
            int beyondChannelI = channelI + "/Channel".length();
            path = fhirBase + path.substring(beyondChannelI);
        }
        return new URI(req.getScheme(), req.getUserInfo(), req.getHost(), req.getPort(), path, req.getQuery(), req.getFragment());
    }
}
