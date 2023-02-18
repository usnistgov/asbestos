package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.events.UIEvent;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.client.resolver.ResourceWrapperBuilder;
import gov.nist.asbestos.simapi.validation.Val;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClientApiTest {

    @Test
    void comprehensiveTest() throws URISyntaxException {
        File testDef = Paths.get(getClass().getResource("/clientApi/tests/comprehensive/TestScript.xml").toURI()).getParent().toFile();
        assertTrue(testDef.exists());
        assertTrue(testDef.isDirectory());

        File eventDir = Paths.get(getClass().getResource("/clientApi/messages/comprehensive/task0/description.txt").toURI()).getParent().getParent().toFile();
        assertTrue(eventDir.exists());
        assertTrue(eventDir.isDirectory());

        EC ec = new EC(Paths.get(getClass().getResource("/clientApi/ec/ec.txt").toURI()).getParent().toFile());  // not used
        UIEvent event = new UIEvent(ec).fromEventDir(eventDir);
        assertEquals(2, event.getTaskCount());

        ResourceWrapper requestWrapper = ResourceWrapperBuilder.fromUIEvent(eventDir, 0, true);
        assertNotNull(requestWrapper);

        ResourceWrapper responseWrapper = ResourceWrapperBuilder.fromUIEvent(eventDir, 1, true);
        assertNotNull(responseWrapper);

        List<String> errors = new TestEngine(testDef, null)
                .setVal(new Val())
                .setTestSession("default")
                .setExternalCache(ec.externalCache)
                .runEval(requestWrapper, responseWrapper, false)
                .getTestReportErrors();


        assertTrue(errors.isEmpty());

    }
}
