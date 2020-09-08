package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.simapi.tk.stubs.UUIDFactory;
import gov.nist.asbestos.simapi.validation.ValE;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureComponent;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureMgr;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

import java.util.*;

public class VariableMgr {
    private final TestScript testScript;
    private final FixtureMgr fixtureMgr;  // variables reference fixtures so this is needed
    private ValE val;
    //private TestReport.SetupActionOperationComponent opReport;
    private Reporter reporter;
    private Map<String, String> externalVariables = new HashMap<>();  // passed by module call

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
        TestScript.TestScriptVariableComponent theVar = null;
        for(TestScript.TestScriptVariableComponent comp : testScript.getVariable()) {
            if (comp.hasName() && name.equals(comp.getName())) {
                if (theVar != null) {
                    String msg = "variable " + name + " is defined multiple times";
                    reporter.reportError(msg);
                }
                theVar = comp;
            }
        }
        return theVar;
    }

    private List<String> getVariableNames() {
        List<String> names = new ArrayList<>();
        for(TestScript.TestScriptVariableComponent comp : testScript.getVariable()) {
            if (comp.hasName()) {
                String theName = comp.getName();
                if (names.contains(theName)) {
                    String msg = "variable " + theName + " is defined multiple times";
                    reporter.reportError(msg);
                }
                names.add(comp.getName());
            }
        }
        return names;
    }

    public Map<String, String> getVariables() {
        return getVariables(true);
    }

    public Map<String, String> getVariables(boolean errorAsValue) {
        Map<String, String> vars = new HashMap<>();
        for (String name : getVariableNames()) {
            String value = eval(name, errorAsValue);
            vars.put(name, value);
        }
        return vars;
    }

    private boolean containsVariable(String reference) {
        if (reference == null)
            return false;
        return reference.contains("${");
    }

    private int variableCount(String reference) {
        if (reference == null)
            return 0;
        int count = 0;
        int pos = 0;
        while (pos != -1) {
            pos = reference.indexOf("${", pos);
            if (pos != -1) {
                count++;
                pos += 2;
            }
        }
        return count;
    }

    public String updateReference(String reference) {
        if (reference == null)
            return null;
        int variableCount = variableCount(reference);
        for (int i=0; i<50; i++) {
            if (!containsVariable(reference))
                return reference;
            reference = updateReference1(reference);
            int variableCount2 = variableCount(reference);
            if (variableCount2 == variableCount) // stuck
                break;
        }
        Variable var = getNextVariable(reference);
        if (var != null) {
            reporter.reportError("variable " + var.name + " cannot be resolved");
            //throw new Error("variable " + var.name + " cannot be resolved");
        }
        return null;
    }

    class Variable {
        String name;
        int from;
        int to;
        String reference;
    }

    private Variable getNextVariable(String reference) {
        if (reference == null)
            return null;
        if (!reference.contains(("${")))
            return null;
        int from = reference.indexOf("${");
        int to = reference.indexOf("}", from);
        if (to == -1) {
            reporter.reportError("reference " + reference + " has no closing }");
            throw new Error("reference " + reference + " has no closing }");
        }
        String varName = reference.substring(from+2, to);
        Variable var = new Variable();
        var.name = varName;
        var.from = from;
        var.to = to;
        var.reference = reference;
        return var;
    }

    private String updateReference1(String reference) {
        Objects.requireNonNull(reference);
        Objects.requireNonNull(reporter);
        if (!reference.contains(("${")))
            return reference;
        Variable var  = getNextVariable(reference); //reference.substring(from+2, to);
        String update = eval(var.name, false);
        if (update == null)
            return reference;
        return reference.substring(0, var.from) + update + reference.substring(var.to+1);
    }

    String eval(String variableName, boolean errorAsValue) {
        Objects.requireNonNull(reporter);
        if (externalVariables.containsKey(variableName))
            return externalVariables.get(variableName);
        TestScript.TestScriptVariableComponent var = getVariable(variableName);
        if (var == null) {
            String error = "Variable " + variableName + " is referenced but not defined";
            if (errorAsValue)
                return error;
            reporter.reportError(error);
            return null;
        }

        // special feature to generate unique UUIDs
        if (var.hasSourceId() && "GENERATEUUID".equals(var.getSourceId())) {
            String newUUID = "urn:uuid:" + UUIDFactory.getInstance().newUUID().toString();
            var.setSourceId(null);
            var.setDefaultValue(newUUID);
        }

        if (var.hasDefaultValue()) {
            String value =  var.getDefaultValue();
            value = updateReference(value);
            return value;
        }

        String sourceId = null;
        if (var.hasSourceId()) {
            sourceId = var.getSourceId();
        } else if (!var.hasDefaultValue()){
            String error = "Variable " + variableName + " does not have a source and does not define a defaultValue";
            if (errorAsValue)
                return error;
            reporter.reportError(error);
            return null;
        }


        if (!fixtureMgr.containsKey(sourceId)) {
            String error = "Variable " + variableName + " references source " + sourceId + " which does not exist";
            if (errorAsValue)
                return error;
            reporter.reportError(error);
            return null;
        }
        FixtureComponent fixture = fixtureMgr.get(sourceId);

        if (var.hasHeaderField()) {
            if (!fixture.hasHttpBase()) {
                String error = "Variable " + variableName + " source " + sourceId + " does not have a HTTP header behind it";
                if (errorAsValue)
                    return error;
                reporter.reportError(error);
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
        } else if (var.hasPath()) {
            String error = "Variable " + variableName + " path not supported";
            if (errorAsValue)
                return error;
            reporter.reportError(error);
            return null;
        } else {
            String error = "Variable " + variableName + " does not define one of headerField, expression, path, defaultValue";
            if (errorAsValue)
                return error;
            reporter.reportError(error);
            return null;
        }
    }

    public VariableMgr setVal(ValE val) {
        this.val = val;
        return this;
    }

    VariableMgr setOpReport(TestReport.SetupActionOperationComponent opReport) {
        Objects.requireNonNull(opReport);
        Objects.requireNonNull(val);
        //this.opReport = opReport;
        reporter = new Reporter(val, opReport, "", "");
        return this;
    }

    VariableMgr setOpReport(TestReport.SetupActionAssertComponent asReport) {
        Objects.requireNonNull(asReport);
        Objects.requireNonNull(val);
        //this.opReport = opReport;
        reporter = new Reporter(val, asReport, "", "");
        return this;
    }


    public VariableMgr setExternalVariables(Map<String, String> variables) {
        this.externalVariables = variables;
        return this;
    }

    public boolean hasReporter() {
        return reporter != null;
    }

    public VariableMgr setReporter(Reporter reporter) {
        this.reporter = reporter;
        return this;
    }
}
