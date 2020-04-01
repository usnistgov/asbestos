package gov.nist.asbestos.testEngine.engine;

import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ModularEngine {
    private List<TestEngine> engines = new ArrayList<>();

    public ModularEngine(File testDefDir) {
        TestEngine testEngine = new TestEngine(testDefDir);
        engines.add(testEngine);
        testEngine.setModularEngine(this);
    }

    public ModularEngine(File testDefDir, URI sut) {
        TestEngine testEngine = new TestEngine(testDefDir, sut);
        engines.add(testEngine);
        testEngine.setModularEngine(this);
    }

    public ModularEngine add(TestEngine engine) {
        engines.add(engine);
        return this;
    }

    public TestEngine getLastTestEngine() {
        return engines.get(engines.size() - 1);
    }

    public TestScript getTestScript() {
        return getLastTestEngine().getTestScript();
    }

    public TestReport getTestReport() {
        return getLastTestEngine().getTestReport();
    }

    public List<TestEngine> getTestEngines() {
        return engines;
    }

}
