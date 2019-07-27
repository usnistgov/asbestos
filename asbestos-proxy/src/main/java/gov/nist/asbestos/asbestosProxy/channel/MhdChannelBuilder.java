package gov.nist.asbestos.asbestosProxy.channel;

import gov.nist.asbestos.asbestosProxy.channels.mhd.MhdChannel;

public class MhdChannelBuilder implements IChannelBuilder{
    @Override
    public BaseChannel build() {
        return new MhdChannel();
    }
}
