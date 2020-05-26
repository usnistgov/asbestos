package gov.nist.asbestos.testEngine.engine;

import ca.uhn.fhir.parser.IParser;
import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.events.UIEvent;
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
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;


import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

/**
 * See http://hl7.org/fhir/testing.html
 */
public class TestEngine  {
    private static Logger log = Logger.getLogger(TestEngine.class);

    private File testDef = null; // directory holding test definition
    private String testScriptName = null;   // name of testscript file, TestScript.xml by default
    private URI sut = null;
    TestEngine parent = null;

    // current script and report
    private TestScript testScript = null;
    private TestReport testReport = new TestReport();
    // all scripts an reports.  [0] is top level, all others are called modules
    private List<TestScript> testScripts = new ArrayList<>();
    private List<TestReport> testReports = new ArrayList<>();

    private boolean failOverride = false;
    private FixtureMgr fixtureMgr = new FixtureMgr();
    private Val val;
    private ValE engineVal;
    private FhirClient fhirClientForFixtures;
    private List<String> errors;
    private FhirClient fhirClient = null;
    private String testSession = null;
    private String channelId = null;   // testSession __ channelName
    private File externalCache = null;
    private String testCollection = null;
    private String testId = null;
    private boolean isRequest = false;  // running an eval on a request message?  alternative is regular server test

    public static final String LAST_OP = "_LAST_OP_";

    private ModularEngine modularEngine = null;
    private Map<String, String> externalVariables = new HashMap<>();

    /**
     *
     * @param testDef  directory containing test definition
     * @param sut base address of fhir server under test
     */
    public TestEngine(File testDef, URI sut) {
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
    public TestEngine(File testDef) {
        Objects.requireNonNull(testDef);
        setTestDef(testDef);
        ResourceCacheMgr inTestResources = new ResourceCacheMgr(testDef, new Ref(""));
        fhirClientForFixtures = new FhirClient().setResourceCacheMgr(inTestResources);
        fixtureMgr.setFhirClient(fhirClientForFixtures);
    }

    // used for client tests
    public TestEngine(File testDef, TestScript testScript) {
        setTestDef(testDef);
        this.testScript = testScript;
        ResourceCacheMgr inTestResources = new ResourceCacheMgr(testDef, new Ref(""));
        fhirClientForFixtures = new FhirClient().setResourceCacheMgr(inTestResources);
        fixtureMgr.setFhirClient(fhirClientForFixtures);
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
            doWorkflow();
        } catch (Throwable t) {
            reportTerminalFailure(t);
        }
        //returnTestReport();

        return this;
    }

    // TODO - This interpretation of TestScript does not use assert.direction for client tests.  Instead it uses separate fixtures for request and response. This is reflected in the tests written.

