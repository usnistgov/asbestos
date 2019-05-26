package gov.nist.asbestos.asbestosProxy.channel;


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
