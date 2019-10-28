package gov.nist.asbestos.asbestosProxy.requests;

// 0 - empty
// 1 - appContext
// 2 - "engine"
// 3 - "collection"
// 4 - testCollectionId
// 5 - testId
// return TestScript

import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.Format;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.TestScript;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class GetTestDefinitionRequest {
    private static Logger log = Logger.getLogger(GetTestDefinitionRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 6 && request.uriParts.get(3).equals("collection");
    }

    public GetTestDefinitionRequest(Request request) {
        this.request = request;
    }

    public void run() {
        log.info("GetTestDefinition");
        String collectionName = request.uriParts.get(4);
        String testName = request.uriParts.get(5);

        File testDef = request.ec.getTest(collectionName, testName);
        if (testDef == null) {
            request.resp.setStatus(request.resp.SC_NOT_FOUND);
            return;
        }

        byte[] bytes;
        File testFile;
        testFile = new File(testDef, "TestScript.json");
        if (!testFile.exists()) {
            testFile = new File(testDef, "TestScript.xml");
        }
        if (testFile.exists()) {
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

            for (  ; index<testComponentCount; ) {
                TestScript.TestScriptTestComponent test = testComponents.get(index);
                if (test.hasModifierExtension() && test.getModifierExtension().get(0).hasUrl()) {
                    TestScript containedTestScript = getContainedTestScript(testScript, test.getModifierExtension().get(0).getUrl());
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


            String json = ProxyBase.getFhirContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(resource);
            Returns.returnString(request.resp, json);

            log.info("OK");
        }
    }

    private TestScript getContainedTestScript(TestScript testScript, String id) {
        List<Resource> containeds = testScript.getContained();
        for (Resource contained : containeds) {
            if (contained instanceof TestScript) {
                TestScript containedTestScript = (TestScript) contained;
                if (contained.hasId() && contained.getId().equals("#" + id)) {
                    return containedTestScript;
                }
            }
        }
        return null;
    }
}
