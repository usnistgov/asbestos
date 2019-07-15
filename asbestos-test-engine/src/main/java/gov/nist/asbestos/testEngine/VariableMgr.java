package gov.nist.asbestos.testEngine;

import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

import java.util.Objects;

public class VariableMgr {
    private TestScript testScript;
    private FixtureMgr fixtureMgr;
    private ValE val;
    private TestReport.SetupActionOperationComponent opReport;
    private TestReport.SetupActionAssertComponent asReport;

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

    String updateReference(String reference) {
        Objects.requireNonNull(reference);
        assert opReport != null || asReport != null;
        if (!reference.contains(("${")))
            return reference;
        int from = reference.indexOf("${");
        int to = reference.indexOf("}");
        if (to == -1) {

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
            Reporter.reportError(val, opReport, asReport, "variable", variableName, "Variable " + variableName + " is references but not defined");
            return null;
        }
        String sourceId = null;
        if (var.hasSourceId()) {
            sourceId = var.getSourceId();
        } else if (!var.hasDefaultValue()){
            Reporter.reportError(val, opReport, asReport, "variable", variableName, "Variable " + variableName + " does not have a sourceId and does not define a defaultValue");
            return null;
        }
        if (var.hasSourceId() && !fixtureMgr.containsKey(sourceId)) {
            Reporter.reportError(val, opReport, asReport, "variable", variableName, "Variable " + variableName + " references sourceId " + sourceId + " which does  not exist");
            return null;
        }
        FixtureComponent fixture = fixtureMgr.get(sourceId);

        if (var.hasHeaderField()) {
            if (!fixture.hasHttpBase()) {
                Reporter.reportError(val, opReport, asReport, "variable", variableName, "Variable " + variableName + " sourceId " + sourceId + " does not have a HTTP header behind it");
                return null;
            }
            HttpBase base = fixture.getHttpBase();
            Headers responseHeaders = base.getResponseHeaders();
            return responseHeaders.getValue(var.getHeaderField());
        } else if (var.hasExpression()) {
            return FhirPathEngineBuilder.evalForString(fixture.getResourceResource(), var.getExpression());
        } else if (var.hasDefaultValue()) {
            return var.getDefaultValue();
        } else if (var.hasPath()) {
            Reporter.reportError(val, opReport, asReport, "variable", variableName, "Variable " + variableName + " path not supported");
            return null;
        } else {
            Reporter.reportError(val, opReport, asReport, "variable", variableName, "Variable " + variableName + " does not define one of headerField, expression, path, defaultValue");
            return null;
        }
    }

    public VariableMgr setVal(ValE val) {
        this.val = val;
        return this;
    }

    public VariableMgr setOpReport(TestReport.SetupActionOperationComponent opReport) {
        this.opReport = opReport;
        return this;
    }

    public VariableMgr setAsReport(TestReport.SetupActionAssertComponent asReport) {
        this.asReport = asReport;
        return this;
    }
}
