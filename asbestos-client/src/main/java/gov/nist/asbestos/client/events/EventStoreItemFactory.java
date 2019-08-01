package gov.nist.asbestos.client.events;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EventStoreItemFactory {

    public static String asJson(EventStoreItem item) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(item);
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
