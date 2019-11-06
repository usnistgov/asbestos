package gov.nist.asbestos.asbestosProxy.requests;

import gov.nist.asbestos.client.events.UITask;
import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.Verb;
import gov.nist.asbestos.http.support.Common;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.List;

// 0 - empty
// 1 - app context  (asbestos)
// 2 - "log"
// 3 - testSession
// 4 - channelId
// 5 - resourceType - may be null
// 6 - event

public class GetEventRequest {
    private static Logger log = Logger.getLogger(GetEventRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 7 && "log".equalsIgnoreCase(request.uriParts.get(2));
    }

    public GetEventRequest(Request request) {
        this.request = request;
    }

    public void run() {
        log.info("GetEventRequest");
        Headers headers = Common.getRequestHeaders(request.req, Verb.GET);
        Header acceptHeader = headers.getAccept();
        boolean htmlOk = acceptHeader.getValue().contains("text/html");
        boolean jsonOk = acceptHeader.getValue().contains("json");
        if (request.uriParts.size() == 7) {  // includes event
            if (jsonOk) {
                request.ec.buildJsonListingOfEvent(
                        request.resp,
                        request.uriParts.get(3),
                        request.uriParts.get(4),
                        request.uriParts.get(5),
                        request.uriParts.get(6));
                return;
            } else if (htmlOk) {
                buildFullHtmlListing(request.resp, request.uriParts);
                return;
            }
        }
        request.resp.setStatus(request.resp.SC_BAD_REQUEST);
    }

    private void buildFullHtmlListing(HttpServletResponse resp, List<String> uriParts) {
        String testSession = uriParts.get(3);
        String channelId = uriParts.get(4);
        String resourcetype = uriParts.get(5);
        String event = uriParts.get(6);

        File psimdb = new File(request.externalCache, "psimdb");
        File testSessionFile = new File(psimdb, testSession);
        File channelFile = new File(testSessionFile, channelId);
        File fhir = new File(channelFile, "fhir");
        File resourceType = new File(fhir, resourcetype);
        File theEvent = new File(resourceType, event);

        if (!theEvent.exists() || !theEvent.canRead() || !theEvent.isDirectory()) {
            resp.setStatus(resp.SC_NOT_FOUND);
            return;
        }

        //resp.addHeader("Content-Type", "text/html; charset=utf-8");
        StringBuilder b = new StringBuilder();
        b.append("<!DOCTYPE HTML>\n<html><body>");

        b.append("<h1>" + event + "</h1>");

        for (int task=0; ; task++) {
            File taskDir = new File(theEvent, "task" + task);
            if (!taskDir.exists())
                break;
            displayEvent(b, theEvent, "Task" + task);
        }

        b.append("</body></html>");

        try {
            resp.getOutputStream().write(b.toString().getBytes());
        } catch (IOException e) {
            resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void displayEvent(StringBuilder b, File theEvent, String label) {
        String section = label.toLowerCase();
        b.append("<h2>").append(label).append("</h2>");
        UITask uiTask = new UITask(theEvent, section);
        String description = uiTask.getDescription();
        if (!description.equals("")) {
            b.append("<h4>Description</h4>");
            b.append("<pre>").append(description).append("</pre>");
        }
        b.append("<h3>Request</h3>");
        b.append("<pre>").append(uiTask.getRequestHeader()).append("</pre>");
        b.append("<pre>").append(uiTask.getRequestBody()).append("</pre>");

        b.append("<h3>Response</h3>");
        b.append("<pre>").append(uiTask.getResponseHeader()).append("</pre>");
        b.append("<pre>").append(uiTask.getResponseBody()).append("</pre>");
    }
}
