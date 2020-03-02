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
}
