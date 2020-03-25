package gov.nist.asbestos.testEngine.engine.translator;

import gov.nist.asbestos.client.Base.ProxyBase;
import org.hl7.fhir.r4.model.TestScript;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VariableInTest {

    @Test
    void run() throws IOException, URISyntaxException {
        File base = new File("/translator/variableInTest");
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
