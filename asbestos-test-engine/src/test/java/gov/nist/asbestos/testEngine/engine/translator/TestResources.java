package gov.nist.asbestos.testEngine.engine.translator;

import gov.nist.asbestos.client.Base.ProxyBase;
import org.hl7.fhir.r4.model.BaseResource;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestResources {

//    public static String get(String path) throws URISyntaxException, IOException {
//        return new String(Files.readAllBytes(Paths.get(asFile(path).toString())));
//    }

    public static File asFile(File base, String file) throws URISyntaxException {
        return Paths.get(TestResources.class.getResource(new File(base, file).toString()).toURI()).toFile();
    }

    public static BaseResource asResource(File base, String file) throws URISyntaxException {
        return ProxyBase.parse(asFile(base, file));
    }
}
