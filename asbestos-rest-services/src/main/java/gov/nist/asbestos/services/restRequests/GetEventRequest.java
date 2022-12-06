package gov.nist.asbestos.services.restRequests;

import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.events.Event;
import gov.nist.asbestos.client.events.ITask;
import gov.nist.asbestos.client.events.UITask;
import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.Verb;
import gov.nist.asbestos.http.support.Common;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static gov.nist.asbestos.client.client.Format.quickEscapeXml;

// 0 - empty
// 1 - app context  (asbestos)
// 2 - "log"
// 3 - testSession
// 4 - channelId
// 5 - resourceType - may be null
// 6 - eventId
// Query string
// textMode=raw
// textMode=prettyprint
// returns UIEvent as JSON

public class GetEventRequest {
    private static Logger log = Logger.getLogger(GetEventRequest.class.getName());

    private Request request;
    private boolean rawTextMode;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 7 && "log".equalsIgnoreCase(request.uriParts.get(2));
    }

    public GetEventRequest(Request request) {
        request.setType(this.getClass().getSimpleName());

        this.request = request;
        String textMode = request.getParm("textMode");
        rawTextMode = (textMode != null && "raw".equals(textMode));
    }

        public void run() throws IOException {
        request.announce("GetEventRequest");
        Headers headers = Common.getRequestHeaders(request.req, Verb.GET);
        Header acceptHeader = headers.getAccept();
        boolean jsonOk = acceptHeader.getValue().contains("json");
        if (request.uriParts.size() == 7) {  // includes event
            if (jsonOk) {
                request.ec.buildEventJson(
                        request,
                        request.uriParts.get(3),
                        request.uriParts.get(4),
                        request.uriParts.get(5),
                        request.uriParts.get(6));
                return;
            } else  {
                buildFullHtmlListing(request.resp, request.uriParts);
            }
        }
        request.ok();
    }

    private void buildFullHtmlListing(HttpServletResponse resp, List<String> uriParts) throws IOException {
        String testSession = uriParts.get(3);
        String channelId = uriParts.get(4);
        String resourcetype = uriParts.get(5);
        String event = uriParts.get(6);

        File psimdb = new File(request.externalCache, "FhirChannels");
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
        b.append("<!DOCTYPE HTML>\n" +
                "<html>" +
                "<head>" +
                "<style>" +
                "      pre {" +
                "         white-space: pre-wrap;" +
                "         word-wrap: break-word;" +
                "    }" +
                "</style>" +
                "</head>" +
                "<body>");

        b.append("<h1>").append(event).append("</h1>");

        for (int task=0; ; task++) {
            File taskDir = new File(theEvent, "task" + task);
            if (!taskDir.exists())
                break;
            displayEvent(b, theEvent, "Task" ,  task);
        }

        b.append("</body></html>");

        resp.getOutputStream().write(b.toString().getBytes());
    }

    private void displayEvent(StringBuilder b, File theEvent, String labelPrefix, int taskIndex ) {
        String label = labelPrefix + taskIndex;
        Format format = null;
        try {
            Event e = new Event(theEvent);
            ITask task = e.getTask(taskIndex);
            if (task != null) {
                String headerValue = task.getRequestHeader().getContentType().getAllValuesAndParmsAsString();
                if (!headerValue.toLowerCase().contains("json")) {
                    format = Format.fromContentType(headerValue); // Sniff xml
                }
            }
        } catch (Exception ex) {
            log.severe("displayEvent Exception: " + ex.toString());
        }
        String section = label.toLowerCase();
        b.append("<h2>").append(label).append("</h2>");
        UITask uiTask = new UITask(theEvent, section, rawTextMode);
        String description = uiTask.getDescription();
        if (!description.equals("")) {
            b.append("<h4>Description</h4>");
            b.append("<pre>").append(description).append("</pre>");
        }
        b.append("<h3>Request</h3>");
        b.append("<pre>").append(uiTask.getRequestHeader()).append("</pre>");
        if (format != null && format == Format.XML) {
            b.append("<pre>").append(quickEscapeXml(uiTask.getRequestBody())).append("</pre>");
        } else {
            b.append("<pre>").append(uiTask.getRequestBody()).append("</pre>");
        }

        b.append("<h3>Response</h3>");
        b.append("<pre>").append(uiTask.getResponseHeader()).append("</pre>");
        if (format != null && format == Format.XML) {
            b.append("<pre>").append(quickEscapeXml(uiTask.getResponseBody())).append("</pre>");
        } else {
            b.append("<pre>").append(uiTask.getResponseBody()).append("</pre>");
        }
    }


}
