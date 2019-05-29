package gov.nist.asbestos.asbestosProxy.log;


import gov.nist.asbestos.asbestosProxy.events.EventStore;

public class Task {
    private EventStore event;
    private int taskIndex;

    public static final int CLIENT_TASK = -1;

    public Task(EventStore event, int index) {
        this.event = event;
        this.taskIndex = index;
    }

    public void select() {
        event.selectTask(taskIndex);
    }

    public EventStore getEventStore() {
        return event;
    }
}
