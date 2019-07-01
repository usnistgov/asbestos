package gov.nist.asbestos.testEngine;

import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.TestScript;

import java.util.List;
import java.util.Map;
import java.util.Objects;

class SetupAction {
    private Map<String, FixtureComponent> fixtures;
    private TestScript.SetupActionComponent action;
    private ValE val;

    SetupAction(Map<String, FixtureComponent> fixtures, TestScript.SetupActionComponent action) {
        this.fixtures = fixtures;
        this.action = action;
    }

    FixtureComponent run() {
        Objects.requireNonNull(val);
        String id = action.hasId() ? action.getId() : "No ID";
        if (action.hasOperation() && action.hasAssert()) {
            val.add(new ValE("Setup.Action " + id + " has both an Operation and an Assertion").asError());
            return null;
        }

        if (action.hasOperation()) {
            TestScript.SetupActionOperationComponent op = action.getOperation();
            int elementCount = 0;
            if (op.hasSourceId()) elementCount++;
            if (op.hasTargetId()) elementCount++;
            if (op.hasParams()) elementCount++;
            if (op.hasUrl()) elementCount++;
            if (elementCount == 0) {
                val.add(new ValE("Setup.Action " + id + " has none of sourceId, targetId, params, url - one is required").asError());
                return null;
            }
            if (elementCount > 1) {
                val.add(new ValE("Setup.Action " + id + " has multiple of sourceId, targetId, params, url - only one is allowed").asError());
                return null;
            }
            if (!op.hasType()) {
                val.add(new ValE("Setup.Action " + id + " has no type").asError());
                return null;
            }
            Coding typeCoding = op.getType();
            String code = typeCoding.getCode();
            String system = typeCoding.getSystem();
            if (!"http://terminology.hl7.org/CodeSystem/testscript-operation-codes".equals(system)) {
                val.add(new ValE("Setup.Action " + id + " do not understand code.system " + system).asError());
                return null;
            }

            if ("read".equals(code)) {
                return new SetupActionRead(fixtures, op).setVal(val).run();
            } else {
                val.add(new ValE("Setup.Action " + id + " do not understand code.code of " + code).asError());
                return null;
            }
        }
        if (action.hasAssert()) {
            TestScript.SetupActionAssertComponent as = action.getAssert();
            if (as.hasCompareToSourceId() && as.hasCompareToSourceExpression()) {
                val.add(new ValE("Setup.Action " + id + " has both compareToSourceId and compareToSourceExpression").asError());
                return null;
            }
            if (!as.hasDirection() || as.getDirection().equals("response")) {
                if (as.hasCompareToSourceId()) {
                    FixtureComponent sourceFixture = fixtures.get(as.getCompareToSourceId());
                    if (sourceFixture == null) {
                        val.add(new ValE("Setup.Assert " + id + " has compareToSourceId " + as.getCompareToSourceId() + " which is undefined").asError());
                        return null;
                    }



                    Base sourceContents = sourceFixture.getResponse().getResource().;
                    List<Base> sourceIdElements = FhirPathEngineBuilder.build().evaluate(sourceFixture.getResponse().getResource(), )
                    if (as.hasValue()) {
                    } else if (as.hasCompareToSourceExpression()) {
                        String sourceExpression = as.getCompareToSourceExpression();  // fhir path
                    } else if (as.hasCompareToSourcePath()) {

                    } else {

                    }
                }
            } else {  // request

            }
        }
        return null;
    }

    SetupAction setVal(ValE val) {
        this.val = val;
        return this;
    }
}
