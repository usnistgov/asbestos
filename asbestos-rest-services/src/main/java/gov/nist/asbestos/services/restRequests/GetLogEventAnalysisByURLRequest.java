package gov.nist.asbestos.services.restRequests;

import com.google.common.base.Strings;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.resolver.Ref;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Map;

// 0 - empty
// 1 - app context  (asbestos)
// 2 - "log"
// 3 - "analysis"
// 4 - "url"
// ?url=TheUrlOfAFHIRResource
// Returns Report

// the url can point to any accessible resource

// optional parameters
// validation (true|false (default)) (accepted but ignored)
// gzip (true|false) - use gzip when issuing queries
// eventId - don't understand how this works - session and channel are needed to use this

public class GetLogEventAnalysisByURLRequest {
    private static Logger log = Logger.getLogger(GetLogEventAnalysisByURLRequest.class);

    public GetLogEventAnalysisByURLRequest(Request request) {
        model = new Model(request);
    }

    public static boolean isRequest(Request request) {
         return (request.uriParts.size() == 5
                 &&"log".equalsIgnoreCase(request.uriParts.get(2))
                && "analysis".equalsIgnoreCase(request.uriParts.get(3))
                && "url".equalsIgnoreCase(request.uriParts.get(4)));
    }

    static class Model {
        Request request;
        EventAnalysisParams eventAnalysisParams;
        Map<String, String> queryParams;
        boolean done = false;

        Model(Request request) {
            this.request = request;
            queryParams = request.getParametersMap();
            eventAnalysisParams = new EventAnalysisParams(request);
        }
    }

    Model model;

    public void run() throws IOException {
        model.request.announce("GetLogEvent by URL");
        String url = model.queryParams.get("url");
        if (Strings.isNullOrEmpty(url)) {
            model.request.badRequest();
            return;
        }
        Ref ref = new Ref(url);
        EventAnalysisCommon.runAndReturnReport(
                model.request,
                ref,
                "By Request",
                model.eventAnalysisParams,
                null);    // contextBundle
    }
}
