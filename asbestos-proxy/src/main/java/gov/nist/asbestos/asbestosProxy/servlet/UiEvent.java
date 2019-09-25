package gov.nist.asbestos.asbestosProxy.servlet;

import gov.nist.asbestos.asbestosProxy.requests.Dirs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class UiEvent {
    public String eventName;
    public String resourceType;
    List<Task> tasks = new ArrayList<>();

    public UiEvent(File eventDir) {
        List<String> parts = Dirs.dirListingAsStringList(eventDir);
        int i = 0;
        for (String part : parts) {
            Task task = new Task(eventDir, part);
            task.label = part;
            task.index = i++;
            tasks.add(task);
        }
    }
}
