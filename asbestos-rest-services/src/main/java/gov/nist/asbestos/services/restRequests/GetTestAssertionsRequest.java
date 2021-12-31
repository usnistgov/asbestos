package gov.nist.asbestos.services.restRequests;

import gov.nist.asbestos.client.Base.Request;
import java.util.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

// 0 - empty
// 1 - appContext
// 2 - "engine"
// 3 - "assertions"
// return JSON object of assertions - name: definition

public class GetTestAssertionsRequest {

    private static Logger log = Logger.getLogger(GetTestAssertionsRequest.class.getName());

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 4 && request.uriParts.get(3).equals("assertions");
    }

    public GetTestAssertionsRequest(Request request) {
        this.request = request;
    }

    public void run() {
        request.announce("GetTestAssertionsRequest");

        File assertsFile = this.request.ec.getTestAssertionsFile();
        String jsonString = null;
        try {
            jsonString = new String(Files.readAllBytes(assertsFile.toPath()));
        } catch (IOException e) {
            request.serverError(e);
        }

        request.returnString(jsonString);
        request.ok();
    }
}
