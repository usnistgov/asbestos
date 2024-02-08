package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.simapi.validation.ValE;
import gov.nist.asbestos.testEngine.engine.assertion.*;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureComponent;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureMgr;
import gov.nist.asbestos.testEngine.engine.fixture.UnregisteredFixtureComponent;
import org.hl7.fhir.r4.model.*;

import java.util.Objects;
import java.util.logging.Logger;

public class AssertionRunner implements AssertionContext {
    private String label;
    private String type;
    private String typePrefix;
    private ValE val;
    private TestScript testScript = null;
    private final FixtureMgr fixtureMgr;
    private VariableMgr variableMgr = null;
    private boolean isRequest = false;  // is assertion being run on request message?
    private String testCollectionId = null;
    private String testId = null;
    private TestEngine testEngine = null;
    private TestReport testReport = null;
    private static final Logger log = Logger.getLogger(AssertionRunner.class.getName());


    AssertionRunner(FixtureMgr fixtureMgr) {
        Objects.requireNonNull(fixtureMgr);
        this.fixtureMgr = fixtureMgr;
    }

    public AssertionRunner setIsRequest(boolean isRequest) {
        this.isRequest = isRequest;
        return this;
    }

    public boolean isRequest() {
        return isRequest;
    }

    @Override
    public boolean validate() {
        if (getSource() == null)
            return false;
        if (getFixtureLabels() == null)
            return false;
        if (getSource().getResourceResource() == null) {
            Reporter.reportError(this, "Fixture referenced <" + getSource().getId()  + "> has no resource.");
            return false;
        }
        return true;
    }

    AssertionRunner setVariableMgr(VariableMgr variableMgr) {
        this.variableMgr = variableMgr;
        return this;
    }

    private UnregisteredFixtureComponent sourceOverride = null;

    public FixtureComponent getSource() {
        FixtureComponent sourceFixture = getSourceIfAvailable();
//        if (sourceFixture == null)
//            Reporter.reportError(this, "no source available for comparison.");
        return sourceFixture;
    }

    private FixtureComponent getSourceIfAvailable() {
        Objects.requireNonNull(currentAssert);
        if (sourceOverride != null)
            return sourceOverride;
        FixtureComponent sourceFixture = currentAssert.hasSourceId() ? fixtureMgr.get(currentAssert.getSourceId()) : fixtureMgr.get(fixtureMgr.getLastOp());
        return sourceFixture;
    }

    public FixtureComponent getCompareToSource() {
        Objects.requireNonNull(currentAssert);
        if (sourceOverride != null)
            return sourceOverride;
        FixtureComponent sourceFixture = currentAssert.hasCompareToSourceId() ? fixtureMgr.get(currentAssert.getCompareToSourceId()) : fixtureMgr.get(fixtureMgr.getLastOp());
        return sourceFixture;
    }

    private FixtureComponent getCompareToSourceIfAvailable(TestScript.SetupActionAssertComponent as) {
        FixtureComponent sourceFixture = as.hasCompareToSourceId() ? fixtureMgr.get(as.getCompareToSourceId()) : fixtureMgr.get(fixtureMgr.getLastOp());
        return sourceFixture;
    }

    void run(TestScript.SetupActionAssertComponent as, TestReport.SetupActionAssertComponent assertReport) {
        Objects.requireNonNull(typePrefix);
        Objects.requireNonNull(val);
        Objects.requireNonNull(testScript);
        Objects.requireNonNull(testEngine);

        boolean reported = false;

        currentAssert = as;

        FixtureComponent source = getSourceIfAvailable();


        if (source != null && "Bundle".equals(source.getResponseType())) {
            if (as.hasExpression() && !as.getExpression().trim().startsWith("Bundle")) {
                // assertion could be targeting Bundle or a resource in the Bundle (search)
                if (as.hasExpression() && as.getExpression().trim().startsWith("Bundle")) {
                    BaseResource sourceResource = source.getResourceResource();
                    if (!(sourceResource instanceof Bundle)) {
                        Reporter.reportError(this, "expression targets a Bundle but sourceId points to a " + sourceResource.getClass().getSimpleName());
                        return;
                    }
                    Bundle bundle = (Bundle) sourceResource;
                    for (Bundle.BundleEntryComponent component : bundle.getEntry()) {
                        if (component.hasResource()) {
                            sourceOverride = new UnregisteredFixtureComponent(component.getResource());
                            reported = true;
                            if (run2(as, assertReport)) {
                                return;  // good enough to find one component that matches assertion
                            }
                        }
                    }
                    Reporter.reportError(this, "no resource in Bundle matches assertion.");
                    return;
                } else {
                    sourceOverride = null;
                    run2(as, assertReport);
                    return;
                }
            } else if (as.hasModifierExtension()) {
                Extension extension = as.getModifierExtension().isEmpty() ? null : as.getModifierExtension().get(0);
                if (extension == null) {
                    Reporter.reportError(this, "Found no stringValue in modifierExtension.");
                    return;
                }
                String resourceType = extension.getValue().toString();
                BaseResource sourceResource = source.getResourceResource();
                Bundle bundle = (Bundle) sourceResource;
                int i=0;
                for (Bundle.BundleEntryComponent component : bundle.getEntry()) {
                    Resource resource = component.getResource();
                    if (resource.getClass().getSimpleName().equals(resourceType)) {
                        ResourceWrapper savedSource = source.getResourceWrapper();  // new value (from Bundle) for FixtureComponent
                        source.setResource(new ResourceWrapper(resource));  // so it applies to the in-bundle resource
                        source.setId(resourceType + " #" + i);
                        reported = true;
                        boolean success = run2(as, assertReport);
                        source.setResource(savedSource);  // restore FixtureComponent
                        if (!success)
                            break;
                    }
                }
                if (reported)
                    return;
                Reporter.reportError(this, "Found no " + resourceType + " resources in Bundle.");
            }
        } else {
            run2(as, assertReport);
            return;
        }

        run2(as, assertReport);
    }

