package gov.nist.asbestos.simapi.validation;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ValFactory {

    public static String toJson(Val val) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(val);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public static String toJson(ValErrors val) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(val);
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
