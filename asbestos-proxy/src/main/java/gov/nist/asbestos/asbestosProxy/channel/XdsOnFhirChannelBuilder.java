package gov.nist.asbestos.asbestosProxy.channel;

import gov.nist.asbestos.asbestosProxy.channels.mhd.XdsOnFhirChannel;

public class XdsOnFhirChannelBuilder implements IChannelBuilder{
    @Override
    public BaseChannel build() {
        return new XdsOnFhirChannel();
    }
}
