package gov.nist.asbestos.asbestosProxy.channel;

import gov.nist.asbestos.client.channel.BaseChannel;
import gov.nist.asbestos.client.channel.ChannelConfig;

public interface IChannelBuilder {
    BaseChannel build(ChannelConfig simConfig);
}
