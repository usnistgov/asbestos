package gov.nist.asbestos.testEngine.engine;

import ca.uhn.fhir.parser.IParser;
import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceCacheMgr;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;

import java.io.*;
import java.net.Proxy;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * See http://hl7.org/fhir/testing.html
 */
public class TestEngine  {
    private File testDef = null;
    private URI sut = null;
    private TestScript testScript = null;
    private FixtureMgr fixtureMgr = new FixtureMgr();
    private Val val;
    private ValE engineVal;
    private FhirClient fhirClientForFixtures;
    private TestReport testReport = new TestReport();
    private List<String> errors;
    private FhirClient fhirClient = null;
    private String testSession = null;
    private File externalCache = null;

    public static final String LAST_OP = "_LAST_OP_";

    /**
     *
     * @param testDef  directory containing test definition
     * @param sut base address of fhir server under test
     */
    public TestEngine(File testDef, URI sut) {
        Objects.requireNonNull(testDef);
        Objects.requireNonNull(sut);
        this.testDef = testDef;
        this.sut = sut;
        ResourceCacheMgr inTestResources = new ResourceCacheMgr(testDef, new Ref(""));
        fhirClientForFixtures = new FhirClient().setResourceCacheMgr(inTestResources);
    }

    // used for evaluation
    public TestEngine(File testDef) {
        Objects.requireNonNull(testDef);
        this.testDef = testDef;
    }

    public TestEngine(File testDef, TestScript testScript) {
        this.testDef = testDef;
        this.testScript = testScript;
    }

    public TestEngine setTestSession(String testSession) {
        this.testSession = testSession;
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
        try {
            doWorkflow();
        } catch (Throwable t) {
            reportException(t);
        }
        returnTestReport();

        return this;
    }

    // if inputResource == null then this is a test
    // if null then this is an evaluation
    public TestEngine runEval(ResourceWrapper requestResource, ResourceWrapper responseResource) {
        Objects.requireNonNull(val);
        Objects.requireNonNull(testSession);
        Objects.requireNonNull(externalCache);
        engineVal = new ValE(val);
        engineVal.setMsg("TestEngine");
        try {
            fixtureMgr.put("request", new FixtureComponent(requestResource));
            fixtureMgr.put("response", new FixtureComponent(responseResource));
            initWorkflow();
            doTest(); // should only be asserts
            errorOut();
        } catch (Throwable t) {
            reportException(t);
        }
        returnTestReport();

        return this;
    }

    private void reportException(Throwable t) {
        String trace = ExceptionUtils.getStackTrace(t);
        TestReport.TestReportSetupComponent setup = testReport.getSetup();
        TestReport.SetupActionComponent comp = setup.addAction();
        TestReport.SetupActionAssertComponent asComp = new TestReport.SetupActionAssertComponent();
        asComp.setMessage(trace);
        asComp.setResult(TestReport.TestReportActionResult.ERROR);
        comp.setAssert(asComp);
        propagateStatus(testReport);
    }

