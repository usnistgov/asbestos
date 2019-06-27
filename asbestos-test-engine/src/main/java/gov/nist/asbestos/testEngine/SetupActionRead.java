package gov.nist.asbestos.testEngine;

import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.TestScript;

import java.util.Map;
import java.util.Objects;

public class SetupActionRead {
    private Map<String, FixtureComponent> fixtures;
    private TestScript.SetupActionOperationComponent op;
    private ValE val;

    SetupActionRead(Map<String, FixtureComponent> fixtures, TestScript.SetupActionOperationComponent op) {
        this.fixtures = fixtures;
        this.op = op;
    }

    void run() {
        Objects.requireNonNull(val);
    }

    SetupActionRead setVal(ValE val) {
        this.val = val;
        return this;
    }
}
