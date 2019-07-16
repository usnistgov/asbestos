package gov.nist.asbestos.testEngine;

import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public class AssertionRunner {
    private String label;
    private String type;
    private String typePrefix;
    private ValE val;
    private TestScript testScript = null;
    private FixtureMgr fixtureMgr;
    private TestReport.SetupActionAssertComponent assertReport;
    private TestReport testReport = null;

    AssertionRunner(FixtureMgr fixtureMgr) {
        Objects.requireNonNull(fixtureMgr);
        this.fixtureMgr = fixtureMgr;
    }

    void run(TestScript.SetupActionAssertComponent as, TestReport.SetupActionAssertComponent assertReport) {
        Objects.requireNonNull(typePrefix);
        Objects.requireNonNull(val);
        Objects.requireNonNull(testScript);
        this.assertReport = assertReport;

        assertReport.setResult(TestReport.TestReportActionResult.PASS);  // may be overwritten

        label = as.getLabel();
        type = typePrefix + ".assert";

        FixtureComponent fixture = null;
        if (fixtureMgr.getLastOp() != null) {
            fixture = fixtureMgr.get(fixtureMgr.getLastOp());
            if (fixture == null) {
                Reporter.reportError(val, assertReport, type, label,"last operation - " + fixtureMgr.getLastOp() + " - does not name a fixture");
                return;
            }
        }

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
            valueToCompare = new VariableMgr(testScript, fixtureMgr)
                    .setVal(val)
                    .setAsReport(assertReport)
                    .updateReference(valueToCompare);
        } else if (as.hasCompareToSourceId()) {
            source = fixtureMgr.get(as.getCompareToSourceId());
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
        if (as.hasSourceId()) {
            fixture = fixtureMgr.get(as.getSourceId());
            if (fixture == null) {
                Reporter.reportFail(val, assertReport, type, label, "sourceId " + as.getSourceId() + " not defined", warningOnly);
                return;
            }
        }

        if (as.hasMinimumId()) {
            FixtureComponent comp = fixtureMgr.get(as.getMinimumId());
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

        if (as.hasResource()) {
            if (hasNoFixtureResource(fixture, "cannot reference resource - no fixture "))
                return;
            // expected resource type in response body (GET)
            String expected = as.getResource();
            String found = fixture.getResponseType();
            if (!compare(val, assertReport, found, expected, operator, warningOnly, type, label))
                return;
        }
        if (as.hasResponse()) {
            if (hasNoFixtureResource(fixture, "cannot reference response - no fixture "))
                return;
            assert fixture != null;  // guarenteed by hasNoFixtureReference
//            if (!fixture.hasHttpBase()) {
//                Reporter.reportFail(val, assertReport, type, label, "fixture has no HTTP operation associated with it", warningOnly);
//                return;
//            }
            int codeFound = fixture.getResourceWrapper().getHttpBase().getStatus();
            String found = responseCodeAsString(codeFound);
            String expected = as.getResponse().toCode();
            if (!compare(val, assertReport, found, expected, operator, warningOnly, type, label))
                return;
        }
        if (as.hasResponseCode()) {
            if (hasNoFixtureResource(fixture, "cannot reference contentType - no fixture "))
                return;
            assert fixture != null;    // guaranteed by hasFixtureResource
            int codeFound = fixture.getResourceWrapper().getHttpBase().getStatus();
            String found = String.valueOf(codeFound);
            String expected = as.getResponseCode();
            if (!compare(val, assertReport, found, expected, operator, warningOnly, type, label))
                return;
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
            // TODO support  minimumId
            Reporter.reportError(val, assertReport, type, label, "minumumId not supported");
            return;
        }
        if (as.hasNavigationLinks()) {
            // TODO support navigationLinks
            Reporter.reportError(val, assertReport, type, label, "navigationLinks not supported");
            return;
        }
        if (as.hasValidateProfileId()) {
            // TODO support validateProfileId
            Reporter.reportError(val, assertReport, type, label, "validateProfileId not supported");
            return;
        }
        if (as.hasRequestURL()) {
            if (fixtureMgr.getLastOp() == null) {
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
            String found = fixtureMgr.get(fixtureMgr.getLastOp()).getHttpBase().getUri().getPath();
            if (!compare(val, assertReport, found, expected, operator, warningOnly, type, label))
                return;
        }
        if (as.hasPath()) {
            // TODO support path
            Reporter.reportError(val, assertReport, type, label, "path not supported - please use expression (FHIRPath)");
            return;
        }
    }

    private boolean hasNoFixtureResource(FixtureComponent fixture, String msg) {
        if (fixture == null) {
            Reporter.reportError(val, assertReport, type, label, msg + " - no fixture is referenced");
            return true;
        }
        if (!fixture.hasResource()) {
            Reporter.reportError(val, assertReport, type, label, msg + "referenced fixture " + fixture.getId() + " has no response");
            return true;
        }
        return false;
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
            return Reporter.reportFail(val, assertReport, type, operator,"Operator is " + operator + " - no value found to compare with " + expected, warningOnly);
        if (operator.equals("equals"))
            return Reporter.report(found.equals(expected), val, assertReport, type, operator,"Operator is " + operator + " expected is " + expected + " found is " + found, warningOnly);
        if (operator.equals("notEquals"))
            return Reporter.report(!found.equals(expected), val, assertReport, type, operator,"Operator is " + operator + " expected is " + expected + " found is " + found, warningOnly);
        if (operator.equals("in")) {
            String[] values = expected.split(",");
            for (String value : values) {
                if (value.equals(found))
                    return Reporter.report(true, val, assertReport, type, operator,"Operator is " + operator + " expected is " + expected + " found is " + found, warningOnly);
            }
            return Reporter.report(false, val, assertReport, type, operator,"Operator is " + operator + " expected is " + expected + " found is " + found, warningOnly);
        }
        if (operator.equals("notIn")) {
            String[] values = expected.split(",");
            for (String value : values) {
                if (value.equals(found))
                    return Reporter.report(false, val, assertReport, type, operator,"Operator is " + operator + " expected is " + expected + " found is " + found, warningOnly);
            }
            return Reporter.report(true, val, assertReport, type, operator,"Operator is " + operator, warningOnly);
        }
        if (operator.equals("greaterThan")) {
            int iExpected = Integer.parseInt(expected);
            int iFound = Integer.parseInt(found);
            return Reporter.report(iFound > iExpected, val, assertReport, type, operator,"Operator is " + operator + " expected is " + expected + " found is " + found, warningOnly);
        }
        if (operator.equals("lessThan")) {
            int iExpected = Integer.parseInt(expected);
            int iFound = Integer.parseInt(found);
            return Reporter.report(iFound < iExpected, val, assertReport, type, operator,"Operator is " + operator + " expected is " + expected + " found is " + found, warningOnly);
        }
        if (operator.equals("empty")) {
            return Reporter.report("".equals(found), val, assertReport, type, operator,"Operator is " + operator + " expected is " + expected + " found is " + found, warningOnly);
        }
        if (operator.equals("notEmpty")) {
            return Reporter.report(!"".equals(found), val, assertReport, type, operator,"Operator is " + operator + " expected is " + expected + " found is " + found, warningOnly);
        }
        if (operator.equals("contains")) {
            return Reporter.report(found.contains(expected), val, assertReport, type, operator,"Operator is " + operator + " expected is " + expected + " found is " + found, warningOnly);
        }
        if (operator.equals("notContains")) {
            return Reporter.report(!found.contains(expected), val, assertReport, type, operator, "Operator is " + operator + " expected is " + expected + " found is " + found, warningOnly);
        }
        return Reporter.report(false, val, assertReport, type, id, "Do not understand operator " + operator + " expected is " + expected + " found is " + found, warningOnly);
    }


    private boolean noFailures(TestReport.SetupActionAssertComponent assertReport) {
        return assertReport.getResult() != TestReport.TestReportActionResult.FAIL;
    }


    public AssertionRunner setTypePrefix(String typePrefix) {
        this.typePrefix = typePrefix;
        return this;
    }

    public AssertionRunner setVal(ValE val) {
        this.val = val;
        return this;
    }

    public AssertionRunner setTestScript(TestScript testScript) {
        this.testScript = testScript;
        return this;
    }

    public AssertionRunner setTestReport(TestReport testReport) {
        this.testReport = testReport;
        return this;
    }
}
