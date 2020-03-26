package gov.nist.asbestos.testEngine.engine.translator;

import gov.nist.asbestos.client.Base.ProxyBase;
import org.hl7.fhir.r4.model.TestScript;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FixtureInOutTest {

    @Test
    void extractComponentReferences() throws URISyntaxException {
        File base = new File("/translator/featureInOutTest");
        File testDef = TestResources.asFile(base, "script.xml").getParentFile();
        TestScript script = (TestScript) ProxyBase.parse(new File(testDef, "script.xml"));
        TestScript.TestScriptTestComponent test = script.getTestFirstRep();

        ScriptTranslator translator = new ScriptTranslator(testDef);
        ComponentReferences refs = translator.extractComponentReferences(test);

        assertEquals(1, refs.getReferences().size()); // single component reference
        ComponentReference ref = refs.getReferences().get(0);
        assertEquals(1, ref.getIn().size());
        assertEquals("pdb-bundle", ref.getIn().get(0).getCallerName());
        assertEquals("pdbRequest", ref.getIn().get(0).getLocalName());
        assertEquals(1, ref.getOut().size());
        assertNotNull(ref.getComponent());
    }

    // Import component containing one test (one action)
    @Test
    void inTest() throws URISyntaxException {
        File base = new File("/translator/featureInOutTest");
        File testDef = TestResources.asFile(base, "script.xml").getParentFile();

        ScriptTranslator translator = new ScriptTranslator(testDef);
        TestScript translated = translator.run();

        assertTrue(translator.getErrors().isEmpty());

        // script has the one test/action
        assertTrue(translated.hasTest());
        assertEquals(1, translated.getTest().size());
        assertTrue(translated.getTest().get(0).hasAction());

        assertTrue(translated.getTest().get(0).getAction().get(0).hasOperation());
        TestScript.SetupActionOperationComponent op = translated.getTest().get(0).getAction().get(0).getOperation();
        assertEquals("pdb-bundle", op.getSourceId());
        assertEquals("pdb-response", op.getResponseId());
    }

    @Test
    void scriptWithOtherModifierExtensionsPresent() {
        assertTrue(false);
    }

    @Test
    void componentWithOtherExtensionsPresent() {
        assertTrue(false);
    }

}
