package gov.nist.asbestos.services.restRequests;

// 0 - empty
// 1 - appContext
// 2 - "engine"
// 3 - "testScript"
// 4 - testCollectionId
// 5 - testId
// returns a TestScript

import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.testEngine.engine.ModularScripts;
import org.apache.log4j.Logger;

import java.io.File;

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
        request.announce("GetTestScriptRequest");

        File testDef = request.ec.getTest(collectionName, testName);
        if (testDef == null) {
            request.badRequest("Test not found");
            return;
        }

        ModularScripts modularScripts = new ModularScripts(testDef);
        String json = modularScripts.asJson();
        request.returnString(json);
        request.ok();
    }
}
