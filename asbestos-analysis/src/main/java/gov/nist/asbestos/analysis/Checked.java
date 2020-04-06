package gov.nist.asbestos.analysis;

import gov.nist.asbestos.testEngine.engine.assertion.MinimumId;

public class Checked {
    String className;
    MinimumId.Report report;
    String script;

    Checked(String className, MinimumId.Report report, String script) {
        this.className = className;
        this.report = report;
        this.script = script;
    }

    Checked(MinimumId.Report report) {
        this.className = "";
        this.script = "";
        this.report = report;
    }

    public String toString() {
        return "Checked: " + className + " Script: " + script + " Atts: " + report.expected;
    }
}
