package gov.nist.asbestos.asbestosProxy.requests;

// 0 - empty
// 1 - appContext
// 2 - "engine"
// 3 - "testScript"
// 4 - testCollectionId
// 5 - testId
// returns a TestScript

import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.testEngine.engine.TestEngine;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.TestScript;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GetTestScriptRequest {
    private static Logger log = Logger.getLogger(GetTestScriptRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 6 && request.uriParts.get(3).equals("testScript");
    }

    public GetTestScriptRequest(Request request) {
        this.request = request;
    }

    public void run() {
        String collectionName = request.uriParts.get(4);
        String testName = request.uriParts.get(5);
        log.info("GetTestScriptRequest");

        File testDef = request.ec.getTest(collectionName, testName);
        if (testDef == null) {
            request.resp.setStatus(request.resp.SC_NOT_FOUND);
            log.info("Not Found");
            return;
        }

        byte[] bytes;
        File testFile = TestEngine.findTestScriptFile(testDef);
        if (!testFile.exists()) {
            request.resp.setStatus(request.resp.SC_NOT_FOUND);
            log.info("Not Found");
            return;
        }
        try {
            bytes = FileUtils.readFileToByteArray(testFile);
        } catch (IOException e) {
            request.resp.setStatus(request.resp.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        BaseResource resource = ProxyBase.parse(bytes, Format.fromContentType(testFile.getName()));
        TestScript testScript = (TestScript) resource;

        // update TestScript with any contained TestScript Test elements

        List<TestScript.TestScriptTestComponent> testComponents = testScript.getTest();
        int index = 0;
        int testComponentCount = testComponents.size();

        for (; index < testComponentCount; ) {
            TestScript.TestScriptTestComponent test = testComponents.get(index);
            if (test.hasModifierExtension() && test.getModifierExtension().get(0).hasValue()) {
                TestScript containedTestScript = getContainedTestScript(testScript, test.getModifierExtension().get(0).getValue().toString());
                if (containedTestScript != null) {
                    if (containedTestScript.hasTest() && !containedTestScript.getTest().isEmpty()) {
                        TestScript.TestScriptTestComponent containedTestComponent = containedTestScript.getTest().get(0);
                        // insert containedTestComponent before test
                        testComponents.add(index, containedTestComponent);
                        index++;
                    }
                }
            }
            index++;
        }

        testScript.setName(testName);

        String json = ProxyBase.getFhirContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(testScript);
        Returns.returnString(request.resp, json);

    }

    private TestScript getContainedTestScript(TestScript testScript, String id) {
        List<Resource> containeds = testScript.getContained();
        for (Resource contained : containeds) {
            if (contained instanceof TestScript) {
                TestScript containedTestScript = (TestScript) contained;
                if (contained.hasId() && contained.getId().equals(id)) {
                    return containedTestScript;
                }
            }
        }
        return null;
    }
}
