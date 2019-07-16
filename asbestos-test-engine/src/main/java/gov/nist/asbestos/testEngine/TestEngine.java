package gov.nist.asbestos.testEngine;

import ca.uhn.fhir.parser.IParser;
import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceCacheMgr;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
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

    public TestEngine run() {
        Objects.requireNonNull(val);
        engineVal = new ValE(val);
        engineVal.setMsg("TestEngine");
        doWorkflow();
        return this;
    }

    private void doWorkflow() {
        testScript = loadTestScript();
        testReport.setName(testScript.getName());
        testReport.setTestScript(new Reference(testScript.getId()));
        doPreProcessing();
        doLoadVariables();
        doLoadFixtures();
        doAutoCreates();
        doSetup();
        doTest();
        doTearDown();
        doPostProcessing();
        propagateStatus(testReport);
        errors = doReportResult();
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

            int nameI = 1;
            for (TestScript.TestScriptFixtureComponent comp : testScript.getFixture()) {
                String id = comp.getId();
                if (id == null || id.equals("")) {
                    throw new Error("Static Fixture has no id and cannot be referenced");
                }
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

    }

    private void doSetup() {
        if (testScript.hasSetup()) {
            TestScript.TestScriptSetupComponent comp = testScript.getSetup();
            ValE fVal = new ValE(engineVal).setMsg("Setup");
            TestReport.TestReportSetupComponent setupReportComponent = testReport.getSetup();
            if (comp.hasAction()) {
                String typePrefix = "setup.action";
                for (TestScript.SetupActionComponent action : comp.getAction()) {
                    TestReport.SetupActionComponent actionReport = setupReportComponent.addAction();
                    if (action.hasOperation() && action.hasAssert()) {
                        Reporter.reportError(fVal, actionReport.getOperation(), "setup", "", "action has both operation and assertion");
                        return;
                    }
                    doAction(typePrefix, action.hasOperation(), action.getOperation(), actionReport.getOperation(), action.hasAssert(), action.getAssert(), actionReport.getAssert());
                }
            }
        }
    }

    private void doAction(String typePrefix, boolean isOperation, TestScript.SetupActionOperationComponent operation, TestReport.SetupActionOperationComponent operation2, boolean isAssert, TestScript.SetupActionAssertComponent anAssert, TestReport.SetupActionAssertComponent anAssert2) {
        if (isOperation) {
            TestScript.SetupActionOperationComponent opComponent = operation;
            TestReport.SetupActionOperationComponent opReport = operation2;
            OperationRunner runner = new OperationRunner(fixtureMgr)
                    .setVal(new ValE(val).setMsg(typePrefix))
                    .setTypePrefix(typePrefix)
                    .setFhirClient(fhirClient)
                    .setTestReport(testReport)
                    .setTestScript(testScript);
            runner.run(opComponent, opReport);
        } else if (isAssert) {
            TestScript.SetupActionAssertComponent actionAssertComponent = anAssert;
            TestReport.SetupActionAssertComponent assertComponent = anAssert2;
            AssertionRunner runner = new AssertionRunner(fixtureMgr)
                    .setVal(new ValE(val).setMsg(typePrefix))
                    .setTypePrefix(typePrefix)
                    .setTestReport(testReport)
                    .setTestScript(testScript);
            runner.run(actionAssertComponent, assertComponent);
        }
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
                if (testComponent.hasAction()) {
                    String typePrefix = "test.action";
                    TestReport.TestActionComponent actionReport = testReportComponent.addAction();
                    for (TestScript.TestActionComponent testActionComponent : testComponent.getAction()) {
                        if (testActionComponent.hasOperation() && testActionComponent.hasAssert()) {
                            Reporter.reportError(tVal, actionReport.getOperation(), "test.action", testName, "action has both operation and assertion");
                            return;
                        }
                        doAction(typePrefix, testActionComponent.hasOperation(), testActionComponent.getOperation(), actionReport.getOperation(), testActionComponent.hasAssert(), testActionComponent.getAssert(), actionReport.getAssert());
                    }
                }
            }

        }

    }

    private void doTearDown() {
        if (testScript.hasTeardown()) {
            ValE fVal = new ValE(engineVal).setMsg("Teardown");
            TestReport.TestReportTeardownComponent teardownReportComponent = testReport.getTeardown();
            TestScript.TestScriptTeardownComponent testScriptTeardownComponent = testScript.getTeardown();
            if (testScriptTeardownComponent.hasAction()) {
                TestReport.TeardownActionComponent actionReport = teardownReportComponent.addAction();
                for (TestScript.TeardownActionComponent teardownActionComponent : testScriptTeardownComponent.getAction()) {
                    if (teardownActionComponent.hasOperation()) {
                        TestScript.SetupActionOperationComponent setupActionOperationComponent = teardownActionComponent.getOperation();
                        doAction("teardown", true, setupActionOperationComponent, actionReport.getOperation(), false, null, null);
                    }
                }
            }
        }
    }

    private void doPostProcessing() {

    }

    private TestScript loadTestScript() {
        Objects.requireNonNull(testDef);
        File location = new File(testDef, "TestScript.xml");
        if (!location.exists() || !location.canRead() ) {
            location = new File(testDef, "TestScript.json");
            if (!location.exists() || !location.canRead() ) {
                throw new RuntimeException("Cannot load TestScript (.xml or .json) from " + testDef);
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

    void propagateStatus(TestReport testReport) {
        testReport.setResult(TestReport.TestReportResult.PASS);
        if (testReport.hasSetup()) {
            TestReport.TestReportSetupComponent setupComponent = testReport.getSetup();
            for (TestReport.SetupActionComponent setupActionComponent : setupComponent.getAction()) {
                if (setupActionComponent.hasOperation()) {
                    reportOnOperation(testReport, setupActionComponent.getOperation());
                }
                if (setupActionComponent.hasAssert()) {
                    reportOnAssertion(testReport, setupActionComponent.getAssert());
                }
            }
        }
        if (testReport.hasTest()) {
            for (TestReport.TestReportTestComponent testComponent : testReport.getTest()) {
                if (testComponent.hasAction()) {
                    for (TestReport.TestActionComponent testActionComponent : testComponent.getAction()) {
                        if (testActionComponent.hasOperation()) {
                            reportOnOperation(testReport, testActionComponent.getOperation());
                        }
                        if (testActionComponent.hasAssert()) {
                            reportOnAssertion(testReport, testActionComponent.getAssert());
                        }
                    }
                }
            }
        }
        if (testReport.hasTeardown()) {
            TestReport.TestReportTeardownComponent teardownActionComponent = testReport.getTeardown();
            for (TestReport.TeardownActionComponent teardownActionComponent1 : teardownActionComponent.getAction()) {
                if (teardownActionComponent1.hasOperation()) {
                    reportOnOperation(testReport, teardownActionComponent1.getOperation());
                }
            }
        }
        testReport.setStatus(TestReport.TestReportStatus.COMPLETED);
    }

    private void reportOnOperation(TestReport testReport, TestReport.SetupActionOperationComponent setupActionOperationComponent) {
        if (setupActionOperationComponent.hasResult()) {
            TestReport.TestReportActionResult testReportActionResult = setupActionOperationComponent.getResult();
            if (testReportActionResult == TestReport.TestReportActionResult.ERROR
                    || testReportActionResult == TestReport.TestReportActionResult.FAIL) {
                setupActionOperationComponent.setResult(testReportActionResult);
                testReport.setResult(TestReport.TestReportResult.FAIL);
            }
        }
    }

    private void reportOnAssertion(TestReport testReport, TestReport.SetupActionAssertComponent setupActionAssertionComponent) {
        if (setupActionAssertionComponent.hasResult()) {
            TestReport.TestReportActionResult testReportActionResult = setupActionAssertionComponent.getResult();
            if (testReportActionResult == TestReport.TestReportActionResult.ERROR
                    || testReportActionResult == TestReport.TestReportActionResult.FAIL) {
                setupActionAssertionComponent.setResult(testReportActionResult);
                testReport.setResult(TestReport.TestReportResult.FAIL);
            }
        }
    }

    TestEngine setVal(Val val) {
        this.val = val;
        return this;
    }

    public TestReport getTestReport() {
        return testReport;
    }

    String getTestReportAsJson() {
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
}
