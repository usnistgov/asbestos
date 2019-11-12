package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.*;

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
    private VariableMgr variableMgr = null;

    AssertionRunner(FixtureMgr fixtureMgr) {
        Objects.requireNonNull(fixtureMgr);
        this.fixtureMgr = fixtureMgr;
    }

    public AssertionRunner setVariableMgr(VariableMgr variableMgr) {
        this.variableMgr = variableMgr;
        return this;
    }

    FixtureComponent sourceOverride = null;
    private FixtureComponent getSource(TestScript.SetupActionAssertComponent as) {
        if (sourceOverride != null)
            return sourceOverride;
        FixtureComponent sourceFixture = as.hasSourceId() ? fixtureMgr.get(as.getSourceId()) : fixtureMgr.get(fixtureMgr.getLastOp());
        if (sourceFixture == null)
            Reporter.reportError(val, assertReport, type, label, "no source available for comparison");
        return sourceFixture;
    }

    TestReport.SetupActionAssertComponent run(TestScript.SetupActionAssertComponent as) {
        Objects.requireNonNull(typePrefix);
        Objects.requireNonNull(val);
        Objects.requireNonNull(testScript);

        assertReport = new TestReport.SetupActionAssertComponent();
        FixtureComponent source = getSource(as);
        if (source == null)
            return assertReport;

        if ("Bundle".equals(source.getResponseType()) &&
        as.hasExpression()
        && !as.getExpression().trim().startsWith("Bundle")) {
            // assertion could be targetting Bundle or a resource in the Bundle (search)
            boolean targetIsBundle = as.hasExpression() && as.getExpression().startsWith("Bundle");
            if (targetIsBundle) {
                BaseResource sourceResource = source.getResourceResource();
                if (!(sourceResource instanceof Bundle)) {
                    assertReport = new TestReport.SetupActionAssertComponent();
                    return Reporter.reportError(val, assertReport, type, label, "expression targets a Bundle but sourceId points to a " + sourceResource.getClass().getSimpleName());
                }
                Bundle bundle = (Bundle) sourceResource;
                for( Bundle.BundleEntryComponent component : bundle.getEntry()) {
                    assertReport = new TestReport.SetupActionAssertComponent();
                    if (component.hasResource() ) {
                        sourceOverride = new FixtureComponent(component.getResource());
                        if (run2(as)) {
                            return assertReport;  // good enough to find one component that matches assertion
                        }
                    }
                }
                assertReport = new TestReport.SetupActionAssertComponent();
                return Reporter.reportError(val, assertReport, type, label, "no resource in Bundle matches assertion");
            } else {
                sourceOverride = null;
                assertReport = new TestReport.SetupActionAssertComponent();
                run2(as);
                return assertReport;
            }
        }

        assertReport = new TestReport.SetupActionAssertComponent();
        run2(as);
        return assertReport;
    }

    // success?
    boolean run2(TestScript.SetupActionAssertComponent as) {
        Objects.requireNonNull(variableMgr);
        //this.assertReport = assertReport;

        assertReport.setResult(TestReport.TestReportActionResult.PASS);  // may be overwritten

        label = as.getLabel();
        type = typePrefix + ".assert";

        boolean warningOnly;
        if (as.hasWarningOnly())
            warningOnly = as.getWarningOnly();
        else {
            Reporter.reportError(val, assertReport, type, label, "warningOnly is required but missing");
            return false;
        }

        if (as.hasCompareToSourcePath() && as.hasCompareToSourceExpression()) {
            Reporter.reportError(val, assertReport, type, label, "has both compareToSourcePath and compareToSourceExpression");
            return false;
        }

        if (as.hasCompareToSourceId()) {
            if (!as.hasCompareToSourceExpression() && !as.hasCompareToSourcePath()) {
                Reporter.reportError(val, assertReport, type, label, "has compareToSourceId and nether compareToSourcePath nor compareToSourceExpression are defined");
                return false;
            }
        }
        String operator = as.hasOperator() ? as.getOperator().toCode() : "equals";
        FixtureComponent sourceFixture;

//        // path and value pattern
//        // source specified by sourceId or lastOperation
//        if (as.hasPath() && as.hasValue()) {
//            sourceFixture = getSource(as);
//            if (sourceFixture == null) return;
//            String pathValue = sourceFixture.applyPath(as.getPath());
//            if (pathValue == null) {
//                Reporter.reportError(val, assertReport, type, label, "value extracted by path is null");
//                return;
//            }
//            String found = pathValue;
//            String expected = as.getValue();
//            if (!compare(val, assertReport, found, expected, operator, warningOnly, type, label))
//                return;
//
//            if (!pathValue.equals(as.getValue())) {
//                Reporter.reportError(val, assertReport, type, label, "comparison fails: pathValue=" + pathValue + " value=" + as.getValue());
//                return;
//            }
//            Reporter.reportPass(val, assertReport, type, label, "path/value comparison");
//            return;
//        }

        if (as.hasCompareToSourceId() && as.hasCompareToSourceExpression()) {
            sourceFixture = fixtureMgr.get(as.getCompareToSourceId());
            if (sourceFixture == null) {
                Reporter.reportError(val, assertReport, type, label, "compareToSourceId references " + as.getCompareToSourceId() + " which cannot be found");
                return false;
            }

            BaseResource sourceResource = sourceFixture.getResourceResource();
            if (sourceResource == null) {
                Reporter.reportError(val, assertReport, type, label,"Fixture referenced " + sourceFixture.getId()  + " has no resource");
                return false;
            }
            String expression = as.getCompareToSourceExpression();
            String found = FhirPathEngineBuilder.evalForString(sourceResource, expression);
            if ("true".equals(found)) {
                Reporter.reportPass(val, assertReport, type, label, "expression comparison completed");
                return true;
            }
            Reporter.reportFail(val, assertReport, type, label, "assertion failed - " + expression, warningOnly);
            return false;
        }

        // resource type pattern
        // resource type specified sourceId or lastOperation must have returned that resource type
        if (as.hasResource()) {
            sourceFixture = getSource(as);
            if (sourceFixture == null) return false;
            if (sourceFixture.getResponseType() == null) {
                Reporter.reportError(val, assertReport, type, label, "sourceId or lastOperation references no resource");
                return false;
            }
            if (!as.getResource().equals(sourceFixture.getResponseType())) {
                Reporter.reportError(val, assertReport, type, label, "expected " + as.getResource() + " found " + sourceFixture.getResponseType());
                return false;
            }
            Reporter.reportPass(val, assertReport, type, label, "resource type comparison (" + sourceFixture.getResponseType() + ")" );
            return true;

        }

        // contentType pattern
        // compares against sourceId or return of lastOperation
        if (as.hasContentType()) {
            sourceFixture = getSource(as);
            if (sourceFixture == null) return false;
            if (!as.getContentType().equalsIgnoreCase(sourceFixture.getResponseType())) {
                Reporter.reportError(val, assertReport, type, label, "expecting " + as.getContentType() + " found " + sourceFixture.getResponseType());
                return false;
            }
            Reporter.reportPass(val, assertReport, type, label, "content type comparison");
            return true;
        }

        // headerField/value comparison pattern
        // compares against sourceId or return of lastOperation
        if (as.hasHeaderField() && as.hasValue()) {
            sourceFixture = getSource(as);
            if (sourceFixture == null) return false;
            String sourceHeaderFieldValue = sourceFixture.getHttpBase().getResponseHeaders().getValue(as.getHeaderField());
            if (sourceHeaderFieldValue == null)
                sourceHeaderFieldValue = "";
            if (!sourceHeaderFieldValue.equalsIgnoreCase(as.getValue())) {
                Reporter.reportError(val, assertReport, type, label, sourceHeaderFieldValue + " != " + as.hasValue());
                return false;

            }
            Reporter.reportPass(val, assertReport, type, label, "headerType/value comparison");
            return true;
        }

        // response pattern
        if (as.hasResponse()) {
            sourceFixture = getSource(as);
            if (sourceFixture == null) return false;
            int codeFound = sourceFixture.getResourceWrapper().getHttpBase().getStatus();
            String found = responseCodeAsString(codeFound);
            String expected = as.getResponse().toCode();
            if (!compare(val, assertReport, found, expected, operator, warningOnly, type, label))
                return false;
            Reporter.reportPass(val, assertReport, type, label, "response comparison completed");
            return true;
        }

        // responseCodePattern
        if (as.hasResponseCode()) {
            sourceFixture = getSource(as);
            if (sourceFixture == null) return false;
            int codeFound = sourceFixture.getResourceWrapper().getHttpBase().getStatus();
            String found = String.valueOf(codeFound);
            String expected = as.getResponseCode();
            if (!compare(val, assertReport, found, expected, operator, warningOnly, type, label))
                return false;
            Reporter.reportPass(val, assertReport, type, label, "responseCode comparison completed");
            return true;
        }

        // expression and value pattern
        if (as.hasExpression() && as.hasValue()) {
            sourceFixture = getSource(as);
            if (sourceFixture == null) return false;
            BaseResource sourceResource = sourceFixture.getResourceResource();
            if (sourceResource == null) {
                Reporter.reportError(val, assertReport, type, label,"Fixture referenced " + sourceFixture.getId()  + " has no resource");
                return false;
            }
            String found = FhirPathEngineBuilder.evalForString(sourceResource, variableMgr.updateReference(as.getExpression()));


            if (found != null && found.contains("/")) {
                Ref foundRef = new Ref(found);
                if (foundRef.getBase().toString().equals("")) {
                    String contentLocation = sourceFixture.getHttpBase().getContentLocation();
                    if (contentLocation != null && !contentLocation.equals("")) {
                        Ref locationRef = new Ref(contentLocation);
                        foundRef = foundRef.rebase(locationRef);
                        found = foundRef.toString();
                    }
                }
            }



            String expected = variableMgr.updateReference(as.getValue());
            if (!compare(val, assertReport, found, expected, operator, warningOnly, type, label))
                return false;
            Reporter.reportPass(val, assertReport, type, label, "expression comparison completed");
            return true;
        }

        // expression
        if (as.hasExpression()) {
            sourceFixture = getSource(as);
            if (sourceFixture == null) return false;
            BaseResource sourceResource = sourceFixture.getResourceResource();
            if (sourceResource == null) {
                Reporter.reportError(val, assertReport, type, label,"Fixture referenced " + sourceFixture.getId()  + " has no resource");
                return false;
            }
            boolean ok = FhirPathEngineBuilder.evalForBoolean(sourceResource, as.getExpression());
            if (ok) {
                Reporter.reportPass(val, assertReport, type, label, "expression evaluated completed");
                return true;
            }
            Reporter.reportError(val, assertReport, type, label, "expression " + as.getExpression()  +  " failed");
            return false;
        }

        if (as.hasRequestMethod()) {
            String requestedMethod = as.getRequestMethod().toCode();
            sourceFixture = getSource(as);
            if (sourceFixture == null) return false;
            String method = sourceFixture.getResourceWrapper().getHttpBase().getVerb();
            if (requestedMethod.equalsIgnoreCase(method)) {
                Reporter.reportPass(val, assertReport, type, label, "Method " + requestedMethod + " found");
                return true;
            }
            Reporter.reportFail(val, assertReport, type, label, "Expected method " + requestedMethod + " found " + method, warningOnly);
            return false;
        }


        Reporter.reportError(val, assertReport, type, label, "No assertion");
        return false;
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
            return Reporter.reportFail(val, assertReport, type, operator,"Operator is " + operator + "\nexpected is " + expected + "\nfound is " + found, warningOnly);
        if (operator.equals("equals"))
            return Reporter.report(found.equals(expected), val, assertReport, type, operator,"Operator is " + operator + "\nexpected is " + expected + "\nfound is " + found, warningOnly);
        if (operator.equals("notEquals"))
            return Reporter.report(!found.equals(expected), val, assertReport, type, operator,"Operator is " + operator + "\nexpected is " + expected + "\nfound is " + found, warningOnly);
        if (operator.equals("in")) {
            String[] values = expected.split(",");
            for (String value : values) {
                if (value.equals(found))
                    return Reporter.report(true, val, assertReport, type, operator,"Operator is " + operator + "\nexpected is " + expected + "\nfound is " + found, warningOnly);
            }
            return Reporter.report(false, val, assertReport, type, operator,"Operator is " + operator + "\nexpected is " + expected + "\nfound is " + found, warningOnly);
        }
        if (operator.equals("notIn")) {
            String[] values = expected.split(",");
            for (String value : values) {
                if (value.equals(found))
                    return Reporter.report(false, val, assertReport, type, operator,"Operator is " + operator + "\nexpected is " + expected + "\nfound is " + found, warningOnly);
            }
            return Reporter.report(true, val, assertReport, type, operator,"Operator is " + operator, warningOnly);
        }
        if (operator.equals("greaterThan")) {
            int iExpected = Integer.parseInt(expected);
            int iFound = Integer.parseInt(found);
            return Reporter.report(iFound > iExpected, val, assertReport, type, operator,"Operator is " + operator + "\nexpected is " + expected + "\nfound is " + found, warningOnly);
        }
        if (operator.equals("lessThan")) {
            int iExpected = Integer.parseInt(expected);
            int iFound = Integer.parseInt(found);
            return Reporter.report(iFound < iExpected, val, assertReport, type, operator,"Operator is " + operator + "\nexpected is " + expected + "\nfound is " + found, warningOnly);
        }
        if (operator.equals("empty")) {
            return Reporter.report("".equals(found), val, assertReport, type, operator,"Operator is " + operator + "\nexpected is " + expected + "\nfound is " + found, warningOnly);
        }
        if (operator.equals("notEmpty")) {
            return Reporter.report(!"".equals(found), val, assertReport, type, operator,"Operator is " + operator + "\nexpected is " + expected + "\nfound is " + found, warningOnly);
        }
        if (operator.equals("contains")) {
            return Reporter.report(found.contains(expected), val, assertReport, type, operator,"Operator is " + operator + "\nexpected is " + expected + "\nfound is " + found, warningOnly);
        }
        if (operator.equals("notContains")) {
            return Reporter.report(!found.contains(expected), val, assertReport, type, operator, "Operator is " + operator + "\nexpected is " + expected + "\nfound is " + found, warningOnly);
        }
        return Reporter.report(false, val, assertReport, type, id, "Do not understand operator " + operator + "\nexpected is " + expected + "\nfound is " + found, warningOnly);
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
