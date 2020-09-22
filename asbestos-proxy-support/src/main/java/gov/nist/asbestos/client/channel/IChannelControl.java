package gov.nist.asbestos.client.channel;


import gov.nist.asbestos.client.events.Event;
import gov.nist.asbestos.client.channel.ChannelConfig;

/**
 * Defines a proxy channel.
 * Implemented as a simulator.
 */

public interface IChannelControl {
    void setup(ChannelConfig simConfig);
    void teardown();
    void validateConfig(ChannelConfig simConfig);
    // throws Exception if error
    void handle(Event event);
}
