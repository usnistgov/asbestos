package gov.nist.asbestos.testEngine.engine.assertion;

import java.util.ArrayList;
import java.util.List;

public class Report {
    public List<String> missing = new ArrayList<>();
    public List<String> expected = new ArrayList<>();
    public List<String> errors = new ArrayList<>();


    public Report() {

    }

    Report(String error) {
        errors.add(error);
    }


    void addAll(Report report) {
        missing.addAll(report.missing);
        expected.addAll(report.expected);
    }
}
