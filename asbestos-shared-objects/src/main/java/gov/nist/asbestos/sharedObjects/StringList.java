package gov.nist.asbestos.sharedObjects;

import java.util.ArrayList;
import java.util.List;

public class StringList {
    private List<String> values;

    public List<String> getValues() {
        if (values == null)
            values = new ArrayList<>();
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }
}
