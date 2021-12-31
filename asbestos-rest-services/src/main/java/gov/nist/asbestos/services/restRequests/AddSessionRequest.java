package gov.nist.asbestos.services.restRequests;

import com.google.gson.Gson;
import gov.nist.asbestos.client.Base.Dirs;
import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.log.SimStore;
import org.apache.commons.io.IOUtils;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static gov.nist.asbestos.client.Base.Returns.returnPlainTextResponse;
// 0 - empty
// 1 - app context
// 2 - "rw" or "accessGuard"
// 3 - "testSession"
// Return list of TestSession IDs

public class AddSessionRequest {
    public static final String[] RESERVED_NAMES = {"undefined","reserved", "backup", "internal", "private"};
    public static final String[] DEFAULT_INCLUDES = {"default"};
    private static Logger log = Logger.getLogger(AddSessionRequest.class.getName());

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 4 && "testSession".equalsIgnoreCase(request.uriParts.get(3));
    }

    public AddSessionRequest(Request request) {
        this.request = request;
    }

    public void run()  throws IOException {
        request.announce("AddSession");
        String newSessionName = IOUtils.toString(request.req.getInputStream(), Charset.defaultCharset());   // unchecked test session name

        if (isInvalidName(newSessionName)) {
            String error = "Invalid Test Session name. Check if name contains an illegal character or is a reserved name.";
            log.warning(error + ": " +  newSessionName);
            returnPlainTextResponse(request.resp, HttpServletResponse.SC_BAD_REQUEST, error);
            return;
        }

        File sessions = EC.ftkSessionsDir(request.externalCache);
        File newSessionDir = new File(sessions, newSessionName);

        if (newSessionDir.exists()) {
            returnPlainTextResponse(request.resp, HttpServletResponse.SC_BAD_REQUEST, "A Test Session with the same name already exists.");
            return;
        }

        if (!newSessionDir.mkdirs()) {
            log.severe(String.format("Test session directory create failed: %s", newSessionDir));
            returnPlainTextResponse(request.resp, HttpServletResponse.SC_BAD_REQUEST, "Test Session could not be created.");
            return;
        }

        SessionConfig config = new SessionConfig(newSessionName, DEFAULT_INCLUDES, false);

        File configFile = new File(newSessionDir, "config.json");
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

    private boolean isInvalidName(String uncheckedName) throws IOException {
        return  ! SimStore.isValidCharsPattern().matcher(uncheckedName).matches()
                || SimStore.isReservedNamesPattern(RESERVED_NAMES).matcher(uncheckedName).matches();


    }
}
