package gov.nist.asbestos.asbestosProxy.event;

import gov.nist.asbestos.asbestosProxy.servlet.ProxyLogServlet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Event {
    String eventName;
    String resourceType;
    List<Task> tasks = new ArrayList<>();

    public Event(File eventDir) {
        List<String> parts = Reader.dirListingAsStringList(eventDir);
        int i = 0;
        for (String part : parts) {
            Task task = new Task(eventDir, part);
            task.setLabel(part);
            task.setIndex(i++);
            tasks.add(task);
        }
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }
}
