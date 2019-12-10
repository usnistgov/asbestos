package gov.nist.asbestos.analysis;

import gov.nist.asbestos.client.resolver.Ref;
import org.junit.jupiter.api.Test;

class AnalysisReportTest {

    @Test
    void simple() {
        String fullUrl = "http://localhost:8081/asbestos/proxy/default__limited/DocumentManifest/1.2.129.6.59.189.2019.12.10.14.49.37.975.2";
        Ref ref = new Ref(fullUrl);
        AnalysisReport ar = new AnalysisReport(ref);
        ar.run();
    }
}
