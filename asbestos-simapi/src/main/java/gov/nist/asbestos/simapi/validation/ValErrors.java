package gov.nist.asbestos.simapi.validation;

import java.util.ArrayList;
import java.util.List;

public class ValErrors {
    private Val val;

    public ValErrors(Val val) {
        this.val = val;
    }

    public List<ValE> getErrors() {
        List<ValE> items = new ArrayList<>();

        for (ValE e : val.elements) {
            if (e.getTypes().contains(ValType.Error)) {
                items.add(e);
            }
            items.addAll(get(e));
        }
        return items;
    }

    private List<ValE> get(ValE ve) {
        List<ValE> items = new ArrayList<>();

        for (ValE e : ve.getEle()) {
            if (e.getTypes().contains(ValType.Error)) {
                items.add(e);
            }
            items.addAll(get(e));
        }

        return items;

    }
}
