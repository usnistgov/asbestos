package gov.nist.asbestos.testEngine.engine.translator;

import gov.nist.asbestos.client.Base.ProxyBase;
import org.hl7.fhir.r4.model.TestScript;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixtureInOutTest {

    @Test
    void run() throws URISyntaxException {
        File base = new File("/translator/featureInOutTest");
        File testDef = TestResources.asFile(base, "script.xml").getParentFile();

        TestScript translated = ScriptTranslator.run(testDef);

        TestScript expected = (TestScript) ProxyBase.parse(TestResources.asFile(base, "result.xml"));

        assertTrue(translated.hasTest());
        assertEquals(1, translated.getTest().size());
        assertTrue(translated.getTest().get(0).hasAction());
        assertTrue(translated.getTest().get(0).getAction().get(0).hasOperation());
        TestScript.SetupActionOperationComponent op = translated.getTest().get(0).getAction().get(0).getOperation();
        assertEquals("pdb-bundle", op.getSourceId());
        assertEquals("pdb-response", op.getResponseId());
    }
}