    // if inputResource == null then this is a test
    // if null then this is an evaluation
    public TestEngine runEval(ResourceWrapper requestResource, ResourceWrapper responseResource) {
        Objects.requireNonNull(val);
        Objects.requireNonNull(testSession);
        Objects.requireNonNull(externalCache);
        isRequest = true;
        engineVal = new ValE(val);
        engineVal.setMsg("TestEngine");
        try {
            initWorkflow();
            doLoadFixtures();
            if (requestResource != null)
                fixtureMgr.add("request", requestResource);
                //fixtureMgr.put("request", new FixtureComponent(requestResource));
            if (responseResource != null)
                fixtureMgr.add("response", responseResource);
                //fixtureMgr.put("response", new FixtureComponent(responseResource));
            doTest(); // should only be asserts
            errorOut();
            fillInSkips();
            doLintTestReport();
        } catch (Throwable t) {
            reportTerminalFailure(t);
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

    public TestReport returnExceptionAsTestReport(Throwable t) {
        testReport = new TestReport();
        reportTerminalFailure(t);
        logTestReport();
        return testReport;
    }

    private void reportTerminalFailure(Throwable t) {
        String msg = t.getClass().getSimpleName() + ": " + t.getMessage();
        if (t.getMessage() == null || t.getMessage().endsWith("Exception"))
            msg = ExceptionUtils.getStackTrace(t);
        reportTerminalFailure(msg);
    }

    private void reportTerminalFailure(String msg) {
        // String trace = ExceptionUtils.getStackTrace(t);
        getTestReport().setStatus(TestReport.TestReportStatus.ENTEREDINERROR);
        getTestReport().setResult(TestReport.TestReportResult.FAIL);

        Extension extension = new Extension().setUrl(ExtensionDef.failure).setValue(new StringType(msg));
        getTestReport().getExtension().add(extension);
        log.error(msg);

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
        String json = ProxyBase.encode(testReport, Format.JSON);
        Path path = new File(logDir, "TestReport.json").toPath();
        try (BufferedWriter writer = Files.newBufferedWriter(path))
        {
            writer.write(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void doWorkflow() {
        initWorkflow();

        try {
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
        if (scriptSetups.size() != reportSetups.size())
            reportTerminalFailure("TestEngine internal Error: Script Setup had " +
                    scriptSetups.size() +
                    " elements but Report had " +
                    reportSetups.size());

        for (int i=0; i< testScript.getTest().size(); i++) {
            List<TestScript.TestActionComponent> tests = testScript.getTest().get(i).getAction();
            List<TestReport.TestActionComponent> reports;
            if (testReport.getTest().size() > i)
                reports = testReport.getTest().get(i).getAction();
            else
                reports = new ArrayList<>();
            if (tests.size() != reports.size())
                reportTerminalFailure("TestEngine internal Error: Script Test " + i + " had " +
                        tests.size() +
                        " elements but Report had " +
                        reports.size());
        }

        List<TestScript.TeardownActionComponent> script = testScript.getTeardown().getAction();
        List<TestReport.TeardownActionComponent> report;
        report = testReport.getTeardown().getAction();
        if (script.size() != report.size())
            reportTerminalFailure("TestEngine internal Error: Script Teardown had " +
                    script.size() +
                    " elements but Report had " +
                    report.size());
    }

    private void initWorkflow() {
        if (testScript == null)
            testScript = loadTestScript(testDef, testScriptName);
        String path = testScript.getName();
        String name = "";
        if (path.contains(File.separator)) {
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

    private boolean errorOut() {
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
                        Ref ref = new Ref(fix.getResource().getReference());
                        fixtureMgr.add(id)
                                .setStaticRef(ref)
                                .setVal(fVal)
                        //        .setCreatedBy(new Action(testScript, fix))
                        ;

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
                reportTerminalFailure(t);
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
                        SetupActionCreate create = new SetupActionCreate(new ActionReference(testScript, comp), fixtureMgr)
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
                        SetupActionDelete delete = new SetupActionDelete(new ActionReference(testScript, comp), fixtureMgr)
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

    private void doSetup() {
        boolean reportAsConditional = false;  // upgrade this when conditional execution comes to setup
        if (testScript.hasSetup()) {
            TestScript.TestScriptSetupComponent comp = testScript.getSetup();
            ValE fVal = new ValE(engineVal).setMsg("Setup");
            TestReport.TestReportSetupComponent setupReportComponent = testReport.getSetup();
            if (comp.hasAction()) {
                String typePrefix = "setup.action";
                for (TestScript.SetupActionComponent action : comp.getAction()) {
                    TestReport.SetupActionComponent actionReportComponent = setupReportComponent.addAction();
                    if (invalidAction(action, actionReportComponent, fVal))
                        return;
                    if (action.hasOperation()) {
                        doOperation(new ActionReference(testScript, action), typePrefix, action.getOperation(), actionReportComponent.getOperation());
                        TestReport.SetupActionOperationComponent opReport = actionReportComponent.getOperation();
                        if (opReport.getResult() == TestReport.TestReportActionResult.ERROR) {
                            testReport.setStatus(TestReport.TestReportStatus.COMPLETED);
                            testReport.setResult(TestReport.TestReportResult.FAIL);
                            return;
                        }
                    }
                    if (action.hasAssert()) {
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
                }
            }
        }
    }

    private void doOperation(ActionReference actionReference, String typePrefix, TestScript.SetupActionOperationComponent operation, TestReport.SetupActionOperationComponent report) {
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
                runner.run(operation, report);
            } catch (Throwable t) {
                report.setMessage(ExceptionUtils.getStackTrace(t));
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
                    report.setMessage("Operation with unknown modifierExtension " + url + " found");
                    report.setResult(TestReport.TestReportActionResult.ERROR);
                }
            }
        } else  {
            report.setMessage("Found operation with no type and no modifierExtension");
            report.setResult(TestReport.TestReportActionResult.ERROR);
        }
    }

    private void doAssert(String typePrefix, TestScript.SetupActionAssertComponent theAssert, TestReport.SetupActionAssertComponent report) {
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
        } catch (Throwable t) {
            reportTerminalFailure(t);
//            report = new TestReport.SetupActionAssertComponent();
//            report.setMessage(ExceptionUtils.getStackTrace(t));
//            report.setResult(TestReport.TestReportActionResult.ERROR);
        }
    }

    private void doTest() {
        if (testScript.hasTest()) {
            ValE fVal = new ValE(engineVal).setMsg("Test");

            try {
                int testCounter = 1;
                for (TestScript.TestScriptTestComponent test : testScript.getTest()) {

                    // if noErrors extension present and script has already hit an error then bail out
                    // and don't run actions in this test
                    if (getExtension(test.getModifierExtension(), ExtensionDef.noErrors) != null) {
                        if (!getTestReport().getResult().equals(TestReport.TestReportResult.PASS))
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
                                doTestPart(test, testReportComponent, testReport);
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
                        doTestPart(test, testReportComponent, testReport);


                }
            } catch (Throwable t) {
                String msg = t.getMessage();
                if (msg == null || msg.equals(""))
                    msg = ExceptionUtils.getStackTrace(t);
                reportParsingError(testReport.addTest(), msg);
            }

        }
    }

    Set<String> moduleIds = new HashSet<>();
    private String assignModuleId(String candidate) {
        String real = candidate;
        int i = 1;
        while (moduleIds.contains(real)) {
            real = candidate + i;
            i++;
        }
        moduleIds.add(real);
        return real;
    }

    private void handleImport(Extension extension, TestScript.SetupActionOperationComponent opScript, TestReport.SetupActionOperationComponent opReport) {

        /*
            Validate and align request input fixtures and variables
         */
        ComponentReference componentReference = new ComponentReference(testDef, Collections.singletonList(extension));

        TestScript module = componentReference.getComponent();
        // fill in componentReference with local names from module definition
        new ComponentDefinition(getTestScriptFile(), module).loadTranslation(componentReference);

        // align fixtures for module
        Map<String, FixtureComponent> inFixturesForComponent = new HashMap<>();
        for (Parameter parm : componentReference.getFixturesIn()) {
            String outerName = parm.getCallerName();
            String innnerName = parm.getLocalName();
            FixtureComponent fixtureComponent = fixtureMgr.get(outerName);
            if (fixtureComponent != null && fixtureComponent.getFixtureSub() != null) {
                // create temporary FixtureComponent containing the translations
                // and the extracted content
                fixtureComponent = fixtureComponent.getFixtureSub().getSubFixture(fixtureComponent);
            }
            if (fixtureComponent == null)
                throw new RuntimeException("Fixture " + outerName + " does not exist");
            inFixturesForComponent.put(innnerName, fixtureComponent);
        }

        // align variables for module
        Map<String, String> externalVariables = new HashMap<>();
        VariableMgr varMgr = new VariableMgr(testScript, fixtureMgr)
                .setVal(engineVal)
                .setOpReport(opReport);
        for (Parameter parm : componentReference.getVariablesIn()) {
            String outerName = parm.getCallerName();
            String innerName = parm.getLocalName();
            String value = varMgr.eval(outerName, false);
            externalVariables.put(innerName, value);
        }
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
        TestEngine testEngine1 = sut == null
                ? new TestEngine(componentReference.getComponentRef())
                : new TestEngine(componentReference.getComponentRef(), this.sut);
        testEngine1
                .setTestSession(testSession)
                .withResourceCacheManager(this.getCacheManager())
                .setVal(new Val())
                .setExternalCache(externalCache)
                .setFixtures(inFixturesForComponent)
                .setExternalVariables(externalVariables)
                .setFhirClient(new FhirClient())
                .setChannelId(channelId)
                .setTestCollection(testCollection)
                .setTestId(testId)
                ;
        modularEngine.add(testEngine1);
        testEngine1.parent = this;
        testEngine1.runTest();

        /*
            Assign moduleName and moduleId in caller's TestReport.
            moduleName is the name of the module.
            moduleId is the same name with a numeric suffix to make this call unique.
            The module call is identified as moduleName/moduleId in the logs
         */
        String moduleName = simpleName(componentReference.getComponentRef());
        String moduleId = assignModuleId(moduleName);
        opReport.addModifierExtension(new Extension(ExtensionDef.moduleId, new StringType(moduleId)));
        opReport.addModifierExtension(new Extension(ExtensionDef.moduleName, new StringType(moduleName)));
        testEngine1.getTestReport().addExtension(ExtensionDef.moduleId, new StringType(moduleId));
        testEngine1.getTestReport().addExtension(ExtensionDef.moduleName, new StringType(moduleName));

        // Report overall module call status into caller's TestReport.operation
        ErrorReport errorReport = getErrorMessage(testEngine1.getTestReport());
        if (errorReport != null) {
            opReport.setResult(errorReport.type);
            opReport.setMessage(errorReport.message);
        }

        // Pass module output variables and fixtures back to caller
        FixtureMgr innerFixtures = testEngine1.fixtureMgr;
        Map<String, FixtureComponent> outFixturesForComponent = new HashMap<>();
        for (Parameter parm : componentReference.getFixturesOut()) {
            if (parm.isVariable())
                throw new RuntimeException("Script import with output variables not supported");
            String outerName = parm.getCallerName();
            String innerName = parm.getLocalName();
            FixtureComponent fixtureComponent = innerFixtures.get(innerName);
            if (errorReport == null) {
                if (fixtureComponent == null)
                    throw new RuntimeException("Script import - " + componentReference.getComponentRef() + " did not produce Fixture " + innerName);
                fixtureMgr.put(outerName, fixtureComponent);
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

    private ResourceCacheMgr getCacheManager() {
        return fixtureMgr.getFhirClient().getResourceCacheMgr();
    }

    static class ErrorReport {
        TestReport.TestReportActionResult type;
        String message;

        ErrorReport(TestReport.SetupActionAssertComponent theAssert) {
            type = theAssert.getResult();
            message = theAssert.getMessage();
        }

        ErrorReport(TestReport.SetupActionOperationComponent theOp) {
            type = theOp.getResult();
            message = theOp.getMessage();
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
        if (report.hasSetup()) {
            for (TestReport.SetupActionComponent action : report.getSetup().getAction()) {
                if (action.hasAssert() && hasErrorOrFail(action.getAssert()))
                    return new ErrorReport(action.getAssert());
                if (action.hasOperation() && hasErrorOrFail(action.getOperation()))
                    return new ErrorReport(action.getOperation());
            }
        }

        if (report.hasTest()) {
            for (TestReport.TestReportTestComponent test : report.getTest()) {
                for (TestReport.TestActionComponent action : test.getAction()) {
                    if (action.hasAssert() && hasErrorOrFail(action.getAssert()))
                        return new ErrorReport(action.getAssert());
                    if (action.hasOperation() && hasErrorOrFail(action.getOperation()))
                        return new ErrorReport(action.getOperation());
                }
            }
        }
        if (report.hasTeardown()) {
            TestReport.TestReportTeardownComponent teardown = report.getTeardown();
            for (TestReport.TeardownActionComponent action : teardown.getAction()) {
                if (action.hasOperation() && hasErrorOrFail(action.getOperation())) {
                    return new ErrorReport(action.getOperation());
                }
            }
        }
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
    private boolean handleConditionalTest(TestScript.TestScriptTestComponent testComponent, TestReport.TestReportTestComponent testReportComponent, Extension extension) {
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
        boolean conditionalTestResult = doTestPart(basicOperationTest, containedTestReportComponent, containedTestReport);

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

        conditionalResult = doTestPart(conditionalTest, containedTestReportComponent, containedTestReport);

        if (containedTestReport.getResult() == TestReport.TestReportResult.FAIL)
            return false;
        return conditionalResult;
    }

    private void reportSkip(TestReport.TestReportTestComponent reportComponent) {
        TestReport.TestActionComponent testActionComponent = reportComponent.addAction();
        TestReport.SetupActionOperationComponent setupActionOperationComponent = testActionComponent.getOperation();
        setupActionOperationComponent.setResult(TestReport.TestReportActionResult.SKIP);
        setupActionOperationComponent.setMessage("skipped");
    }

    private boolean doTestPart(TestScript.TestScriptTestComponent testScriptElement, TestReport.TestReportTestComponent testReportComponent, TestReport testReport) {
        ValE fVal = new ValE(engineVal).setMsg("Test");
        if (testScriptElement.hasAction()) {
            String typePrefix = "contained.action";

            boolean multiErrorsAllowed = getExtension(testScriptElement.getModifierExtension(), ExtensionDef.multiErrors) != null;

            for (int i=0; i<testScriptElement.getAction().size(); i++) {
                TestScript.TestActionComponent action = testScriptElement.getAction().get(i);
                Extension conditional = getExtension(action.getModifierExtension(), ExtensionDef.ts_conditional);
                boolean isConditional = conditional != null;
                TestReport.TestActionComponent actionReportComponent = testReportComponent.addAction();
                if (invalidAction(action, actionReportComponent, fVal))
                    return false;
                if (action.hasOperation()) {
                    TestReport.SetupActionOperationComponent reportOp = actionReportComponent.getOperation();
                    doOperation(new ActionReference(testScript, action), typePrefix, action.getOperation(), reportOp);
                    TestReport.SetupActionOperationComponent opReport = actionReportComponent.getOperation();
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
                    if (i + 1 < testScriptElement.getAction().size()) {
                        TestScript.TestActionComponent nextAction = testScriptElement.getAction().get(i+1);
                        if (!nextAction.hasAssert() && actionReportComponent.getOperation().getResult().equals(TestReport.TestReportActionResult.FAIL)) {
                            testReport.setStatus(TestReport.TestReportStatus.COMPLETED);
                            testReport.setResult(TestReport.TestReportResult.FAIL);
                            if (!multiErrorsAllowed)
                                return false;
                        }
                    }
                }
                if (action.hasAssert()) {
                    TestReport.SetupActionAssertComponent actionReport = actionReportComponent.getAssert();
                    doAssert(typePrefix, action.getAssert(), actionReport);
                    if (actionReport == null)
                        return false;
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
        }
        return true;
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
            reporter.reportError( "action has both operation and assertion");
            return true;
        }
        return false;
    }

    private boolean invalidAction(TestScript.TestActionComponent action, TestReport.TestActionComponent actionReportComponent, ValE fVal) {
        if (action.hasOperation() && action.hasAssert()) {
            Reporter reporter = new Reporter(fVal, actionReportComponent.getOperation(), "", "");
            reporter.reportError( "action has both operation and assertion");
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

    private void doTearDown() {
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
                            doOperation(new ActionReference(testScript, action), typePrefix, action.getOperation(), actionReport.getOperation());

                        if (hasError())
                            return;
                    }
                }
            }
        }
    }

    // build cache entry if testscript has property cache=true
    private void doPostProcessing() {
        if (testCollection == null)
            return;
        if (sut == null)
            return;
        EC ec = new  EC(externalCache);
        Properties tcProperties = ec.getTestCollectionProperties(testCollection);
        String useCache = tcProperties.getProperty("cache");
        if (useCache == null || !useCache.equals("true"))
            return;

        for (TestReport.TestReportTestComponent testComponent : testReport.getTest()) {


            for (TestReport.TestActionComponent actionResult : testComponent.getAction()) {
                if (!actionResult.hasOperation())
                    continue;
                TestReport.SetupActionOperationComponent op = actionResult.getOperation();
                buildCacheEntry(op, ec);
            }
        }

        TestScript.TestActionComponent foo;

    }

    private void buildCacheEntry(TestReport.SetupActionOperationComponent op, EC ec) {
        if ("pass".equals(op.getResult().toCode())) {
            //if (op.getMessage().startsWith("GET") || op.getMessage().startsWith("CREATE")) {
            if (op.getDetail() == null)
                throw new Error("Error building cache entry - op.detail is null");
            URI uri;
            try {
                uri = new URI(op.getDetail());
            } catch (URISyntaxException e) {
                throw new Error("Error building cache entry - " + e.getMessage());
            }
            UIEvent uiEvent = new UIEvent(ec).fromURI(uri);
            buildCacheEntry(uiEvent, ec);
            // }
        }
    }

    private void buildCacheEntry(UIEvent uiEvent, EC ec) {
        if (uiEvent != null) {
            // add to cache
            File cacheDir = ec.getTestLogCacheDir(channelId);
            String responseBody = uiEvent.getClientTask().getResponseBody();
            BaseResource baseResource = ProxyBase.parse(responseBody, Format.fromContent(responseBody));
            if (baseResource instanceof Bundle) {
                Bundle bundle = (Bundle) baseResource;
                for (Bundle.BundleEntryComponent comp : bundle.getEntry()) {
                    if (comp.getResource() instanceof Patient) {
                        String fullUrl = comp.getFullUrl();
                        if (fullUrl != null && !fullUrl.equals("") && bundle.getTotal() == 1)
                            buildCacheEntry(cacheDir, bundle, (Patient) comp.getResource());
                    }
                }
            }
        }
    }

    private void buildCacheEntry(File cacheDir, Bundle bundle, Patient patient) {
        File resourceTypeFile = new File(cacheDir, "Patient");
        resourceTypeFile.mkdirs();

        String given;
        try {
            given = patient.getNameFirstRep().getGiven().get(0).toString();
        } catch (Throwable t) {
            given = "Missing";
        }
        String family;
        try {
            family = patient.getNameFirstRep().getFamily();
        } catch (Throwable t) {
            family = "Person";
        }
        if (given != null &&!given.equals("") && family != null && !family.equals("")) {
            log.info("Writing cache entry to " + resourceTypeFile + " for " + given + "_" + family);
            ProxyBase.toFile(bundle, resourceTypeFile, given + "_" + family, Format.JSON);
        }
    }

    public TestEngine addCache(File cacheDir) {
        fhirClientForFixtures.getResourceCacheMgr().addCache(cacheDir);
        return this;
    }

    public static TestScript loadTestScript(File testDefDir) {
        return loadTestScript(testDefDir, null);
    }

    public static TestScript loadTestScript(File testDefDir, String fileName)  {
        Objects.requireNonNull(testDefDir);
        File location;
        if (fileName == null)
            location = findTestScriptFile(testDefDir);
        else
            location = new File(testDefDir, fileName);
        InputStream is;
        try {
            is = new FileInputStream(location);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        IParser parser = (location.toString().endsWith("xml") ? ProxyBase.getFhirContext().newXmlParser() : ProxyBase.getFhirContext().newJsonParser());
        IBaseResource resource = parser.parseResource(is);
        assert resource instanceof TestScript;
        TestScript testScript = (TestScript) resource;
        testScript.setName(location.toString());

        return testScript;
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

    private boolean isFixtureDefined(String id) {
        return fixtureMgr.containsKey(id);
    }

    FixtureMgr getFixtures() {
        return fixtureMgr;
    }

    private boolean fixturesOk() {
        for (FixtureComponent fixtureComp : fixtureMgr.values()) {
            if (!fixtureComp.IsOk())
                return false;
        }
        return true;
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
        return ProxyBase
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
        }
        return this;
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

    public String getTestScriptName() {
        return testScriptName;
    }

    public File getExternalCache() {
        return externalCache;
    }

//    public String getChannelId() {
//        return channelId;
//    }

    public String getTestCollection() {
        return testCollection;
    }

    String getTestEnginePath() {
        if (parent != null) {
            String parentPath = parent.getTestEnginePath();
            if (parentPath.endsWith(":"))
                return parentPath + " " + testScriptName;
            return parentPath + "/" + testScriptName;
        }
        return "/" + testCollection + "/" + testId + ":";
    }

//    public String getTestSession() {
//        return testSession;
//    }

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

    public URI getSut() {
        return sut;
    }
}
