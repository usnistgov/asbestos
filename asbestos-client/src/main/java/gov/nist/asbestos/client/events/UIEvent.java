package gov.nist.asbestos.client.events;

import gov.nist.asbestos.client.Base.EC;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UIEvent {
    private String hostPort;
    private String testSession;
    private String channelId;
    private String eventName;
    private String resourceType;
    private List<UITask> tasks = new ArrayList<>();
    private EC ec;

    public UIEvent(EC ec) {
        this.ec = ec;
    }

    public UIEvent fromEventDir(File eventDir) {
        List<String> parts = Reader.dirListingAsStringList(eventDir);
        int i = 0;
        for (String part : parts) {
            UITask uiTask = new UITask(eventDir, part);
            uiTask.setLabel(part);
            uiTask.setIndex(i++);
            tasks.add(uiTask);
        }
        return this;
    }

    UIEvent fromParms(String testSession, String channelId, String resourceType, String eventName) {
        File fhir = ec.fhirDir(testSession, channelId);
        if (resourceType.equals("null")) {
            resourceType = ec.resourceTypeForEvent(fhir, eventName);
            if (resourceType == null) {
                return null;
            }
        }
        File resourceTypeFile = new File(fhir, resourceType);
        File eventDir = new File(resourceTypeFile, eventName);

        UIEvent uiEvent = fromEventDir(eventDir);
        uiEvent.eventName = eventName;
        uiEvent.resourceType = resourceType;
        return uiEvent;
    }

    public UIEvent fromURI(URI uri) {
        List<String> parts = Arrays.asList(uri.toString().split("/"));
        for (int i=0; i<parts.size(); i++) {
            String part = parts.get(i);
            if ("asbestos".equals(part) && "log".equals(parts.get(i+1))) {
                String testSession = parts.get(i+2);
                String channelId = parts.get(i+3);
                String resourceType = parts.get(i+4);
                String eventName = parts.get(i+5);
                return fromParms(testSession, channelId, resourceType, eventName);
            }
        }
        return null;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public URI getURI() {
        try {
            return new URI("http://" +
            hostPort +
            "/asbestos/log/" +
            testSession + "/" +
            channelId + "/" +
            resourceType + "/" +
            eventName);
        } catch (URISyntaxException e) {
            throw new Error(e);
        }

    }

    public void setHostPort(String hostPort) {
        this.hostPort = hostPort;
    }

    public void setTestSession(String testSession) {
        this.testSession = testSession;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }
}
