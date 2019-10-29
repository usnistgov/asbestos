package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

import java.util.Objects;

public class VariableMgr {
    private TestScript testScript;
    private FixtureMgr fixtureMgr;
    private ValE val;
    private TestReport.SetupActionOperationComponent opReport;
    private Reporter reporter;

    VariableMgr(TestScript testScript, FixtureMgr fixtureMgr) {
        Objects.requireNonNull(testScript);
        Objects.requireNonNull(fixtureMgr);
        this.testScript = testScript;
        this.fixtureMgr = fixtureMgr;
    }

    boolean hasVariable(String name) {
        Objects.requireNonNull(name);
        for(TestScript.TestScriptVariableComponent comp : testScript.getVariable()) {
            if (comp.hasName() && name.equals(comp.getName()))
                return true;
        }
        return false;
    }

    private TestScript.TestScriptVariableComponent getVariable(String name) {
        Objects.requireNonNull(name);
        for(TestScript.TestScriptVariableComponent comp : testScript.getVariable()) {
            if (comp.hasName() && name.equals(comp.getName()))
                return comp;
        }
        return null;

    }

    private boolean containsVariable(String reference) {
        if (reference == null)
            return false;
        return reference.contains("${");
    }

    String updateReference(String reference) {
        for (int i=0; i<50; i++) {
            if (!containsVariable(reference))
                return reference;
            reference = updateReference1(reference);
        }
        return null;
    }

    private String updateReference1(String reference) {
        Objects.requireNonNull(reference);
        Objects.requireNonNull(reporter);
        if (!reference.contains(("${")))
            return reference;
        int from = reference.indexOf("${");
        int to = reference.indexOf("}");
        if (to == -1) {
            reporter.reportError("reference " + reference + " has no closing }");
            throw new Error("reference " + reference + " has no closing }");
        }
        String varName = reference.substring(from+2, to);
        String update = eval(varName);
        if (update == null)
            return null;
        return reference.substring(0, from) + update + reference.substring(to+1);
    }

    private String eval(String variableName) {
        TestScript.TestScriptVariableComponent var = getVariable(variableName);
        if (var == null) {
            reporter.reportError( "Variable " + variableName + " is references but not defined");
            return null;
        }
        String sourceId = null;
        if (var.hasSourceId()) {
            sourceId = var.getSourceId();
        } else if (!var.hasDefaultValue()){
            reporter.reportError("Variable " + variableName + " does not have a sourceId and does not define a defaultValue");
            return null;
        }
        if (!fixtureMgr.containsKey(sourceId)) {
            reporter.reportError( "Variable " + variableName + " references sourceId " + sourceId + " which does  not exist");
            return null;
        }
        FixtureComponent fixture = fixtureMgr.get(sourceId);

        if (var.hasHeaderField()) {
            if (!fixture.hasHttpBase()) {
                reporter.reportError( "Variable " + variableName + " sourceId " + sourceId + " does not have a HTTP header behind it");
                return null;
            }
            HttpBase base = fixture.getHttpBase();
            if (var.getHeaderField().equals("Location") && base instanceof HttpPost) {
                HttpPost poster = (HttpPost) base;
                return poster.getLocationHeader().getValue();
            }
            Headers responseHeaders = base.getResponseHeaders();
            if (responseHeaders == null)
                responseHeaders = new Headers();
            return responseHeaders.getValue(var.getHeaderField());
        } else if (var.hasExpression()) {
            return FhirPathEngineBuilder.evalForString(fixture.getResourceResource(), var.getExpression());
        } else if (var.hasDefaultValue()) {
            return var.getDefaultValue();
        } else if (var.hasPath()) {
            reporter.reportError("Variable " + variableName + " path not supported");
            return null;
        } else {
            reporter.reportError( "Variable " + variableName + " does not define one of headerField, expression, path, defaultValue");
            return null;
        }
    }

    public VariableMgr setVal(ValE val) {
        this.val = val;
        return this;
    }

    VariableMgr setOpReport(TestReport.SetupActionOperationComponent opReport) {
        Objects.requireNonNull(opReport);
        this.opReport = opReport;
        reporter = new Reporter(val, opReport, "", "");
        return this;
    }

}
