package gov.nist.asbestos.client.Base;

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
