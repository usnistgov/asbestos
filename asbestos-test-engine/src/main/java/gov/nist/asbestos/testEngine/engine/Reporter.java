package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.TestReport;

class Reporter {


    private final ValE val;
    private final TestReport.SetupActionOperationComponent opReport;
    private final String type;
    private final String label;

    Reporter(ValE val, TestReport.SetupActionOperationComponent opReport, String type, String label) {
        this.val = val;
        this.opReport = opReport;
        this.type = type;
        this.label = label;
    }

    void reportError(String msg) {
        String theMsg = formatMsg(type, label, msg);
        val.add(new ValE(theMsg).asError());
        opReport.setResult(TestReport.TestReportActionResult.ERROR);
        opReport.setMessage(theMsg);
    }

    void report(String msg) {
        String theMsg = formatMsg(type, label, msg);
        String existing = opReport.getMessage();
        opReport.setMessage(existing == null ? theMsg : existing + "\n" + theMsg);
    }

    void report(String msg, String link) {
        if (link != null) {
            msg = "loglink=" + link + " " + msg;
        }
        String theMsg = formatMsg(type, label, msg);
        String existing = opReport.getMessage();
        opReport.setMessage(existing == null ? theMsg : existing + "\n" + theMsg);
    }

    static String formatMsg(String type, String id, String msg) {
        return type + " : " + id + " : " + msg;
    }

    static TestReport.SetupActionOperationComponent reportError(ValE val, TestReport.SetupActionOperationComponent opReport, TestReport.SetupActionAssertComponent assertReport, String type, String id, String msg) {
        assert assertReport != null || opReport != null;
        if (assertReport != null)
            reportError(val, assertReport, type, id, msg);
        else
            reportError(val, opReport, type, id, msg);
        return opReport;
    }

    static TestReport.SetupActionOperationComponent reportError(ValE val, TestReport.SetupActionOperationComponent opReport, TestReport.SetupActionAssertComponent assertReport, String type, String id, String msg, String link) {
        assert assertReport != null || opReport != null;
        if (link != null) {
            msg = "loglink=" + link + " " + msg;
        }
        if (assertReport != null)
            reportError(val, assertReport, type, id, msg);
        else
            reportError(val, opReport, type, id, msg);
        return opReport;
    }


    static TestReport.SetupActionOperationComponent reportError(ValE val, TestReport.SetupActionOperationComponent opReport, String type, String id, String msg) {
        String theMsg = formatMsg(type, id, msg);
        val.add(new ValE(theMsg).asError());
        opReport.setResult(TestReport.TestReportActionResult.ERROR);
        opReport.setMessage(theMsg);
        return opReport;
    }

    static TestReport.SetupActionAssertComponent reportError(ValE val, TestReport.SetupActionAssertComponent assertReport, String type, String id, String msg) {
        String theMsg = formatMsg(type, id, msg);
        val.add(new ValE(theMsg).asError());
        assertReport.setResult(TestReport.TestReportActionResult.ERROR);
        assertReport.setMessage(theMsg);
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
        assertReport.setMessage(theMsg);
        return false;
    }

    static boolean reportPass(ValE val, TestReport.SetupActionAssertComponent assertReport, String type, String id, String msg) {
        String theMsg = formatMsg(type, id, msg);
        val.add(new ValE(theMsg));
        assertReport.setResult(TestReport.TestReportActionResult.PASS);
        assertReport.setMessage(theMsg);
        return true;
    }

}
