package gov.nist.asbestos.testEngine;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

class TestEngineTest {

    @Test
    void test1() throws URISyntaxException {
        File test1 = Paths.get(getClass().getResource("/fixtures/simple/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = new TestEngine(test1, new URI(""));
        testEngine.run();
    }
}
