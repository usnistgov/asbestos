package gov.nist.asbestos.testEngine;

import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;

class SetupAction {
    private Map<String, FixtureComponent> fixtures;
    private TestScript.SetupActionComponent action;
    private String lastOp = null;  // name of last FixtureComponent
    private ValE val;
    private TestReport testReport = null;
    private FixtureComponent result = null;
    private FhirClient fhirClient = null;
    private TestScript testScript = null;

    SetupAction(Map<String, FixtureComponent> fixtures, TestScript.SetupActionComponent action) {
        this.fixtures = fixtures;
        this.action = action;
    }

    void run() {
        Objects.requireNonNull(val);
        Objects.requireNonNull(testReport);
        Objects.requireNonNull(testScript);

        TestReport.TestReportSetupComponent setupReport = testReport.getSetup();
        TestReport.SetupActionComponent actionReport = setupReport.addAction();

        if (action.hasOperation()) {
            String label = action.getOperation().hasLabel() ? action.getOperation().getLabel() : "No Label";
            runOperation(label, actionReport);
            return;
        }
        if (action.hasAssert()) {
            String label = action.getAssert().hasLabel() ? action.getAssert().getLabel() : "No Label";
            runAssert(label, actionReport);

        }
    }

    TestReport.SetupActionAssertComponent assertReport;
    String label;
    String type;

