package gov.nist.asbestos.testEngine;

import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

import java.util.List;
import java.util.Map;
import java.util.Objects;

class SetupAction {
    private Map<String, FixtureComponent> fixtures;
    private TestScript.SetupActionComponent action;
    private String lastOp;  // name of last FixtureComponent
    private ValE val;
    private TestReport testReport = new TestReport();
    private FixtureComponent result = null;

    SetupAction(Map<String, FixtureComponent> fixtures, TestScript.SetupActionComponent action) {
        this.fixtures = fixtures;
        this.action = action;
    }

    void run() {
        Objects.requireNonNull(val);
        TestReport.TestReportSetupComponent setupReport = testReport.getSetup();
        String id = action.hasId() ? action.getId() : "No ID";
        if (action.hasOperation() && action.hasAssert()) {
            String msg = "Setup.Action " + id + " has both an Operation and an Assertion";
            val.add(new ValE(msg).asError());
            return;
        }
        TestReport.SetupActionComponent actionReport = setupReport.addAction();

        if (action.hasOperation()) {
            TestReport.SetupActionOperationComponent operationReport = actionReport.getOperation();
            if (action.hasAssert()) {
                String msg = "Setup.Action " + id + " has both an Operation and an Assertion";
                val.add(new ValE(msg).asError());
                actionReport.getAssert()
                        .setMessage(msg)
                        .setResult(TestReport.TestReportActionResult.ERROR);
                return;
            }
            TestScript.SetupActionOperationComponent op = action.getOperation();
            int elementCount = 0;
            if (op.hasSourceId()) elementCount++;
            if (op.hasTargetId()) elementCount++;
            if (op.hasParams()) elementCount++;
            if (op.hasUrl()) elementCount++;
            if (elementCount == 0) {
                val.add(new ValE("Setup.Action " + id + " has none of sourceId, targetId, params, url - one is required").asError());
                return;
            }
            if (elementCount > 1) {
                val.add(new ValE("Setup.Action " + id + " has multiple of sourceId, targetId, params, url - only one is allowed").asError());
                return;
            }
            if (!op.hasType()) {
                val.add(new ValE("Setup.Action " + id + " has no type").asError());
                return;
            }
            Coding typeCoding = op.getType();
            String code = typeCoding.getCode();
            String system = typeCoding.getSystem();
            if (!"http://terminology.hl7.org/CodeSystem/testscript-operation-codes".equals(system)) {
                val.add(new ValE("Setup.Action " + id + " do not understand code.system " + system).asError());
                return;
            }

            if ("read".equals(code)) {
                new SetupActionRead(fixtures, op).setVal(val).run();
                return;
            } else {
                val.add(new ValE("Setup.Action " + id + " do not understand code.code of " + code).asError());
                return;
            }
        }
        if (action.hasAssert()) {
            TestReport.SetupActionAssertComponent assertReport = actionReport.getAssert();
            FixtureComponent fixture = null;
            if (lastOp != null)
                fixture = fixtures.get(lastOp);
            TestScript.SetupActionAssertComponent as = action.getAssert();
            if (as.hasSourceId())
                fixture = fixtures.get(as.getSourceId());
            if (fixture == null) {
                reportError(val, assertReport, "Setup.Assert " + id + " no fixture referenced");
                return;
            }
            boolean useTestResponse =  !as.hasDirection() || as.getDirection().toCode().equals("response");
            if (useTestResponse && !fixture.hasResponse()) {
                reportError(val, assertReport, "Setup.Assert " + id + " - referenced fixture " + fixture.getId() + " has no response");
                return;
            }
            if (!useTestResponse && !fixture.hasRequest()) {
                reportError(val, assertReport, "Setup.Assert " + id + " - referenced fixture " + fixture.getId() + " has no request");
                return;
            }

            if (as.hasCompareToSourceId() && as.hasCompareToSourceExpression()) {
                val.add(new ValE("Setup.Action " + id + " has both compareToSourceId and compareToSourceExpression").asError());
                return;
            }

            // TODO compareToSourcePath
            String valueToCompare = null;
            Base sourceToCompare = null;
            Base minimumToCompare = null;
            if (as.hasValue()) {
                valueToCompare = as.getValue();
            } else if (as.hasCompareToSourceId()) {
                FixtureComponent source = fixtures.get(as.getCompareToSourceId());
                if (source == null) {
                    val.add(new ValE("Setup.Assert " + id + " has compareToSourceId " + as.getCompareToSourceId() + " which is undefined").asError());
                    return;
                }

                sourceToCompare = useTestResponse
                        ? (source.hasResponse() ? source.getResponse().getResource() : null)
                        : (source.hasRequest() ? source.getRequest().getResource() : null);
            }
            if (as.hasMinimumId()) {
                FixtureComponent comp = fixtures.get(as.getMinimumId());
                if (comp == null) {
                    val.add(new ValE("Setup.Assert " + id + " has minimumId " + as.getMinimumId() + " which is undefined").asError());
                    return;
                }
                minimumToCompare = comp.hasResponse() ? comp.getResponse().getResource() : null;
            }
            if (valueToCompare != null) {
                String operator = as.hasOperator() ? as.getOperator().toCode() : "equals";
                if (as.hasContentType()) {
                    String expected = as.getContentType();
                    String found = fixture.getResponse().getHttpBase().getResponseContentType();
                    if (!compare(val, assertReport, found, expected, operator))
                        return;
                }
                if (as.hasHeaderField()) {
                    String headerFieldName = as.getHeaderField();
                    if (useTestResponse) {
                        String expected = valueToCompare;
                        String found = fixture.getResponse().getHttpBase().getResponseHeaders().get(headerFieldName).getValue();
                        if (!compare(val, assertReport, found, expected, operator))
                            return;
                    }
                }
                if (as.hasMinimumId()) {
                    reportError(val, assertReport, "minumumId not supported");
                    return;
                }
                if (as.hasNavigationLinks()) {
                    reportError(val, assertReport, "navigationLinks not supported");
                    return;
                }


            }


        }
    }

