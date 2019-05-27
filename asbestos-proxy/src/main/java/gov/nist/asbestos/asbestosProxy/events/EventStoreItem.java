package gov.nist.asbestos.asbestosProxy.events;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.io.File;

@JsonIgnoreProperties(value = {"file"})
public class EventStoreItem {
    String eventId;
    String actor;
    String resource;
    String verb;
    File file;
}
