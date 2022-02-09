package gov.nist.asbestos.client.channel;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ChannelConfigFactory {
    private static Logger log = Logger.getLogger(ChannelConfigFactory.class.getName());

    public static ChannelConfig load(File file) {
        try {
            ObjectMapper objectMapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)    ;
            return objectMapper.readValue(file, ChannelConfig.class);
        } catch (Throwable e ) {
            log.log(Level.SEVERE, "ChannelConfig load failed.", e);
            throw new RuntimeException(e);
        }
    }

    public static void store(ChannelConfig config, File file) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            try (FileOutputStream fos = new FileOutputStream(file);
                OutputStreamWriter outputFile = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                outputFile.write(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config));
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "ChannelConfig store failed.", e);
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
