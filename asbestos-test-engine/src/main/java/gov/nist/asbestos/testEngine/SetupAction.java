package gov.nist.asbestos.testEngine;

import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.TestScript;

import java.util.Map;
import java.util.Objects;

public class SetupAction {
    private Map<String, FixtureComponent> fixtures;
    private TestScript.SetupActionComponent action;
    private ValE val;

    public SetupAction(Map<String, FixtureComponent> fixtures, TestScript.SetupActionComponent action) {
        this.fixtures = fixtures;
        this.action = action;
    }

    void run() {
        Objects.requireNonNull(val);
        String id = action.hasId() ? action.getId() : "No ID";
        if (action.hasOperation() && action.hasAssert()) {
            val.add(new ValE("Setup.Action " + id + " has both an Operation and an Assertion").asError());
            return;
        }

        if (action.hasOperation()) {
            TestScript.SetupActionOperationComponent op = action.getOperation();
            int elementCount = 0;
            if (op.hasSourceId()) elementCount++;
            if (op.hasTargetId()) elementCount++;
            if (op.hasParams()) elementCount++;
            if (op.hasUrl()) elementCount++;
            if (elementCount == 0) {
                val.add(new ValE("Setup.Action " + id + " has none of sourceId, targetId, params, or url - one is required").asError());
                return;
            }
            if (elementCount > 1) {
                val.add(new ValE("Setup.Action " + id + " has multiple of sourceId, targetId, params, or url - only one is allowed").asError());
                return;
            }
            if (!op.hasType()) {
                val.add(new ValE("Setup.Action " + id + " has no type").asError());
                return;
            }
            Coding typeCoding = op.getType();
            String code = typeCoding.getCode();
            String system = typeCoding.getSystem();
            if (!"http://terminology.hl7.org/CodeSystem/testscript-operation-codes".equals(system)) {
                val.add(new ValE("Setup.Action " + id + " do not understand code.system " + system).asError());
                return;
            }
            if ("read".equals(code)) {
                new SetupActionRead(fixtures, op).setVal(val).run();
            } else {
                val.add(new ValE("Setup.Action " + id + " do not understand code.code of " + code).asError());
                return;
            }
        }
        if (action.hasAssert()) {

        }
    }

    SetupAction setVal(ValE val) {
        this.val = val;
        return this;
    }
}
