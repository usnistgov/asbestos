package gov.nist.asbestos.asbestosProxy.requests;

import com.google.gson.Gson;
import gov.nist.asbestos.analysis.AnalysisReport;
import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.events.UIEvent;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.Verb;
import gov.nist.asbestos.http.support.Common;
import org.apache.log4j.Logger;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Bundle;

import java.io.IOException;

// 0 - empty
// 1 - app context  (asbestos)
// 2 - "log"
// 3 - "analysis"
// 4 - testSession
// 5 - channelId
// 6 - eventId

public class GetLogEventAnalysis {
    private static Logger log = Logger.getLogger(GetLogEventAnalysis.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 7 && "log".equalsIgnoreCase(request.uriParts.get(2)) && "analysis".equalsIgnoreCase(request.uriParts.get(3));
    }

    public GetLogEventAnalysis(Request request) {
        this.request = request;
    }

    public void run() {
        log.info("GetLogEventAnalysisRequest");
        Headers headers = Common.getRequestHeaders(request.req, Verb.GET);
        Header acceptHeader = headers.getAccept();
        boolean htmlOk = acceptHeader.getValue().contains("text/html");
        boolean jsonOk = acceptHeader.getValue().contains("json");

        String testSession = request.uriParts.get(4);
        String channelId = request.uriParts.get(5);
        String eventId = request.uriParts.get(6);

        UIEvent event = request.ec.getEvent(testSession, channelId, "null", eventId);
        String responseBodyString = event.getClientTask().getResponseBody();
        Headers responseHeaders = new Headers(event.getClientTask().getResponseHeader());

        BaseResource baseResource = ProxyBase.parse(responseBodyString, Format.fromContentType(responseHeaders.getContentType().getValue()));
        if (baseResource instanceof Bundle) {
            Bundle bundle = (Bundle) baseResource;
            String manifestReference = getManifestLocation(bundle);
            if (manifestReference == null) {
                request.resp.setStatus(request.resp.SC_NO_CONTENT);
                return;
            }
            AnalysisReport analysisReport = new AnalysisReport(new Ref(manifestReference));
            AnalysisReport.Report report = analysisReport.run();
            String json = new Gson().toJson(report);
            request.resp.setContentType("application/json");
            try {
                request.resp.getOutputStream().print(json);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            request.resp.setStatus(request.resp.SC_OK);
            return;
        }

        request.resp.setStatus(request.resp.SC_BAD_REQUEST);
    }

    // from response bundle
    private String getManifestLocation(Bundle bundle) {
        if (!bundle.hasEntry())
            return null;
        for (Bundle.BundleEntryComponent component : bundle.getEntry()) {
            String location = component.getResponse().getLocation();
            if (location == null)
                continue;
            if (location.startsWith("DocumentManifest")) {
                if (bundle.hasLink()) {
                    return bundle.getLink("self").getUrl() + "/" + location;
                }
            }
            if (location.contains("DocumentManifest")) {
                return location;
            }
        }
        return null;
    }

}
