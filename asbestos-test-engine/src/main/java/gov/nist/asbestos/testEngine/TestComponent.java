package gov.nist.asbestos.testEngine;

import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.TestScript;

import java.util.Map;

public class TestComponent {
    private Map<String, FixtureComponent> fixtures;
    private TestScript.TestScriptTestComponent test;
    private ValE val;

    public TestComponent(Map<String, FixtureComponent> fixtures, TestScript.TestScriptTestComponent test) {
        this.fixtures = fixtures;
        this.test = test;
    }

    private void run() {
        if (test.hasAction()) {
            for (TestScript.TestActionComponent action : test.getAction()) {

            }
        }
    }

    TestComponent setVal(ValE val) {
        this.val = val;
        return this;
    }
}
