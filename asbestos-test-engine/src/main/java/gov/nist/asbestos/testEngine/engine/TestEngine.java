package gov.nist.asbestos.testEngine.engine;

import ca.uhn.fhir.parser.IParser;
import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.debug.StopDebugTestScriptException;
import gov.nist.asbestos.client.debug.TestScriptDebugInterface;
import gov.nist.asbestos.client.debug.TestScriptDebugState;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceCacheMgr;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureComponent;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureMgr;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureSub;
import gov.nist.asbestos.testEngine.engine.translator.ComponentDefinition;
import gov.nist.asbestos.testEngine.engine.translator.ComponentReference;
import gov.nist.asbestos.testEngine.engine.translator.Parameter;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * See http://hl7.org/fhir/testing.html
 */
public class TestEngine  implements TestDef {
    private static Logger log = Logger.getLogger(TestEngine.class.getName());

    private File testDef = null; // directory holding test definition
    private String testScriptName = null;   // name of testscript file, TestScript.xml by default
    private String multiUseTestScriptName = null;
    private URI sut = null;
    TestEngine parent = null;

    // current script and report
    private TestScript testScript = null;
    private TestReport testReport = new TestReport();
    // all scripts and reports.  [0] is top level, all others are called modules
    private final List<TestScript> testScripts = new ArrayList<>();
    private final List<TestReport> testReports = new ArrayList<>();

    private boolean failOverride = false;
    private final FixtureMgr fixtureMgr = new FixtureMgr();
    private Val val;
    private ValE engineVal;
    private FhirClient fhirClient = null;
    private FhirClient fhirClientForFixtures;
    private List<String> errors;
    private String testSession = null;
    private String channelId = null;   // testSession __ channelName
    private File externalCache = null;
    private String testCollection = null;
    private String testId = null;
    private boolean isRequest = false;  // running an eval on a request message?  alternative is regular server test
    public static final String LAST_OP = "_LAST_OP_";

    private ModularEngine modularEngine = null;
    private Map<String, String> callFixtureMap = new HashMap<>();
    private Map<String, String> callVariableMap = new HashMap<>();
    private Map<String, String> externalVariables = new HashMap<>();
    private TestScriptDebugInterface debugger = null;
    private List<Parameter> fixtureOutParams;

    /**
     *
     * @param testDef  directory containing test definition
     * @param sut base address of fhir server under test
     */
    public TestEngine(File testDef, URI sut, Set<String> moduleIds) {
        this();
        Objects.requireNonNull(testDef);
        Objects.requireNonNull(sut);
        Objects.requireNonNull(Ref.asURL(sut));
        setTestDef(testDef);
        this.sut = sut;
        // make test definition dir a temporary resource cache so elements of the TestScript
        // can be found
        // this.testDef is important.  testDef, the parameter can be a path to a testscript.
        // this.testDef is always the test definition directory
        ResourceCacheMgr inTestResources = new ResourceCacheMgr(this.testDef, new Ref(""));
        fhirClientForFixtures = new FhirClient().setResourceCacheMgr(inTestResources);
        fixtureMgr.setFhirClient(fhirClientForFixtures);
    }

    // used for evaluation including in the Inspector
    public TestEngine(File testDef, Set<String> moduleIds) {
        this();
        Objects.requireNonNull(testDef);
        setTestDef(testDef);
        ResourceCacheMgr inTestResources = new ResourceCacheMgr(testDef, new Ref(""));
        fhirClientForFixtures = new FhirClient().setResourceCacheMgr(inTestResources);
        fixtureMgr.setFhirClient(fhirClientForFixtures);
    }

    // used for client tests
    public TestEngine(File testDef, TestScript testScript, Set<String> moduleIds) {
        this();
        setTestDef(testDef);
        this.testScript = testScript;
        ResourceCacheMgr inTestResources = new ResourceCacheMgr(testDef, new Ref(""));
        fhirClientForFixtures = new FhirClient().setResourceCacheMgr(inTestResources);
        fixtureMgr.setFhirClient(fhirClientForFixtures);
    }

    private TestEngine() {
        if (moduleIds != null) {
            this.moduleIds.addAll(moduleIds);
        }
    }

    public TestEngine setFixtures(Map<String, FixtureComponent> fixtures) {
        fixtureMgr.putAll(fixtures);
        return this;
    }

    public TestEngine setExternalVariables(Map<String, String> externalVariables) {
        this.externalVariables =externalVariables;
        return this;
    }

    public TestEngine setTestSession(String testSession) {
        this.testSession = testSession;
        return this;
    }

    public TestEngine setChannelId(String channelId) {
        if (channelId.split("__").length != 2)
            throw new Error("TestEngine: channelId must be testSession__channelId format - found " + channelId );
        this.channelId = channelId;
        return this;
    }

    public TestEngine setExternalCache(File externalCache) {
        this.externalCache = externalCache;
        return this;
    }

    private void clearTestLog() throws IOException {
        if (channelId == null || testCollection == null || testId == null)
            return;
        File testLogDir = this.getEC().getTestLogDir(channelId, testCollection, testId);
        if (! testLogDir.exists())
            return;
        Path path = testLogDir.toPath();
        try (Stream<Path> walk = Files.walk(path)) {
            walk
                    .sorted(Comparator.reverseOrder())
                    .forEach(this::deleteLogDirectory);
        } finally {
            log.info("Exiting clear Test log directory: " + testLogDir.toString());
        }
    }

