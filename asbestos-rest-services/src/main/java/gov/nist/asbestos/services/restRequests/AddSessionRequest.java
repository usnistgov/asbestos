package gov.nist.asbestos.services.restRequests;

import com.google.gson.Gson;
import gov.nist.asbestos.client.Base.Dirs;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.Base.ReturnIs;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
// 0 - empty
// 1 - app context
// 2 - "channel"
// 3 - "addSession"
// 4 - new session name
// Return list of TestSession IDs

public class AddSessionRequest {
    private static Logger log = Logger.getLogger(AddSessionRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 5 && "addSession".equalsIgnoreCase(request.uriParts.get(3));
    }

    public AddSessionRequest(Request request) {
        this.request = request;
    }

    public void run()  {
        request.announce("AddSession");
        String newSessionName = request.uriParts.get(4);
        File sessions = new File(request.externalCache, "FhirSessions");
        File newSession = new File(sessions, newSessionName);
        ReturnIs.Boolean(newSession.mkdirs());

        SessionConfig config = new SessionConfig();
        config.name = newSessionName;
        config.includes.add("default");

        File configFile = new File(newSession, "config.json");
        Gson gson = new Gson();
        String json = gson.toJson(config);

        try {
            Files.write(Paths.get(configFile.toString()), json.getBytes());
        } catch (IOException e) {
            request.serverError(e);
            return;
        }

        List<String> names = Dirs.dirListingAsStringList(sessions);
        request.returnList(names);
        request.ok();
    }
}
