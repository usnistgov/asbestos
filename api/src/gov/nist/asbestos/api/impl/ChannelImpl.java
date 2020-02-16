package gov.nist.asbestos.api.impl;

import gov.nist.asbestos.api.Channel;

import java.util.Objects;

public class ChannelImpl implements Channel {
    private String environment;
    private String testSession;
    private String channelId;
    private String channelType;

    public ChannelImpl(String environment, String testSession, String channelId, String channelType) {
        this.environment = environment;
        this.testSession = testSession;
        this.channelId = channelId;
        this.channelType = channelType;
    }

    @Override
    public String getEnvironment() {
        return environment;
    }

    @Override
    public String getTestSession() {
        return testSession;
    }

    @Override
    public String getChannelId() {
        return channelId;
    }

    @Override
    public String getChannelType() {
        return channelType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChannelImpl channel = (ChannelImpl) o;
        return Objects.equals(environment, channel.environment) &&
                Objects.equals(testSession, channel.testSession) &&
                Objects.equals(channelId, channel.channelId) &&
                Objects.equals(channelType, channel.channelType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(environment, testSession, channelId, channelType);
    }
}
