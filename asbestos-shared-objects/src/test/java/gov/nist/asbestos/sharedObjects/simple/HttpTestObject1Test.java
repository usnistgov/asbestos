package gov.nist.asbestos.sharedObjects.simple;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class HttpTestObject1Test {

    @Test
    void serializeUnserialize() {
        ObjectMapper objectMapper = new ObjectMapper();
        HttpTestObject1 to = new HttpTestObject1();
        to.field1 = "Dr";
        to.field2 = "Who";
        to.list = new ArrayList<>();
        to.list.add("foo");
        String str;
        try {
            str = objectMapper.writeValueAsString(to);
        } catch (JsonProcessingException e) {
            fail(e.getMessage());
            return;
        }

        HttpTestObject1 to2;
        try {
            to2 = objectMapper.readValue(str, HttpTestObject1.class);
        } catch (IOException e) {
            fail(e.getMessage());
            return;
        }
        assertEquals(to, to2);
    }
}
