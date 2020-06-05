package gov.nist.asbestos.testEngine.engine;

import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

import javax.swing.*;
import java.util.List;
import java.util.Objects;

// Action addressing within TestScript/TestReport.
public class ActionReference {
    private ActionSection section;  // which section - SETUP, TEARDOWN, TEST
    private ActionType type;     // assertion or operation or either (don't care -  used for queries)
    private String id;           // used for static fixtures
    private int testIndex;   // if test
    private int actionIndex;  // within section
    private boolean anAssertion;  // otherwise it is an operation

    @Override
    public String toString()  {
        StringBuilder buf = new StringBuilder();

        switch (section) {
            case SETUP:
                buf.append("SETUP[");
                buf.append(actionIndex);
                buf.append("]");
                break;
            case TEST:
                buf.append("TEST[");
                buf.append(testIndex);
                buf.append("]");
                buf.append(".ACTION[");
                buf.append(actionIndex);
                buf.append("]");
                break;
            case TEARDOWN:
                buf.append("TEARDOWN[");
                buf.append(actionIndex);
                buf.append("]");
                break;
            default:
                buf.append("UNKNOWN");
        }

        return buf.toString();
    }

    public ActionReference(TestScript testScript, TestScript.TestScriptFixtureComponent comp) {
        id = comp.getId();
        section = ActionSection.STATIC;
        type = ActionType.STATIC;
    }

    public ActionReference(TestScript testScript, TestScript.SetupActionComponent comp) {
        if (comp.hasOperation())
            build(testScript, comp.getOperation());
        else
            build(testScript, comp.getAssert());
    }

    public ActionReference(TestScript testScript, TestScript.SetupActionOperationComponent comp) {
        build(testScript, comp);
    }

    public ActionReference(TestScript testScript, TestScript.SetupActionAssertComponent as) {
        build(testScript, as);
    }

    public void build(TestScript testScript, TestScript.SetupActionOperationComponent comp) {
        Objects.requireNonNull(comp);
        TestScript.TestScriptSetupComponent setup = testScript.getSetup();
        int actionI = 0;
        for (TestScript.SetupActionComponent actionComponent : setup.getAction()) {
            if (comp.equals(actionComponent.getOperation())) {
                section = ActionSection.SETUP;
                type = ActionType.EITHER;
                actionIndex = actionI;
                anAssertion = false;
                return;
            }
            actionI++;
        }
        throw new Error("Setup component not found in Script");
    }
    public void build(TestScript testScript, TestScript.SetupActionAssertComponent comp) {
        Objects.requireNonNull(comp);
        TestScript.TestScriptSetupComponent setup = testScript.getSetup();
        int actionI = 0;
        for (TestScript.SetupActionComponent actionComponent : setup.getAction()) {
            if (comp.equals(actionComponent.getAssert())) {
                section = ActionSection.SETUP;
                type = ActionType.EITHER;
                actionIndex = actionI;
                anAssertion = true;
                return;
            }
            actionI++;
        }
        int testI = 0;
        for (TestScript.TestScriptTestComponent test : testScript.getTest()) {
            int actionI2 = 0;
            for (TestScript.TestActionComponent actionComponent : test.getAction()) {
                TestScript.SetupActionAssertComponent anAssert = actionComponent.getAssert();
                if (anAssert != null && anAssert.equals(comp)) {
                    section = ActionSection.TEST;
                    type = ActionType.EITHER;
                    actionIndex = actionI2;
                    testIndex = testI;
                    anAssertion = true;
                    return;
                }
                actionI2++;
            }
            testI++;
        }
        throw new Error("Setup component not found in Script");
    }

    public ActionReference(TestScript testScript, TestScript.TestActionComponent comp) {
        Objects.requireNonNull(comp);
        int testI = 0;
        for (TestScript.TestScriptTestComponent testComponent : testScript.getTest()) {
            int actionI = 0;
            for (TestScript.TestActionComponent actionComponent : testComponent.getAction()) {
                if (comp.equals(actionComponent)) {
                    section = ActionSection.TEST;
                    type = ActionType.EITHER;
                    actionIndex = actionI;
                    testIndex = testI;
                    anAssertion = actionComponent.hasAssert();
                    return;
                }
                actionI++;
            }

            testI++;
        }
        throw new Error("Test component not found in Script");
    }

    public ActionReference(TestScript testScript, TestScript.TeardownActionComponent comp) {
        Objects.requireNonNull(comp);
        TestScript.TestScriptTeardownComponent teardown = testScript.getTeardown();
        int actionI = 0;
        for (TestScript.TeardownActionComponent teardownActionComponent : teardown.getAction()) {
            if (comp.equals(teardownActionComponent)) {
                section = ActionSection.TEARDOWN;
                type = ActionType.EITHER;
                actionIndex = actionI;
                anAssertion = false;
                return;
            }
            actionI++;
        }
        throw new Error("Teardown component not found in Script");
    }

    public ActionReference(TestReport testReport, TestReport.SetupActionComponent comp) {
        Objects.requireNonNull(comp);
        TestReport.TestReportSetupComponent setup = testReport.getSetup();
        int actionI = 0;
        for (TestReport.SetupActionComponent actionComponent : setup.getAction()) {
            if (comp.equals(actionComponent)) {
                section = ActionSection.SETUP;
                type = ActionType.EITHER;
                actionIndex = actionI;
                anAssertion = comp.hasAssert();
                return;
            }
            actionI++;
        }
        throw new Error("Setup component not found in Report");
    }

    public ActionReference(TestReport testReport, TestReport.TestActionComponent comp) {
        Objects.requireNonNull(comp);
        int testI = 0;
        for (TestReport.TestReportTestComponent testComponent : testReport.getTest()) {
            int actionI = 0;
            for (TestReport.TestActionComponent actionComponent : testComponent.getAction()) {
                if (comp.equals(actionComponent)) {
                    section = ActionSection.TEST;
                    type = ActionType.EITHER;
                    actionIndex = actionI;
                    testIndex = testI;
                    anAssertion = actionComponent.hasAssert();
                    return;
                }
                actionI++;
            }

            testI++;
        }
        throw new Error("Test component not found in Report");
    }

    public ActionReference(TestReport testReport, TestReport.TeardownActionComponent comp) {
        Objects.requireNonNull(comp);
        TestReport.TestReportTeardownComponent teardown = testReport.getTeardown();
        int actionI = 0;
        for (TestReport.TeardownActionComponent teardownActionComponent : teardown.getAction()) {
            if (comp.equals(teardownActionComponent)) {
                section = ActionSection.TEARDOWN;
                type = ActionType.EITHER;
                actionIndex = actionI;
                anAssertion = false;
                return;
            }
            actionI++;
        }
        throw new Error("Teardown component not found in Report");
    }

    public ActionSection getSection() {
        return section;
    }

    public ActionType getType() {
        return type;
    }

    public int getTestIndex() {
        return testIndex;
    }

    public int getActionIndex() {
        return actionIndex;
    }

    public boolean isAnAssertion() {
        return anAssertion;
    }
}
