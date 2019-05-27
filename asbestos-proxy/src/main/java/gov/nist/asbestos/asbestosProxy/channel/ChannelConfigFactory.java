package gov.nist.asbestos.asbestosProxy.channel;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileOutputStream;

public class ChannelConfigFactory {

    public static ChannelConfig load(File file) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(file, ChannelConfig.class);
        } catch (Exception e ) {
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
}
