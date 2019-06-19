package gov.nist.asbestos.simapi.validation;

import java.util.ArrayList;
import java.util.List;

public class ValWarnings {
    private Val val;

    public ValWarnings(Val val) {
        this.val = val;
    }

    public List<ValE> getWarnings() {
        List<ValE> items = new ArrayList<>();

        for (ValE e : val.elements) {
            if (e.getType().equals(ValType.Warn)) {
                items.add(e);
            }
            items.addAll(get(e));
        }
        return items;
    }

    private List<ValE> get(ValE ve) {
        List<ValE> items = new ArrayList<>();

        for (ValE e : ve.getEle()) {
            if (e.getType().equals(ValType.Warn)) {
                items.add(e);
            }
            items.addAll(get(e));
        }

        return items;

    }
}
