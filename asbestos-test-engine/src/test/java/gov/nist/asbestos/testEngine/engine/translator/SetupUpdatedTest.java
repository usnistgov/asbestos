package gov.nist.asbestos.testEngine.engine.translator;

import gov.nist.asbestos.client.Base.ProxyBase;
import org.hl7.fhir.r4.model.TestScript;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

class SetupUpdatedTest {

    // Import component containing one setup (one action)
    @Test
    void setupUpdated() throws URISyntaxException {
        File base = new File("/translator/setupUpdatedTest");
        File testDef = TestResources.asFile(base, "script.xml").getParentFile();

        ScriptTranslator translator = new ScriptTranslator(testDef);
        TestScript translated = translator.run();

        assertTrue(translator.getErrors().isEmpty());

        // script has the one setup/action
        assertTrue(translated.hasSetup());
        assertTrue(translated.getSetup().hasAction());

        assertTrue(translated.getSetup().getAction().get(0).hasOperation());
        TestScript.SetupActionOperationComponent op = translated.getSetup().getAction().get(0).getOperation();
        assertEquals("pdb-bundle", op.getSourceId());
        assertEquals("pdb-response", op.getResponseId());
    }

    // action used in setup is not coded for setup in component
    @Test
    void setupReferencesActions() {
        assertTrue(false);
    }

}
