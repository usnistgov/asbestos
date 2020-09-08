package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.simapi.validation.ValE;
import gov.nist.asbestos.testEngine.engine.assertion.AssertionContext;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureComponent;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.TestReport;

import java.util.Objects;

public class Reporter {

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

    public static boolean reportFail(AssertionContext ctx, String msg) {
        boolean warningOnly = ctx.getWarningOnly();
        String theMsg = formatMsg(ctx.getType(), ctx.getLabel(), msg);
        ctx.getVal().add(new ValE(theMsg).asError());
        ctx.getCurrentAssertReport().setResult(warningOnly? TestReport.TestReportActionResult.WARNING : TestReport.TestReportActionResult.FAIL);
        String existing = ctx.getCurrentAssertReport().getMessage();
        ctx.getCurrentAssertReport().setMessage(existing == null ? (debug ? theMsg : msg) : existing + "\n" + (debug ? theMsg : msg));
        return false;
    }

    public void report(String msg) {
        String theMsg = formatMsg(type, label, msg);
        String existing = report.getMessage();
        report.setMessage(existing == null ? (debug ? theMsg : msg) : existing + "\n" + (debug ? theMsg : msg));
    }

    public void report(String msg, ResourceWrapper wrapper) {
        if (wrapper != null) {
            report.setDetail(wrapper.logLink());
        }
        report.setActionContext(msg);
    }

    public static boolean report(AssertionContext ctx, boolean ok, String msg) {
        if (ok)
            reportPass(ctx, msg);
        else
            reportFail(ctx, msg);
        return ok;
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

    public static TestReport.SetupActionAssertComponent reportError(AssertionContext ctx, String msg) {
        String theMsg = formatMsg(ctx.getType(), ctx.getLabel(), msg);
        ctx.getVal().add(new ValE(theMsg).asError());
        ctx.getCurrentAssertReport().setResult(TestReport.TestReportActionResult.ERROR);
        String existing = ctx.getCurrentAssertReport().getMessage();
        ctx.getCurrentAssertReport().setMessage(existing == null ? (debug ? theMsg : msg) : existing + "\n" + (debug ? theMsg : msg));
        //assertReport.setMessage(theMsg);
        return ctx.getCurrentAssertReport();
    }

    public static boolean reportPass(AssertionContext ctx, String msg) {
        String theMsg = formatMsg(ctx.getType(), ctx.getLabel(), msg);
        ctx.getVal().add(new ValE(theMsg));
        ctx.getCurrentAssertReport().setResult(TestReport.TestReportActionResult.PASS);
        String existing = ctx.getCurrentAssertReport().getMessage();
        ctx.getCurrentAssertReport().setMessage(existing == null ? (debug ? theMsg : msg) : existing + "\n" + (debug ? theMsg : msg));
        return true;
    }

    public static void assertDescription(TestReport.SetupActionAssertComponent assertReport, String description) {
        assertReport.addExtension("urn:resultDescription", new StringType(description));
    }

    public static void operationDescription(TestReport.SetupActionOperationComponent  opComponent, String description) {
        opComponent.addExtension("urn:resultDescription", new StringType(description));
    }

}
