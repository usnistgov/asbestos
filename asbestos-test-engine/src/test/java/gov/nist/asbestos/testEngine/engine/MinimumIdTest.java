package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.testEngine.engine.assertion.MinimumId;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.TestReport;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinimumIdTest {

    @Test
    void mapKeys1() {
        Map map = new HashMap();
        map.put("one", "a");
        map.put("two", "a");
        Map map1 = new HashMap();
        map1.put("a", "a");
        map.put("three", map1);

        Set<String> answer = new HashSet<>();
        answer.add(".one");
        answer.add(".two");
        answer.add(".three.a");

        MinimumId mid = new MinimumId();
        Set<String> result = mid.mapKeys(".", map);
        assertEquals(answer, result);
    }

    @Test
    void mapKeys2() {
        Map map = new HashMap();
        map.put("one", "a");
        map.put("two", "a");
        Map map2 = new HashMap();
        map2.put("p", "p");
        Map map1 = new HashMap();
        map1.put("a", map2);
        map.put("three", map1);

        Set<String> answer = new HashSet<>();
        answer.add(".one");
        answer.add(".two");
        answer.add(".three.a.p");

        MinimumId mid = new MinimumId();
        Set<String> result = mid.mapKeys(".", map);
        assertEquals(answer, result);
    }

    @Test
    void mapKeys3() {
        Map map = new HashMap();
        map.put("one", "a");
        map.put("two", "a");
        List list1 = new ArrayList();
        Map map2 = new HashMap();
        map2.put("p", "p");
        list1.add(map2);
        Map map1 = new HashMap();
        map1.put("a", list1);
        map.put("three", map1);

        Set<String> answer = new HashSet<>();
        answer.add(".one");
        answer.add(".two");
        answer.add(".three.a.p");

        MinimumId mid = new MinimumId();
        Set<String> result = mid.mapKeys(".", map);
        assertEquals(answer, result);
    }

    Map dataset1() {
        Map map = new HashMap();
        map.put("one", "a");
        map.put("two", "a");
        List list1 = new ArrayList();
        Map map2 = new HashMap();
        map2.put("p", "p");
        list1.add(map2);
        Map map3 = new HashMap();
        map3.put("m1", "m1");
        map3.put("p", "q");
        list1.add(map3);
        Map map1 = new HashMap();
        map1.put("a", list1);
        map.put("three", map1);
        return map;
    }

    @Test
    void mapKeys4() {
        Map map = dataset1();

        Set<String> answer = new HashSet<>();
        answer.add(".one");
        answer.add(".two");
        answer.add(".three.a.p");
        answer.add(".three.a.m1");

        MinimumId mid = new MinimumId();
        Set<String> result = mid.mapKeys(".", map);
        assertEquals(answer, result);
        List<String> result2 = new ArrayList<>();
        result2.addAll(result);
        Collections.sort(result2);
        System.out.println(result2);
    }

    @Test
    void mapKeys5() {
        Map ref = dataset1();
        Map sut = dataset1();
        sut.put("four", "x");

        MinimumId mid = new MinimumId();
        Set<String> refKeys = mid.mapKeys(".", ref);
        Set<String> sutKeys = mid.mapKeys(".", sut);

        List<String> sutAtts = new ArrayList<>(sutKeys);
        Collections.sort(sutAtts);
        System.out.println(sutAtts);

        List<String> diff = mid.diff(refKeys, sutKeys);
        assertTrue(diff.isEmpty());
    }

    @Test
    void test1() throws URISyntaxException {
        File refFile = Paths.get(getClass().getResource("/minimumId/reference/minimal/DocumentReference.xml").toURI()).toFile();
        DocumentReference reference = (DocumentReference) ParserBase.parse(refFile);
        DocumentReference sut = (DocumentReference) ParserBase.parse(Paths.get(getClass().getResource("/minimumId/missingSubject/DocumentReference/DocRef3.xml").toURI()).toFile());
        MinimumId.Report report = new MinimumId().run(reference, sut, false);
        assertEquals(1, report.missing.size());
        assertEquals(".masterIdentifier.value", report.missing.get(0));
    }

    @Test
    void test2() throws URISyntaxException {
        TestReport.SetupActionAssertComponent assertReport = new TestReport.SetupActionAssertComponent();
        File refFile = Paths.get(getClass().getResource("/minimumId/reference/minimal/DocumentReferenceSecurityLabel.xml").toURI()).toFile();
        DocumentReference reference = (DocumentReference) ParserBase.parse(refFile);
        DocumentReference sut = (DocumentReference) ParserBase.parse(Paths.get(getClass().getResource("/minimumId/missingSubject/DocumentReference/DocRef4.xml").toURI()).toFile());
        MinimumId.Report report = new MinimumId().run(reference, sut, false);
        assertEquals(1, report.missing.size());
        assertEquals(".securityLabel.coding.code", report.missing.get(0));
    }

    @Test
    void wrongType() throws URISyntaxException {
        Val val = new Val();
        File test1 = Paths.get(getClass().getResource("/minimumId/wrongType/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = new TestEngine(test1, new URI(""))
                .setVal(val)
                .setFhirClient(new FhirClient())
                .setTestSession("default")
                .setExternalCache(new File("foo"))
                .runTest();
        TestReport report = testEngine.getTestReport();
        List<String> errors = testEngine.getErrors();
        TestReport.TestReportResult result = report.getResult();
        assertEquals(TestReport.TestReportResult.FAIL, result);
        assertEquals(1, errors.size());
        assertEquals("minimumId: cannot compare org.hl7.fhir.r4.model.Patient and org.hl7.fhir.r4.model.DocumentReference", errors.get(0));
    }

    @Test
    void missingSubject() throws URISyntaxException {
        Val val = new Val();
        File test1 = Paths.get(getClass().getResource("/minimumId/missingSubject/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = new TestEngine(test1, new URI(""))
                .setVal(val)
                .setFhirClient(new FhirClient())
                .setTestSession("default")
                .setExternalCache(new File("foo"))
                .runTest();
        TestReport report = testEngine.getTestReport();
        List<String> errors = testEngine.getErrors();
        TestReport.TestReportResult result = report.getResult();
        assertEquals(TestReport.TestReportResult.FAIL, result);
        assertEquals(1, errors.size());
        assertEquals("minimumId: attribute Subject not found", errors.get(0));

    }

    @Test
    void hasExtra() throws URISyntaxException {
        Val val = new Val();
        File test1 = Paths.get(getClass().getResource("/minimumId/hasExtra/TestScript.xml").toURI()).getParent().toFile();
        TestEngine testEngine = new TestEngine(test1, new URI(""))
                .setVal(val)
                .setFhirClient(new FhirClient())
                .setTestSession("default")
                .setExternalCache(new File("foo"))
                .runTest();
        TestReport report = testEngine.getTestReport();
        List<String> errors = testEngine.getErrors();
        TestReport.TestReportResult result = report.getResult();
        assertEquals(TestReport.TestReportResult.PASS, result);
        assertEquals(0, errors.size());
    }
}
