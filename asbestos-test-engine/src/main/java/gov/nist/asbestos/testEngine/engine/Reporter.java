package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.TestReport;

import java.util.Objects;

class Reporter {

    private static class Report {
        private TestReport.SetupActionOperationComponent opReport = null;
        private TestReport.SetupActionAssertComponent asReport = null;

        Report(TestReport.SetupActionOperationComponent opReport) {
            this.opReport = opReport;
        }
        Report(TestReport.SetupActionAssertComponent asReport) {
            this.asReport = asReport;
        }
        void setDetail(String s) {
            if (opReport == null) asReport.setDetail(s);
            else opReport.setDetail(s);
        }
        void setMessage(String s) {
            if (opReport == null) asReport.setMessage(s);
            else {
                opReport.setMessage(s);
            }
        }
        void setActionContext(String s) {
            Extension extension = opReport == null ? asReport.addExtension() : opReport.addExtension();
            extension.setUrl("urn:action-context");
            extension.setValue(new StringType(s));
        }
        void setModuleActionContext(String s) {
            Extension extension = opReport == null ? asReport.addExtension() : opReport.addExtension();
            extension.setUrl("urn:module-action-context");
            extension.setValue(new StringType(s));
        }
        String getMessage() {
            if (opReport == null) return asReport.getMessage();
            return opReport.getMessage();
        }
        void setResult(TestReport.TestReportActionResult result) {
            if (opReport == null) asReport.setResult(result);
            else opReport.setResult(result);
        }
    }

    private final ValE val;
    private final Report report;
    private final String type;
    private final String label;
    private static boolean debug = false;

    Reporter(ValE val, TestReport.SetupActionOperationComponent opReport, String type, String label) {
        Objects.requireNonNull(val);
        this.val = val;
        this.report = new Report(opReport);
        this.type = type;
        this.label = label;
    }

    Reporter(ValE val, TestReport.SetupActionAssertComponent asReport, String type, String label) {
        Objects.requireNonNull(val);
        this.val = val;
        this.report = new Report(asReport);
        this.type = type;
        this.label = label;
    }

    TestReport.SetupActionOperationComponent getOpReport() {
        return report.opReport;
    }

    void reportError(String msg, ResourceWrapper wrapper) {
        if (wrapper != null)
            report.setDetail(wrapper.logLink());
        reportError(msg);
    }

    void reportError(String msg) {
        String theMsg = formatMsg(type, label, msg);
        val.add(new ValE(theMsg).asError());
        report.setResult(TestReport.TestReportActionResult.ERROR);
        String existing = report.getMessage();
        report.setMessage(existing == null ? (debug ? theMsg : msg) : existing + "\n" + (debug ? theMsg : msg));

        //throw new RuntimeException("Internal Error");
    }

    void reportFail(String msg, ResourceWrapper wrapper) {
        if (wrapper != null)
            report.setDetail(wrapper.logLink());
        reportFail(msg);
    }

    private void reportFail(String msg) {
        String theMsg = formatMsg(type, label, msg);
        val.add(new ValE(theMsg).asError());
        report.setResult(TestReport.TestReportActionResult.FAIL);
        report.setMessage(debug ? theMsg : msg);
    }

    private void report(String msg) {
        String theMsg = formatMsg(type, label, msg);
        String existing = report.getMessage();
        report.setMessage(existing == null ? (debug ? theMsg : msg) : existing + "\n" + (debug ? theMsg : msg));
    }

    void report(String msg, ResourceWrapper wrapper) {
        if (wrapper != null) {
            report.setDetail(wrapper.logLink());
        }
        report.setActionContext(msg);
    }

    void setActionContext(String msg, ResourceWrapper wrapper) {
        if (wrapper != null) {
            report.setDetail(wrapper.logLink());
        }
        report.setActionContext(msg);
    }

    void setModuleActionContext(String msg, ResourceWrapper wrapper) {
        if (wrapper != null) {
            report.setDetail(wrapper.logLink());
        }
        report.setModuleActionContext(msg);
    }

    static String formatMsg(String type, String id, String msg) {
        return type + " : " + id + " : " + msg;
    }

    static TestReport.SetupActionAssertComponent reportError(ValE val, TestReport.SetupActionAssertComponent assertReport, String type, String id, String msg) {
        String theMsg = formatMsg(type, id, msg);
        val.add(new ValE(theMsg).asError());
        assertReport.setResult(TestReport.TestReportActionResult.ERROR);
        String existing = assertReport.getMessage();
        assertReport.setMessage(existing == null ? (debug ? theMsg : msg) : existing + "\n" + (debug ? theMsg : msg));
        //assertReport.setMessage(theMsg);
        return assertReport;
    }

    static boolean report(boolean ok, ValE val, TestReport.SetupActionAssertComponent assertReport, String type, String id, String msg, boolean warningOnly) {
        if (ok)
            reportPass(val, assertReport, type, id, msg);
        else
            reportFail(val, assertReport, type, id, msg, warningOnly);
        return ok;
    }

    static boolean reportFail(ValE val, TestReport.SetupActionAssertComponent assertReport, String type, String id, String msg, boolean warningOnly) {
        String theMsg = formatMsg(type, id, msg);
        val.add(new ValE(theMsg).asError());
        assertReport.setResult(warningOnly? TestReport.TestReportActionResult.WARNING : TestReport.TestReportActionResult.FAIL);
        String existing = assertReport.getMessage();
        assertReport.setMessage(existing == null ? (debug ? theMsg : msg) : existing + "\n" + (debug ? theMsg : msg));
        return false;
    }

    static boolean reportPass(ValE val, TestReport.SetupActionAssertComponent assertReport, String type, String id, String msg) {
        String theMsg = formatMsg(type, id, msg);
        val.add(new ValE(theMsg));
        assertReport.setResult(TestReport.TestReportActionResult.PASS);
        String existing = assertReport.getMessage();
        assertReport.setMessage(existing == null ? (debug ? theMsg : msg) : existing + "\n" + (debug ? theMsg : msg));
        return true;
    }

    static void assertDescription(TestReport.SetupActionAssertComponent assertReport, String description) {
        assertReport.addExtension("urn:resultDescription", new StringType(description));
    }

}
