package gov.nist.asbestos.services.restRequests;

// 0 - empty
// 1 - appContext
// 2 - "rw"
// 3 - "testSession"
// 4 - sessionId
// return JSON object of session definition

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.Base.Returns;
import org.apache.log4j.Logger;

import java.io.File;

public class GetSessionConfigRequest {
    private final static Logger log = Logger.getLogger(GetSessionConfigRequest.class);

    private final Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 5 && "testSession".equalsIgnoreCase(request.uriParts.get(3));
    }

    public GetSessionConfigRequest(Request request) { this.request = request; }

    public void run() {
        String sessionId = request.uriParts.get(4);
        request.announce("GetSessionConfig");
        File sessionDir = request.ec.getSessionDir(sessionId);
        File sessionFile = new File(sessionDir, "config.json");
        Returns.returnString(request.resp, EC.readFromFile(sessionFile));
        request.ok();
    }

    public static SessionConfig load(File file) {
        try {
            ObjectMapper objectMapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return objectMapper.readValue(file, SessionConfig.class);
        } catch (Throwable e ) {
            throw new RuntimeException(e);
        }
    }

}
