package gov.nist.asbestos.asbestosProxy.channel;


import gov.nist.asbestos.client.events.Event;
import gov.nist.asbestos.sharedObjects.ChannelConfig;

/**
 * Defines a proxy channel.
 * Implemented as a simulator.
 */

interface IChannelControl {
    void setup(ChannelConfig simConfig);
    void teardown();
    void validateConfig(ChannelConfig simConfig);
    // throws Exception if error
    void handle(Event event);
}
