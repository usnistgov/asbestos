package gov.nist.asbestos.api;

import gov.nist.asbestos.sharedObjects.ChannelConfig;

public interface Event {
    Event resend(TestSession testSession, ChannelConfig channel);
}
