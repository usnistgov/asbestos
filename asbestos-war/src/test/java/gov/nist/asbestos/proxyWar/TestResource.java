package gov.nist.asbestos.proxyWar;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestResource {

    public static String get(String path) throws URISyntaxException, IOException {
        File file =  Paths.get(TestResource.class.getClassLoader().getResource("./validation/patient.xml").toURI()).toFile();
        return new String(Files.readAllBytes(Paths.get(file.toString())));
    }
}
