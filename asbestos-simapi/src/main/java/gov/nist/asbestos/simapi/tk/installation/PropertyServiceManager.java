package gov.nist.asbestos.simapi.tk.installation;


import java.util.Objects;

public class PropertyServiceManager {
    private String toolkitHost = "localhost";
    private String toolkitTlsPort = "8443";
    private String toolkitPort = "8080";
    private String proxyPort = "7297";
    private String toolkitGazelleConfigURL = "x";

    public String getToolkitHost() {
        return toolkitHost;
    }

    public void setToolkitHost(String toolkitHost) {
        this.toolkitHost = toolkitHost;
    }

    public String getToolkitTlsPort() {
        return toolkitTlsPort;
    }

    public void setToolkitTlsPort(String toolkitTlsPort) {
        this.toolkitTlsPort = toolkitTlsPort;
    }

    public String getToolkitPort() {
        return toolkitPort;
    }

    public void setToolkitPort(String toolkitPort) {
        this.toolkitPort = toolkitPort;
    }

    public String getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(String proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getToolkitGazelleConfigURL() {
        return toolkitGazelleConfigURL;
    }

    public void setToolkitGazelleConfigURL(String toolkitGazelleConfigURL) {
        this.toolkitGazelleConfigURL = toolkitGazelleConfigURL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropertyServiceManager that = (PropertyServiceManager) o;
        return Objects.equals(toolkitHost, that.toolkitHost) &&
                Objects.equals(toolkitTlsPort, that.toolkitTlsPort) &&
                Objects.equals(toolkitPort, that.toolkitPort) &&
                Objects.equals(proxyPort, that.proxyPort) &&
                Objects.equals(toolkitGazelleConfigURL, that.toolkitGazelleConfigURL);
    }

    @Override
    public int hashCode() {
        return Objects.hash(toolkitHost, toolkitTlsPort, toolkitPort, proxyPort, toolkitGazelleConfigURL);
    }
}
