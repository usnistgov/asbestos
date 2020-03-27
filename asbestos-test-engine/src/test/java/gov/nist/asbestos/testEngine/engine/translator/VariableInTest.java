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

    // variable is given value in script, listed as a parameter to component and referenced
    // in the test.  It must be updated.
    @Test
    void run() throws URISyntaxException {
        File base = new File("/translator/variableInTest");
        File testDef = TestResources.asFile(base, "script.xml").getParentFile();

        ScriptTranslator translator = new ScriptTranslator(testDef);
        TestScript translated = translator.run();

        assertTrue(translated.hasTest());
        assertEquals(1, translated.getTest().size());
        assertTrue(translated.getTest().get(0).hasAction());

        assertTrue(translated.getTest().get(0).getAction().get(0).hasAssert());

        TestScript.SetupActionAssertComponent as = translated.getTest().get(0).getAction().get(0).getAssert();
        assertEquals("patient", as.getSourceId());
        assertEquals("${familyName}", as.getValue());
    }
}