    private void runAssert(String theLabel, TestReport.SetupActionComponent actionReport) {
        assertReport = actionReport.getAssert();
        assertReport.setResult(TestReport.TestReportActionResult.PASS);  // may be overwritten

        label = theLabel;
        type = "setup.action.assert";

        FixtureComponent fixture = null;
        if (lastOp != null) {
            fixture = fixtures.get(lastOp);
            if (fixture == null) {
                Reporter.reportError(val, assertReport, type, label,"last operation - " + lastOp + " - does not name a fixture");
                return;
            }
        }
        TestScript.SetupActionAssertComponent as = action.getAssert();

        boolean warningOnly;
        if (as.hasWarningOnly())
            warningOnly = as.getWarningOnly();
        else {
            Reporter.reportError(val, assertReport, type, label, "warningOnly is required but missing");
            return;
        }

        if (as.hasCompareToSourcePath() && as.hasCompareToSourceExpression()) {
            Reporter.reportError(val, assertReport, type, label, "has both compareToSourcePath and compareToSourceExpression");
            return;
        }

        // TODO compareToSourcePath
        String valueToCompare = null;
        Base minimumToCompare = null;
        FixtureComponent source = null;
        if (as.hasValue()) {
            valueToCompare = as.getValue();
            valueToCompare = new VariableMgr(testScript, fixtures, opReport).updateReference(valueToCompare);
        } else if (as.hasCompareToSourceId()) {
            source = fixtures.get(as.getCompareToSourceId());
            if (source == null) {
                Reporter.reportError(val, assertReport, type, label, "has compareToSourceId " + as.getCompareToSourceId() + " which is undefined");
                return;
            }
        }
        if (as.hasCompareToSourceExpression() && source != null) {
            String expression = as.getCompareToSourceExpression();
            BaseResource sourceResource = source.getResourceResource();
            if (sourceResource == null) {
                Reporter.reportError(val, assertReport, type, label,"Fixture referenced in compareToSourceExpression" +  source.getId()  + "has no resource");
                return;
            }
            if (!FhirPathEngineBuilder.evalForBoolean(sourceResource, expression)) {
                Reporter.reportFail(val, assertReport, type, label, "Assertion failed", warningOnly);
                return;
            }
        }
        if (as.hasSourceId())
            fixture = fixtures.get(as.getSourceId());

        if (as.hasMinimumId()) {
            FixtureComponent comp = fixtures.get(as.getMinimumId());
            if (comp == null) {
                Reporter.reportError(val, assertReport, type, label, "has minimumId " + as.getMinimumId() + " which is undefined");
                return;
            }
            minimumToCompare = comp.hasResource() ? comp.getResourceResource() : null;
        }
        String operator = as.hasOperator() ? as.getOperator().toCode() : "equals";

        if (as.hasHeaderField()) {
            if (!as.hasValue()) {
                Reporter.reportError(val, assertReport, type, label, "has headerField but no value for comparison is specified");
                return;
            }
            if (hasNoFixtureResource(fixture, "cannot reference headerField - no fixture "))
                return;

            assert fixture != null; // guaranteed by hasFixtureResource
            if (!fixture.hasHttpBase()) {
                Reporter.reportError(val, assertReport, type, label, "cannot reference headerField - fixture includes no HTTP operation");
                return;
            }
            String headerFieldName = as.getHeaderField();
            if (as.hasDirection() && as.getDirection() == TestScript.AssertionDirectionType.REQUEST) {
                String found = fixture.getHttpBase().getRequestHeaders().get(headerFieldName).getValue();
                if (!compare(val, assertReport, found, valueToCompare, operator, warningOnly, type, label))
                    return;
            } else {
                String found = fixture.getHttpBase().getResponseHeaders().get(headerFieldName).getValue();
                if (!compare(val, assertReport, found, valueToCompare, operator, warningOnly, type, label))
                    return;
            }
        }


        if (valueToCompare != null) {
            if (as.hasContentType()) {
                if (hasNoFixtureResource(fixture, "cannot reference contentType - no fixture "))
                    return;
                assert fixture != null;    // guaranteed by hasFixtureResource
                String expected = as.getContentType();
                String found = fixture.getHttpBase().getResponseContentType();
                if (!compare(val, assertReport, found, expected, operator, warningOnly, type, label))
                    return;
            }
            if (as.hasRequestMethod()) {
                if (hasNoFixtureResource(fixture, "cannot reference requestMethod - no fixture "))
                    return;
                assert fixture != null;    // guaranteed by hasFixtureResource
                String expected = as.getRequestMethod().toCode();
                String found = fixture.getHttpBase().getVerb();
                if (!compare(val, assertReport, found, expected, operator, warningOnly, type, label))
                    return;
            }
            if (as.hasResource()) {
                if (hasNoFixtureResource(fixture, "cannot reference resource - no fixture "))
                    return;
                assert fixture != null;    // guaranteed by hasFixtureResource
                // expected resource type in response body (GET)
                String expected = valueToCompare;
                String found = fixture.getResponseType();
                if (!compare(val, assertReport, found, expected, operator, warningOnly, type, label))
                    return;
            }
            if (as.hasResponse()) {
                if (hasNoFixtureResource(fixture, "cannot reference response - no fixture "))
                    return;
                assert fixture != null;    // guaranteed by hasFixtureResource
                int codeFound = fixture.getHttpBase().getStatus();
                String found = responseCodeAsString(codeFound);
                String expected = valueToCompare;
                if (!compare(val, assertReport, found, expected, operator, warningOnly, type, label))
                    return;
            }
            if (as.hasResponseCode()) {
                if (hasNoFixtureResource(fixture, "cannot reference contentType - no fixture "))
                    return;
                assert fixture != null;    // guaranteed by hasFixtureResource
                int codeFound = fixture.getHttpBase().getStatus();
                String found = String.valueOf(codeFound);
                String expected = valueToCompare;
                if (!compare(val, assertReport, found, expected, operator, warningOnly, type, label))
                    return;
            }
        }
        if (as.hasExpression()) {
            String expression = as.getExpression();
            if (hasNoFixtureResource(fixture, "cannot evaluate expression - no fixture"))
                return;
            assert fixture != null;    // guaranteed by hasFixtureResource
            BaseResource sourceResource = fixture.getResourceResource();
            if (sourceResource == null) {
                Reporter.reportError(val, assertReport, type, label,"Fixture referenced " + fixture.getId()  + " has no resource");
                return;
            }
            if (!FhirPathEngineBuilder.evalForBoolean(sourceResource, expression)) {
                Reporter.reportFail(val, assertReport, type, label, "Assertion failed", warningOnly);
                return;
            }

        }
        if (as.hasMinimumId()) {
            Reporter.reportError(val, assertReport, type, label, "minumumId not supported");
            return;
        }
        if (as.hasNavigationLinks()) {
            Reporter.reportError(val, assertReport, type, label, "navigationLinks not supported");
            return;
        }
        if (as.hasValidateProfileId()) {
            Reporter.reportError(val, assertReport, type, label, "validateProfileId not supported");
            return;
        }
        if (as.hasRequestURL()) {
            if (lastOp == null) {
                Reporter.reportError(val, assertReport, type, label, " has requestURL tested but no last operation recorded");
                return;
            }
            String expected = null;
            try {
                expected = new URI(as.getRequestURL()).getPath();
            } catch (URISyntaxException e) {
                Reporter.reportError(val, assertReport, type, label, " requestURL (" + as.getRequestURL() + ") cannot be parsed");
                return;
            }
            String found = fixtures.get(lastOp).getHttpBase().getUri().getPath();
            if (!compare(val, assertReport, found, expected, operator, warningOnly, type, label))
                return;
        }
    }

    private boolean hasNoFixtureResource(FixtureComponent fixture, String msg) {
        if (fixture == null) {
            Reporter.reportError(val, assertReport, type, label, msg + "no fixture is referenced");
            return true;
        }
        if (!fixture.hasResource()) {
            Reporter.reportError(val, assertReport, type, label, msg + "referenced fixture " + fixture.getId() + " has no response");
            return true;
        }
        return false;
    }

