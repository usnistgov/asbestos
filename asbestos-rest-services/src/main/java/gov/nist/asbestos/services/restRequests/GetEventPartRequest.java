package gov.nist.asbestos.services.restRequests;

import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.Base.Returns;
import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.events.UIEvent;
import gov.nist.asbestos.client.events.UITask;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.services.servlet.ChannelConnector;
import gov.nist.asbestos.testEngine.engine.ModularReports;
import org.apache.log4j.Logger;
import org.hl7.fhir.r4.model.TestReport;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
// 0 - empty
// 1 - app context
// 2 - "engine"
// 3 - "eventPart"
// 4 - channelId
// 5 - testCollectionId
// 6 - testId
// Example: https://fhirtoolkit.test:9743/asbestos/engine/eventPart/default__limited/MHD_DocumentRecipient_minimal/1_Prerequisite_Single_Document_Submit_with_Binary
// ?module=SendPDB&testIndex=0&actionIndex=0&eventPartLocation=operation.detail&targetTaskIndex=0&return=responseBody
//

public class GetEventPartRequest {
    private static Logger log = Logger.getLogger(GetEventPartRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        if (request.uriParts.size() == 7) {
            String uriPart3 = request.uriParts.get(3);
            return "eventPart".equals(uriPart3);
        }
        return false;
    }

    public GetEventPartRequest(Request request) {
        this.request = request;
    }

    public void run() throws IOException {
        request.announce("GetEventPartRequest");
        String channelId = request.uriParts.get(4);
        String testCollection = request.uriParts.get(5);

        ChannelConfig channelConfig = ChannelConnector.getChannelConfig(request.resp, request.externalCache, channelId);
        if (channelConfig == null) {
            request.badRequest("Channel " + channelId + " does not exist");
            return;
        }

        String testName = URLDecoder.decode(request.uriParts.get(6), StandardCharsets.UTF_8.toString());
        Map<String, String> paramsMap = request.getParametersMap();

        ModularReports modularReports = new ModularReports(request.ec, channelId, testCollection, testName, true);
        Map<String,TestReport> reportsMap = modularReports.getReportsObject();
        // Check if entire test passed
        // /Missing_DocumentManifest?module=SendPDB&testIndex=0&actionIndex=0&eventPartLocation=operation.detail&targetTaskIndex=0&return=responseBody
        TestReport theReport = reportsMap.get(testName);
        if (theReport == null) {
            unexpectedMessage(String.format("TestReport was not found for %s. Test was probably not yet run.", testName));
            return;
        }
        if (TestReport.TestReportResult.PASS.equals(theReport.getResult())) {
            String moduleName = paramsMap.get("module");
            if (moduleName != null) {
                testName = testName + "/" + moduleName;
                theReport = reportsMap.get(testName);
            }
            if (! TestReport.TestReportStatus.COMPLETED.equals(theReport.getStatus())) {
                String message = String.format("%s: Test is not Completed.", testName);
                unexpectedMessage(message);
                return;
            }
            int testIndex = Integer.parseInt(paramsMap.get("testIndex"));
            TestReport.TestReportTestComponent testComponent = theReport.getTest().get(testIndex);
            if (testComponent.hasAction()) {
                int actionIndex = Integer.parseInt(paramsMap.get("actionIndex"));
                TestReport.TestActionComponent actionComponent =  testComponent.getAction().get(actionIndex);
                String eventPartLocation = paramsMap.get("eventPartLocation");
                if (eventPartLocation == null) {
                    String message = "eventPartLocation parameter cannot be null.";
                    unexpectedMessage(message);
                    return;
                }
                String[] eventParts = eventPartLocation.split("\\.");
                if (eventParts.length == 2) {
                   if ("operation".equals(eventParts[0])) {
                       if ("detail".equals(eventParts[1])) {
                           String detailString = actionComponent.getOperation().getDetail();
                           try {
                               URI eventPartLocationDetail = new URI(detailString);
                               UIEvent uiEvent = new UIEvent(request.ec).fromURI(eventPartLocationDetail);
                               int targetTaskIndex = Integer.parseInt(paramsMap.get("targetTaskIndex"));
                               if (targetTaskIndex < uiEvent.getTaskCount()) {
                                   UITask uiTask = uiEvent.getTask(targetTaskIndex);
                                   if ("responseBody".equals(paramsMap.get("return"))) {
                                       // May need to encode to JSON if original response format is XML
                                       String responseBody = uiTask.getResponseBody();
                                       if (responseBody == null) {
                                           unexpectedMessage("Response body is null for uiTask Label: " + uiTask.getLabel());
                                           return;
                                       }
                                       String responseHeader = uiTask.getResponseHeader();
                                       Headers headers = new Headers(responseHeader);
                                       if (headers != null
                                               && headers.getContentType() != null
                                               && headers.getContentType().getAllValuesAsString().toLowerCase().contains("xml")) {
                                           try {
                                               String jsonStr = ParserBase.encode(ParserBase.parse(uiTask.getResponseBody(), Format.XML), Format.JSON);
                                               Returns.returnString(request.resp, jsonStr);
                                               return;
                                           } catch (Exception ex) {
                                               unexpectedMessage("responseBody Parser Exception: " + ex.toString());
                                               return;
                                           }
                                       } else {
                                           Returns.returnString(request.resp, uiTask.getResponseBody());
                                           return;
                                       }
                                   } else {
                                       unexpectedMessage("Unknown or missing Return parameter.");
                                       return;
                                   }
                               }
                           } catch (Exception ex) {
                               unexpectedMessage(String.format("%s is not an URI. Exception is: %s", detailString, ex.toString()));
                               return;
                           }
                       } else {
                           unexpectedMessage("Not a detail.");
                           return;
                       }
                   } else {
                       unexpectedMessage("Not an operation.");
                       return;
                   }
                } else {
                    unexpectedMessage(String.format("Invalid parts length %d", eventParts.length));
                    return;
                }
            }
            } else {
                String message = String.format("%s: Test did not Pass.", testName);
                unexpectedMessage(message);
                return;
            }

        request.ok();
    }

    private void unexpectedMessage(String message) throws IOException {
        log.warn(message);
        Returns.returnPlainTextResponse(request.resp, 400, message);
    }
}
