package gov.nist.asbestos.simapi.validation;


import org.hl7.fhir.r4.model.OperationOutcome;

import java.util.ArrayList;
import java.util.List;

// TODO order elements are added must be preserved
public class Val {
    List<ValE> elements = new ArrayList<>();

    public Val() {}

    public Val msg(String msg) {
        elements.add(new ValE(msg));
        return this;
    }

    public Val ref(String ref) {
        elements.add(new ValE(ref).asRef());
        return this;
    }

    public Val err(String err) {
        elements.add(new ValE(err).asError());
        return this;
    }

    // TODO need test
    public Val warn(String err) {
        elements.add(new ValE(err).asWarning());
        return this;
    }

    public Val frameworkDoc(String doc) {
        elements.add(new ValE(doc).asDoc());
        return this;
    }

    public Val add(ValE vale) {
        elements.add(vale);
        return this;
    }

    public boolean hasErrors() {
        for (ValE e : elements) {
            if (e.getTypes().contains(ValType.Error) && !e.getTypes().contains(ValType.Ignored))
                return true;
            if (!e.ele.isEmpty() && e.hasErrors())
                return true;
        }
        return false;
    }

    public List<ValE> getErrors() {
        List<ValE> errors = new ArrayList<>();
        for (ValE e : elements) {
            if (e.getTypes().contains(ValType.Error) && !e.getTypes().contains(ValType.Ignored))
                errors.add(e);
            if (!e.ele.isEmpty())
                errors.addAll(e.getErrors());
        }
        return errors;
    }

    public OperationOutcome errorsAsOperationOutcome() {
        OperationOutcome oo = new OperationOutcome();
        for (ValE vale : getErrors()) {
            addErrorToOperationOutcome(oo, vale.getMsg());
        }
        return oo;
    }

    private OperationOutcome addErrorToOperationOutcome(OperationOutcome oo, String msg) {
        OperationOutcome.OperationOutcomeIssueComponent issue = oo.addIssue();
        issue.setCode(OperationOutcome.IssueType.UNKNOWN);
        issue.setSeverity(OperationOutcome.IssueSeverity.ERROR);
        issue.setDiagnostics(msg);
        return oo;
    }


    public boolean hasWarnings() {
        for (ValE e : elements) {
            if (e.getTypes().contains(ValType.Warn) && !e.getTypes().contains(ValType.Ignored))
                return true;
            if (!e.ele.isEmpty() && e.hasWarnings())
                return true;
        }
        return false;
    }

    public boolean ignore(String msg) {
        boolean ignored = false;
        for (ValE e : elements)
            ignored = ignored || e.ignore(msg);
        return ignored;
    }


    public String toString() {
        return ValFactory.toJson(this);
    }

    public List<ValE> getElements() {
        return elements;
    }
}
