package gov.nist.asbestos.simapi.validation;


import java.util.ArrayList;
import java.util.List;

public class ValidationReport {
    List<Val> vals = new ArrayList<>();

    public ValidationReport add(Val val) {
        vals.add(val);
        return this;
    }

    public boolean hasErrors() {
        return search(vals);
    }

    private boolean search(List<Val> vals) {
        boolean found = false;

        for (Val val : vals) {
            if (val.hasErrors()) found = true;
            found = found || search(val.getChildren());
        }

        return found;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append("Validation Report\n");

        vals.forEach(val -> buf.append(val.toString()));

        buf.append("Validation result: ");
        if (hasErrors())
            buf.append("Failure\n");
        else
            buf.append("Success\n");

        return buf.toString();
    }
}
