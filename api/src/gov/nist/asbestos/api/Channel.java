package gov.nist.asbestos.api;

public interface Channel {
    String getEnvironment();
    String getTestSession();
    String getChannelId();
    String getChannelType();
}
