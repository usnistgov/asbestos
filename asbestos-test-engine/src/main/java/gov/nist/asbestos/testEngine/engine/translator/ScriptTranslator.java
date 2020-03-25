package gov.nist.asbestos.testEngine.engine.translator;

import gov.nist.asbestos.client.Base.ProxyBase;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.TestScript;

import java.io.File;

public class ScriptTranslator {
    private File testDef;
    private TestScript script;
    private TestScript output = new TestScript();

    public ScriptTranslator(File testDef) {
        this.testDef = testDef;
        this.script = (TestScript) ProxyBase.parse(new File(testDef, "script.xml"));
    }

    // Compiles script and returns resulting TestScript
    public TestScript run() {

        // scan all tests for imports and load them
        if (script.hasTest()) {
            for (TestScript.TestScriptTestComponent test : script.getTest()) {
                if (test.hasModifierExtension()) {
                    for (Extension e : test.getModifierExtension()) {
                        if (e.getUrl().equals("urn:import")) {
                            ComponentReference ref = new ComponentReference(testDef, e.getExtension());
                            ref.loadComponentHeader();
                            resolve(ref, test);
                        }
                    }
                }
            }
        }

        return output;
    }

    private void resolve(ComponentReference ref, TestScript.TestScriptTestComponent test) {
        // within test, update
        // requestId, sourceId, targetId from parameter-in
        // responseId from parameter-out

    }
}
