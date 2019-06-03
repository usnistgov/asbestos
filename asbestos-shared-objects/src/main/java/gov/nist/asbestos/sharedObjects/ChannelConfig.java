package gov.nist.asbestos.sharedObjects;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public class ChannelConfig {
    private String environment;
    private String testSession;
    private String channelId;
    private String actorType;
    private String channelType;
    private String fhirBase;

    public String toString() {
        return new StringBuilder().append("Channel ").append(testSession).append("__").append(channelId)
                .append(" of ").append(actorType).append(" in ").append(environment)
                .append(" with base ").append(fhirBase).toString();
    }

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

    public ChannelConfig setEnvironment(String environment) {
        this.environment = environment;
        return this;
    }

    public String getTestSession() {
        return testSession;
    }

    public ChannelConfig setTestSession(String testSession) {
        this.testSession = testSession;
        return this;
    }

    public String getChannelId() {
        return channelId;
    }

    public ChannelConfig setChannelId(String channelId) {
        this.channelId = channelId;
        return this;
    }

    public String getActorType() {
        return actorType;
    }

    public ChannelConfig setActorType(String actorType) {
        this.actorType = actorType;
        return this;
    }

    public String getChannelType() {
        return channelType;
    }

    public ChannelConfig setChannelType(String channelType) {
        this.channelType = channelType;
        return this;
    }

    public String getFhirBase() {
        return fhirBase;
    }

    public ChannelConfig setFhirBase(String fhirBase) {
        this.fhirBase = fhirBase;
        return this;
    }

    public String asFullId() {
        return testSession + "__" + channelId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChannelConfig that = (ChannelConfig) o;
        return Objects.equals(environment, that.environment) &&
                Objects.equals(testSession, that.testSession) &&
                Objects.equals(channelId, that.channelId) &&
                Objects.equals(actorType, that.actorType) &&
                Objects.equals(channelType, that.channelType) &&
                Objects.equals(fhirBase, that.fhirBase);
    }

    @Override
    public int hashCode() {
        return Objects.hash(environment, testSession, channelId, actorType, channelType, fhirBase);
    }
}
