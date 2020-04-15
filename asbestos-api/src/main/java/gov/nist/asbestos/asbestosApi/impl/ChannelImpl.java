package gov.nist.asbestos.asbestosApi.impl;

import gov.nist.asbestos.asbestosApi.Channel;

import java.util.Objects;

/**
 * A channel is a path through the asbestos proxy. All testing is based on messages passing through the proxy.
 * Messages sent on a channel are logged so they can be reviewed/graded later.
 *
 * For Server testing, the channel is configured to rely messages to the SUT, a FHIR server. The test engine generates
 * messages to the channel. For Client testing,
 * the channel is configured to point to the internal default FHIR server.  The SUT sends messages to the channel which
 * forwards them to the default FHIR server. The test engine judges the test messages by reviewing the logs.
 *
 * Channels have a type which directs the channel implementation how to process the messages. The most basic channel type,
 * passthrough, relays the messages without change.  The mhd channel type expects MHD messages, it translates MHD to
 * XDS and relays them to a support XDS simulator in XDS Toolkit.
 */
public class ChannelImpl implements Channel {
    /**
     * Environment has the same meaning as in XDS Toolkit.  The environment holds the metadata coding rules and
     * certificates to be used on the channel.
     */
    private String environment;
    /**
     * TestSession has the same meaning as in XDS Toolkit.  TestSessions store data independent of one another so
     * multiple users can share an instance of the toolkit without sharing data.
     */
    private String testSession;
    /**
     * Identifier for the channel.
     */
    private String channelId;
    /**
     * Type of the channel: passthrough or mhd.
     */
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