    private boolean compare(ValE val, TestReport.SetupActionAssertComponent assertReport, String found, String expected, String operator) {
        if (operator.equals("equals"))
            return report(found.equals(expected), val, assertReport, "Operator " + operator);
        if (operator.equals("notEquals"))
            return report(!found.equals(expected), val, assertReport, "Operator " + operator);
        if (operator.equals("in")) {
            String[] values = expected.split(",");
            for (String value : values) {
                if (value.equals(found))
                    return report(true, val, assertReport, "Operator " + operator);
            }
            return report(false, val, assertReport, "Operator " + operator);
        }
        if (operator.equals("notIn")) {
            String[] values = expected.split(",");
            for (String value : values) {
                if (value.equals(found))
                    return report(false, val, assertReport, "Operator " + operator);
            }
            return report(true, val, assertReport, "Operator " + operator);
        }
        if (operator.equals("greaterThan")) {
            int iExpected = Integer.parseInt(expected);
            int iFound = Integer.parseInt(found);
            return report(iFound > iExpected, val, assertReport, "Operator " + operator);
        }
        if (operator.equals("lessThan")) {
            int iExpected = Integer.parseInt(expected);
            int iFound = Integer.parseInt(found);
            return report(iFound < iExpected, val, assertReport, "Operator " + operator);
        }
        if (operator.equals("empty")) {
            return report("".equals(found), val, assertReport, "Operator " + operator);
        }
        if (operator.equals("notEmpty")) {
            return report(!"".equals(found), val, assertReport, "Operator " + operator);
        }
        if (operator.equals("contains")) {
            return report(found.contains(expected), val, assertReport, "Operator " + operator);
        }
        if (operator.equals("notContains")) {
            return report(!found.contains(expected), val, assertReport, "Operator " + operator);
        }
        return report(false, val, assertReport, "Do not understand operator " + operator);
    }

    private void reportError(ValE val, TestReport.SetupActionAssertComponent assertReport, String msg) {
        val.add(new ValE(msg).asError());
        assertReport.setResult(TestReport.TestReportActionResult.ERROR);
        assertReport.setMessage(msg);
    }

    private boolean report(boolean ok, ValE val, TestReport.SetupActionAssertComponent assertReport, String msg) {
        if (ok)
            reportPass(val, assertReport, msg);
        else
            reportFail(val, assertReport, msg);
        return ok;
    }

    private void reportFail(ValE val, TestReport.SetupActionAssertComponent assertReport, String msg) {
        val.add(new ValE(msg).asError());
        assertReport.setResult(TestReport.TestReportActionResult.FAIL);
        assertReport.setMessage(msg);
    }

    private void reportPass(ValE val, TestReport.SetupActionAssertComponent assertReport, String msg) {
        val.add(new ValE(msg));
        assertReport.setResult(TestReport.TestReportActionResult.PASS);
        assertReport.setMessage(msg);
    }

    private boolean noFailures(TestReport.SetupActionAssertComponent assertReport) {
        return assertReport.getResult() != TestReport.TestReportActionResult.FAIL;
    }

    SetupAction setVal(ValE val) {
        this.val = val;
        return this;
    }

    public void setLastOp(String lastOp) {
        this.lastOp = lastOp;
    }

    public TestReport getTestReport() {
        return testReport;
    }

    public FixtureComponent getResult() {
        return result;
    }
}
