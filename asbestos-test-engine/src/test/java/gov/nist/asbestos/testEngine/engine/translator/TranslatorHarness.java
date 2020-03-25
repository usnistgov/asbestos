package gov.nist.asbestos.testEngine.engine.translator;

import gov.nist.asbestos.client.Base.ProxyBase;
import org.hl7.fhir.r4.model.TestScript;

import java.io.File;

public class TranslatorHarness {
    private File testDef;

    public TranslatorHarness(File testDef) {
        this.testDef = testDef;
    }

    public TestScript getScript() {
        return (TestScript) ProxyBase.parse(new File(testDef, "script.xml"));
    }

    public TestScript getComponent() {
        return (TestScript) ProxyBase.parse(new File(testDef, "component.xml"));
    }

    public TestScript getResult() {
        return (TestScript) ProxyBase.parse(new File(testDef, "result.xml"));
    }
}
