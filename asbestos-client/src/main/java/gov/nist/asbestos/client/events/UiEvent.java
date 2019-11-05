package gov.nist.asbestos.client.events;

import gov.nist.asbestos.client.Base.Dirs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class UiEvent {
    public String eventName;
    public String resourceType;
    List<UITask> tasks = new ArrayList<>();

    public UiEvent(File eventDir) {
        List<String> parts = Dirs.dirListingAsStringList(eventDir);
        int i = 0;
        for (String part : parts) {
            UITask task = new UITask(eventDir, part);
            task.label = part;
            task.index = i++;
            tasks.add(task);
        }
    }
}
