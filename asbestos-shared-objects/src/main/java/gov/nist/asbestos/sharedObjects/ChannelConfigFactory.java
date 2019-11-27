package gov.nist.asbestos.sharedObjects;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileOutputStream;

public class ChannelConfigFactory {

    public static ChannelConfig load(File file) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(file, ChannelConfig.class);
        } catch (Throwable e ) {
            throw new RuntimeException(e);
        }
    }

    public static void store(ChannelConfig config, File file) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new FileOutputStream(file), config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ChannelConfig convert(String string) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(string, ChannelConfig.class);
        } catch (Exception e ) {
            throw new RuntimeException(e);
        }
    }

    public static String convert(ChannelConfig config) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
