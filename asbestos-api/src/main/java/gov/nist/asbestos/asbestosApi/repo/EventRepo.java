package gov.nist.asbestos.asbestosApi.repo;

import gov.nist.asbestos.asbestosApi.Event;
import gov.nist.asbestos.asbestosApi.EventId;
import gov.nist.asbestos.asbestosApi.TestSession;
import gov.nist.asbestos.sharedObjects.ChannelConfig;

public interface EventRepo {
    Event getEvent(TestSession testSession, ChannelConfig channel, EventId eventId);
    Event getEvent(EventId eventId);
}
