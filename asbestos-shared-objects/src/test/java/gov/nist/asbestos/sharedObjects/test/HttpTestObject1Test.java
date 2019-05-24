package gov.nist.asbestos.sharedObjects.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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

    @Test
    void serializeUnserializeWithNested() {
        ObjectMapper objectMapper = new ObjectMapper();
        HttpTestObject1 to = new HttpTestObject1();
        to.field1 = "Dr";
        to.field2 = "Who";
        to.list = new ArrayList<>();
        to.list.add("foo");
        HttpTestObject2 obj2 = new HttpTestObject2();
        obj2.field3 = "sam";
        obj2.field4 = "iam";
        obj2.object1 = to;
        String str;
        try {
            str = objectMapper.writeValueAsString(obj2);
        } catch (JsonProcessingException e) {
            fail(e.getMessage());
            return;
        }

        HttpTestObject2 to2;
        try {
            to2 = objectMapper.readValue(str, HttpTestObject2.class);
        } catch (IOException e) {
            fail(e.getMessage());
            return;
        }
        assertEquals(obj2, to2);
    }
}
