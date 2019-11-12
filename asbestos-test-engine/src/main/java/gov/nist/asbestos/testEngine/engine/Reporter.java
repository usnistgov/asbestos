package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.TestReport;

import java.util.Objects;

class Reporter {


    private final ValE val;
    private final TestReport.SetupActionOperationComponent opReport;
    private final String type;
    private final String label;
    private static boolean debug = false;

    Reporter(ValE val, TestReport.SetupActionOperationComponent opReport, String type, String label) {
        Objects.requireNonNull(val);
        this.val = val;
        this.opReport = opReport;
        this.type = type;
        this.label = label;
    }

    void reportError(String msg, ResourceWrapper wrapper) {
        if (wrapper != null)
            opReport.setDetail(wrapper.logLink());
        reportError(msg);
    }

    void reportError(String msg) {
        String theMsg = formatMsg(type, label, msg);
        val.add(new ValE(theMsg).asError());
        opReport.setResult(TestReport.TestReportActionResult.ERROR);
        String existing = opReport.getMessage();
        opReport.setMessage(existing == null ? (debug ? theMsg : msg) : existing + "\n" + (debug ? theMsg : msg));

        //throw new RuntimeException("Internal Error");
    }

    void reportFail(String msg, ResourceWrapper wrapper) {
        if (wrapper != null)
            opReport.setDetail(wrapper.logLink());
        reportFail(msg);
    }

    void reportFail(String msg) {
        String theMsg = formatMsg(type, label, msg);
        val.add(new ValE(theMsg).asError());
        opReport.setResult(TestReport.TestReportActionResult.FAIL);
        opReport.setMessage(debug ? theMsg : msg);
    }

    void report(String msg) {
        String theMsg = formatMsg(type, label, msg);
        String existing = opReport.getMessage();
        opReport.setMessage(existing == null ? (debug ? theMsg : msg) : existing + "\n" + (debug ? theMsg : msg));
    }

    void report(String msg, ResourceWrapper wrapper) {
        if (wrapper != null)
            opReport.setDetail(wrapper.logLink());
        report(msg);
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

}
