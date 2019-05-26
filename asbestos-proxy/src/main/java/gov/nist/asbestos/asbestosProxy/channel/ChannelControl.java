package gov.nist.asbestos.asbestosProxy.channel;


import gov.nist.asbestos.asbestosProxy.events.EventStore;

/**
 * Defines a proxy channel.
 * Implemented as a simulator.
 */

interface ChannelControl {
    void setup(ChannelConfig simConfig);
    void teardown();
    void validateConfig(ChannelConfig simConfig);
    // throws Exception if error
    void handle(EventStore event);
}
