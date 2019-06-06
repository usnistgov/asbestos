package gov.nist.asbestos.asbestosProxy.events;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.File;
import java.util.Objects;

@JsonIgnoreProperties(value = {"file"})
public class EventStoreItem {
    String eventId;
    String actor;
    String resource;
    String verb;
    File file;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getVerb() {
        return verb;
    }

    public void setVerb(String verb) {
        this.verb = verb;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventStoreItem that = (EventStoreItem) o;
        return Objects.equals(eventId, that.eventId) &&
                Objects.equals(actor, that.actor) &&
                Objects.equals(resource, that.resource) &&
                Objects.equals(verb, that.verb) &&
                Objects.equals(file, that.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, actor, resource, verb, file);
    }
}