    private void returnTestReport() {
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
        } finally {
            doAutoDeletes();
            doPostProcessing();
        }
    }

    private void initWorkflow() {
        if (testScript == null)
            testScript = loadTestScript(testDef);
        testReport.setName(testScript.getName());
        testReport.setTestScript(new Reference(testScript.getId()));
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
            return true;
        }
        return false;
    }

    public boolean hasError() {
        return testReport.hasResult() && testReport.getResult() == TestReport.TestReportResult.FAIL;
    }

    private List<String> doReportResult() {
        List<String> failingComponents = new ArrayList<>();

        TestReport.TestReportSetupComponent setup = testReport.getSetup();
        for (TestReport.SetupActionComponent action : setup.getAction()) {
            if (action.hasOperation()) {
                TestReport.SetupActionOperationComponent op = action.getOperation();
                TestReport.TestReportActionResult result = op.getResult();
                if (result == TestReport.TestReportActionResult.FAIL || result == TestReport.TestReportActionResult.ERROR)
                    failingComponents.add(op.getMessage());
            }
            if (action.hasAssert()) {
                TestReport.SetupActionAssertComponent as = action.getAssert();
                TestReport.TestReportActionResult result2 = as.getResult();
                if (result2 == TestReport.TestReportActionResult.FAIL || result2 == TestReport.TestReportActionResult.ERROR)
                    failingComponents.add(as.getMessage());
            }
        }
        for (TestReport.TestReportTestComponent test : testReport.getTest()) {
            for (TestReport.TestActionComponent action : test.getAction()) {
                if (action.hasOperation()) {
                    TestReport.SetupActionOperationComponent op = action.getOperation();
                    TestReport.TestReportActionResult result = op.getResult();
                    if (result == TestReport.TestReportActionResult.FAIL || result == TestReport.TestReportActionResult.ERROR)
                        failingComponents.add(op.getMessage());
                }
                if (action.hasAssert()) {
                    TestReport.SetupActionAssertComponent as = action.getAssert();
                    TestReport.TestReportActionResult result2 = as.getResult();
                    if (result2 == TestReport.TestReportActionResult.FAIL || result2 == TestReport.TestReportActionResult.ERROR)
                        failingComponents.add(as.getMessage());
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

        if (testScript.hasFixture()) {
            ValE fVal = new ValE(engineVal).setMsg("Fixtures");

            for (TestScript.TestScriptFixtureComponent comp : testScript.getFixture()) {
                String id = comp.getId();
                if (id == null || id.equals("")) {
                    throw new Error("Static Fixture has no id and cannot be referenced");
                }
//                if (!comp.hasAutocreate())
//                    throw new Error("fixture.autocreate is a required field");
//                if (!comp.hasAutodelete())
//                    throw new Error("fixture.autodelete is a required field");
                Ref ref = new Ref(comp.getResource().getReference());
                Optional<ResourceWrapper> optWrapper = fhirClientForFixtures.readCachedResource(ref);
                if (!optWrapper.isPresent())
                    throw new Error("Static Fixture " + ref + " cannot be loaded");
                ResourceWrapper wrapper = optWrapper.get();
                FixtureComponent fixtureComponent;
                try {
                    fixtureComponent = new FixtureComponent(id).setResource(wrapper).setVal(fVal).load(wrapper);
                    if (fixtureComponent != null)
                        fixtureMgr.put(id, fixtureComponent);
                } catch (Throwable e) {
                    throw new Error(e);
                }
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
                        TestReport.SetupActionComponent actionReport = setupReportComponent.addAction();
                        Reference resource = comp.getResource();
                        String resourceType = resource.getType();
                        TestReport.SetupActionOperationComponent operationReport = actionReport.getOperation();
                        operationReport.setResult(TestReport.TestReportActionResult.PASS);  // may be overwritten
                        SetupActionCreate create = new SetupActionCreate(fixtureMgr)
                                .setFhirClient(fhirClient)
                                .setSut(sut)
                                .setType("fixture.autocreate")
                                .setVal(fVal)
                                .setVariableMgr(new VariableMgr(testScript, fixtureMgr)
                                        .setVal(fVal)
                                        .setOpReport(operationReport));
                        create.run(comp.getId(), comp.getResource(), operationReport);
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
                        TestReport.SetupActionOperationComponent operationReport = actionReport.getOperation();
                        operationReport.setResult(TestReport.TestReportActionResult.PASS);  // may be overwritten
                        SetupActionDelete delete = new SetupActionDelete(fixtureMgr)
                                .setFhirClient(fhirClient)
                                .setSut(sut)
                                .setType("fixture.autodelete")
                                .setVal(fVal)
                                .setVariableMgr(new VariableMgr(testScript, fixtureMgr)
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
        if (testScript.hasSetup()) {
            TestScript.TestScriptSetupComponent comp = testScript.getSetup();
            ValE fVal = new ValE(engineVal).setMsg("Setup");
            TestReport.TestReportSetupComponent setupReportComponent = testReport.getSetup();
            if (comp.hasAction()) {
                String typePrefix = "setup.action";
                for (TestScript.SetupActionComponent action : comp.getAction()) {
                    TestReport.SetupActionComponent actionReportComponent = setupReportComponent.addAction();
                    if (action.hasOperation() && action.hasAssert()) {
                        Reporter reporter = new Reporter(fVal, actionReportComponent.getOperation(), "", "");
                        reporter.reportError( "action has both operation and assertion");
                        return;
                    }
                    if (action.hasOperation())
                        doOperation(typePrefix, action.getOperation(), actionReportComponent.getOperation());
                    if (action.hasAssert()) {
                        TestReport.SetupActionAssertComponent actionReport = doAssert(typePrefix, action.getAssert());

                    }
                    if (hasError())
                        return;
                }
            }
        }
    }

//    private void doAction(String typePrefix, boolean isOperation, TestScript.SetupActionOperationComponent operation, TestReport.TestActionComponent actionReport, boolean isAssert, TestScript.SetupActionAssertComponent anAssert) {
//        if (isOperation) {
//            TestScript.SetupActionOperationComponent opComponent = operation;
//            TestReport.SetupActionOperationComponent opReport = actionReport.getOperation();
//            OperationRunner runner = new OperationRunner(fixtureMgr)
//                    .setVal(new ValE(val).setMsg(typePrefix))
//                    .setTypePrefix(typePrefix)
//                    .setFhirClient(fhirClient)
//                    .setSut(sut)
//                    .setTestReport(testReport)
//                    .setTestScript(testScript);
//            runner.run(opComponent, opReport);
//        } else if (isAssert) {
//            TestScript.SetupActionAssertComponent actionAssertComponent = anAssert;
//            TestReport.SetupActionAssertComponent assertComponent = actionReport.getAssert();
//            AssertionRunner runner = new AssertionRunner(fixtureMgr)
//                    .setVal(new ValE(val).setMsg(typePrefix))
//                    .setTypePrefix(typePrefix)
//                    .setTestReport(testReport)
//                    .setTestScript(testScript);
//            runner.run(actionAssertComponent, assertComponent);
//        }
//        propagateStatus(testReport);
//    }

    private void doOperation(String typePrefix, TestScript.SetupActionOperationComponent operation, TestReport.SetupActionOperationComponent report) {
        try {
            OperationRunner runner = new OperationRunner(fixtureMgr)
                    .setVal(new ValE(val).setMsg(typePrefix))
                    .setTypePrefix(typePrefix)
                    .setFhirClient(fhirClient)
                    .setSut(sut)
                    .setTestReport(testReport)
                    .setTestScript(testScript);
            runner.run(operation, report);
        } catch (Throwable t) {
            report.setMessage(ExceptionUtils.getStackTrace(t));
            report.setResult(TestReport.TestReportActionResult.ERROR);
        }
        propagateStatus(testReport);
    }

    private TestReport.SetupActionAssertComponent doAssert(String typePrefix, TestScript.SetupActionAssertComponent operation) {
        TestReport.SetupActionAssertComponent report;
        try {
            AssertionRunner runner = new AssertionRunner(fixtureMgr)
                    .setVal(new ValE(val).setMsg(typePrefix))
                    .setTypePrefix(typePrefix)
                    .setTestReport(testReport)
                    .setTestScript(testScript);
            report = runner.run(operation);
        } catch (Throwable t) {
            report = new TestReport.SetupActionAssertComponent();
            report.setMessage(ExceptionUtils.getStackTrace(t));
            report.setResult(TestReport.TestReportActionResult.ERROR);
        }
        return report;
    }

    private void doTest() {
        if (testScript.hasTest()) {
            ValE fVal = new ValE(engineVal).setMsg("Test");

            int testCounter = 1;
            for (TestScript.TestScriptTestComponent testComponent : testScript.getTest()) {
                String testName = testComponent.getName();
                if (testName == null || testName.equals(""))
                    testName = "Test" + testCounter;
                testCounter++;
                ValE tVal = new ValE(fVal).setMsg(testName);
                TestReport.TestReportTestComponent testReportComponent = testReport.addTest();
                boolean testEnabled = true;

                if (testComponent.hasModifierExtension()) {
                    Extension extension = testComponent.getModifierExtensionFirstRep();
                    String containedTestScriptId =  "#" + extension.getUrl();
                    String testLabel = extension.getValue().toString();

                    List<Resource> containedList = testScript.getContained();
                    Resource contained = null;
                    for (Resource theContained : containedList) {
                        if (theContained.getId() != null && theContained.getId().equals(containedTestScriptId)) {
                            contained = theContained;
                            break;
                        }
                    }
                    if (contained == null) {
                        TestReport.TestActionComponent actionReportComponent = testReportComponent.addAction();
                        Reporter reporter = new Reporter(fVal, actionReportComponent.getOperation(), "", "");
                        reporter.reportError( "cannot locate contained TestScript " + containedTestScriptId);
                        return;
                    }

                    // All Operations must succeed
                    // An Assert must fail
                    // ... to enable the test

                    TestScript containedTestScript = (TestScript) contained;
                    // find test with id of assertLabel
                    TestScript.TestScriptTestComponent theContainedTest = findTest(containedTestScript, testLabel);
                    if (theContainedTest == null) {
                        TestReport.TestActionComponent actionReportComponent = testReportComponent.addAction();
                        Reporter reporter = new Reporter(fVal, actionReportComponent.getOperation(), "", "");
                        reporter.reportError( "cannot locate test with label " + testLabel + " in contained TestScript " + containedTestScriptId);
                        return;
                    }

                    if (theContainedTest.hasAction()) {
                        String typePrefix = "contained.action";
                        for (TestScript.TestActionComponent action : theContainedTest.getAction()) {
                            TestReport.TestActionComponent actionReportComponent = testReportComponent.addAction();
                            if (invalidAction(action, actionReportComponent, fVal))
                                return;
                            if (action.hasOperation())
                                doOperation(typePrefix, action.getOperation(), actionReportComponent.getOperation());
                            if (action.hasAssert()) {
                                TestReport.SetupActionAssertComponent actionReport = doAssert(typePrefix, action.getAssert());
                                actionReportComponent.setAssert(actionReport);
                                if (!"pass".equals(actionReport.getResult().toCode())) {
                                    testEnabled = false;
                                    break;
                                }
                            }
                        }
                    }
                }



                // real test starts here
                if (testEnabled && testComponent.hasAction()) {
                    String typePrefix = "test.action";
                    for (TestScript.TestActionComponent action : testComponent.getAction()) {
                        TestReport.TestActionComponent actionReportComponent = testReportComponent.addAction();
                        if (invalidAction(action, actionReportComponent, fVal))
                            return;
                        if (action.hasOperation())
                            doOperation(typePrefix, action.getOperation(), actionReportComponent.getOperation());
                        if (action.hasAssert()) {
                            TestReport.SetupActionAssertComponent actionReport = doAssert(typePrefix, action.getAssert());
                            actionReportComponent.setAssert(actionReport);
                        }
                        if (hasError())
                            return;
                    }
                }
            }

        }

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
                            doOperation(typePrefix, action.getOperation(), actionReport.getOperation());

                        if (hasError())
                            return;
                    }
                }
            }
        }
    }

    private void doPostProcessing() {

    }

    public static TestScript loadTestScript(File testDefDir) {
        Objects.requireNonNull(testDefDir);
        File location = new File(testDefDir, "TestScript.xml");
        if (!location.exists() || !location.canRead() ) {
            location = new File(testDefDir, "TestScript.json");
            if (!location.exists() || !location.canRead() ) {
                throw new RuntimeException("Cannot load TestScript (.xml or .json) from " + testDefDir);
            }
        }
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

    private boolean isFixtureDefined(String id) {
        return fixtureMgr.containsKey(id);
    }

    private TestEngine addFixture(FixtureComponent fixtureComp) {
        fixtureMgr.put(fixtureComp.getId(), fixtureComp);
        return this;
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
}
