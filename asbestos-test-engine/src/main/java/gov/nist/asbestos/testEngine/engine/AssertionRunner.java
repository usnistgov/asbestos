package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.simapi.validation.ValE;
import gov.nist.asbestos.testEngine.engine.assertion.MinimumId;
import org.hl7.fhir.r4.model.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class AssertionRunner {
    private String label;
    private String type;
    private String typePrefix;
    private ValE val;
    private TestScript testScript = null;
    private FixtureMgr fixtureMgr;
    private TestReport.SetupActionAssertComponent assertReport;
    //private TestReport testReport = null;
    private VariableMgr variableMgr = null;
    private boolean isRequest = false;  // is assertion being run on request message?

    AssertionRunner(FixtureMgr fixtureMgr) {
        Objects.requireNonNull(fixtureMgr);
        this.fixtureMgr = fixtureMgr;
    }

    public AssertionRunner setIsRequest(boolean isRequest) {
        this.isRequest = isRequest;
        return this;
    }

    AssertionRunner setVariableMgr(VariableMgr variableMgr) {
        this.variableMgr = variableMgr;
        return this;
    }

    private FixtureComponent sourceOverride = null;
    private FixtureComponent getSource(TestScript.SetupActionAssertComponent as) {
        FixtureComponent sourceFixture = getSourceIfAvailable(as);
        if (sourceFixture == null)
            Reporter.reportError(val, assertReport, type, label, "no source available for comparison.");
        return sourceFixture;
    }

    private FixtureComponent getSourceIfAvailable(TestScript.SetupActionAssertComponent as) {
        if (sourceOverride != null)
            return sourceOverride;
        FixtureComponent sourceFixture = as.hasSourceId() ? fixtureMgr.get(as.getSourceId()) : fixtureMgr.get(fixtureMgr.getLastOp());
//        if (sourceFixture == null)
//            Reporter.reportError(val, assertReport, type, label, "no source available for comparison.");
        return sourceFixture;
    }

    TestReport.SetupActionAssertComponent run(TestScript.SetupActionAssertComponent as) {
        Objects.requireNonNull(typePrefix);
        Objects.requireNonNull(val);
        Objects.requireNonNull(testScript);

        assertReport = new TestReport.SetupActionAssertComponent();
        FixtureComponent source = getSourceIfAvailable(as);
//        if (source == null)
//            return assertReport;


        if (source != null && "Bundle".equals(source.getResponseType())) {
            if (as.hasExpression() && !as.getExpression().trim().startsWith("Bundle")) {
                // assertion could be targeting Bundle or a resource in the Bundle (search)
                if (as.hasExpression() && as.getExpression().trim().startsWith("Bundle")) {
                    BaseResource sourceResource = source.getResourceResource();
                    if (!(sourceResource instanceof Bundle)) {
                        assertReport = new TestReport.SetupActionAssertComponent();
                        return Reporter.reportError(val, assertReport, type, label, "expression targets a Bundle but sourceId points to a " + sourceResource.getClass().getSimpleName());
                    }
                    Bundle bundle = (Bundle) sourceResource;
                    for (Bundle.BundleEntryComponent component : bundle.getEntry()) {
                        assertReport = new TestReport.SetupActionAssertComponent();
                        if (component.hasResource()) {
                            sourceOverride = new FixtureComponent(component.getResource());
                            if (run2(as)) {
                                return assertReport;  // good enough to find one component that matches assertion
                            }
                        }
                    }
                    assertReport = new TestReport.SetupActionAssertComponent();
                    return Reporter.reportError(val, assertReport, type, label, "no resource in Bundle matches assertion.");
                } else {
                    sourceOverride = null;
                    assertReport = new TestReport.SetupActionAssertComponent();
                    run2(as);
                    return assertReport;
                }
            } else if (as.hasModifierExtension()) {
                Extension extension = as.getModifierExtension().isEmpty() ? null : as.getModifierExtension().get(0);
                if (extension == null) {
                    assertReport = new TestReport.SetupActionAssertComponent();
                    Reporter.reportError(val, assertReport, type, label, "Found no stringValue in modifierExtension.");
                    return assertReport;
                }
                String resourceType = extension.getValue().toString();
                BaseResource sourceResource = source.getResourceResource();
                assertReport = null;
                Bundle bundle = (Bundle) sourceResource;
                int i=0;
                for (Bundle.BundleEntryComponent component : bundle.getEntry()) {
                    Resource resource = component.getResource();
                    if (resource.getClass().getSimpleName().equals(resourceType)) {
                        assertReport = new TestReport.SetupActionAssertComponent();
                        ResourceWrapper savedSource = source.getResourceWrapper();  // new value (from Bundle) for FixtureComponent
                        source.setResource(new ResourceWrapper(resource));  // so it applies to the in-bundle resource
                        source.setId(resourceType + " #" + i);
                        boolean success = run2(as);
                        source.setResource(savedSource);  // restore FixtureComponent
                        if (!success)
                            break;
                        //assertReport = null;
                    }
                }
                if (assertReport != null)
                    return assertReport;
                assertReport = new TestReport.SetupActionAssertComponent();
                Reporter.reportError(val, assertReport, type, label, "Found no " + resourceType + " resources in Bundle.");
                return assertReport;
            }
        } else {
            run2(as);
            return assertReport;
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

//        INSTRUCTIONS

        if (as.hasCompareToSourceId() && as.hasCompareToSourceExpression()) return instSourceIdSourceExpression(as, warningOnly);

        if (as.hasMinimumId()) return instMinimumId(as, warningOnly);

        // resource type pattern
        // resource type specified sourceId or lastOperation must have returned that resource type
        if (as.hasResource()) return instResource(as, warningOnly);

        // contentType pattern
        // compares against sourceId or return of lastOperation
        if (as.hasContentType()) return instContentType(as, warningOnly);

        // headerField/value comparison pattern
        // compares against sourceId or return of lastOperation
        if (as.hasHeaderField() && as.hasValue()) return instHeaderFieldValue(as, warningOnly);

        // response pattern
        if (as.hasResponse()) return instResponse(as, warningOnly);

        // responseCodePattern
        if (as.hasResponseCode()) return instResponseCode(as, warningOnly);

        // expression and value pattern
        if (as.hasExpression() && as.hasValue()) return instExpressionValue(as, warningOnly);

        // expression
        if (as.hasExpression()) return instExpression(as, warningOnly);

        if (as.hasRequestMethod()) return instRequestMethod(as, warningOnly);

        Reporter.reportError(val, assertReport, type, label, "No assertion.");
        return false;
    }

    private boolean instSourceIdSourceExpression(TestScript.SetupActionAssertComponent as, boolean warningOnly) {
        FixtureComponent  sourceFixture = fixtureMgr.get(as.getCompareToSourceId());
        if (sourceFixture == null) {
            Reporter.reportError(val, assertReport, type, label, "compareToSourceId references " + as.getCompareToSourceId() + " which cannot be found.");
            return false;
        }

        BaseResource sourceResource = sourceFixture.getResourceResource();
        if (sourceResource == null) {
            Reporter.reportError(val, assertReport, type, label,"Fixture referenced " + sourceFixture.getId()  + " has no resource.");
            return false;
        }
        String expression = variableMgr.updateReference(as.getCompareToSourceExpression());
        String found;
        try {
            found = FhirPathEngineBuilder.evalForString(sourceResource, expression);
        } catch (Throwable e) {
            Reporter.reportError(val, assertReport, type, label,"Error evaluating expression: " + expression + "\n" + e.getMessage());
            return false;
        }
        if ("true".equals(found))
            return Reporter.reportPass(val, assertReport, type, label, expression);

        return Reporter.reportFail(val, assertReport, type, label, "assertion failed - " + expression + " ==> " + found, warningOnly);
    }

    static private List<String> hide = Arrays.asList("Description", "Id", "Meta", "Text");

    static public final String EVALUATING_TYPE = "Evaluating type";
    static public final String SCRIPT = "Script";
    static public final String RAW_REPORT = "Raw Report";
//    static public final String ATTS_NOT_FOUND = "Atts Not Found";

    private boolean instMinimumId(TestScript.SetupActionAssertComponent as, boolean warningOnly) {
        FixtureComponent sourceFixture = getSource(as);
        if (sourceFixture == null) return false;

        FixtureComponent miniFixture  = fixtureMgr.get(as.getMinimumId());
        if (miniFixture == null) {
            Reporter.reportError(val, assertReport, type, label, "minimumId references " + as.getMinimumId() + " which cannot be found.");
            return false;
        }

        BaseResource miniR = miniFixture.getResourceResource();   // reference
        BaseResource sourceR = sourceFixture.getResourceResource();  // sut

//        Class<?> miniClass = miniR.getClass();
        Class<?> sourceClass = sourceR.getClass();

        assertReport.setUserData(EVALUATING_TYPE, sourceClass.getSimpleName());   // resource type being evaluated
        assertReport.setUserData(SCRIPT, as.getLabel());   // assert label (documentation only - nothing computed)

//        if (!miniClass.equals(sourceClass)) {
//            assertReport.setUserData("No Comparison", "minimumId: cannot compare " + miniClass.getName() + " and " + sourceClass.getName());
//            assertReport.setResult(TestReport.TestReportActionResult.SKIP);
//            return false;
//        }

        MinimumId.Report report = new MinimumId().run(miniR, sourceR, isRequest);
        if (!report.errors.isEmpty()) {
            assertReport.setUserData("No Comparison", report.errors.get(0));
            assertReport.setResult(TestReport.TestReportActionResult.SKIP);
            return false;
        }

        // This UserData is a properties style list for private transport between components
        // It is used here to pass back the raw MinimumId.Report to the caller which might be AnalysisReport.java
        assertReport.setUserData(RAW_REPORT, report);

        assertReport.setDetail(
                "[" +
                        String.join(", ", report.expected)
                + "]"
        );
        if (report.missing.isEmpty()) {
            Reporter.reportPass(val, assertReport, type, label, "pass");
            return true;
        } else {
            String atts = String.join(", ", report.missing);
            Reporter.reportFail(val, assertReport, type, label, "minimumId: attributes [" + atts + "] not found ", warningOnly);
            return false;
        }

    }

    private boolean instResource(TestScript.SetupActionAssertComponent as, boolean warningOnly) {
        FixtureComponent sourceFixture = getSource(as);
        if (sourceFixture == null) return false;
        if (sourceFixture.getResponseType() == null) {
            Reporter.reportError(val, assertReport, type, label, "sourceId or lastOperation references no resource.");
            return false;
        }
        if (!as.getResource().equals(sourceFixture.getResponseType()))
            return Reporter.reportFail(val, assertReport, type, label, "expected " + as.getResource() + " found " + sourceFixture.getResponseType(), warningOnly);
        return Reporter.reportPass(val, assertReport, type, label, "resource type comparison (" + sourceFixture.getResponseType() + ")" );
    }

    private boolean instContentType(TestScript.SetupActionAssertComponent as, boolean warningOnly) {
        FixtureComponent sourceFixture = getSource(as);
        if (sourceFixture == null) return false;
        if (!as.getContentType().equalsIgnoreCase(sourceFixture.getResponseType()))
            return Reporter.reportFail(val, assertReport, type, label, "expecting " + as.getContentType() + " found " + sourceFixture.getResponseType(), warningOnly);
        return Reporter.reportPass(val, assertReport, type, label, as.getContentType() + " = " + sourceFixture.getResponseType());
    }

    private boolean instHeaderFieldValue(TestScript.SetupActionAssertComponent as, boolean warningOnly) {
        FixtureComponent sourceFixture = getSource(as);
        if (sourceFixture == null) return false;
        String sourceHeaderFieldValue = sourceFixture.getHttpBase().getResponseHeaders().getValue(as.getHeaderField());
        if (sourceHeaderFieldValue == null)
            sourceHeaderFieldValue = "";
        if (!sourceHeaderFieldValue.equalsIgnoreCase(as.getValue()))
            return Reporter.reportFail(val, assertReport, type, label, sourceHeaderFieldValue + " != " + as.hasValue(), warningOnly);
        return Reporter.reportPass(val, assertReport, type, label, sourceHeaderFieldValue + " = " + as.getValue());
    }

    private boolean instResponse(TestScript.SetupActionAssertComponent as, boolean warningOnly) {
        FixtureComponent sourceFixture = getSource(as);
        if (sourceFixture == null) return false;
        int codeFound = sourceFixture.getResourceWrapper().getHttpBase().getStatus();
        String found = responseCodeAsString(codeFound);
        String expected = as.getResponse().toCode();
        String operator = as.hasOperator() ? as.getOperator().toCode() : "equals";
        if (!compare(val, assertReport, found, expected, operator, warningOnly, type, label))
            return false;
        return Reporter.reportPass(val, assertReport, type, label, found + " " + operator + " " + expected);
    }

    private boolean instResponseCode(TestScript.SetupActionAssertComponent as, boolean warningOnly) {
        FixtureComponent sourceFixture = getSource(as);
        if (sourceFixture == null) return false;
        int codeFound = sourceFixture.getResourceWrapper().getHttpBase().getStatus();
        String found = String.valueOf(codeFound);
        String expected = as.getResponseCode();
        String operator = as.hasOperator() ? as.getOperator().toCode() : "equals";
        if (!compare(val, assertReport, found, expected, operator, warningOnly, type, label))
            return false;
        return Reporter.reportPass(val, assertReport, type, label, found + " " + operator + " " + expected);
    }

    private boolean instExpressionValue(TestScript.SetupActionAssertComponent as, boolean warningOnly) {
        FixtureComponent sourceFixture = getSource(as);
        if (sourceFixture == null) return false;
        BaseResource sourceResource = sourceFixture.getResourceResource();
        if (sourceResource == null) {
            Reporter.reportError(val, assertReport, type, label,"Fixture referenced " + sourceFixture.getId()  + " has no resource");
            return false;
        }
        String expression = variableMgr.updateReference(as.getExpression());
        String found;
        try {
            found = FhirPathEngineBuilder.evalForString(sourceResource, expression);
        } catch (Throwable e) {
            Reporter.reportError(val, assertReport, type, label,"Error evaluating expression: " + expression + "\n" + e.getMessage());
            return false;
        }

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

        // remove semantic name from status for comparison
        if (expression.endsWith("response.status")) {
            String[] parts = found.split(" ");
            if (parts.length > 1) {
                found = parts[0];
            }
        }

        String expected = variableMgr.updateReference(as.getValue());
        String operator = as.hasOperator() ? as.getOperator().toCode() : "equals";
        if (!compare(val, assertReport, found, expected, operator, warningOnly, type, label))
            return false;
        return Reporter.reportPass(val, assertReport, type, label, expression);
    }

    private boolean instExpression(TestScript.SetupActionAssertComponent as, boolean warningOnly) {
        FixtureComponent sourceFixture = getSource(as);
        if (sourceFixture == null) return false;
        BaseResource sourceResource = sourceFixture.getResourceResource();
        if (sourceResource == null) {
            Reporter.reportError(val, assertReport, type, label,"Fixture referenced " + sourceFixture.getId()  + " has no resource.");
            return false;
        }
        String expression = as.getExpression();
        boolean ok;
        try {
            ok = FhirPathEngineBuilder.evalForBoolean(sourceResource, expression);
        } catch (Throwable e) {
            Reporter.reportError(val, assertReport, type, label,"Error evaluating expression: " + expression + "\n" + e.getMessage());
            return false;
        }
        if (ok)
            return Reporter.reportPass(val, assertReport, type, label, expression);

        return Reporter.reportFail(val, assertReport, type, label, "expression " + as.getExpression()  +  " failed.", warningOnly);
    }

    private boolean instRequestMethod(TestScript.SetupActionAssertComponent as, boolean warningOnly) {
        FixtureComponent sourceFixture = getSource(as);
        String requestedMethod = as.getRequestMethod().toCode();
        if (sourceFixture == null) return false;
        String method = sourceFixture.getResourceWrapper().getHttpBase().getVerb();
        if (requestedMethod.equalsIgnoreCase(method)) {
            Reporter.reportPass(val, assertReport, type, label, "Method " + requestedMethod + " found");
            return true;
        }
        Reporter.reportFail(val, assertReport, type, label, "Expected method " + requestedMethod + " found " + method, warningOnly);
        return false;
    }

    private boolean hasNoFixtureResource(FixtureComponent fixture, String msg) {
        if (fixture == null) {
            Reporter.reportError(val, assertReport, type, label, msg + " - no fixture is referenced.");
            return true;
        }
        if (!fixture.hasResource()) {
            Reporter.reportError(val, assertReport, type, label, msg + "referenced fixture " + fixture.getId() + " has no response.");
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


//    private boolean noFailures(TestReport.SetupActionAssertComponent assertReport) {
//        return assertReport.getResult() != TestReport.TestReportActionResult.FAIL;
//    }


    AssertionRunner setTypePrefix(String typePrefix) {
        this.typePrefix = typePrefix;
        return this;
    }

    public AssertionRunner setVal(ValE val) {
        this.val = val;
        return this;
    }

    AssertionRunner setTestScript(TestScript testScript) {
        this.testScript = testScript;
        return this;
    }

//    public AssertionRunner setTestReport(TestReport testReport) {
//        this.testReport = testReport;
//        return this;
//    }
}
