package gov.nist.asbestos.services.restRequests;

// 0 - empty
// 1 - appContext
// 2 - "channel"
// 3 - "sessionConfig"
// 4 - sessionId
// return JSON object of session definition

import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.Base.Returns;
import org.apache.log4j.Logger;

import java.io.File;

public class GetSessionConfigRequest {
    private static Logger log = Logger.getLogger(GetSessionConfigRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 5 && "sessionConfig".equalsIgnoreCase(request.uriParts.get(3));
    }

    public GetSessionConfigRequest(Request request) { this.request = request; }

    public void run() {
        String sessionId = request.uriParts.get(4);
        request.announce("GetSessionConfig");
        File sessionDir = request.ec.getSessionConfig(sessionId);
        File sessionFile = new File(sessionDir, "config.json");
        Returns.returnString(request.resp, EC.readFromFile(sessionFile));
        request.ok();
    }

}
