package gov.nist.asbestos.asbestosApi;

import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

public interface TestResult {
    TestScript getTestScript();
    TestReport getTestReport();
    Channel getChannel();
    String getTestCollection();
    String getTestName();
}