    TestScript.SetupActionAssertComponent currentAssert;
    TestReport.SetupActionAssertComponent currentAssertReport;

    public TestScript.SetupActionAssertComponent getCurrentAssert() {
        return currentAssert;
    }

    public TestReport.SetupActionAssertComponent getCurrentAssertReport() {
        return currentAssertReport;
    }

    boolean warningOnly;

    public boolean getWarningOnly() {
        return warningOnly;
    }

    // success?
    boolean run2(TestScript.SetupActionAssertComponent as, TestReport.SetupActionAssertComponent assertReport) {
        Objects.requireNonNull(variableMgr);
        currentAssert = as;
        currentAssertReport = assertReport;

        if (as.hasId())
            assertReport.setId(as.getId());
        assertReport.setResult(TestReport.TestReportActionResult.PASS);  // may be overwritten

        FixtureComponent source = getSourceIfAvailable();
        if (source == null)
            source = getCompareToSourceIfAvailable(as);

        try {
            label = as.getLabel();
            type = typePrefix + ".assert";

            if (as.hasWarningOnly())
                warningOnly = as.getWarningOnly();
            else {
                Reporter.reportError(this, "warningOnly is required but missing");
                return false;
            }

            if (as.hasCompareToSourcePath() && as.hasCompareToSourceExpression()) {
                Reporter.reportError(this, "has both compareToSourcePath and compareToSourceExpression");
                return false;
            }

            if (as.hasCompareToSourceId()) {
                if (!as.hasCompareToSourceExpression() && !as.hasCompareToSourcePath()) {
                    Reporter.reportError(this, "has compareToSourceId and nether compareToSourcePath nor compareToSourceExpression are defined");
                    return false;
                }
            }

//        INSTRUCTIONS

            if (as.hasCompareToSourceId() && as.hasCompareToSourceExpression())
                return SourceIdSourceExpressionAssertion.run(this);

            if (as.hasMinimumId()) return MinimumIdAssertion.run(this);

            // resource type pattern
            // resource type specified sourceId or lastOperation must have returned that resource type
            if (as.hasResource()) return ResourceAssertion.run(this);

            // contentType pattern
            // compares against sourceId or return of lastOperation
            if (as.hasContentType()) return ContentTypeAssertion.run(this);

            // headerField/value comparison pattern
            // compares against sourceId or return of lastOperation
            if (as.hasHeaderField() && as.hasValue()) return HeaderFieldAssertion.run(this);

            // response pattern
            if (as.hasResponse()) return ResponseAssertion.run(this);

            // responseCodePattern
            if (as.hasResponseCode()) return ResponseCodeAssertion.run(this);

            // expression and value pattern
            if (as.hasExpression() && as.hasValue()) return ExpressionValueAssertion.run(this);

            // expression
            if (as.hasExpression()) return ExpressionAssertion.run(this);

            if (as.hasRequestMethod()) return RequestMethodAssertion.run(this);
            
            if (as.hasValidateProfileId()) return ValidateProfileAssertion.run(this);

        } catch (Exception ex) {
            log.severe("AssertionRunner: " + ex.toString());
        } finally {
            // add context to report
            testEngine.reportAssertion(new Reporter(val, assertReport, "", ""), as, source);
        }
        Reporter.reportError(this, "No assertion or missing assertion fixture dependency (a fixture is required to evaluate an assertion).");
        return false;
    }

    //static private List<String> hide = Arrays.asList("Description", "Id", "Meta", "Text");

    static public final String EVALUATING_TYPE = "Evaluating type";
    static public final String SCRIPT = "Script";
    static public final String RAW_REPORT = "Raw Report";

    private boolean hasNoFixtureResource(FixtureComponent fixture, TestReport.SetupActionAssertComponent assertReport, String msg) {
        if (fixture == null) {
            Reporter.reportError(this, msg + " - no fixture is referenced.");
            return true;
        }
        if (!fixture.hasResource()) {
            Reporter.reportError(this, msg + "referenced fixture " + fixture.getId() + " has no response.");
            return true;
        }
        return false;
    }

