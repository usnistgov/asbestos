package gov.nist.asbestos.testEngine.engine.assertion;

import gov.nist.asbestos.simapi.validation.ValE;
import gov.nist.asbestos.testEngine.engine.FixtureLabels;
import gov.nist.asbestos.testEngine.engine.TestDef;
import gov.nist.asbestos.testEngine.engine.VariableMgr;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureComponent;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureMgr;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

public interface AssertionContext {
    TestDef getTestDef();
    FixtureComponent getSource();
    boolean getWarningOnly();
    TestScript.SetupActionAssertComponent getCurrentAssert();
    TestReport.SetupActionAssertComponent getCurrentAssertReport();
    ValE getVal();
    String getType();
    String getLabel();
    FixtureLabels getFixtureLabels();
    VariableMgr getVariableMgr();
    FixtureMgr getFixtureMgr();
    boolean isRequest();
}
