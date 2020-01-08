package gov.nist.asbestos.sharedObjects;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ChannelConfig {
    private String environment;
    private String testSession;
    private String channelId;   // simple id (no testSesssion__ prefix)
    private String actorType;
    private String channelType;
    private boolean includeValidation;
    private String fhirBase;
    private String xdsSiteName;
    private boolean writeLocked;

    public String toString() {
        return new StringBuilder().append("Channel ").append(testSession).append("__").append(channelId)
                .append(" of ").append(actorType).append(" in ").append(environment)
                .append(" with base ").append(fhirBase)
                .append(" with xdsSite ").append(xdsSiteName).toString();
    }

    // TODO test needed
    public URI translateEndpointToFhirBase(URI req) throws URISyntaxException {
        String path  = req.getPath();
        List<String> parts1 = Arrays.asList(path.split("/"));
        List<String> parts = new ArrayList<>(parts1);  // deletable
        // 0 - empty
        // 1 - proxy (appContext)
        // 2 - fhir
        // 3 - channelId
        // 4+ - parts to pass on the FHIR server
        parts.remove(0);
        parts.remove(0);
        parts.remove(0);
        parts.remove(0);
        String query = req.getQuery();

        String uriString = fhirBase + "/" +  String.join("/", parts) + (query == null || query.equals("") ? "" : "?" + query);
        URI uri = new URI(uriString);
        return uri;
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
        return includeValidation == that.includeValidation &&
                Objects.equals(environment, that.environment) &&
                Objects.equals(testSession, that.testSession) &&
                Objects.equals(channelId, that.channelId) &&
                Objects.equals(actorType, that.actorType) &&
                Objects.equals(channelType, that.channelType) &&
                Objects.equals(fhirBase, that.fhirBase) &&
                Objects.equals(xdsSiteName, that.xdsSiteName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(environment, testSession, channelId, actorType, channelType, includeValidation, fhirBase, xdsSiteName);
    }

    public String getXdsSiteName() {
        return xdsSiteName;
    }

    public ChannelConfig setXdsSiteName(String xdsSiteName) {
        this.xdsSiteName = xdsSiteName;
        return this;
    }

    public boolean isIncludeValidation() {
        return includeValidation;
    }

    public void setIncludeValidation(boolean includeValidation) {
        this.includeValidation = includeValidation;
    }

    public boolean isWriteLocked() {
        return writeLocked;
    }

    public void setWriteLocked(boolean writeLocked) {
        this.writeLocked = writeLocked;
    }
}