    public static boolean compare(AssertionContext ctx, String found, String expected, String operator) {
        if (found == null)
            return Reporter.reportFail(ctx,"Operator is " + operator + "\nexpected is " + expected + "\nfound is " + found);
        if (operator.equals("equals"))
            return Reporter.report(ctx, found.equals(expected),"Operator is " + operator + "\nexpected is " + expected + "\nfound is " + found);
        if (operator.equals("notEquals"))
            return Reporter.report(ctx, !found.equals(expected),"Operator is " + operator + "\nexpected is " + expected + "\nfound is " + found);
        if (operator.equals("in")) {
            String[] values = expected.split(",");
            for (String value : values) {
                if (value.equals(found))
                    return Reporter.report(ctx, true, "Operator is " + operator + "\nexpected is " + expected + "\nfound is " + found);
            }
            return Reporter.report(ctx, false, "Operator is " + operator + "\nexpected is " + expected + "\nfound is " + found);
        }
        if (operator.equals("notIn")) {
            String[] values = expected.split(",");
            for (String value : values) {
                if (value.equals(found))
                    return Reporter.report(ctx, false, "Operator is " + operator + "\nexpected is " + expected + "\nfound is " + found);
            }
            return Reporter.report(ctx, true, "Operator is " + operator);
        }
        if (operator.equals("greaterThan")) {
            int iExpected = Integer.parseInt(expected);
            int iFound = Integer.parseInt(found);
            return Reporter.report(ctx, iFound > iExpected,"Operator is " + operator + "\nexpected is " + expected + "\nfound is " + found);
        }
        if (operator.equals("lessThan")) {
            int iExpected = Integer.parseInt(expected);
            int iFound = Integer.parseInt(found);
            return Reporter.report(ctx, iFound < iExpected, "Operator is " + operator + "\nexpected is " + expected + "\nfound is " + found);
        }
        if (operator.equals("empty")) {
            return Reporter.report(ctx, "".equals(found), "Operator is " + operator + "\nexpected is " + expected + "\nfound is " + found);
        }
        if (operator.equals("notEmpty")) {
            return Reporter.report(ctx, !"".equals(found), "Operator is " + operator + "\nexpected is " + expected + "\nfound is " + found);
        }
        if (operator.equals("contains")) {
            return Reporter.report(ctx, found.contains(expected), "Operator is " + operator + "\nexpected is " + expected + "\nfound is " + found);
        }
        if (operator.equals("notContains")) {
            return Reporter.report(ctx, !found.contains(expected), "Operator is " + operator + "\nexpected is " + expected + "\nfound is " + found);
        }
        return Reporter.report(ctx, false, "Do not understand operator " + operator + "\nexpected is " + expected + "\nfound is " + found);
    }

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

    public AssertionRunner setTestCollectionId(String testCollectionId) {
        this.testCollectionId = testCollectionId;
        return this;
    }

    public AssertionRunner setTestId(String testId) {
        this.testId = testId;
        return this;
    }

    public AssertionRunner setTestEngine(TestEngine testEngine) {
        this.testEngine = testEngine;
        return this;
    }

    public AssertionRunner setTestReport(TestReport testReport) {
        this.testReport = testReport;
        return this;
    }

    public TestDef getTestDef() {
        return testEngine;
    }

    public ValE getVal() {
        return val;
    }

    public String getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public FixtureLabels getFixtureLabels() {
        FixtureComponent sourceFixture = getSource();
        if (sourceFixture == null)
            return null;
        if (sourceFixture.getResponseType() == null) {
            Reporter.reportError(this, "sourceId or lastOperation references no resource.");
            return null;
        }

        ResourceWrapper wrapper = sourceFixture.getResourceWrapper();
        FixtureLabels.Source source = wrapper.isRequest() ? FixtureLabels.Source.REQUEST : FixtureLabels.Source.RESPONSE;

        return new FixtureLabels(
                getTestDef(),
                sourceFixture,
                source);
    }

    public FixtureLabels getCompareToFixtureLabels() {
        FixtureComponent sourceFixture = getCompareToSource();
        if (sourceFixture == null)
            return null;
        if (sourceFixture.getResponseType() == null) {
            Reporter.reportError(this, "compareToSourceId or lastOperation references no resource.");
            return null;
        }

        return new FixtureLabels(
                getTestDef(),
                getCompareToSource(),
                FixtureLabels.Source.REQUEST);
    }

    @Override
    public VariableMgr getVariableMgr() {
        return variableMgr;
    }

    @Override
    public FixtureMgr getFixtureMgr() {
        return fixtureMgr;
    }

    @Override
    public String getProfile(String id) {
        Reference profile = testScript.getProfile().stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
        return profile != null ? profile.getReference() : null;
    }

    @Override
    public TestReport getTestReport() {
        return this.testReport;
    }

}
