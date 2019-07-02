package gov.nist.asbestos.testEngine;

import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

import java.net.URI;
import java.net.URISyntaxException;
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
//            String system = typeCoding.getSystem();
//            if (!"http://terminology.hl7.org/CodeSystem/testscript-operation-codes".equals(system)) {
//                val.add(new ValE("Setup.Action " + id + " do not understand code.system " + system).asError());
//                return;
//            }

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
            if (lastOp != null) {
                fixture = fixtures.get(lastOp);
                if (fixture == null) {
                    reportError(val, assertReport, "Setup.Assert " + id + " last operation - " + lastOp + " - does not name a fixture");
                    return;
                }
            }
            TestScript.SetupActionAssertComponent as = action.getAssert();
            boolean warningOnly;
            if (as.hasWarningOnly())
                warningOnly = as.getWarningOnly();
            else {
                reportError(val, assertReport, "Setup.Assert " + id + " warningOnly is required but missing");
                return;
            }
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
            Base minimumToCompare = null;
            if (as.hasValue()) {
                valueToCompare = as.getValue();
            } else if (as.hasCompareToSourceId()) {
                FixtureComponent source = fixtures.get(as.getCompareToSourceId());
                if (source == null) {
                    val.add(new ValE("Setup.Assert " + id + " has compareToSourceId " + as.getCompareToSourceId() + " which is undefined").asError());
                    return;
                }
            }
            if (as.hasMinimumId()) {
                FixtureComponent comp = fixtures.get(as.getMinimumId());
                if (comp == null) {
                    val.add(new ValE("Setup.Assert " + id + " has minimumId " + as.getMinimumId() + " which is undefined").asError());
                    return;
                }
                minimumToCompare = comp.hasResponse() ? comp.getResponseResource() : null;
            }
            String operator = as.hasOperator() ? as.getOperator().toCode() : "equals";
            if (valueToCompare != null) {
                if (as.hasContentType()) {
                    String expected = as.getContentType();
                    String found = fixture.getHttpBase().getResponseContentType();
                    if (!compare(val, assertReport, found, expected, operator, warningOnly))
                        return;
                }
                if (as.hasHeaderField()) {
                    String headerFieldName = as.getHeaderField();
                    String expected = valueToCompare;
                    if (useTestResponse) {
                        String found = fixture.getHttpBase().getResponseHeaders().get(headerFieldName).getValue();
                        if (!compare(val, assertReport, found, expected, operator, warningOnly))
                            return;
                    } else {
                        String found = fixture.getHttpBase().getRequestHeaders().get(headerFieldName).getValue();
                        if (!compare(val, assertReport, found, expected, operator, warningOnly))
                            return;
                    }
                }
                if (as.hasRequestMethod()) {
                    String expected = as.getRequestMethod().toCode();
                    String found = fixture.getHttpBase().getVerb();
                    if (!compare(val, assertReport, found, expected, operator, warningOnly))
                        return;
                }
                if (as.hasResource()) {
                    // expected resource type in response body (GET)
                    String expected = valueToCompare;
                    String found = fixture.getResponseType();
                    if (!compare(val, assertReport, found, expected, operator, warningOnly))
                        return;
                }
                if (as.hasResponse()) {
                    int codeFound = fixture.getHttpBase().getStatus();
                    String found = responseCodeAsString(codeFound);
                    String expected = valueToCompare;
                    if (!compare(val, assertReport, found, expected, operator, warningOnly))
                        return;
                }
                if (as.hasResponseCode()) {
                    int codeFound = fixture.getHttpBase().getStatus();
                    String found = String.valueOf(codeFound);
                    String expected = valueToCompare;
                    if (!compare(val, assertReport, found, expected, operator, warningOnly))
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
            if (as.hasValidateProfileId()) {
                reportError(val, assertReport, "validateProfileId not supported");
                return;
            }
            if (as.hasRequestURL()) {
                if (lastOp == null) {
                    reportError(val, assertReport, "Setup.Assert " + id + " has requestURL tested but no last operation recorded");
                    return;
                }
                String expected = null;
                try {
                    expected = new URI(as.getRequestURL()).getPath();
                } catch (URISyntaxException e) {
                    reportError(val, assertReport, "Setup.Assert " + id + " requestURL (" + as.getRequestURL() + ") cannot be parsed");
                    return;
                }
                String found = fixtures.get(lastOp).getHttpBase().getUri().getPath();
                if (!compare(val, assertReport, found, expected, operator, warningOnly))
                    return;
            }

        }
    }

    private String responseCodeAsString(int code) {
        switch (code) {
            case 200: return "okay";
            case 201: return "created";
            case 204: return "noContent";
            case 304: return "notModified";
            case 400: return "bad";
            case 403: return "forbidden";
            case 404: return "notFound";
            case 405: return "methodNotAllowed";
            case 409: return "conflict";
            case 410: return "gone";
            case 412: return "preconditionFailed";
            case 422: return "unprocessable";
            default: return "CODE_NOT_UNDERSTOOD";
        }
    }

    private boolean compare(ValE val, TestReport.SetupActionAssertComponent assertReport, String found, String expected, String operator, boolean warningOnly) {
        if (found == null)
            return reportFail(val, assertReport, "Operator " + operator + " - no value found to compare with " + expected, warningOnly);
        if (operator.equals("equals"))
            return report(found.equals(expected), val, assertReport, "Operator " + operator, warningOnly);
        if (operator.equals("notEquals"))
            return report(!found.equals(expected), val, assertReport, "Operator " + operator, warningOnly);
        if (operator.equals("in")) {
            String[] values = expected.split(",");
            for (String value : values) {
                if (value.equals(found))
                    return report(true, val, assertReport, "Operator " + operator, warningOnly);
            }
            return report(false, val, assertReport, "Operator " + operator, warningOnly);
        }
        if (operator.equals("notIn")) {
            String[] values = expected.split(",");
            for (String value : values) {
                if (value.equals(found))
                    return report(false, val, assertReport, "Operator " + operator, warningOnly);
            }
            return report(true, val, assertReport, "Operator " + operator, warningOnly);
        }
        if (operator.equals("greaterThan")) {
            int iExpected = Integer.parseInt(expected);
            int iFound = Integer.parseInt(found);
            return report(iFound > iExpected, val, assertReport, "Operator " + operator, warningOnly);
        }
        if (operator.equals("lessThan")) {
            int iExpected = Integer.parseInt(expected);
            int iFound = Integer.parseInt(found);
            return report(iFound < iExpected, val, assertReport, "Operator " + operator, warningOnly);
        }
        if (operator.equals("empty")) {
            return report("".equals(found), val, assertReport, "Operator " + operator, warningOnly);
        }
        if (operator.equals("notEmpty")) {
            return report(!"".equals(found), val, assertReport, "Operator " + operator, warningOnly);
        }
        if (operator.equals("contains")) {
            return report(found.contains(expected), val, assertReport, "Operator " + operator, warningOnly);
        }
        if (operator.equals("notContains")) {
            return report(!found.contains(expected), val, assertReport, "Operator " + operator, warningOnly);
        }
        return report(false, val, assertReport, "Do not understand operator " + operator, warningOnly);
    }

    private void reportError(ValE val, TestReport.SetupActionAssertComponent assertReport, String msg) {
        val.add(new ValE(msg).asError());
        assertReport.setResult(TestReport.TestReportActionResult.ERROR);
        assertReport.setMessage(msg);
    }

    private boolean report(boolean ok, ValE val, TestReport.SetupActionAssertComponent assertReport, String msg, boolean warningOnly) {
        if (ok)
            reportPass(val, assertReport, msg);
        else
            reportFail(val, assertReport, msg, warningOnly);
        return ok;
    }

    private boolean reportFail(ValE val, TestReport.SetupActionAssertComponent assertReport, String msg, boolean warningOnly) {
        val.add(new ValE(msg).asError());
        assertReport.setResult(warningOnly? TestReport.TestReportActionResult.WARNING : TestReport.TestReportActionResult.FAIL);
        assertReport.setMessage(msg);
        return false;
    }

    private boolean reportPass(ValE val, TestReport.SetupActionAssertComponent assertReport, String msg) {
        val.add(new ValE(msg));
        assertReport.setResult(TestReport.TestReportActionResult.PASS);
        assertReport.setMessage(msg);
        return true;
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
