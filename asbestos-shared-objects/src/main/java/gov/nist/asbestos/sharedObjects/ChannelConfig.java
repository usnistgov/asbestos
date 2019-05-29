package gov.nist.asbestos.sharedObjects;

import java.net.URI;
import java.net.URISyntaxException;

public class ChannelConfig {
    private String environment;
    private String testSession;
    private String channelId;
    private String actorType;
    private String channelType;
    private String fhirBase;

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

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getTestSession() {
        return testSession;
    }

    public void setTestSession(String testSession) {
        this.testSession = testSession;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getActorType() {
        return actorType;
    }

    public void setActorType(String actorType) {
        this.actorType = actorType;
    }

    public String getChannelType() {
        return channelType;
    }

    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }

    public String getFhirBase() {
        return fhirBase;
    }

    public void setFhirBase(String fhirBase) {
        this.fhirBase = fhirBase;
    }
}
