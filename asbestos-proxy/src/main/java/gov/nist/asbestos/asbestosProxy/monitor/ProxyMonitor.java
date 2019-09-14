package gov.nist.asbestos.asbestosProxy.monitor;

import gov.nist.asbestos.client.events.Event;
import gov.nist.asbestos.simapi.simCommon.SimId;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class ProxyMonitor {

    private class EventNotification {
        SimId simId;
        Event event;
        boolean canceled = false;

        EventNotification(SimId simId, Event event) {
            this.simId = simId;
            this.event = event;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EventNotification that = (EventNotification) o;
            return Objects.equals(simId, that.simId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(simId);
        }
    }


    private final List<EventNotification> monitors = Collections.synchronizedList(new LinkedList<>());

    public Event waitForEvent(SimId simId) throws InterruptedException {
        EventNotification notification = new EventNotification(simId, null);
        if (monitors.contains(notification))
            throw new Error("Busy");
        synchronized (monitors) {
            monitors.add(notification);
            while (monitors.contains(notification) && notification.event == null && !notification.canceled) {
                monitors.wait(30 * 1000);
            }
            int i = monitors.indexOf(notification);
            EventNotification not = monitors.remove(i);
            return not.event;
        }
    }

    public void newEvent(SimId simId, Event event) {
        EventNotification notification = find(simId);
        if (notification == null)
            return;
        synchronized  (monitors) {
            notification.event = event;
            monitors.notify();
        }
    }

    public void cancel(SimId simId) {
        EventNotification notification = find(simId);
        if (notification == null)
            return;
        synchronized  (monitors) {
            notification.canceled = true;
            monitors.notify();
        }
    }


    private EventNotification find(SimId simId) {
        for (EventNotification not : monitors) {
            if (not.simId.equals(simId))
                return not;
        }
        return null;
    }
}
