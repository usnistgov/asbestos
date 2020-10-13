package gov.nist.asbestos.services.restRequests;

import com.google.common.base.Strings;
import gov.nist.asbestos.analysis.AnalysisReport;
import gov.nist.asbestos.analysis.RelatedReport;
import gov.nist.asbestos.analysis.Report;
import gov.nist.asbestos.client.Base.EventContext;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.events.UIEvent;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import org.apache.log4j.Logger;

import java.io.IOException;

public class GetLogEventAnalysisByEventRequest {
    private static Logger log = Logger.getLogger(GetLogEventAnalysisByEventRequest.class);

    public GetLogEventAnalysisByEventRequest(Request request) {
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
        ResourceWrapper wrapper;
        EventContext eventContext;
        EventAnalysisParams eventAnalysisParams;
        boolean done = false;

        Model(Request request) {
            this.request = request;
            request.testSession = request.uriParts.get(5);
            request.channelId = request.uriParts.get(6);

            String url = request.getParametersMap().get("focusUrl");
            if (!Strings.isNullOrEmpty(url)) {
                wrapper = new ResourceWrapper(new Ref(url));
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
                focusOnRequest = "request".equals(request.uriParts.get(8));
                wrapper = new ResourceWrapper();
                wrapper.setEvent(uiEvent, focusOnRequest);
                wrapper.getRef().addParameters(request.getParametersMap());
                eventContext = new EventContext(uiEvent);
                eventAnalysisParams = new EventAnalysisParams(request);
            }
        }
    }

    Model model;

    public void run() throws IOException {
        model.request.announce(("GetLogAnalysis by event"));
        if (model.done) return;
        EventAnalysisCommon.runAndReturnReport(model.wrapper, model.request, model.eventContext);
    }


}
