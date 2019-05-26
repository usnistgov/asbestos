package gov.nist.asbestos.asbestosProxy.events;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.io.File;

@JsonIgnoreProperties(value = {"file"})
public class EventStoreItem {
    String eventId;
    String actor;
    String resource;
    String verb;
    File file;

    public String asJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static EventStoreItem fromJson(String item) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(item, EventStoreItem.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
