package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

import java.net.URI;
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
    private URI sut = null;
    private Reporter reporter;

    OperationRunner(FixtureMgr fixtureMgr) {
        Objects.requireNonNull(fixtureMgr);
        this.fixtureMgr = fixtureMgr;
    }

    void run(TestScript.SetupActionOperationComponent op, TestReport.SetupActionOperationComponent operationReport) {
        Objects.requireNonNull(typePrefix);
        Objects.requireNonNull(val);
        Objects.requireNonNull(fhirClient);
        Objects.requireNonNull(testScript);
        Objects.requireNonNull(operationReport);

        reporter = new Reporter(val, operationReport, "", "");

        operationReport.setResult(TestReport.TestReportActionResult.PASS);  // may be overwritten

        this.label = op.getLabel();
        type = typePrefix + ".operation";

        int elementCount = 0;
        if (op.hasSourceId()) elementCount++;
        if (op.hasTargetId()) elementCount++;
        if (op.hasParams()) elementCount++;
        if (op.hasUrl()) elementCount++;
        if (elementCount == 0 && sut == null) {
            reporter.reportError("has none of sourceId, targetId, params, url - one is required");
            return;
        }
        if (elementCount > 1) {
            boolean itsOk = false;
            if (op.hasSourceId()) {
                FixtureComponent fixtureComponent = fixtureMgr.get(op.getSourceId());
                if (fixtureComponent == null) {
                    reporter.reportError("fixture " + op.getSourceId() + " is not defined");
                    return;
                }
                if (!fixtureComponent.getResourceWrapper().hasHttpBase()) {
                    // it's ok - loaded as static fixture
                    itsOk = true;
                }
            }
            if (!itsOk) {
                reporter.reportError( "has multiple of sourceId, targetId, params, url - only one is allowed");
                return;
            }
        }
        if (!op.hasType()) {
            reporter.reportError("has no type");
            return;
        }

        if (op.hasDestination()) {
            reporter.reportError("destination not supported");
            return;
        }

        Coding typeCoding = op.getType();
        String code = typeCoding.getCode();

        fhirClient.setFormat(op.hasContentType() ? Format.fromContentType(op.getContentType()) : Format.XML);

        if ("read".equals(code)) {
            SetupActionRead setupActionRead = new SetupActionRead(fixtureMgr)
                    .setVal(val)
                    .setFhirClient(fhirClient)
                    .setSut(sut)
                    .setType(type + ".read")
                    .setTestReport(testReport);
            setupActionRead.setVariableMgr(new VariableMgr(testScript, fixtureMgr)
                            .setVal(val)
                        .setOpReport(operationReport)
                        );
            setupActionRead.run(op, operationReport);
        } else if ("search".equals(code)) {
                SetupActionSearch setupActionSearch = new SetupActionSearch(fixtureMgr)
                        .setVal(val)
                        .setFhirClient(fhirClient)
                        .setSut(sut)
                        .setType(type + ".search")
                        .setTestReport(testReport);
                setupActionSearch
                        .setVal(val)
                        .setVariableMgr(
                                new VariableMgr(testScript, fixtureMgr)
                                        .setVal(val)
                                        .setOpReport(operationReport));
                setupActionSearch.run(op, operationReport);
        } else if ("create".equals(code)) {
            SetupActionCreate setupActionCreate =
                    new SetupActionCreate(fixtureMgr)
                            .setFhirClient(fhirClient)
                            .setType(type + ".create")
                            .setSut(sut)
                            .setVal(val);
            setupActionCreate.setVariableMgr(
                    new VariableMgr(testScript, fixtureMgr)
                            .setVal(val)
                            .setOpReport(operationReport));
            setupActionCreate.run(op, operationReport);
        } else if ("delete".equals(code)) {
            SetupActionDelete setupActionDelete =
                    new SetupActionDelete(fixtureMgr)
                            .setSut(sut)
                            .setFhirClient(fhirClient)
                            .setType(type + ".delete")
                            .setVal(val);
            setupActionDelete.setVariableMgr(
                    new VariableMgr(testScript, fixtureMgr)
                            .setVal(val)
                            .setOpReport(operationReport));
            setupActionDelete.run(op, operationReport);
        } else if ("transaction".equals(code)) {
            SetupActionTransaction setupActionTransaction =
                    new SetupActionTransaction(fixtureMgr)
                            .setSut(sut)
                            .setFhirClient(fhirClient)
                            .setType(type + ".transaction")
                            .setVal(val);
            setupActionTransaction.setVariableMgr(
                    new VariableMgr(testScript, fixtureMgr)
                            .setVal(val)
                            .setOpReport(operationReport));
            setupActionTransaction.run(op, operationReport);
        } else if ("mhd-pdb-transaction".equals(code)) {
            SetupActionMhdPdbTransaction setupActionTransaction =
                    new SetupActionMhdPdbTransaction(fixtureMgr);
            setupActionTransaction.setSut(sut)
                            .setFhirClient(fhirClient)
                            .setType(type + ".mhd-pdb-transaction")
                            .setVal(val);
            setupActionTransaction.setVariableMgr(
                    new VariableMgr(testScript, fixtureMgr)
                            .setVal(val)
                            .setOpReport(operationReport));
            setupActionTransaction.run(op, operationReport);
        } else {
            reporter.reportError("do not understand code.code of " + code);
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

    public OperationRunner setSut(URI sut) {
        this.sut = sut;
        return this;
    }
}
