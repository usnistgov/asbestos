package gov.nist.asbestos.analysis;

import org.hl7.fhir.r4.model.DocumentManifest;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuildReferencesTest {
    static DocumentManifest dm;

    @BeforeAll
    static void beforeAll() {
        dm = new DocumentManifest()
                .setSubject(new Reference("Patient/123"))
                .addContent(new Reference("DocumentReference/3.4.5"));
    }

    @Test
    void buildRefs() {
        AnalysisReport ar = new AnalysisReport();
        List<Reference2> refs = Reference2Builder.buildReferences(dm);
        assertEquals(2, refs.size());
    }
}
