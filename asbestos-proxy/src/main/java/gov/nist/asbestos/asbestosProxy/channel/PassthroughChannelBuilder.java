package gov.nist.asbestos.asbestosProxy.channel;

import gov.nist.asbestos.asbestosProxy.channels.passthrough.PassthroughChannel;
import gov.nist.asbestos.client.channel.BaseChannel;
import gov.nist.asbestos.client.channel.ChannelConfig;

public class PassthroughChannelBuilder implements IChannelBuilder {
    @Override
    public BaseChannel build(ChannelConfig simConfig) {
        return new PassthroughChannel();
    }
}
