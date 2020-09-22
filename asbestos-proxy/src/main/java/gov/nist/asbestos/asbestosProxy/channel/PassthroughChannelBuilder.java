package gov.nist.asbestos.asbestosProxy.channel;

import gov.nist.asbestos.asbestosProxy.channels.passthrough.PassthroughChannel;
import gov.nist.asbestos.client.channel.BaseChannel;

public class PassthroughChannelBuilder implements IChannelBuilder {
    @Override
    public BaseChannel build() {
        return new PassthroughChannel();
    }
}
