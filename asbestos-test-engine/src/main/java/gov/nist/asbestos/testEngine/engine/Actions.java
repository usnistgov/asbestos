package gov.nist.asbestos.testEngine.engine;

import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

public class Actions {

    static public TestScript.SetupActionComponent getSetupComponent(TestScript testScript, ActionReference action) {
        if (testScript.hasSetup()) {
            if (action.getActionIndex() < testScript.getSetup().getAction().size()) {
                return testScript.getSetup().getAction().get(action.getActionIndex());
            }
        }
        return null;
    }

    static public TestScript.TestActionComponent getTestComponent(TestScript testScript, ActionReference action) {
        if (testScript.hasTest()) {
            if (action.getTestIndex() < testScript.getTest().size()) {
                TestScript.TestScriptTestComponent test = testScript.getTest().get(action.getTestIndex());
                if (action.getActionIndex() < test.getAction().size()) {
                    return test.getAction().get(action.getActionIndex());
                }
            }
        }
        return null;
    }

    static public TestScript.TeardownActionComponent getTeardownComponent(TestScript testScript, ActionReference action) {
        if (testScript.hasTeardown()) {
            if (action.getActionIndex() < testScript.getTeardown().getAction().size() ) {
                return testScript.getTeardown().getAction().get(action.getActionIndex());
            }
        }
        return null;
    }


    static public TestReport.SetupActionComponent getSetupComponent(TestReport testReport, ActionReference action) {
        if (testReport.hasSetup()) {
            if (action.getActionIndex() < testReport.getSetup().getAction().size()) {
                return testReport.getSetup().getAction().get(action.getActionIndex());
            }
        }
        return null;
    }

    static public TestReport.TestActionComponent getTestComponent(TestReport testReport, ActionReference action) {
        if (testReport.hasTest()) {
            if (action.getTestIndex() < testReport.getTest().size()) {
                TestReport.TestReportTestComponent test = testReport.getTest().get(action.getTestIndex());
                if (action.getActionIndex() < test.getAction().size()) {
                    return test.getAction().get(action.getActionIndex());
                }
            }
        }
        return null;
    }
    static public TestReport.TeardownActionComponent getTeardownComponent(TestReport testReport, ActionReference action) {
        if (testReport.hasTeardown()) {
            if (action.getActionIndex() < testReport.getTeardown().getAction().size()) {
                return testReport.getTeardown().getAction().get(action.getActionIndex());
            }
        }
        return null;
    }
}
