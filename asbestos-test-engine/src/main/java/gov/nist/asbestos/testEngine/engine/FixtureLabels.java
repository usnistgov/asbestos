package gov.nist.asbestos.testEngine.engine;

import com.google.common.base.Strings;
import org.hl7.fhir.r4.model.TestScript;

class FixtureLabels {
    private final ActionReporter actionReporter;
    boolean sourceId = false;
    boolean responseId = false;
    String rawReference;
    String referenceLabel;
    String label = null;
    String tail = "";

    FixtureLabels() {
        this.actionReporter = null;
    }

    FixtureLabels(ActionReporter actionReporter) {
        this.actionReporter = actionReporter;
    }

    FixtureLabels(ActionReporter actionReporter, TestScript.SetupActionOperationComponent op, String key) {
        this.actionReporter = actionReporter;
        if (key != null && actionReporter.getAssertionSource() != null && key.equals(actionReporter.getAssertionSource().getId())) {
            sourceId = true;
            label = "sourceId (" + key + ")";
        }
        if (key != null && op != null && op.hasResponseId() && key.equals(op.getResponseId())) {
            responseId = true;
            label = "responseId (" + key + ")";
        }
        if (key != null && op != null && op.hasSourceId() && key.equals(op.getSourceId())) {
            sourceId = true;
            label = "sourceId (" + key + ")";
        }
        if (sourceId)
            tail = "/req";
        else if (responseId)
            tail = "/resp";
        else if (key != null && key.equals("lastOperation")) {
            tail = "/resp";
            label = key;
        }

        if (label == null) {
            label = key;
            if (key.equals("request"))
                tail = "/req";
            if (key.equals("response"))
                tail = "/resp";
        }
    }

    FixtureLabels(ActionReporter actionReporter, TestScript.SetupActionAssertComponent op, String key) {
        this.actionReporter = actionReporter;
        if (key != null && actionReporter.getAssertionSource() != null && key.equals(actionReporter.getAssertionSource().getId())) {
            sourceId = true;
            label = "sourceId (" + key + ")";
        }
        if (key != null && op != null && op.hasSourceId() && key.equals(op.getSourceId())) {
            sourceId = true;
            label = "sourceId (" + key + ")";
        }
        if (sourceId)
            tail = "/req";
        else if (responseId)
            tail = "/resp";
        else if (key != null && key.equals("lastOperation")) {
            tail = "/resp";
            label = key;
        }

        if (label == null) {
            label = key;
            if (key.equals("request"))
                tail = "/req";
            if (key.equals("response"))
                tail = "/resp";
        }
    }

    String getReference() {
        String label = referenceLabel;
        if (Strings.isNullOrEmpty(label))
            label = rawReference;

        return "<a href=\"" + rawReference + "\"" + " target=\"_blank\">" +
                referenceLabel +
                "</a>";
    }

}