    private void deleteLogDirectory(Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            log.warning(String.format("Unable to delete log path %s: %s", path, e.getMessage()));
        }
    }

    public TestEngine runTest() {
        Objects.requireNonNull(val);
        Objects.requireNonNull(testSession);
        Objects.requireNonNull(externalCache);
        engineVal = new ValE(val);
        engineVal.setMsg("TestEngine");
        if (testId == null || testCollection == null) {
            String[] parts = testDef.toString().split(Pattern.quote(File.separator));
            if (parts.length > 1) {
                setTestId(parts[parts.length - 1]);
                setTestCollection(parts[parts.length - 2]);
            }
        }
        try {
            clearTestLog();
            doWorkflow();
        }
        catch (StopDebugTestScriptException sdex) {
            if (debugger.getState().hasParentExecutionIndex())
                throw sdex;
        }
        catch (Throwable t) {
            addTerminalFailureToTestReport("TestEngine runTest Error: " + t);
        }
        //returnTestReport();

        return this;
    }

    // TODO - This interpretation of TestScript does not use assert.direction for client tests.  Instead it uses separate fixtures for request and response. This is reflected in the tests written.

    // if inputResource == null then this is a test
    // if null then this is an evaluation
    public TestEngine runEval(ResourceWrapper requestResource, ResourceWrapper responseResource, boolean skipAll) {
        Objects.requireNonNull(val);
        Objects.requireNonNull(testSession);
        Objects.requireNonNull(externalCache);
        isRequest = true;
        engineVal = new ValE(val);
        engineVal.setMsg("TestEngine");
        try {
            initWorkflow();
            doPreProcessing();
            doLoadVariables();
            doLoadFixtures();
            if (requestResource != null)
                fixtureMgr.add("request", requestResource);
                //fixtureMgr.put("request", new FixtureComponent(requestResource));
            if (responseResource != null)
                fixtureMgr.add("response", responseResource);
                //fixtureMgr.put("response", new FixtureComponent(responseResource));
            if (! skipAll) {
                doAutoCreates();
                doSetup();
                doTest(); // should only be asserts
            }
            errorOut();
            fillInSkips();
            doLintTestReport();
        } catch (Throwable t) {
            addTerminalFailureToTestReport("TestEngine runEval Error: " + t);
        }
        //returnTestReport();

        return this;
    }

    public List<String> getTestReportErrors() {
        List<String> errors = new ArrayList<>();
        if (testReport.hasExtension()) {
            for (Extension extension : testReport.getExtension()) {
                if (extension.getUrl().equals(ExtensionDef.failure)) {
                    errors.add(extension.getValue().toString());
                }
            }
        }
        TestReport.TestReportSetupComponent testComponent = testReport.getSetup();
        for (TestReport.SetupActionComponent actionComponent : testComponent.getAction()) {
            if (actionComponent.hasAssert()) {
                TestReport.SetupActionAssertComponent assertComponent = actionComponent.getAssert();
                if (assertComponent.hasResult()) {
                    TestReport.TestReportActionResult actionResult = assertComponent.getResult();
                    if (actionResult.equals(TestReport.TestReportActionResult.ERROR) ||
                            actionResult.equals(TestReport.TestReportActionResult.FAIL))
                        errors.add(assertComponent.getMessage());
                }
            }
        }

        for (TestReport.TestReportTestComponent testComponent1 : testReport.getTest()) {
            for (TestReport.TestActionComponent actionComponent : testComponent1.getAction()) {
                if (actionComponent.hasAssert()) {
                    TestReport.SetupActionAssertComponent assertComponent = actionComponent.getAssert();
                    if (assertComponent.hasResult()) {
                        TestReport.TestReportActionResult actionResult = assertComponent.getResult();
                        if (actionResult.equals(TestReport.TestReportActionResult.ERROR) ||
                                actionResult.equals(TestReport.TestReportActionResult.FAIL))
                            errors.add(assertComponent.getMessage());
                    }
                }
            }
        }
        return errors;
    }

    public List<String> getTestReportWarnings() {
        List<String> errors = new ArrayList<>();
        if (testReport.hasExtension()) {
            for (Extension extension : testReport.getExtension()) {
                if (extension.getUrl().equals(ExtensionDef.failure)) {
                    errors.add(extension.getValue().toString());
                }
            }
        }
        TestReport.TestReportSetupComponent testComponent = testReport.getSetup();
        for (TestReport.SetupActionComponent actionComponent : testComponent.getAction()) {
            if (actionComponent.hasAssert()) {
                TestReport.SetupActionAssertComponent assertComponent = actionComponent.getAssert();
                if (assertComponent.hasResult()) {
                    TestReport.TestReportActionResult actionResult = assertComponent.getResult();
                    if (actionResult.equals(TestReport.TestReportActionResult.WARNING))
                        errors.add(assertComponent.getMessage());
                }
            }
        }

        for (TestReport.TestReportTestComponent testComponent1 : testReport.getTest()) {
            for (TestReport.TestActionComponent actionComponent : testComponent1.getAction()) {
                if (actionComponent.hasAssert()) {
                    TestReport.SetupActionAssertComponent assertComponent = actionComponent.getAssert();
                    if (assertComponent.hasResult()) {
                        TestReport.TestReportActionResult actionResult = assertComponent.getResult();
                        if (actionResult.equals(TestReport.TestReportActionResult.WARNING))
                            errors.add(assertComponent.getMessage());
                    }
                }
            }
        }
        return errors;
    }

    public TestReport returnExceptionAsTestReport(Throwable t) {
        testReport = new TestReport();
        addTerminalFailureToTestReport(t.toString());
        logTerminalFailure(t);
        logTestReport();
        return testReport;
    }

    private void logTerminalFailure(Throwable t) {
        log.log(Level.SEVERE, "logTerminalFailure", t);
    }

    private void addTerminalFailureToTestReport(String msg) {
        getTestReport().setStatus(TestReport.TestReportStatus.ENTEREDINERROR);
        getTestReport().setResult(TestReport.TestReportResult.FAIL);

        Extension extension = new Extension().setUrl(ExtensionDef.failure).setValue(new StringType(msg));
        getTestReport().getExtension().add(extension);

//        TestReport.TestReportSetupComponent setup = testReport.getSetup();
//        TestReport.SetupActionComponent comp = setup.addAction();
//        TestReport.SetupActionAssertComponent asComp = new TestReport.SetupActionAssertComponent();
//        asComp.setMessage(t.getMessage());
//        asComp.setResult(TestReport.TestReportActionResult.ERROR);
//        comp.setAssert(asComp);
//        propagateStatus(testReport);
    }

    private void logTestReport() {
        File logDir = new File(new File(externalCache, testSession), testDef.getName());
        logDir.mkdirs();
        TestReport testReport = getTestReport();
        String json = ParserBase.encode(testReport, Format.JSON);
        Path path = new File(logDir, "TestReport.json").toPath();
        try (BufferedWriter writer = Files.newBufferedWriter(path))
        {
            writer.write(json);
            writer.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void doWorkflow() {

        try {
            initWorkflow();
            doPreProcessing();
            if (errorOut()) return;
            doLoadVariables();
            if (errorOut()) return;
            doLoadFixtures();
            if (errorOut()) return;
            doAutoCreates();
            if (errorOut()) return;
            doSetup();
            if (errorOut()) return;
            doTest();
            if (errorOut()) return;
            doTearDown();
            fillInSkips();
            doLintTestReport();
        } catch (Exception ex) {
                String msg = ex.getMessage();
                if (msg == null || msg.equals("")) {
                    msg = "TestEngine#doWorkflow Error: Check server log for details.";
                }
                log.log(Level.SEVERE, msg, ex);
        } finally {
            doAutoDeletes();
            doPostProcessing();
            if (failOverride)
                testReport.setResult(TestReport.TestReportResult.FAIL);
        }
    }

    private void fillInSkips() {
        int scriptCount;
        int reportCount;

        scriptCount = testScript.getSetup().getAction().size();
        reportCount = testReport.getSetup().getAction().size();
        for (int i=reportCount; i<scriptCount; i++) {
            if (testScript.getSetup().getAction().get(i).hasOperation()) {
                testReport.getSetup().addAction().setOperation(
                        new TestReport.SetupActionOperationComponent().setResult(TestReport.TestReportActionResult.SKIP)
                );
            } else {
                testReport.getSetup().addAction().setAssert(
                        new TestReport.SetupActionAssertComponent().setResult(TestReport.TestReportActionResult.SKIP)
                );
            }
        }

        for (int testi=0; testi<testScript.getTest().size(); testi++) {
            scriptCount = testScript.getTest().get(testi).getAction().size();
            if (testReport.getTest().size() <= testi)
                testReport.addTest();
            reportCount = testReport.getTest().get(testi).getAction().size();
            for (int i=reportCount; i<scriptCount; i++) {
                if (testScript.getTest().get(testi).getAction().get(i).hasOperation()) {
                    testReport.getTest().get(testi).addAction().setOperation(
                            new TestReport.SetupActionOperationComponent().setResult(TestReport.TestReportActionResult.SKIP)
                    );
                } else {
                    testReport.getTest().get(testi).addAction().setAssert(
                            new TestReport.SetupActionAssertComponent().setResult(TestReport.TestReportActionResult.SKIP)
                    );
                }
            }
        }

        scriptCount = testScript.getTeardown().getAction().size();
        reportCount = testReport.getTeardown().getAction().size();
        for (int i=reportCount; i<scriptCount; i++) {
            if (testScript.getTeardown().getAction().get(i).hasOperation()) {
                testReport.getTeardown().addAction().setOperation(
                        new TestReport.SetupActionOperationComponent().setResult(TestReport.TestReportActionResult.SKIP)
                );
            }
        }
    }

    // number of actions in script and report must be the same
    // this goes for setup, test, teardown
    private void doLintTestReport() {
        List<TestScript.SetupActionComponent> scriptSetups = testScript.getSetup().getAction();
        List<TestReport.SetupActionComponent> reportSetups = testReport.getSetup().getAction();
        if (scriptSetups.size() != reportSetups.size()) {
            String msg = "TestEngine doLintTestReport script or report size Error: Script Setup had " +
                    scriptSetups.size() +
                    " elements but Report had " +
                    reportSetups.size();
            addTerminalFailureToTestReport(msg);
            log.severe(msg);
        }

        for (int i=0; i< testScript.getTest().size(); i++) {
            List<TestScript.TestActionComponent> tests = testScript.getTest().get(i).getAction();
            List<TestReport.TestActionComponent> reports;
            if (testReport.getTest().size() > i)
                reports = testReport.getTest().get(i).getAction();
            else
                reports = new ArrayList<>();
            if (tests.size() != reports.size()) {
                String msg = "TestEngine doLintTestReport tests or reports size Error: Script Test " + i + " had " +
                        tests.size() +
                        " elements but Report had " +
                        reports.size();
                addTerminalFailureToTestReport(msg);
                log.severe(msg);
            }
        }

        List<TestScript.TeardownActionComponent> script = testScript.getTeardown().getAction();
        List<TestReport.TeardownActionComponent> report;
        report = testReport.getTeardown().getAction();
        if (script.size() != report.size()) {
            String msg = "TestEngine internal Error: Script Teardown had " +
                    script.size() +
                    " elements but Report had " +
                    report.size();
            addTerminalFailureToTestReport(msg);
            log.severe(msg);
        }
    }

    private void initWorkflow() {
        if (testScript == null) {
            if (this.modularEngine == null) {
                testScript = loadTestScript(testDef, testScriptName);
                String path = testScript.getName();
                String name = "";
                if (path!=null && path.contains(File.separator)) {
                    String[] parts = path.split(Pattern.quote(File.separator));
                    name = parts[parts.length - 2];  // testId
                } else
                    name = path;
                testReport.setName(name);
                String def;
                if (testScriptName == null) {
                    File file;
                    file = new File(testDef, "TestScript.xml");
                    if (file.exists()) {
                        def = file.toString();
                    } else {
                        def = new File(testDef, "TestScript.json").toString();
                    }
                } else {
                    def = new File(testDef, testScriptName).toString();
                }
                testReport.setTestScript(new Reference(def));
            } else {
                ModularScripts modularScripts =  modularEngine.getModularScripts();
                if (modularScripts != null) {
                    File testScriptFile = findTestScriptFile(testDef, testScriptName);
                    String testScriptId = null;
                    String componentPart = null;
                    if (testScriptName == null) {
                        testScriptId = testDef.getName();
                    } else {
                        componentPart = this.modularEngine.stripExtension(testScriptName);
                        testScriptId = testId + "/" + componentPart;
                    }
                    testReport.setName(testScriptId);
                    testReport.setTestScript(new Reference(testScriptFile.toString()));
                    testScript = modularScripts.getTestScriptMap().get(testScriptId);
                    if (testScript != null) {
                        testScript.setId(testScriptId.contains("/") ? componentPart : testScriptId);
                        testScript.setName(testScriptId);
                    } else {
                        String errorMsg = "Null testScriptObj for testDef: " + testDef + " testScriptName: " + testScriptName;
                        log.severe(errorMsg);
                        throw new RuntimeException(errorMsg);
                    }
                } else {
                    String errorMsg = "Null modularScript";
                    log.severe(errorMsg);
                    throw new RuntimeException(errorMsg);
                }
            }
        }

        testReport.setIssued(new Date());
        TestReport.TestReportParticipantComponent part = testReport.addParticipant();
        part.setType(TestReport.TestReportParticipantType.SERVER);
        if (sut != null)
            part.setUri(sut.toString());
        part.setDisplay("NIST Asbestos Proxy");

        part = testReport.addParticipant();
        part.setType(TestReport.TestReportParticipantType.TESTENGINE);
        part.setUri("https://github.com/usnistgov/asbestos");
        part.setDisplay("NIST Asbestos TestEngine");
    }

    private boolean errorOut() throws IOException, CircularModularScriptReferenceException {
//        propagateStatus(testReport);
        errors = doReportResult();
        if (hasError()) {
            doTearDown();
            fillInSkips();
            doLintTestReport();
            return true;
        }
        return false;
    }

    public boolean hasError() {
        return testReport.hasResult() && testReport.getResult() == TestReport.TestReportResult.FAIL;
    }

    private List<String> doReportResult() {

        if (testReport.getStatus() == TestReport.TestReportStatus.ENTEREDINERROR) {
            // more complicated (TestScript dependency) - trust result
            return new ArrayList<>();
        }

        List<String> failingComponents = new ArrayList<>();

        TestReport.TestReportSetupComponent setup = testReport.getSetup();
        for (TestReport.SetupActionComponent action : setup.getAction()) {
            if (action.hasOperation()) {
                TestReport.SetupActionOperationComponent op = action.getOperation();
                TestReport.TestReportActionResult result = op.getResult();
                if (result == TestReport.TestReportActionResult.FAIL || result == TestReport.TestReportActionResult.ERROR) {
                    if (op.getExtensionByUrl(ExtensionDef.conditional) == null)
                        failingComponents.add(op.getMessage());
                }
            }
            if (action.hasAssert()) {
                TestReport.SetupActionAssertComponent as = action.getAssert();
                TestReport.TestReportActionResult result2 = as.getResult();
                if (result2 == TestReport.TestReportActionResult.FAIL || result2 == TestReport.TestReportActionResult.ERROR) {
                    if (as.getExtensionByUrl(ExtensionDef.conditional) == null)
                        failingComponents.add(as.getMessage());
                }
            }
        }
        for (TestReport.TestReportTestComponent test : testReport.getTest()) {
            for (TestReport.TestActionComponent action : test.getAction()) {
                if (action.hasOperation()) {
                    TestReport.SetupActionOperationComponent op = action.getOperation();
                    TestReport.TestReportActionResult result = op.getResult();
                    if (result == TestReport.TestReportActionResult.FAIL || result == TestReport.TestReportActionResult.ERROR) {
                        if (op.getExtensionByUrl(ExtensionDef.conditional) == null && op.getExtensionByUrl(ExtensionDef.expectFailure) == null)
                            failingComponents.add(op.getMessage());
                    }
                }
                if (action.hasAssert()) {
                    TestReport.SetupActionAssertComponent as = action.getAssert();
                    TestReport.TestReportActionResult result2 = as.getResult();
                    if (result2 == TestReport.TestReportActionResult.FAIL || result2 == TestReport.TestReportActionResult.ERROR) {
                        if (as.getExtensionByUrl(ExtensionDef.conditional) == null)
                            failingComponents.add(as.getMessage());
                    }
                }
            }
        }
        TestReport.TestReportTeardownComponent teardown = testReport.getTeardown();
        for (TestReport.TeardownActionComponent action : teardown.getAction()) {
            if (action.hasOperation()) {
                TestReport.SetupActionOperationComponent op = action.getOperation();
                TestReport.TestReportActionResult result = op.getResult();
                if (result == TestReport.TestReportActionResult.FAIL || result == TestReport.TestReportActionResult.ERROR)
                    failingComponents.add(op.getMessage());
            }
        }

        testReport.setResult(
                failingComponents.isEmpty()
                        ? TestReport.TestReportResult.PASS
                        : TestReport.TestReportResult.FAIL
        );
        return failingComponents;
    }

    private void doPreProcessing() {

    }

    private void doLoadVariables() {
        if (testScript.hasVariable()) {
            ValE fVal = new ValE(engineVal).setMsg("Variables");

            for(TestScript.TestScriptVariableComponent comp : testScript.getVariable()) {
                if (!comp.hasName())
                    throw new Error("Variable defined without name");
            }
        }
    }


    private void doLoadFixtures() {
        Objects.requireNonNull(fhirClientForFixtures);
        if (testScript.hasFixture()) {
            ValE fVal = new ValE(engineVal).setMsg("Fixtures");

            try {
                for (TestScript.TestScriptFixtureComponent fix : testScript.getFixture()) {
                    String id = fix.getId();
                    if (id == null || id.equals("")) {
                        throw new Error("Static Fixture has no id and cannot be referenced");
                    }
                    if (!fix.hasAutocreate())
                        throw new Error("fixture.autocreate is a required field");
                    if (!fix.hasAutodelete())
                        throw new Error("fixture.autodelete is a required field");

                    if (fix.hasResource()) {
                        Ref ref = new Ref(fix.getResource().getReference(), sut);
                        FixtureComponent fixtureComponent = fixtureMgr.add(id)
                                .setStaticRef(ref)   // static means defined in this test
                                .setVal(fVal);
                        fixtureComponent.getResourceResource();
                    } else if (fix.hasExtension(ExtensionDef.subFixture)) {
                        Extension subfix = fix.getExtensionByUrl(ExtensionDef.subFixture);

                        Extension fhirPathExt = subfix.getExtensionByUrl(ExtensionDef.fhirPath);
                        if (fhirPathExt == null || fhirPathExt.getValue() == null)
                            throw new Error("Extension urn:subFixture has no value for its " + ExtensionDef.failure + " subExtension");
                        String fhirPath = fhirPathExt.getValue().toString();

                        Extension sourceIdExt = subfix.getExtensionByUrl(ExtensionDef.sourceId);
                        if(sourceIdExt == null || sourceIdExt.getValue() == null)
                            throw new Error("Extension " + ExtensionDef.subFixture + " has no value for its " + ExtensionDef.sourceId + " subExtension");
                        String sourceId = sourceIdExt.getValue().toString();

                        FixtureSub fixtureSub = new FixtureSub(fixtureMgr, sourceId, fhirPath);
                        FixtureComponent fixtureComponent = fixtureMgr
                                .add(id)
                                .setFixtureSub(fixtureSub)
                                .setVal(fVal);
                    }
                }
            } catch (Throwable t) {
                addTerminalFailureToTestReport("TestEngine doLoadFixtures Error: " + t);
            }

        }
    }

    private void doAutoCreates() {
        if (testScript.hasFixture()) {
            ValE fVal = new ValE(engineVal).setMsg("Fixtures.autocreate");

            TestReport.TestReportSetupComponent setupReportComponent = testReport.getSetup();
            for (TestScript.TestScriptFixtureComponent comp : testScript.getFixture()) {
                if (comp.hasAutocreate()) {
                    if (comp.getAutocreate()) {
                        TestReport.SetupActionComponent actionReport = new TestReport.SetupActionComponent(); //setupReportComponent.addAction();
                        Reference resource = comp.getResource();
                        String resourceType = resource.getType();
                        // create unregistered operationReport - nobody wants to know how this went
//                        TestReport.SetupActionOperationComponent operationReport = actionReport.getOperation();
                        TestReport.SetupActionOperationComponent operationReport = new TestReport.SetupActionOperationComponent();
                        operationReport.setResult(TestReport.TestReportActionResult.PASS);  // may be overwritten
                        SetupActionCreate create = new SetupActionCreate(new ActionReference(testScript, comp), fixtureMgr, false)
                                .setFhirClient(fhirClient)
                                .setSut(sut)
                                .setType("fixture.autocreate")
                                .setVal(fVal)
                                .setVariableMgr(new VariableMgr(testScript, fixtureMgr)
                                        .setExternalVariables(externalVariables)
                                        .setVal(fVal)
                                        .setOpReport(operationReport));
                        create.setTestEngine(this);
                        create.setChannelId(this.getChannelId());
                        create.run(testScript, comp, comp.getId(), comp.getResource(), operationReport);
                        if (propagateStatus(testReport))
                            return;  // fail
                    }
                }
            }

        }
    }

    private void doAutoDeletes() {
        if (testScript.hasFixture()) {
            ValE fVal = new ValE(engineVal).setMsg("Fixtures.autodelete");

            TestReport.TestReportTeardownComponent teardownReportComponent = testReport.getTeardown();
            for (TestScript.TestScriptFixtureComponent comp : testScript.getFixture()) {
                if (comp.hasAutodelete()) {
                    if (comp.getAutodelete()) {
                        TestReport.TeardownActionComponent actionReport = teardownReportComponent.addAction();
                        Reference resource = comp.getResource();
                        String resourceType = resource.getType();
                        TestReport.SetupActionOperationComponent operationReport = new TestReport.SetupActionOperationComponent(); //actionReport.getOperation();
                        operationReport.setResult(TestReport.TestReportActionResult.PASS);  // may be overwritten
                        SetupActionDelete delete = new SetupActionDelete(new ActionReference(testScript, comp), fixtureMgr, false)
                                .setFhirClient(fhirClient)
                                .setSut(sut)
                                .setType("fixture.autodelete")
                                .setVal(fVal)
                                .setVariableMgr(new VariableMgr(testScript, fixtureMgr)
                                        .setExternalVariables(externalVariables)
                                        .setVal(fVal)
                                        .setOpReport(operationReport));
                        delete.run(comp.getId(), comp.getResource(), operationReport);
                        if (propagateStatus(testReport))
                            return;  // fail
                    }
                }
            }
        }
    }


    private void doSetup() throws IOException, CircularModularScriptReferenceException {
        boolean reportAsConditional = false;  // upgrade this when conditional execution comes to setup
        if (testScript.hasSetup()) {
           if (hasDebugger())
                getDebugger().pauseIfBreakpoint("setup", 0); // There is only one TestScript.Setup so parent index is always 0
            TestScript.TestScriptSetupComponent comp = testScript.getSetup();
            ValE fVal = new ValE(engineVal).setMsg("Setup");
            TestReport.TestReportSetupComponent setupReportComponent = testReport.getSetup();
            if (comp.hasAction()) {
                String typePrefix = "setup.action";
                int actionIndex = 0;
                for (TestScript.SetupActionComponent action : comp.getAction()) {
                    TestReport.SetupActionComponent actionReportComponent = setupReportComponent.addAction();
                    if (invalidAction(action, actionReportComponent, fVal))
                        return;
                    if (action.hasOperation()) {
                        boolean isFollowedByAssert = false;
                        if (actionIndex+1 < comp.getAction().size()) {
                            // there is a following action
                            TestScript.SetupActionComponent nextAction = comp.getAction().get(actionIndex+1);
                            if (nextAction.hasAssert())
                                isFollowedByAssert = true;
                        }

                        if (hasDebugger())
                            getDebugger().pauseIfBreakpoint("setup", 0, actionIndex, hasImportModifierExtension(action.getOperation()));
                        doOperation(new ActionReference(testScript, action), typePrefix, action.getOperation(), actionReportComponent.getOperation(), isFollowedByAssert);
                        TestReport.SetupActionOperationComponent opReport = actionReportComponent.getOperation();
                        if (TestReport.TestReportActionResult.ERROR.equals(opReport.getResult())) {
                            testReport.setStatus(TestReport.TestReportStatus.COMPLETED);
                            testReport.setResult(TestReport.TestReportResult.FAIL);
                            return;
                        } else if (TestReport.TestReportActionResult.PASS.equals(opReport.getResult())) {
                            testReport.setStatus(TestReport.TestReportStatus.COMPLETED);
                            testReport.setResult(TestReport.TestReportResult.PASS);
                        }
                    }
                    if (action.hasAssert()) {
                        if (hasDebugger())
                            getDebugger().pauseIfBreakpoint("setup", 0, action.getAssert(), actionIndex);
                        TestReport.SetupActionAssertComponent actionReport = actionReportComponent.getAssert();
                        doAssert(typePrefix, action.getAssert(), actionReport);
                        if (actionReport == null)
                            return;
                        if ("fail".equals(actionReport.getResult().toCode())) {
                            if (reportAsConditional) {
                                testReport.setStatus(TestReport.TestReportStatus.ENTEREDINERROR);
                                testReport.setResult(TestReport.TestReportResult.PASS);
                            } else {
                                testReport.setStatus(TestReport.TestReportStatus.COMPLETED);
                                testReport.setResult(TestReport.TestReportResult.FAIL);
                            }
                            return;
                        }
                        if ("error".equals(actionReport.getResult().toCode())) {
                            testReport.setStatus(TestReport.TestReportStatus.COMPLETED);
                            testReport.setResult(TestReport.TestReportResult.FAIL);
                            return;
                        }
                    }
                    if (hasError())
                        return;
                    actionIndex++;
                }
            }
        }
    }

    private void doOperation(ActionReference actionReference, String typePrefix, TestScript.SetupActionOperationComponent operation, TestReport.SetupActionOperationComponent report, boolean isFollowedByAssert) throws IOException, CircularModularScriptReferenceException {
        Objects.requireNonNull(channelId);
        if (operation.hasType()) {
            try {
                OperationRunner runner = new OperationRunner(actionReference, fixtureMgr, externalVariables)
                        .setVal(new ValE(val).setMsg(typePrefix))
                        .setTypePrefix(typePrefix)
                        .setFhirClient(fhirClient)
                        .setSut(sut)
                        .setTestReport(testReport)
                        .setTestScript(testScript)
                        .setTestCollectionId(testCollection)
                        .setTestId(testId);
                runner.setTestEngine(this);
                runner.run(operation, report, isFollowedByAssert);
            } catch (Throwable t) {
                String error = String.format("TestEngine#doOperation: %s. Check server log for details.", t.toString());
                log.log(Level.SEVERE, error, t);
                report.setMessage(error);
                report.setResult(TestReport.TestReportActionResult.ERROR);
            }
            propagateStatus(testReport);
        } else if (operation.hasModifierExtension()) {
            List<Extension> extensions = operation.getModifierExtension();
            for (Extension extension : extensions) {
                String url = extension.getUrl();
                if (url.equals(ExtensionDef.ts_import)) {
                    handleImport(extension, operation, report);
                } else {
                    report.setMessage("Operation with unknown modifierExtension " + url + " found.");
                    report.setResult(TestReport.TestReportActionResult.ERROR);
                }
            }
        } else  {
            report.setMessage("Found operation with no type and no modifierExtension.");
            report.setResult(TestReport.TestReportActionResult.ERROR);
        }
    }

    void doAssert(String typePrefix, TestScript.SetupActionAssertComponent theAssert, TestReport.SetupActionAssertComponent report) {
        try {
            ValE vale = new ValE(val);
            AssertionRunner runner = new AssertionRunner(fixtureMgr)
                    .setVal(vale.setMsg(typePrefix))
                    .setTypePrefix(typePrefix)
                    .setVariableMgr(new VariableMgr(testScript, fixtureMgr)
                            .setExternalVariables(externalVariables)
                            .setVal(vale)
                            .setOpReport(report))
//                    .setTestReport(testReport)
                    .setTestScript(testScript)
                    .setIsRequest(isRequest);
            runner
                    .setTestCollectionId(testCollection)
                    .setTestId(testId)
                    .setTestEngine(this);
            runner.run(theAssert, report);
        } catch (NotABundleException nabEx) {
            /*
             GetClientTestEvalRequest uses N message limit, ex. 30
            When a client test is run, against the last 30 messages, this message also repeats at least 30 times in the log:
	        RuntimeException: Fixture request does not contain a Bundle
             */
            report.setMessage(nabEx.getMessage());
            report.setResult(TestReport.TestReportActionResult.ERROR);
            addTerminalFailureToTestReport("TestEngine NotABundleException: " + nabEx.toString());
        } catch (Throwable t) {
            String error = "doAssert Error: " + t.toString();
            report.setMessage(error);
            report.setResult(TestReport.TestReportActionResult.ERROR);
            logTerminalFailure(t);
            addTerminalFailureToTestReport(error);
        }
    }

    private void doTest() {
        if (testScript.hasTest()) {
            ValE fVal = new ValE(engineVal).setMsg("Test");

            try {
                int testCounter = 1;
                for (TestScript.TestScriptTestComponent test : testScript.getTest()) {
                    int testIndex = testScript.getTest().indexOf(test);
                    if (hasDebugger())
                        getDebugger().pauseIfBreakpoint("test", testIndex);
                    // if noErrors extension present and script has already hit an error then bail out
                    // and don't run actions in this test
                    if (getExtension(test.getModifierExtension(), ExtensionDef.noErrors) != null) {
                        if (! TestReport.TestReportResult.PASS.equals(getTestReport().getResult()))
                            return;
                    }


                    String testName = test.getName();
                    if (testName == null || testName.equals(""))
                        testName = "Test" + testCounter;
                    testCounter++;
                    ValE tVal = new ValE(fVal).setMsg(testName);
                    TestReport.TestReportTestComponent testReportComponent = testReport.addTest();

                    //
                    //  handle modifier extensions
                    //

                    boolean isConditional = false;
                    List<Extension> extensions = test.getModifierExtension();
                    for (Extension extension : extensions ) {
                        if (!extension.hasUrl()) {
                            reportParsingError(testReportComponent, "Extension found without URL");
                            return;
                        }
                        // conditional no longer used this way
                        String url = extension.getUrl();
                        if (url.equals(ExtensionDef.conditional)) {
                            isConditional = true;
                            boolean conditionalResult = handleConditionalTest(test, testReportComponent, extension);
                            if (conditionalResult) {
                                doTestPart(test, testReportComponent, testReport, false);
                            } else {
                                reportSkip(testReportComponent);   // for the then part
                            }
                        } else if (url.equals(ExtensionDef.multiErrors)) {
                            // will be handled in doTestPart
                        } else if (url.equals(ExtensionDef.noErrors)) {
                            // handled above
                        } else {
                            reportParsingError(testReportComponent, "Do not understand ModifierExtension " + url);
                            return;
                        }
                    }

                    if (!isConditional)
                        doTestPart(test, testReportComponent, testReport, false);
                }
            }
            catch (StopDebugTestScriptException sdex) {
               failOverride = true;
               throw sdex;
            }
            catch (Throwable t) {
                String msg = t.getMessage();
                if (msg == null || msg.equals("")) {
                    msg = "TestEngine#doTest Error: Check server log for details.";
                }
                log.log(Level.SEVERE, msg, t);
                reportParsingError(testReport.addTest(), msg);
            }

        }
    }

    Set<String> moduleIds = new LinkedHashSet<>();


    private void handleImport(Extension extension, TestScript.SetupActionOperationComponent opScript, TestReport.SetupActionOperationComponent opReport) throws IOException, CircularModularScriptReferenceException {

        /*
            Validate and align request input fixtures and variables
         */
        final Properties propertiesMap = getEC().getTestCollectionProperties(getTestCollection());
        ComponentReference componentReference = new ComponentReference( propertiesMap, testScript.getVariable(),  testDef, Collections.singletonList(extension));

        String theModuleKey = this.testId + "/" + MultiUseScriptId.getComponentPart(componentReference.getComponentRef().toString());
        if (this.modularEngine.getModularScripts().getTestScriptMap().containsKey(theModuleKey)) {
            TestScript componentTestScript = this.modularEngine.getModularScripts().getTestScriptMap().get(theModuleKey);
//            String componentXml = this.modularEngine.getModularScripts().getTestScriptMap().get(theModuleKey);
//            TestScript module = (TestScript) ParserBase.parse(componentXml, Format.XML);
            // fill in componentReference with local names from module definition
            new ComponentDefinition(getTestScriptFile(), componentTestScript).loadTranslation(componentReference);
        } else {
            String errorMessage = String.format("theModuleKey %s does not exist", theModuleKey);
            log.severe(errorMessage);
            throw new RuntimeException(errorMessage);
        }

        // align fixtures for module
        Map<String, FixtureComponent> inFixturesForComponent = new HashMap<>();
        Map<String, String> fixtureNameMap = new HashMap<>();
        for (Parameter parm : componentReference.getFixturesIn()) {
            String callerName = parm.getCallerName();
            String innnerName = parm.getLocalName();
            fixtureNameMap.put(callerName, innnerName);
            FixtureComponent fixtureComponent = fixtureMgr.get(callerName);
            if (fixtureComponent != null && fixtureComponent.getFixtureSub() != null) {
                // create temporary FixtureComponent containing the translations
                // and the extracted content
                fixtureComponent = fixtureComponent.getFixtureSub().getSubFixture(fixtureComponent);
            }
            if (fixtureComponent == null)
                throw new RuntimeException("Fixture " + callerName + " does not exist");
            inFixturesForComponent.put(innnerName, fixtureComponent);
        }
        /*
        for (Parameter parm : componentReference.getFixturesOut()) {
            String outerName = parm.getCallerName();
            String innnerName = parm.getLocalName();
            outFixtureNameMap.put(outerName, innnerName);
            FixtureComponent fixtureComponent = parent.fixtureMgr.get(outerName);
            if (fixtureComponent != null && fixtureComponent.getFixtureSub() != null) {
                // create temporary FixtureComponent containing the translations
                // and the extracted content
                fixtureComponent = fixtureComponent.getFixtureSub().getSubFixture(fixtureComponent);
            }
            if (fixtureComponent == null)
                throw new RuntimeException("outFixture " + outerName + " does not exist");
            outFixturesForComponent.put(innnerName, fixtureComponent);
        }

         */

        // align variables for module
        Map<String, String> externalVariables = new HashMap<>();
        Map<String, String> variableNameMap = new HashMap<>();
        VariableMgr varMgr = new VariableMgr(testScript, fixtureMgr)
                .setExternalVariables(this.externalVariables)
                .setVal(engineVal)
                .setOpReport(opReport)
                .setPropertiesMap(propertiesMap);
        if (engineVal.hasErrors())
            log.warning("engineVal hasErrors is true.");
        for (Parameter parm : componentReference.getVariablesIn()) {
            String outerName = parm.getCallerName();
            String innerName = parm.getLocalName();
            variableNameMap.put(outerName, innerName);
            String value = varMgr.eval(outerName, false);
            externalVariables.put(innerName, value);
        }
        if (engineVal.hasErrors())
            log.info("errors");
        for (Parameter parm : componentReference.getVariablesInNoTranslation()) {
            String outerName = parm.getCallerName();
            String innerName = parm.getLocalName();
            // this generates errors
            String value = varMgr.eval(outerName, false);

            externalVariables.put(innerName, value);
        }

        // if variables and fixtures don't align, report it out into the caller's TestReport
        if (engineVal.hasErrors()) {
            new ActionReporter()
                    .setTestEngine(this)
                    .setTestCollectionId(testCollection)
                    .setTestId(testId)
                    .reportOperation(
                    null,
                        fixtureMgr,
                        varMgr,
                        new Reporter(new ValE(engineVal), opReport, "", ""),
                        opScript
                    );
            return;
        }

        /*
            Call module
         */
        if (hasDebugger())
            getDebugger().getState().pushParentExecutionIndex();

        // check if moduleIds are really accessible to new test engines
        Set<String> moduleIds = new LinkedHashSet<>();
                for (TestEngine te : modularEngine.getTestEngines()) {
                       moduleIds.addAll(te.moduleIds);
                }

       if (this.parent != null) {
           File parentTestScript = this.parent.getTestScriptFile().getCanonicalFile();
           if (parentTestScript.equals(componentReference.getComponentRef().getCanonicalFile())) {
               throw new CircularModularScriptReferenceException(String.format("Parent TestScript file has a circular reference: %s.", parentTestScript));
           }
       }

        TestEngine testEngine1 = sut == null
                ? new TestEngine(componentReference.getComponentRef(), moduleIds)
                : new TestEngine(componentReference.getComponentRef(), this.sut, moduleIds);
        FhirClient testEngine1fhirClient = new FhirClient();
        if (fhirClient != null) {
            if (fhirClient.getFormat() != null) {
                testEngine1fhirClient.setFormat(fhirClient.getFormat());
            }
            testEngine1fhirClient.sendGzip(fhirClient.isSendGzip());
            testEngine1fhirClient.requestGzip(fhirClient.isRequestGzip());
        }
        testEngine1
                .setTestSession(testSession)
                .withResourceCacheManager(this.getCacheManager())
                .setVal(new Val())
                .setExternalCache(externalCache)
                .setFixtures(inFixturesForComponent)
                .setExternalVariables(externalVariables)
                .setFhirClient(testEngine1fhirClient)
                .setChannelId(channelId)
                .setTestCollection(testCollection)
                .setTestId(testId)
                .setCallFixtureMap(fixtureNameMap)
                .setCallVariableMap(variableNameMap)
                .setModularEngine(modularEngine)
                .setFixtureOutParams(componentReference.getFixturesOut())
                ;
        if (hasDebugger()) {
            testEngine1.setTestScriptDebugState(getDebugger().getState());
        }
        modularEngine.add(testEngine1);
        testEngine1.parent = this;
        String moduleName = simpleName(componentReference.getComponentRef());
        String moduleId = moduleName; // testScript.getId(); // modularEngine.getMultiUseScriptId(moduleName, (multiUseTestScriptName != null) ? simpleName(new File(multiUseTestScriptName)) : moduleName);
        moduleIds.add(moduleId);
        opReport.addModifierExtension(new Extension(ExtensionDef.moduleId, new StringType(moduleId)));
        opReport.addModifierExtension(new Extension(ExtensionDef.moduleName, new StringType(moduleName)));
        opReport.setResult(TestReport.TestReportActionResult.PASS); // may be overwritten
        testEngine1.getTestReport().addExtension(ExtensionDef.moduleId, new StringType(moduleId));
        testEngine1.getTestReport().addExtension(ExtensionDef.moduleName, new StringType(moduleName));

        try {
            testEngine1.runTest();
        } catch (StopDebugTestScriptException sdex) {
           throw sdex;
        }

        if (hasDebugger())
            getDebugger().getState().popParentExecutionIndex();

        // Report overall module call status into caller's TestReport.operation
        ErrorReport errorReport = getErrorMessage(testEngine1.getTestReport());
        if (errorReport != null) {
            opReport.setResult(errorReport.type);
            opReport.setMessage(errorReport.message);
        }

        // Pass module fixtures back to caller
        FixtureMgr innerFixtures = testEngine1.fixtureMgr;
        updateCurrentTestEngineFixtureOut(innerFixtures, componentReference, errorReport);

//        Map<String, FixtureComponent> outFixturesForComponent = new HashMap<>();
        /*
        for (Parameter parm : componentReference.getFixturesOut()) {
            if (parm.isVariable()) { // Do not know why fixtureOut and isVariable can be mixed together like this
                throw new RuntimeException("Script import with output variables not supported");
            }
            String outerName = parm.getCallerName();
            String innerName = parm.getLocalName();
            FixtureComponent fixtureComponent = innerFixtures.get(innerName);
            if (errorReport == null) {
                if (fixtureComponent == null)
                    throw new RuntimeException("Script import - " + componentReference.getComponentRef() + " did not produce Fixture " + innerName);
                fixtureMgr.put(outerName, fixtureComponent);
            }
        }

         */
        // Script import with output variables
        // The variable-out scope is only limited to the TestScript which called the module
        // Inject variable-outs into the caller's TestEngine's TestScript as a defaultValue string. This pattern can be repeated as many times as needed.
        // Parent TestScript cannot declare the same variable name, otherwise the variable-out has no effect, and this is silent.
        // In other words, parent TestScript variable is immutable if it already exists.
        for (Parameter parm : componentReference.getVariablesOut()) {
            String vOutName = parm.getCallerName();
            if (! varMgr.hasVariable(vOutName)) {
                String vInName = parm.getLocalName();
                VariableMgr te1vMgr = new VariableMgr(testEngine1.getTestScript(), testEngine1.fixtureMgr).setVal(testEngine1.engineVal).setOpReport(opReport);
                // TestScript.TestScriptVariableComponent srcVariableComponent = te1vMgr.getVariable(vInName);
                TestScript.TestScriptVariableComponent variableOut = new TestScript.TestScriptVariableComponent();
                variableOut.setName(parm.getCallerName());
                String value = FhirPathEngineBuilder.evalForString(testEngine1.getTestScript(), te1vMgr.eval(parm.getLocalName(), false));
                if (value == null || "".equals(value)) {
                    value = "asbts_undefined_var";
                }
                variableOut.setDefaultValue(value);
                if (testScript.getVariable().add(variableOut)) { // Inject
                    externalVariables.put(vOutName, value);
//                    variableNameMap.put(vOutName, innerName);
                }
            }
        }

        String result = testEngine1.getTestReport().getResult().toCode();
        opReport.setResult(TestReport.TestReportActionResult.fromCode(result));

        // If module call reported failure then overall script reports failure.
        if (testEngine1.getTestReport().getResult() == TestReport.TestReportResult.FAIL)
            getTestReport().setResult(TestReport.TestReportResult.FAIL);
    }



    private TestEngine withResourceCacheManager(ResourceCacheMgr mgr) {
        for (File file : mgr.getDefaultCacheDirs()) {
            fixtureMgr.getFhirClient().getResourceCacheMgr().insertIntoFileSystemResourceCache(file);
        }
        return this;
    }

    public ResourceCacheMgr getCacheManager() {
        return fixtureMgr.getFhirClient().getResourceCacheMgr();
    }

    public void setFixtureOutParams(List<Parameter> fixtureOutParams) {
        this.fixtureOutParams = fixtureOutParams;
    }

    /**
     * This only supports initial assertion failure, and the message is propagated to the parent operation of the module containing assertions
     */
    static class ErrorReport {
        TestReport.TestReportActionResult type;
        String message;
        List<String> failedAssertionIds = new ArrayList<>();

        ErrorReport(TestReport.SetupActionAssertComponent theAssert, List<String> ids) {
            type = theAssert.getResult();
            failedAssertionIds.addAll(ids);
            // Was  message = theAssert.getMessage();
            message = //"(".concat(
                    ids.stream()
                    .map(s -> "'".concat(s).concat("'"))
                    .collect(Collectors.joining("|"));
           ; //.concat(")");
        }

        ErrorReport(TestReport.SetupActionOperationComponent theOp, String message) {
            type = theOp.getResult();
            this.message = message;
        }
    }

    private boolean hasErrorOrFail(TestReport.SetupActionAssertComponent theAssert) {
        return theAssert.hasResult() &&
                (theAssert.getResult().equals(TestReport.TestReportActionResult.FAIL) ||
                        theAssert.getResult().equals(TestReport.TestReportActionResult.ERROR));
    }

    private boolean hasErrorOrFail(TestReport.SetupActionOperationComponent theOp) {
        return theOp.hasResult() &&
                (theOp.getResult().equals(TestReport.TestReportActionResult.FAIL) ||
                        theOp.getResult().equals(TestReport.TestReportActionResult.ERROR));
    }

    private ErrorReport getErrorMessage(TestReport report) {
        TestReport.SetupActionAssertComponent firstFailedAssertion = null;
        TestReport.SetupActionOperationComponent firstFailedOperation = null;
        List<String> failedIds = new ArrayList<>();
        List<String> messageList = new ArrayList<>();

        if (report.hasSetup()) {
            for (TestReport.SetupActionComponent action : report.getSetup().getAction()) {
                if (action.hasAssert() && hasErrorOrFail(action.getAssert())) {
                    TestReport.SetupActionAssertComponent failedAssert = action.getAssert();
                    if (firstFailedAssertion == null)
                        firstFailedAssertion = failedAssert;
                    if (failedAssert.hasId()) {
                        failedIds.add(failedAssert.getId());
                    }
                }
                if (action.hasOperation() && hasErrorOrFail(action.getOperation())) {
                    TestReport.SetupActionOperationComponent failedOperation = action.getOperation();
                    if (failedOperation.hasMessage())
                        messageList.add( failedOperation.getMessage());
                    if (firstFailedOperation == null)
                        firstFailedOperation = action.getOperation();
                }
            }
        }

        if (report.hasTest()) {
            for (TestReport.TestReportTestComponent test : report.getTest()) {
                for (TestReport.TestActionComponent action : test.getAction()) {
                    if (action.hasAssert() && hasErrorOrFail(action.getAssert())) {
                        TestReport.SetupActionAssertComponent failedAssert = action.getAssert();
                        if (firstFailedAssertion == null)
                            firstFailedAssertion = failedAssert;
                        if (failedAssert.hasId()) {
                            failedIds.add(failedAssert.getId());
                        }
                    }
                    if (action.hasOperation() && hasErrorOrFail(action.getOperation())) {
                        TestReport.SetupActionOperationComponent failedOperation = action.getOperation();
                        if (failedOperation.hasMessage()) {
                            messageList.add(failedOperation.getMessage());
                        }
                        if (firstFailedOperation == null)
                            firstFailedOperation = failedOperation;
                    }
                }
            }
        }
        if (report.hasTeardown()) {
            TestReport.TestReportTeardownComponent teardown = report.getTeardown();
            for (TestReport.TeardownActionComponent action : teardown.getAction()) {
                if (action.hasOperation() && hasErrorOrFail(action.getOperation())) {
                    if (firstFailedOperation == null)
                        firstFailedOperation = action.getOperation();
                }
            }
        }
        if (firstFailedAssertion != null)
            return new ErrorReport(firstFailedAssertion, failedIds);
        else if (firstFailedOperation != null)
            return new ErrorReport(firstFailedOperation, String.join("|", messageList));
        return null;
    }

    private String simpleName(File file) {
        String name = file.getName();
        int i = name.indexOf(".");
        if (i == -1)
            return name;
        return name.substring(0, i);
    }

    // returns ok?
    private boolean handleConditionalTest(TestScript.TestScriptTestComponent testComponent, TestReport.TestReportTestComponent testReportComponent, Extension extension) throws IOException, CircularModularScriptReferenceException {
        TestScript containedTestScript = null;

        if (extension.getValue() instanceof Reference) {
            Reference ref = (Reference) extension.getValue();
            if (ref.getResource() instanceof TestScript) {
                containedTestScript = (TestScript) ref.getResource();
            }
        }
        if (containedTestScript == null)
            return false;
        //
        // handle conditional execution
        //
        //testReportComponent = testReport.addTest();
        boolean conditionalResult = true;


        List<TestScript.TestScriptTestComponent> tests = containedTestScript.getTest();
//        if (tests.size() != 2) {
//            reportParsingError(testReportComponent, "test condition must contain two test elements");
//            return false;
//        }

        TestReport containedTestReport = new TestReport();

        Extension extension1 = new Extension(ExtensionDef.ts_conditional,
                new Reference(containedTestReport));
        testReportComponent.addModifierExtension(extension1);

        // basic operation and validation
        TestScript.TestScriptTestComponent basicOperationTest = tests.get(0);
        TestReport.TestReportTestComponent containedTestReportComponent = containedTestReport.addTest();
        boolean conditionalTestResult = doTestPart(basicOperationTest, containedTestReportComponent, containedTestReport, false);

        if (!conditionalTestResult) {
            //failOverride = true;
            containedTestReportComponent = containedTestReport.addTest();
            reportSkip(containedTestReportComponent);
            //reportSkip(testReportComponent);
            return false;
        }
        // asserts to trigger conditional
        TestScript.TestScriptTestComponent conditionalTest = tests.get(1);

        containedTestReportComponent = containedTestReport.addTest();

        conditionalResult = doTestPart(conditionalTest, containedTestReportComponent, containedTestReport, true);

        if (containedTestReport.getResult() == TestReport.TestReportResult.FAIL)
            return false;
        return conditionalResult;
    }

    private void reportSkip(TestReport.TestReportTestComponent reportComponent) {
        TestReport.TestActionComponent testActionComponent = reportComponent.addAction();
        TestReport.SetupActionOperationComponent setupActionOperationComponent = testActionComponent.getOperation();
        setupActionOperationComponent.setResult(TestReport.TestReportActionResult.SKIP);
        setupActionOperationComponent.setMessage("skipped.");
    }

    private boolean doTestPart(TestScript.TestScriptTestComponent testScriptElement, TestReport.TestReportTestComponent testReportComponent, TestReport testReport, boolean reportAsConditional) throws IOException, CircularModularScriptReferenceException {
        int testIndex = testScript.getTest().indexOf(testScriptElement);
        ValE fVal = new ValE(engineVal).setMsg("Test");

        if (!testScriptElement.hasAction()) {
            String msg = "TestEngine doTestPart Error. Has no Action.";
            addTerminalFailureToTestReport(msg);
            log.severe(msg);
            return false;
        }
            String typePrefix = "contained.action";

            boolean multiErrorsAllowed = getExtension(testScriptElement.getModifierExtension(), ExtensionDef.multiErrors) != null;

            for (int testPartIndex=0; testPartIndex<testScriptElement.getAction().size(); testPartIndex++) {
                TestScript.TestActionComponent action = testScriptElement.getAction().get(testPartIndex);
                Extension conditionalExtension = getExtension(action.getModifierExtension(), ExtensionDef.ts_conditional);
                Extension expectFailureExtension = getExtension(action.getModifierExtension(), ExtensionDef.expectFailure);
                Extension mayHaveBugsExtension = getExtension(action.getModifierExtension(), ExtensionDef.mayHaveBugs);
                boolean isConditional = conditionalExtension != null;
                boolean isExpectFailure = expectFailureExtension != null;
                boolean isMayHaveBugsExtension = mayHaveBugsExtension != null;
                TestReport.TestActionComponent actionReportComponent = testReportComponent.addAction();
                if (invalidAction(action, actionReportComponent, fVal)) {
                    String msg = "TestEngine invalidAction. Action must contain operation or assert.";
                    addTerminalFailureToTestReport(msg);
                    log.severe(msg);
                    return false;
                }
                if (action.hasOperation()) {
                    boolean isFollowedByAssert = false;
                    if (testPartIndex+1 < testScriptElement.getAction().size()) {
                        // there is a following action
                        TestScript.TestActionComponent nextAction = testScriptElement.getAction().get(testPartIndex+1);
                        if (nextAction.hasAssert())
                            isFollowedByAssert = true;
                    }
                    if (hasDebugger())
                        getDebugger().pauseIfBreakpoint("test", testIndex, testPartIndex, hasImportModifierExtension(action.getOperation()));
                    TestReport.SetupActionOperationComponent reportOp = actionReportComponent.getOperation();
                    doOperation(new ActionReference(testScript, action), typePrefix, action.getOperation(), reportOp, isFollowedByAssert);
                    TestReport.SetupActionOperationComponent opReport = actionReportComponent.getOperation();
                    if (isMayHaveBugsExtension && opReport.getResult() == TestReport.TestReportActionResult.FAIL) {
                        actionReportComponent.addModifierExtension(mayHaveBugsExtension);
                        overrideTestResult(testReport, opReport, TestReport.TestReportActionResult.WARNING);
                        return true;
                    } else if (isExpectFailure && opReport.getResult() == TestReport.TestReportActionResult.FAIL) {
                        actionReportComponent.addModifierExtension(expectFailureExtension);
                        // If the failed assertion ids match the expected assertion id list, the it is a PASS
                        if (expectFailureExtension.hasExtension()) {
                            Extension assertionIdList = expectFailureExtension.getExtension().get(0);
                            if (ExtensionDef.assertionIdList.equals(assertionIdList.getUrl()) && opReport.hasMessage()) {
                                String assertionIdListValue = assertionIdList.getValue().toString();
                                if (Parameter.isVariable(assertionIdListValue)) {
                                  assertionIdListValue = Parameter.extractParameterName(assertionIdListValue);
                                  assertionIdListValue = externalVariables.get(assertionIdListValue);
                                }
                                if (assertionIdListValue != null && !"".equals(assertionIdListValue)) {
                                    String expression = String.format("%s~(%s)",assertionIdListValue, opReport.getMessage());
                                    boolean result = FhirPathEngineBuilder.evalForBoolean(new TestReport(), expression);
                                    if (result) {
                                        overrideTestResult(testReport, opReport, TestReport.TestReportActionResult.PASS);
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                    if (opReport.getResult() == TestReport.TestReportActionResult.ERROR) {
                        testReport.setStatus(TestReport.TestReportStatus.COMPLETED);
                        testReport.setResult(TestReport.TestReportResult.FAIL);
                        return false;
                    }
                    if (isConditional && testReport.getResult().equals(TestReport.TestReportResult.FAIL)) {
                        // report error  but do not propagate
                        reportOp.addExtension(ExtensionDef.conditional, new StringType("x"));
                        return false;
                    }
                    // an operation not followed by an assert that fails causes test to fail
                    if (testPartIndex + 1 < testScriptElement.getAction().size()) {
                        TestScript.TestActionComponent nextAction = testScriptElement.getAction().get(testPartIndex+1);
                        if (!nextAction.hasAssert() && actionReportComponent.getOperation().getResult().equals(TestReport.TestReportActionResult.FAIL)) {
                            testReport.setStatus(TestReport.TestReportStatus.COMPLETED);
                            testReport.setResult(TestReport.TestReportResult.FAIL);
                            if (!multiErrorsAllowed)
                                return false;
                        }
                    }
                }
                if (action.hasAssert()) {
                    TestScript.SetupActionAssertComponent assertComponent = action.getAssert();
                    if (hasDebugger())
                        getDebugger().pauseIfBreakpoint("test", testIndex, assertComponent, testPartIndex);
                    TestReport.SetupActionAssertComponent actionReport = actionReportComponent.getAssert();
                    doAssert(typePrefix, assertComponent, actionReport);
                    if (actionReport == null)
                        return false;
                    if (isConditional && "warning".equals(actionReport.getResult().toCode())) {
                        log.warning("Conditional extension should not use assert warningOnly='true' in TestScript");
                    }
                    if ("fail".equals(actionReport.getResult().toCode())) {
                        if (isConditional) {
                            // report error  but do not propagate
                            actionReport.addExtension(ExtensionDef.conditional, new StringType("x"));
                            return false;
                        } else {
                            testReport.setStatus(TestReport.TestReportStatus.COMPLETED);
                            testReport.setResult(TestReport.TestReportResult.FAIL);
                            if (!multiErrorsAllowed)
                                return false;
                        }
                        //result = false;
                        //return false;  // don't jump ship on first assertion failure
                    }
                    if ("error".equals(actionReport.getResult().toCode())) {
                        testReport.setStatus(TestReport.TestReportStatus.COMPLETED);
                        testReport.setResult(TestReport.TestReportResult.FAIL);
                        return false;
                    }
                }
            }

        return true;
    }

    private void overrideTestResult(TestReport testReport, TestReport.SetupActionOperationComponent opReport, TestReport.TestReportActionResult opResult) {
        testReport.setStatus(TestReport.TestReportStatus.COMPLETED);
        testReport.setResult(TestReport.TestReportResult.PASS);
        opReport.setResult(opResult);
    }

    private Extension getExtension(List<Extension> extensions, String url) {
        for (Extension extension : extensions) {
            if (extension.hasUrl() && extension.getUrl().equals(url))
                return extension;
        }
        return null;
    }

    // bill
    private void reportParsingError(TestReport.TestReportTestComponent testReportComponent, String message) {
        ValE fVal = new ValE(engineVal).setMsg("Test");
        TestReport.TestActionComponent actionReportComponent = testReportComponent.addAction();
        Reporter reporter = new Reporter(fVal, actionReportComponent.getOperation(), "", "");
        reporter.reportError( message);
        return;
    }

    private boolean invalidAction(TestScript.SetupActionComponent action, TestReport.SetupActionComponent actionReportComponent, ValE fVal) {
        if (action.hasOperation() && action.hasAssert()) {
            Reporter reporter = new Reporter(fVal, actionReportComponent.getOperation(), "", "");
            reporter.reportError( "Action has both operation and assertion");
            return true;
        }
        if (!action.hasOperation() && !action.hasAssert()) {
            Reporter reporter = new Reporter(fVal, actionReportComponent.getOperation(), "", "");
            reporter.reportError( "Action must have operation or assertion");
            return true;
        }
        return false;
    }

    private boolean invalidAction(TestScript.TestActionComponent action, TestReport.TestActionComponent actionReportComponent, ValE fVal) {
        if (action.hasOperation() && action.hasAssert()) {
            Reporter reporter = new Reporter(fVal, actionReportComponent.getOperation(), "", "");
            reporter.reportError( "Action has both operation and assertion");
            return true;
        }
        if (!action.hasOperation() && !action.hasAssert()) {
            Reporter reporter = new Reporter(fVal, actionReportComponent.getOperation(), "", "");
            reporter.reportError( "Action must have operation or assertion");
            return true;
        }
        return false;
    }

    private TestScript.TestScriptTestComponent findTest(TestScript testScript, String name) {
        for (TestScript.TestScriptTestComponent testComponent : testScript.getTest()) {
            if (name.equals(testComponent.getName()))
                return testComponent;
        }
        return null;
    }

    private void doTearDown() throws IOException, CircularModularScriptReferenceException {
        if (testScript.hasTeardown()) {
            String typePrefix = "teardown";
            ValE fVal = new ValE(engineVal).setMsg("Teardown");
            TestReport.TestReportTeardownComponent teardownReportComponent = testReport.getTeardown();
            TestScript.TestScriptTeardownComponent testScriptTeardownComponent = testScript.getTeardown();
            if (testScriptTeardownComponent.hasAction()) {
                TestReport.TeardownActionComponent actionReport = teardownReportComponent.addAction();
                for (TestScript.TeardownActionComponent action : testScriptTeardownComponent.getAction()) {
                    if (action.hasOperation()) {
                        TestScript.SetupActionOperationComponent setupActionOperationComponent = action.getOperation();
                        if (action.hasOperation())
                            doOperation(new ActionReference(testScript, action), typePrefix, action.getOperation(), actionReport.getOperation(), false);

                        if (hasError())
                            return;
                    }
                }
            }
        }
    }

    private void doPostProcessing() {
    }

    public TestEngine addCache(File cacheDir) {
        fhirClientForFixtures.getResourceCacheMgr().addCache(cacheDir);
        return this;
    }

    public static TestScript loadTestScript(File testDefDir) {
            return loadTestScript(testDefDir, null);
    }

    public static TestScript loadTestScript(File testDefDir, String fileName) {
        Objects.requireNonNull(testDefDir);
        File location = findTestScriptFile(testDefDir, fileName);
        InputStream is = null;
        try {
            is = new FileInputStream(location);
            IParser parser = (location.getName().endsWith("xml") ? ParserBase.getFhirContext().newXmlParser() : ParserBase.getFhirContext().newJsonParser());
            IBaseResource resource = parser.parseResource(is);
            assert resource instanceof TestScript;
            TestScript testScript = (TestScript) resource;
            testScript.setName(location.toString());

            return testScript;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) {
                // oops
                log.severe("TestScript file close error: " + location);
            }
        }
    }

    private static File findTestScriptFile(File testDefDir, String fileName) {
        if (fileName == null)
            return findTestScriptFile(testDefDir);
        else
            return new File(testDefDir, fileName);
    }

    public static File findTestScriptFile(File testDefDir) {
        File location = new File(testDefDir, "TestScript.xml");
        if (location.exists())
            return location;
        location = new File(testDefDir, "TestScript.json");
        if (location.exists())
            return location;
        location = new File(testDefDir, "../TestScript.xml");
        if (location.exists())
            return location;
        location = new File(testDefDir, "../TestScript.json");
        if (location.exists())
            return location;
        throw new RuntimeException("Cannot load TestScript (.xml or .json) from " + testDefDir + " or " + testDefDir + "/..");
    }

    FixtureMgr getFixtures() {
        return fixtureMgr;
    }

    /**
     *
     * @param testReport
     * @return fail
     */
    boolean propagateStatus(TestReport testReport) {
        testReport.setResult(TestReport.TestReportResult.PASS);
        if (testReport.hasSetup()) {
            TestReport.TestReportSetupComponent setupComponent = testReport.getSetup();
            for (TestReport.SetupActionComponent setupActionComponent : setupComponent.getAction()) {
                if (setupActionComponent.hasOperation()) {
                    if (reportOnOperation(testReport, setupActionComponent.getOperation()))
                        return true;
                }
                if (setupActionComponent.hasAssert()) {
                    if (reportOnAssertion(testReport, setupActionComponent.getAssert()))
                        return true;
                }
            }
        }
        if (testReport.hasTest()) {
            for (TestReport.TestReportTestComponent testComponent : testReport.getTest()) {
                if (testComponent.hasAction()) {
                    for (TestReport.TestActionComponent testActionComponent : testComponent.getAction()) {
                        if (testActionComponent.hasOperation()) {
                            if (reportOnOperation(testReport, testActionComponent.getOperation()))
                                return true;
                        }
                        if (testActionComponent.hasAssert()) {
                            if (reportOnAssertion(testReport, testActionComponent.getAssert()))
                                return true;
                        }
                    }
                }
            }
        }
        if (testReport.hasTeardown()) {
            TestReport.TestReportTeardownComponent teardownActionComponent = testReport.getTeardown();
            for (TestReport.TeardownActionComponent teardownActionComponent1 : teardownActionComponent.getAction()) {
                if (teardownActionComponent1.hasOperation()) {
                    if (reportOnOperation(testReport, teardownActionComponent1.getOperation()))
                        return true;
                }
            }
        }
        testReport.setStatus(TestReport.TestReportStatus.COMPLETED);
        return false;
    }

    /**
     *
     * @param testReport
     * @param setupActionOperationComponent
     * @return fail
     */
    private boolean reportOnOperation(TestReport testReport, TestReport.SetupActionOperationComponent setupActionOperationComponent) {
        if (setupActionOperationComponent.hasResult()) {
            TestReport.TestReportActionResult testReportActionResult = setupActionOperationComponent.getResult();
            if (testReportActionResult == TestReport.TestReportActionResult.ERROR
                    || testReportActionResult == TestReport.TestReportActionResult.FAIL) {
                setupActionOperationComponent.setResult(testReportActionResult);
                testReport.setResult(TestReport.TestReportResult.FAIL);
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @param testReport
     * @param setupActionAssertionComponent
     * @return fail
     */
    private boolean reportOnAssertion(TestReport testReport, TestReport.SetupActionAssertComponent setupActionAssertionComponent) {
        if (setupActionAssertionComponent.hasResult()) {
            TestReport.TestReportActionResult testReportActionResult = setupActionAssertionComponent.getResult();
            if (testReportActionResult == TestReport.TestReportActionResult.ERROR
                    || testReportActionResult == TestReport.TestReportActionResult.FAIL) {
                setupActionAssertionComponent.setResult(testReportActionResult);
                testReport.setResult(TestReport.TestReportResult.FAIL);
                return true;
            }
        }
        return false;
    }

    public TestEngine setVal(Val val) {
        this.val = val;
        return this;
    }

    public TestReport getTestReport() {
        return testReport;
    }

    public String getTestReportAsJson() {
        return ParserBase
                .getFhirContext()
                .newJsonParser()
                .setPrettyPrint(true)
                .encodeResourceToString(testReport);
    }

    public List<String> getErrors() {
        return errors;
    }

    public TestScript getTestScript() {
        return testScript;
    }

    public FhirClient getFhirClient() {
        return fhirClient;
    }

    public TestEngine setFhirClient(FhirClient fhirClient) {
        this.fhirClient = fhirClient;
        return this;
    }

    public FixtureMgr getFixtureMgr() {
        return fixtureMgr;
    }

    public TestEngine setSut(URI sut) {
        this.sut = sut;
        return this;
    }

    public void setTestScript(TestScript testScript) {
        this.testScript = testScript;
    }

    public TestEngine setTestCollection(String testCollection) {
        this.testCollection = testCollection;
        if (fixtureMgr != null)
            fixtureMgr.setTestCollectionId(testCollection);
        return this;
    }

    public TestEngine setTestId(String testId) {
        this.testId = testId;
        if (fixtureMgr != null)
            fixtureMgr.setTestId(testId);
        return this;
    }

    private TestEngine setTestDef(File testDef) {
        Objects.requireNonNull(testDef);
        if (testDef.isDirectory()) {
            this.testDef = testDef;
            this.testScriptName = null;
        } else {
            this.testDef = testDef.getParentFile();
            this.testScriptName = testDef.getName();
            this.multiUseTestScriptName = testScriptName;
        }
        return this;
    }

    public File getTestDef() {
        if (this.testDef.isDirectory())
            return this.testDef;
        else
            return new File(this.testDef, this.testScriptName);
    }

    private File getTestScriptFile() {
        if (testScriptName == null) {
            File file = new File(testDef, "TestScript.xml");
            if (file.exists())
                return file;
            return new File(testDef, "TestScript.json");
        }
        return new File(testDef, testScriptName);
    }

    public TestEngine setModularEngine(ModularEngine modularEngine) {
        this.modularEngine = modularEngine;
        return this;
    }

    public ModularEngine getModularEngine() {
        return modularEngine;
    }

    public String getTestScriptName() {
        return testScriptName;
    }

    public File getExternalCache() {
        return externalCache;
    }

    public EC getEC() {
        return new EC(getExternalCache());
    }

//    public String getChannelId() {
//        return channelId;
//    }

    public String getTestCollection() {
        return testCollection;
    }

    String getTestEnginePath() {
        if (parent != null) {
            String libraryName = testDef.getName();
            return /* parent.getTestEnginePath() + */
                     /*"**Module**: " + */ testScriptName + " (" + libraryName + ")\n";
        }
        return /* "**Script**: " + */ testCollection + " / " + testId + "\n";
    }

    public String getChannelId() {  // testSession __ channelName
        return channelId;
    }

    public String getTestSession() {
        Objects.requireNonNull(getChannelId());
        String[] parts = getChannelId().split("__");
        assert parts.length == 2;
            return parts[0];
    }

    public String getChannelName() {
        String[] parts = getChannelId().split("__");
        assert parts.length == 2;
        return parts[1];
    }

    /**
     *
     * Required if debugging a test script through this test engine
    */
    public TestEngine setTestScriptDebugState(TestScriptDebugState state) {
        if (state != null) {
            this.debugger = new TestScriptDebugger(this, state);
        }
        return this;
    }


    public URI getSut() {
        return sut;
    }


    private boolean hasImportModifierExtension(TestScript.SetupActionOperationComponent operation) {
        if (operation.hasModifierExtension()) {
            Optional<Extension> extension = operation.getModifierExtension()
                    .stream()
                    .filter(s -> ExtensionDef.ts_import.equals(s.getUrl()))
                    .findFirst();
            return extension.isPresent();
        }
        return false;
    }

    /**
     *
     * @param innerFixtures
     * @param componentReference
     * @param errorReport
     */
    private void updateCurrentTestEngineFixtureOut(FixtureMgr innerFixtures, ComponentReference componentReference, ErrorReport errorReport) {
        for (Parameter parm : componentReference.getFixturesOut()) {
            String outerName = parm.getCallerName();
            String innerName = parm.getLocalName();
            FixtureComponent fixtureComponent = innerFixtures.get(innerName);
            if (errorReport == null) {
                if (fixtureComponent == null)
                    throw new RuntimeException("Script import - " + componentReference.getComponentRef() + " did not produce out Fixture " + innerName);
            }
            if (fixtureComponent != null) {
                // Allow the case where expected assertion can fail but fixture out is a valid pass-back value
                fixtureMgr.put(outerName, fixtureComponent);
            }
        }

    }

    /**
     * Pass module fixtures back to caller
     * @param op
     */
    void updateParentFixtureOut(TestScript.SetupActionOperationComponent op) {
        if (this.parent != null && fixtureOutParams != null) {
            // Just a safety to avoid blank approach
//            if ("internalFtkRequest".equals(op.getType().getCode())
//                    || "ftkValidate".equals(op.getType().getCode())) {
                for (Parameter parm : fixtureOutParams) {
                    String outerName = parm.getCallerName();
                    String innerName = parm.getLocalName();
                    ErrorReport errorReport = getErrorMessage(this.getTestReport());
                    FixtureComponent fixtureComponent = this.fixtureMgr.get(innerName);
                    if (errorReport == null) {
                        if (fixtureComponent == null)
                            throw new RuntimeException("updateParentFixtureOut: Script import - " + this.getTestDef() /*componentReference.getComponentRef()*/ + " did not produce out Fixture " + innerName);
                        if (this.parent.fixtureMgr.containsKey(outerName) && this.parent.fixtureMgr.get(outerName).getId() != null && this.parent.fixtureMgr.get(outerName).getId().equals(fixtureComponent.getId())) {
                            log.info("updateParentFixtureOut: TestEngine parent already has outName in map: " + outerName);
                        } else {
                            this.parent.fixtureMgr.put(outerName, fixtureComponent);
                        }
                    } else {
                        log.severe("updateParentFixtureOut: errorReport is not Null: TestDef: " + this.testDef + ", TestScript: " + this.getTestScriptFile());
                    }
                }
//            } else {
//                log.warning("Parent fixture out was not updated for operation code: " + op.getType().getCode());
//            }
        }

        /*
        if (this.parent != null) {
            if (op.getType().getCode().equals("internalFtkRequest")) { // getFixtureString
                // This is required for reporting purposes. Some sub-fixtures within the parent TestScript may use the output fixture of this operation, which is not yet available without this step.
                // Even though this 'missing fixture' is not critical at this point, reporting method likes to dump the entire TestScript state
                String lastOp = this.fixtureMgr.getLastOp();
                if (lastOp != null) {
                    te.parent.getFixtureMgr().put(lastOp, this.fixtureMgr.get(lastOp));
                } else {
                    log.severe("Error: lastOp is null. lastOp should be non-empty when internalFtkRequest (getFixtureString) is used. Check fixture-out parameter mapping.");
                }
            }
        }
        *
         */
    }

    void reportOperation(ResourceWrapper wrapper, Reporter reporter, TestScript.SetupActionOperationComponent op) {
        if (parent != null) {
            new ActionReporter()
                    .setModule(false)
                    .setImAParent(true)
                    .setTestEngine(parent)
                    .setTestCollectionId(parent.getTestCollection())
                    .setTestId(parent.getTestId())
                    .reportOperation(wrapper,
                            parent.getFixtureMgr(),
                            new VariableMgr(parent.getTestScript(), parent.getFixtureMgr()).setExternalVariables(parent.externalVariables),
                            reporter,
                            op);
        }

        new ActionReporter()
                .setModule(parent != null)
                .setTestEngine(this)
                .setTestCollectionId(testCollection)
                .setTestId(testId)
                .reportOperation(wrapper,
                        fixtureMgr,
                        new VariableMgr(getTestScript(), getFixtureMgr()).setExternalVariables(externalVariables),
                        reporter,
                        op);
    }

    void reportAssertion(Reporter reporter, TestScript.SetupActionAssertComponent asrt,
                         FixtureComponent source) {
        if (parent != null) {
            new ActionReporter()
                    .setModule(false)
                    .setImAParent(true)
                    .setTestEngine(parent)
                    .setTestCollectionId(parent.getTestCollection())
                    .setTestId(parent.getTestId())
                    .reportAssertion(
                            parent.getFixtureMgr(),
                            new VariableMgr(parent.getTestScript(), parent.getFixtureMgr()).setExternalVariables(parent.externalVariables),
                            reporter,
                            source,
                            asrt);
        } else {
            new ActionReporter()
                    .setModule(parent != null)
                    .setTestEngine(this)
                    .setTestCollectionId(testCollection)
                    .setTestId(testId)
                    .reportAssertion(
                            fixtureMgr,
                            new VariableMgr(getTestScript(), getFixtureMgr()).setExternalVariables(externalVariables),
                            reporter,
                            source,
                            asrt);
        }
    }

    public String getTestId() {
        return testId;
    }

    @Override
    public String getTestCollectionId() {
        return testCollection;
    }

    @Override
    public String getTestSessionId() {
        return testSession;
    }

    public TestEngine setCallFixtureMap(Map<String, String> callFixtureMap) {
        this.callFixtureMap = callFixtureMap;
        return this;
    }

    public TestEngine setCallVariableMap(Map<String, String> callVariableMap) {
        this.callVariableMap = callVariableMap;
        return this;
    }

    public Map<String, String> getCallFixtureMap() {
        return callFixtureMap;
    }

    public Map<String, String> getCallVariableMap() {
        return callVariableMap;
    }

    public boolean hasDebugger() {
        return debugger != null;
    }

    public TestScriptDebugInterface getDebugger() {
        return debugger;
    }

    public void setMultiUseTestScriptName(String multiUseTestScriptName) {
        this.multiUseTestScriptName = multiUseTestScriptName;
    }
}
