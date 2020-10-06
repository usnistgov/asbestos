package gov.nist.asbestos.client.channel;

import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ChannelConfig {
    private String environment;
    private String testSession;
    private String channelName;   // simple id (no testSesssion__ prefix)
    private String actorType;
    private String channelType;
    private boolean includeValidation;
    private String fhirBase = null;   // points to fhir server is channel type if FHIR
    private String xdsSiteName;       // point to XDS server if channel type is MHD
    private boolean writeLocked;
    private boolean logMhdCapabilityStatementRequest;

    public String toString() {
        return "Channel " + testSession + "__" + channelName +
                " of " + actorType + " in " + environment +
                " with base " + fhirBase +
                " with xdsSite " + xdsSiteName;
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

        String joinedparts = parts.size() == 0 ? "" : String.join("/", parts);
        if (joinedparts.length() > 0)
            joinedparts = "/" + joinedparts;
        String uriString = fhirBase + joinedparts + (query == null || query.equals("") ? "" : "?" + query);
        return new URI(uriString);
    }

    public URI proxyURI() {
        ServicePropertiesEnum key = ServicePropertiesEnum.FHIR_TOOLKIT_BASE;
        String proxyStr = ServiceProperties.getInstance().getPropertyOrStop(key);
        proxyStr += "/proxy/" + testSession + "__" + channelName;
        try {
            return new URI(proxyStr);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
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
        return testSession + "__" + channelName;
    }

    public ChannelConfig setChannelName(String channelName) {
        this.channelName = channelName;
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

    /**
     *  When the channel forwards to the system behind the proxy, it uses fhirBase.
     * @return
     */
    public String getFhirBase() {
        if (fhirBase != null)
            return fhirBase;
        return null;
    }

    public ChannelConfig setFhirBase(String fhirBase) {
        this.fhirBase = fhirBase;
        return this;
    }

    public String asFullId() {
        return testSession + "__" + channelName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChannelConfig that = (ChannelConfig) o;
        return includeValidation == that.includeValidation &&
                Objects.equals(environment, that.environment) &&
                Objects.equals(testSession, that.testSession) &&
                Objects.equals(channelName, that.channelName) &&
                Objects.equals(actorType, that.actorType) &&
                Objects.equals(channelType, that.channelType) &&
                Objects.equals(fhirBase, that.fhirBase) &&
                Objects.equals(xdsSiteName, that.xdsSiteName) &&
                Objects.equals(writeLocked, that.writeLocked) &&
                Objects.equals(logMhdCapabilityStatementRequest, that.logMhdCapabilityStatementRequest);
    }

    @Override
    public int hashCode() {
        return Objects.hash(environment, testSession, channelName, actorType, channelType, includeValidation, fhirBase, xdsSiteName, writeLocked, logMhdCapabilityStatementRequest);
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

    public boolean isLogMhdCapabilityStatementRequest() {
        return logMhdCapabilityStatementRequest;
    }

    public ChannelConfig setLogMhdCapabilityStatementRequest(boolean logMhdCapabilityStatementRequest) {
        this.logMhdCapabilityStatementRequest = logMhdCapabilityStatementRequest;
        return this;
    }

    public String getChannelName() {
        return channelName;
    }
}
