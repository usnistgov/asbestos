package gov.nist.asbestos.services.restRequests;

// 0 - ""
// 1 - context
// 2 - "log"
// 3 - "analysis"
// 4 - "event"
// 5 - session
// 6 - channelName
// 7 - eventId
// 8 - "request" or "response"


import com.google.common.base.Strings;
import gov.nist.asbestos.analysis.AnalysisReport;
import gov.nist.asbestos.analysis.RelatedReport;
import gov.nist.asbestos.analysis.Report;
import gov.nist.asbestos.client.Base.EventContext;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.events.UIEvent;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import java.util.logging.Logger;

import java.io.IOException;

public class GetLogEventAnalysisByEventRequest {
    private static Logger log = Logger.getLogger(GetLogEventAnalysisByEventRequest.class.getName());

    public GetLogEventAnalysisByEventRequest(Request request) {
        request.setType(this.getClass().getSimpleName());
        model = new Model(request);
    }

    public static boolean isRequest(Request request) {
        return (request.uriParts.size() == 9
                && "log".equalsIgnoreCase(request.uriParts.get(2))
                && "analysis".equalsIgnoreCase(request.uriParts.get(3))
                && "event".equalsIgnoreCase(request.uriParts.get(4)));
    }

    static class Model {
        Request request;
        String eventId;
        String resourceType;
        boolean focusOnRequest;
        UIEvent uiEvent;
        ResourceWrapper focusWrapper;
        ResourceWrapper uiEventWrapper;
        EventContext eventContext;
        EventAnalysisParams eventAnalysisParams;
        ResourceWrapper contextBundle;
        boolean done = false;

        Model(Request request) {
            this.request = request;
            request.testSession = request.uriParts.get(5);
            request.channelId = request.uriParts.get(6);

            String url = request.getParametersMap().get("focusUrl");
            if (!Strings.isNullOrEmpty(url) && url.startsWith("http")) {
                focusWrapper = new ResourceWrapper(new Ref(url));
                focusWrapper.getResource();
            } else {

                eventId = request.uriParts.get(7);
                resourceType = request.ec.resourceTypeForEvent(
                        request.ec.fhirDir(request.testSession, request.channelId),
                        eventId);
                uiEvent = new UIEvent(request.ec).fromParms(
                        request.testSession,
                        request.channelId,
                        resourceType,
                        eventId);
                if (uiEvent == null) {
                    request.badRequest();
                    done = true;
                }
                String requestType = request.uriParts.get(8);
                focusOnRequest = "request".equals(requestType);
                uiEventWrapper = new ResourceWrapper();
                uiEventWrapper.setEvent(uiEvent, focusOnRequest);
                uiEventWrapper.getRef().addParameters(request.getParametersMap());
                uiEventWrapper.setFocusUrl(url);
                eventContext = new EventContext(uiEvent);
                eventContext.setRequestFocus(focusOnRequest);
                uiEventWrapper.getResource();
                eventAnalysisParams = new EventAnalysisParams(request);

                if ("Bundle".equals(uiEvent.getResourceType())) {
                    Ref bundleBase = new Ref(
                            uiEventWrapper.getRef().uriWithoutParams()
                    );
                    contextBundle = new ResourceWrapper(bundleBase);
                    contextBundle.setRequest(focusOnRequest);
                    contextBundle.setEvent(uiEvent, focusOnRequest);
                    contextBundle.getResource();
                }
            }
        }
    }

    Model model;

    public void run() throws IOException {
        model.request.announce(("GetLogAnalysis by event"));
        if (model.done) return;
        if (model.uiEventWrapper != null)
            EventAnalysisCommon.runAndReturnReport(model.uiEventWrapper, model.request, model.eventContext, model.contextBundle);
        else if (model.focusWrapper != null)
            EventAnalysisCommon.runAndReturnReport(model.focusWrapper, model.request, model.eventContext, model.contextBundle);

    }


}
