package gov.nist.asbestos.api.repo;

import gov.nist.asbestos.api.Event;
import gov.nist.asbestos.api.EventId;
import gov.nist.asbestos.api.TestSession;
import gov.nist.asbestos.sharedObjects.ChannelConfig;

public interface EventRepo {
    Event getEvent(TestSession testSession, ChannelConfig channel, EventId eventId);
    Event getEvent(EventId eventId);
}