    private void runOperation(String id, TestReport.SetupActionComponent actionReport) {
        TestReport.SetupActionOperationComponent operationReport = actionReport.getOperation();
        operationReport.setResult(TestReport.TestReportActionResult.PASS);  // may be overwritten

        label = id;
        type = "setup.action.operation";

        if (action.hasAssert()) {
            Reporter.reportError(val, operationReport, type, id,"has both an Operation and an Assertion");
            return;
        }
        TestScript.SetupActionOperationComponent op = action.getOperation();
        int elementCount = 0;
        if (op.hasTargetId()) elementCount++;
        if (op.hasParams()) elementCount++;
        if (op.hasUrl()) elementCount++;
        if (elementCount == 0) {
            Reporter.reportError(val, operationReport, type, id,"has none of sourceId, targetId, params, url - one is required");
            return;
        }
        if (elementCount > 1) {
            Reporter.reportError(val, operationReport, type, id,"has multiple of sourceId, targetId, params, url - only one is allowed");
            return;
        }
        if (!op.hasType()) {
            Reporter.reportError(val, operationReport, type, id,"has no type");
            return;
        }
        Coding typeCoding = op.getType();
        String code = typeCoding.getCode();

        if ("read".equals(code)) {
            FixtureComponent fixture = new SetupActionRead(fixtures, op)
                    .setVal(val)
                    .setVariableMgr(new VariableMgr(testScript, fixtures).setVal(val))
                    .run();
            if (fixture != null)
                lastOp = fixture.getId();
            return;
        } else if ("create".equals(code)) {
            SetupActionCreate setupActionCreate = new SetupActionCreate(fixtures, op, operationReport)
                    .setFhirClient(fhirClient)
                    .setVariableMgr(new VariableMgr(testScript, fixtures).setVal(val);)
                    .setVal(val);
            setupActionCreate.run();
            FixtureComponent fixture = setupActionCreate.getFixtureComponent();
            if (fixture == null)
                return;  // failed
            if (fixture != null)
                lastOp = fixture.getId();
            return;
        } else {
            Reporter.reportError(val, operationReport, type, id,"do not understand code.code of " + code);
            return;
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
            return Reporter.reportFail(val, assertReport, type, operator,"Operator " + operator + " - no value found to compare with " + expected, warningOnly);
        if (operator.equals("equals"))
            return Reporter.report(found.equals(expected), val, assertReport, type, operator,"Operator " + operator, warningOnly);
        if (operator.equals("notEquals"))
            return Reporter.report(!found.equals(expected), val, assertReport, type, operator,"Operator " + operator, warningOnly);
        if (operator.equals("in")) {
            String[] values = expected.split(",");
            for (String value : values) {
                if (value.equals(found))
                    return Reporter.report(true, val, assertReport, type, operator,"Operator " + operator, warningOnly);
            }
            return Reporter.report(false, val, assertReport, type, operator,"Operator " + operator, warningOnly);
        }
        if (operator.equals("notIn")) {
            String[] values = expected.split(",");
            for (String value : values) {
                if (value.equals(found))
                    return Reporter.report(false, val, assertReport, type, operator,"Operator " + operator, warningOnly);
            }
            return Reporter.report(true, val, assertReport, type, operator,"Operator " + operator, warningOnly);
        }
        if (operator.equals("greaterThan")) {
            int iExpected = Integer.parseInt(expected);
            int iFound = Integer.parseInt(found);
            return Reporter.report(iFound > iExpected, val, assertReport, type, operator,"Operator " + operator, warningOnly);
        }
        if (operator.equals("lessThan")) {
            int iExpected = Integer.parseInt(expected);
            int iFound = Integer.parseInt(found);
            return Reporter.report(iFound < iExpected, val, assertReport, type, operator,"Operator " + operator, warningOnly);
        }
        if (operator.equals("empty")) {
            return Reporter.report("".equals(found), val, assertReport, type, operator,"Operator " + operator, warningOnly);
        }
        if (operator.equals("notEmpty")) {
            return Reporter.report(!"".equals(found), val, assertReport, type, operator,"Operator " + operator, warningOnly);
        }
        if (operator.equals("contains")) {
            return Reporter.report(found.contains(expected), val, assertReport, type, operator,"Operator " + operator, warningOnly);
        }
        if (operator.equals("notContains")) {
            return Reporter.report(!found.contains(expected), val, assertReport, type, operator, "Operator " + operator, warningOnly);
        }
        return Reporter.report(false, val, assertReport, type, id, "Do not understand operator " + operator, warningOnly);
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

    public SetupAction setFhirClient(FhirClient fhirClient) {
        this.fhirClient = fhirClient;
        return this;
    }

    private FhirClient getFhirClient() {
        if (fhirClient == null)
            fhirClient = new FhirClient();
        return fhirClient;
    }

    public void setTestScript(TestScript testScript) {
        this.testScript = testScript;
    }
}
