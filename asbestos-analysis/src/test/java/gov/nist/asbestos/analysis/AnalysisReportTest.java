package gov.nist.asbestos.analysis;

import gov.nist.asbestos.client.resolver.Ref;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisReportTest {

    @Test
    void simple() {
        Ref ref = new Ref("http://localhost:8080/fhir/fhir/DocumentManifest/45203");
        AnalysisReport ar = new AnalysisReport(ref);
        ar.run();
        assertEquals(0, ar.getGeneralErrors().size());
    }
}
