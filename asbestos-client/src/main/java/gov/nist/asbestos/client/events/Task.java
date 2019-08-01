package gov.nist.asbestos.client.events;


public class Task {
    private EventStore eventStore;
    private int taskIndex;
    private Event event;

    public static final int CLIENT_TASK = -1;

    public Task(Event event, int index) {
        this.event = event;
        this.eventStore = event.getStore();
        this.taskIndex = index;
    }

    public void select() {
        eventStore.selectTask(taskIndex);
    }

    public EventStore getEventStore() {
        return eventStore;
    }

    int getTaskIndex() {
        return taskIndex;
    }

    void setEventStore(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    public Event getEvent() {
        return event;
    }
}
