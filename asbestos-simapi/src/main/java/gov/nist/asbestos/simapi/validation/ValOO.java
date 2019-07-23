package gov.nist.asbestos.simapi.validation;

import org.hl7.fhir.r4.model.Bundle;

public class ValOO {
    private Bundle.BundleEntryResponseComponent responseEntry;
    private ValE vale;

    public ValOO(Bundle.BundleEntryResponseComponent responseEntry, ValE vale) {
        this.responseEntry = responseEntry;
        this.vale = vale;
    }

    public ValOO setMsg(String msg) {
        vale.setMsg(msg);
        return this;
    }

    public ValOO add(ValE e) {
        vale.add(e);
        return this;
    }

    public ValOO addTr(ValE e) {
        vale.addTr(e);
        return this;
    }
}
