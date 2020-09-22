package gov.nist.asbestos.client.general;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileOutputStream;

public class GenericJSFactory {

    public static <T> T load(File file, Class<?> clazz) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JavaType type = objectMapper.getTypeFactory().constructType(clazz);
            return objectMapper.readValue(file, type);
        } catch (Exception e ) {
            throw new RuntimeException(e);
        }
    }

    public static <T> void store(T config, File file) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new FileOutputStream(file), config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T convert(String string, Class<?> clazz) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JavaType type = objectMapper.getTypeFactory().constructType(clazz);
            return objectMapper.readValue(string, type);
        } catch (Exception e ) {
            throw new RuntimeException(e);
        }
    }

    public static <T> String convert(T config) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
