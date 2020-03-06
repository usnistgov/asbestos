package gov.nist.asbestos.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Report {
    String source = null;
    RelatedReport base = null;
    List<RelatedReport> objects = new ArrayList<>();
    List<String> errors;
    List<String> warnings;
    String baseObjectEventId = null;
    String baseObjectResourceType = null;

    public Report() {}

    public Report(String error) {
        errors = Collections.singletonList(error);
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public RelatedReport getBase() {
        return base;
    }

    public List<RelatedReport> getObjects() {
        return objects;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("Report");
        for (RelatedReport rr : objects) {
            buf.append("\n  ").append(rr.toString());
        }
        return buf.toString();
    }
}
