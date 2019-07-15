package gov.nist.asbestos.testEngine;

import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

import java.util.Objects;

public class OperationRunner {
    private String label;
    private String type;
    private String typePrefix;
    private ValE val;
    private FhirClient fhirClient = null;
    private TestScript testScript = null;
    private FixtureMgr fixtureMgr;
    private TestReport testReport = null;

    OperationRunner(FixtureMgr fixtureMgr) {
        Objects.requireNonNull(fixtureMgr);
        this.fixtureMgr = fixtureMgr;
    }

    void run(TestScript.SetupActionOperationComponent op, TestReport.SetupActionOperationComponent operationReport) {
        Objects.requireNonNull(typePrefix);
        Objects.requireNonNull(val);
        Objects.requireNonNull(fhirClient);
        Objects.requireNonNull(testScript);
        operationReport.setResult(TestReport.TestReportActionResult.PASS);  // may be overwritten

        this.label = op.getLabel();
        type = typePrefix + ".operation";

        int elementCount = 0;
        if (op.hasTargetId()) elementCount++;
        if (op.hasParams()) elementCount++;
        if (op.hasUrl()) elementCount++;
        if (elementCount == 0) {
            Reporter.reportError(val, operationReport, type, label,"has none of sourceId, targetId, params, url - one is required");
            return;
        }
        if (elementCount > 1) {
            Reporter.reportError(val, operationReport, type, label,"has multiple of sourceId, targetId, params, url - only one is allowed");
            return;
        }
        if (!op.hasType()) {
            Reporter.reportError(val, operationReport, type, label,"has no type");
            return;
        }
        Coding typeCoding = op.getType();
        String code = typeCoding.getCode();

        if ("read".equals(code)) {
            SetupActionRead setupActionRead = new SetupActionRead(fixtureMgr)
                    .setVal(val)
                    .setFhirClient(fhirClient)
                    .setTestReport(testReport);
            setupActionRead.setVariableMgr(new VariableMgr(testScript, fixtureMgr)
                    .setOpReport(operationReport)
                    .setVal(val));
            setupActionRead.run(op, operationReport);
        } else if ("create".equals(code)) {
            SetupActionCreate setupActionCreate =
                    new SetupActionCreate(fixtureMgr)
                            .setFhirClient(fhirClient)
                            .setVal(val);
            setupActionCreate.setVariableMgr(
                    new VariableMgr(testScript, fixtureMgr)
                            .setVal(val)
                            .setOpReport(operationReport));
            setupActionCreate.run(op, operationReport);
        } else {
            Reporter.reportError(val, operationReport, type, label,"do not understand code.code of " + code);
        }
    }

    public OperationRunner setTypePrefix(String typePrefix) {
        this.typePrefix = typePrefix;
        return this;
    }

    public OperationRunner setVal(ValE val) {
        this.val = val;
        return this;
    }

    public OperationRunner setFhirClient(FhirClient fhirClient) {
        this.fhirClient = fhirClient;
        return this;
    }

    public OperationRunner setTestScript(TestScript testScript) {
        this.testScript = testScript;
        return this;
    }

    public OperationRunner setTestReport(TestReport testReport) {
        this.testReport = testReport;
        return this;
    }
}
