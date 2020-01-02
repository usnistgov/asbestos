package gov.nist.asbestos.analysis;

import gov.nist.asbestos.client.Base.EC;
import org.hl7.fhir.r4.model.Coding;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CodesValidationTest {
    private static EC ec;

    static File findExternalCache() throws URISyntaxException {
        Path ec = Paths.get(CodesValidationTest.class.getResource("/ec/findme.txt").toURI()).getParent();
        return ec.toFile();
    }

    @BeforeAll
    static void beforeAll() throws URISyntaxException {
        File externalCache = findExternalCache();
        ec = new EC(externalCache);
    }

    @Test
    void facilityType() {
        CodesValidation codesValidation = new CodesValidation(ec);
        Coding coding = new Coding();
        coding.setSystem("http://snomed.info/sct");
        coding.setCode("264372000");

        String fhirCodeName = " facilityType";
        String xdsCodeName = "healthcareFacilityTypeCode";
        List<String> errors = new ArrayList<>();

        codesValidation.check(coding, fhirCodeName, xdsCodeName, errors);
        assertTrue(errors.isEmpty());
    }
}
