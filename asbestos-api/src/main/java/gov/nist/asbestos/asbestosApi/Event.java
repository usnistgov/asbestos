package gov.nist.asbestos.asbestosApi;

import gov.nist.asbestos.sharedObjects.ChannelConfig;

public interface Event {
    Event resend(TestSession testSession, ChannelConfig channel);
}
