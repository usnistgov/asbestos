package gov.nist.asbestos.client.Base;

import gov.nist.asbestos.client.events.UIEvent;

public class EventContext {
    private String testSession;
    private String channelId;
    private String eventId;
    private boolean requestFocus;  // focus on the request

    public EventContext(String testSession, String channelId, String eventId, boolean requestFocus) {
        this.testSession = testSession;
        this.channelId = channelId;
        this.eventId = eventId;
        this.requestFocus = requestFocus;
    }

    public EventContext(UIEvent event) {
        this.testSession = event.getTestSession();
        this.channelId = event.getChannelId();
        this.eventId = event.getEventName();
        this.requestFocus = false;  // irrelevant
    }

    public String getTestSession() {
        return testSession;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getEventId() {
        return eventId;
    }

    public boolean isRequestFocus() {
        return requestFocus;
    }
}
