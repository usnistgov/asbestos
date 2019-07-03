package gov.nist.asbestos.testEngine;

import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class SetupAction {
    private Map<String, FixtureComponent> fixtures;
    private TestScript.SetupActionComponent action;
    private String lastOp = null;  // name of last FixtureComponent
    private ValE val;
    private TestReport testReport = null;
    private FixtureComponent result = null;

    SetupAction(Map<String, FixtureComponent> fixtures, TestScript.SetupActionComponent action) {
        this.fixtures = fixtures;
        this.action = action;
    }

    void run() {
        Objects.requireNonNull(val);
        Objects.requireNonNull(testReport);
        String id = action.hasId() ? action.getId() : "No ID";

        TestReport.TestReportSetupComponent setupReport = testReport.getSetup();
        TestReport.SetupActionComponent actionReport = setupReport.addAction();

        if (action.hasOperation()) {
            TestReport.SetupActionOperationComponent operationReport = actionReport.getOperation();
            operationReport.setResult(TestReport.TestReportActionResult.PASS);  // may be overwritten

            String type = "setup.action.operation";

            if (action.hasAssert()) {
                reportError(val, operationReport, type, id,"has both an Operation and an Assertion");
                return;
            }
            TestScript.SetupActionOperationComponent op = action.getOperation();
            int elementCount = 0;
            if (op.hasSourceId()) elementCount++;
            if (op.hasTargetId()) elementCount++;
            if (op.hasParams()) elementCount++;
            if (op.hasUrl()) elementCount++;
            if (elementCount == 0) {
                reportError(val, operationReport, type, id,"has none of sourceId, targetId, params, url - one is required");
                return;
            }
            if (elementCount > 1) {
                reportError(val, operationReport, type, id,"has multiple of sourceId, targetId, params, url - only one is allowed");
                return;
            }
            if (!op.hasType()) {
                reportError(val, operationReport, type, id,"has no type");
                return;
            }
            Coding typeCoding = op.getType();
            String code = typeCoding.getCode();

            if ("read".equals(code)) {
                FixtureComponent fixture = new SetupActionRead(fixtures, op).setVal(val).run();
                lastOp = fixture.getId();
                return;
            } else {
                reportError(val, operationReport, type, id,"do not understand code.code of " + code);
                return;
            }
        }
        if (action.hasAssert()) {
            TestReport.SetupActionAssertComponent assertReport = actionReport.getAssert();
            assertReport.setResult(TestReport.TestReportActionResult.PASS);  // may be overwritten

            String type = "setup.action.assert";

            FixtureComponent fixture = null;
            if (lastOp != null) {
                fixture = fixtures.get(lastOp);
                if (fixture == null) {
                    reportError(val, assertReport, type, id,"last operation - " + lastOp + " - does not name a fixture");
                    return;
                }
            }
            TestScript.SetupActionAssertComponent as = action.getAssert();

            boolean warningOnly;
            if (as.hasWarningOnly())
                warningOnly = as.getWarningOnly();
            else {
                reportError(val, assertReport, type, id, "warningOnly is required but missing");
                return;
            }
            boolean directionIsResponse =  !as.hasDirection() || as.getDirection().toCode().equals("response");

            if (as.hasCompareToSourcePath() && as.hasCompareToSourceExpression()) {
                reportError(val, assertReport, type, id, "has both compareToSourcePath and compareToSourceExpression");
                return;
            }

            // TODO compareToSourcePath
            String valueToCompare = null;
            Base minimumToCompare = null;
            FixtureComponent source = null;
            if (as.hasValue()) {
                valueToCompare = as.getValue();
            } else if (as.hasCompareToSourceId()) {
                source = fixtures.get(as.getCompareToSourceId());
                if (source == null) {
                    reportError(val, assertReport, type, id, "has compareToSourceId " + as.getCompareToSourceId() + " which is undefined");
                    return;
                }
            }
            if (as.hasCompareToSourceExpression() && source != null) {
                String expression = as.getCompareToSourceExpression();
                BaseResource sourceResource = directionIsResponse ? source.getResponseResource() : source.getRequestResource();
                if (sourceResource == null) {
                    reportError(val, assertReport, type, id,"Fixture referenced " +  source.getId()  + "has no " + (directionIsResponse ? "response" : "request"));
                    return;
                }
                List<Base> results = FhirPathEngineBuilder.build().evaluate(sourceResource, expression);
                if (results.isEmpty()) {
                    reportFail(val, assertReport, type, id, "Assertion failed", warningOnly);
                    return;
                }
            }
            if (as.hasSourceId())
                fixture = fixtures.get(as.getSourceId());

            if (directionIsResponse && fixture != null && !fixture.hasResponse()) {
                reportError(val, assertReport, type, id,"referenced fixture " + fixture.getId() + " has no response");
                return;
            }
            if (!directionIsResponse && fixture != null && !fixture.hasRequest()) {
                reportError(val, assertReport, type, id, "referenced fixture " + fixture.getId() + " has no request");
                return;
            }
            if (as.hasMinimumId()) {
                FixtureComponent comp = fixtures.get(as.getMinimumId());
                if (comp == null) {
                    reportError(val, assertReport, type, id, "has minimumId " + as.getMinimumId() + " which is undefined");
                    return;
                }
                minimumToCompare = comp.hasResponse() ? comp.getResponseResource() : null;
            }
            String operator = as.hasOperator() ? as.getOperator().toCode() : "equals";
            if (valueToCompare != null) {
                if (fixture == null) {
                    reportError(val, assertReport, type, id, "no sourceId to compare against");
                    return;
                }
                if (as.hasContentType()) {
                    String expected = as.getContentType();
                    String found = fixture.getHttpBase().getResponseContentType();
                    if (!compare(val, assertReport, found, expected, operator, warningOnly, type, id))
                        return;
                }
                if (as.hasHeaderField()) {
                    String headerFieldName = as.getHeaderField();
                    String expected = valueToCompare;
                    if (directionIsResponse) {
                        String found = fixture.getHttpBase().getResponseHeaders().get(headerFieldName).getValue();
                        if (!compare(val, assertReport, found, expected, operator, warningOnly, type, id))
                            return;
                    } else {
                        String found = fixture.getHttpBase().getRequestHeaders().get(headerFieldName).getValue();
                        if (!compare(val, assertReport, found, expected, operator, warningOnly, type, id))
                            return;
                    }
                }
                if (as.hasRequestMethod()) {
                    String expected = as.getRequestMethod().toCode();
                    String found = fixture.getHttpBase().getVerb();
                    if (!compare(val, assertReport, found, expected, operator, warningOnly, type, id))
                        return;
                }
                if (as.hasResource()) {
                    // expected resource type in response body (GET)
                    String expected = valueToCompare;
                    String found = fixture.getResponseType();
                    if (!compare(val, assertReport, found, expected, operator, warningOnly, type, id))
                        return;
                }
                if (as.hasResponse()) {
                    int codeFound = fixture.getHttpBase().getStatus();
                    String found = responseCodeAsString(codeFound);
                    String expected = valueToCompare;
                    if (!compare(val, assertReport, found, expected, operator, warningOnly, type, id))
                        return;
                }
                if (as.hasResponseCode()) {
                    int codeFound = fixture.getHttpBase().getStatus();
                    String found = String.valueOf(codeFound);
                    String expected = valueToCompare;
                    if (!compare(val, assertReport, found, expected, operator, warningOnly, type, id))
                        return;
                }
            }
            if (as.hasExpression()) {
                if (fixture == null) {
                    reportError(val, assertReport, type, id, "no sourceId to compare against");
                    return;
                }
                String expression = as.getExpression();
                BaseResource sourceResource = directionIsResponse ? fixture.getResponseResource() : fixture.getRequestResource();
                if (sourceResource == null) {
                    reportError(val, assertReport, type, id,"Fixture referenced " + fixture.getId()  + " has no " + (directionIsResponse ? "response" : "request"));
                    return;
                }
                List<Base> results = FhirPathEngineBuilder.build().evaluate(sourceResource, expression);
                if (results.isEmpty()) {
                    reportFail(val, assertReport, type, id, "Assertion failed", warningOnly);
                    return;
                }

            }
            if (as.hasMinimumId()) {
                reportError(val, assertReport, type, id, "minumumId not supported");
                return;
            }
            if (as.hasNavigationLinks()) {
                reportError(val, assertReport, type, id, "navigationLinks not supported");
                return;
            }
            if (as.hasValidateProfileId()) {
                reportError(val, assertReport, type, id, "validateProfileId not supported");
                return;
            }
            if (as.hasRequestURL()) {
                if (lastOp == null) {
                    reportError(val, assertReport, type, id, " has requestURL tested but no last operation recorded");
                    return;
                }
                String expected = null;
                try {
                    expected = new URI(as.getRequestURL()).getPath();
                } catch (URISyntaxException e) {
                    reportError(val, assertReport, type, id, " requestURL (" + as.getRequestURL() + ") cannot be parsed");
                    return;
                }
                String found = fixtures.get(lastOp).getHttpBase().getUri().getPath();
                if (!compare(val, assertReport, found, expected, operator, warningOnly, type, id))
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

    private boolean compare(ValE val, TestReport.SetupActionAssertComponent assertReport, String found, String expected, String operator, boolean warningOnly, String type, String id) {
        if (found == null)
            return reportFail(val, assertReport, type, operator,"Operator " + operator + " - no value found to compare with " + expected, warningOnly);
        if (operator.equals("equals"))
            return report(found.equals(expected), val, assertReport, type, operator,"Operator " + operator, warningOnly);
        if (operator.equals("notEquals"))
            return report(!found.equals(expected), val, assertReport, type, operator,"Operator " + operator, warningOnly);
        if (operator.equals("in")) {
            String[] values = expected.split(",");
            for (String value : values) {
                if (value.equals(found))
                    return report(true, val, assertReport, type, operator,"Operator " + operator, warningOnly);
            }
            return report(false, val, assertReport, type, operator,"Operator " + operator, warningOnly);
        }
        if (operator.equals("notIn")) {
            String[] values = expected.split(",");
            for (String value : values) {
                if (value.equals(found))
                    return report(false, val, assertReport, type, operator,"Operator " + operator, warningOnly);
            }
            return report(true, val, assertReport, type, operator,"Operator " + operator, warningOnly);
        }
        if (operator.equals("greaterThan")) {
            int iExpected = Integer.parseInt(expected);
            int iFound = Integer.parseInt(found);
            return report(iFound > iExpected, val, assertReport, type, operator,"Operator " + operator, warningOnly);
        }
        if (operator.equals("lessThan")) {
            int iExpected = Integer.parseInt(expected);
            int iFound = Integer.parseInt(found);
            return report(iFound < iExpected, val, assertReport, type, operator,"Operator " + operator, warningOnly);
        }
        if (operator.equals("empty")) {
            return report("".equals(found), val, assertReport, type, operator,"Operator " + operator, warningOnly);
        }
        if (operator.equals("notEmpty")) {
            return report(!"".equals(found), val, assertReport, type, operator,"Operator " + operator, warningOnly);
        }
        if (operator.equals("contains")) {
            return report(found.contains(expected), val, assertReport, type, operator,"Operator " + operator, warningOnly);
        }
        if (operator.equals("notContains")) {
            return report(!found.contains(expected), val, assertReport, type, operator, "Operator " + operator, warningOnly);
        }
        return report(false, val, assertReport, type, id, "Do not understand operator " + operator, warningOnly);
    }

    private String formatMsg(String type, String id, String msg) {
        return type + " : " + id + " : " + msg;
    }

    private void reportError(ValE val, TestReport.SetupActionOperationComponent opReport, String type, String id, String msg) {
        String theMsg = formatMsg(type, id, msg);
        val.add(new ValE(theMsg).asError());
        opReport.setResult(TestReport.TestReportActionResult.ERROR);
        opReport.setMessage(theMsg);
    }

    private void reportError(ValE val, TestReport.SetupActionAssertComponent assertReport, String type, String id, String msg) {
        String theMsg = formatMsg(type, id, msg);
        val.add(new ValE(theMsg).asError());
        assertReport.setResult(TestReport.TestReportActionResult.ERROR);
        assertReport.setMessage(theMsg);
    }

    private boolean report(boolean ok, ValE val, TestReport.SetupActionAssertComponent assertReport, String type, String id, String msg, boolean warningOnly) {
        if (ok)
            reportPass(val, assertReport, type, id, msg);
        else
            reportFail(val, assertReport, type, id, msg, warningOnly);
        return ok;
    }

    private boolean reportFail(ValE val, TestReport.SetupActionAssertComponent assertReport, String type, String id, String msg, boolean warningOnly) {
        String theMsg = formatMsg(type, id, msg);
        val.add(new ValE(theMsg).asError());
        assertReport.setResult(warningOnly? TestReport.TestReportActionResult.WARNING : TestReport.TestReportActionResult.FAIL);
        assertReport.setMessage(theMsg);
        return false;
    }

    private boolean reportPass(ValE val, TestReport.SetupActionAssertComponent assertReport, String type, String id, String msg) {
        String theMsg = formatMsg(type, id, msg);
        val.add(new ValE(theMsg));
        assertReport.setResult(TestReport.TestReportActionResult.PASS);
        assertReport.setMessage(theMsg);
        return true;
    }

    private boolean noFailures(TestReport.SetupActionAssertComponent assertReport) {
        return assertReport.getResult() != TestReport.TestReportActionResult.FAIL;
    }

    SetupAction setVal(ValE val) {
        this.val = val;
        return this;
    }

    public SetupAction setLastOp(String lastOp) {
        this.lastOp = lastOp;
        return this;
    }

    public String getLastOp() {
        return lastOp;
    }

    public TestReport getTestReport() {
        return testReport;
    }

    public FixtureComponent getResult() {
        return result;
    }

    public SetupAction setTestReport(TestReport testReport) {
        this.testReport = testReport;
        return this;
    }
}
