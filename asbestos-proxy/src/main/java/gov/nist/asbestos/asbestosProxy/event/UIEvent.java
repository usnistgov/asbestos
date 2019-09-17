package gov.nist.asbestos.asbestosProxy.event;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class UIEvent {
    private String eventName;
    private String resourceType;
    private List<UITask> tasks = new ArrayList<>();

    public UIEvent(File eventDir) {
        List<String> parts = Reader.dirListingAsStringList(eventDir);
        int i = 0;
        for (String part : parts) {
            UITask uiTask = new UITask(eventDir, part);
            uiTask.setLabel(part);
            uiTask.setIndex(i++);
            tasks.add(uiTask);
        }
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }
}
