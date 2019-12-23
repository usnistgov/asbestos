package gov.nist.asbestos.asbestosProxy.requests;

import com.google.gson.Gson;
import gov.nist.asbestos.analysis.AnalysisReport;
import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.events.UIEvent;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.Verb;
import gov.nist.asbestos.http.support.Common;
import org.apache.log4j.Logger;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Bundle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// 0 - empty
// 1 - app context  (asbestos)
// 2 - "log"
// 3 - "analysis"
// 4 - "event"
// 5 - testSession
// 6 - channelId
// 7 - eventId

// OR

// 0 - empty
// 1 - app context  (asbestos)
// 2 - "log"
// 3 - "analysis"
// 4 - "url"
// ?url=TheUrlOfAFHIRResource    -- this is a full url (http:...included)



public class GetLogEventAnalysis {
    private static Logger log = Logger.getLogger(GetLogEventAnalysis.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 8
                && "log".equalsIgnoreCase(request.uriParts.get(2))
                && "analysis".equalsIgnoreCase(request.uriParts.get(3))
                && "event".equalsIgnoreCase(request.uriParts.get(4))

        ||

        request.uriParts.size() == 5
                && "log".equalsIgnoreCase(request.uriParts.get(2))
                && "analysis".equalsIgnoreCase(request.uriParts.get(3))
                && "url".equalsIgnoreCase(request.uriParts.get(4));
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

        if (request.uriParts.size() == 8) {
            String testSession = request.uriParts.get(5);
            String channelId = request.uriParts.get(6);
            String eventId = request.uriParts.get(7);

            UIEvent event = request.ec.getEvent(testSession, channelId, "null", eventId);
            String responseBodyString = event.getClientTask().getResponseBody();
            Headers responseHeaders = new Headers(event.getClientTask().getResponseHeader());

            BaseResource baseResource = ProxyBase.parse(responseBodyString, Format.fromContentType(responseHeaders.getContentType().getValue()));
            if (baseResource instanceof Bundle) {
                Bundle bundle = (Bundle) baseResource;
                String manifestReference = getManifestLocation(bundle);
                boolean isSearchSet = bundle.hasType() && bundle.getType() == Bundle.BundleType.SEARCHSET;
                if (manifestReference != null)
                    runAndReturnReport(new Ref(manifestReference), "reference for Manifest taken from transaction response");
                else if (isSearchSet) {
                    List<Ref> refs = new ArrayList<>();
                    for (Bundle.BundleEntryComponent component : bundle.getEntry()) {
                        String url = component.getFullUrl();
                        if (url != null && !url.equals(""))
                            refs.add(new Ref(url));
                    }
                    returnReport(new AnalysisReport.Report("Do not understand event"));
                } else
                    returnReport(new AnalysisReport.Report("Do not understand event"));
            } else if (responseHeaders.hasHeader("Content-Location")) {
                Ref ref = new Ref(responseHeaders.get("Content-Location").getValue());
                runAndReturnReport(ref, "link taken from response Content-location header");
            } else {
                returnReport(new AnalysisReport.Report("Do not understand event"));
            }
        } else {
            String query = request.req.getQueryString();
            if (query != null && query.contains("url=http")) {
                int urlIndex = query.indexOf("url=http") + 4;
                String url = query.substring(urlIndex);
                Ref ref = new Ref(url);
                runAndReturnReport(ref, "By Request");
            }
        }
    }

    private void returnReport(AnalysisReport.Report report) {
        String json = new Gson().toJson(report);
        request.resp.setContentType("application/json");
        try {
            request.resp.getOutputStream().print(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        request.resp.setStatus(request.resp.SC_OK);
    }

    private void runAndReturnReport(Ref ref, String source) {
        AnalysisReport analysisReport = new AnalysisReport(ref, source, request.ec);
        AnalysisReport.Report report = analysisReport.run();
        returnReport(report);
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
