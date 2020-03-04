package gov.nist.asbestos.asbestosProxy.requests;

import com.google.gson.Gson;
import gov.nist.asbestos.analysis.AnalysisReport;
import gov.nist.asbestos.analysis.RelatedReport;
import gov.nist.asbestos.analysis.Report;
import gov.nist.asbestos.client.Base.EventContext;
import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.events.UIEvent;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.http.headers.Headers;
import org.apache.log4j.Logger;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentManifest;

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
// 8 - "request" or "response"
// ?validation=true
// Returns Report

// OR

// 0 - empty
// 1 - app context  (asbestos)
// 2 - "log"
// 3 - "analysis"
// 4 - "url"
// ?url=TheUrlOfAFHIRResource;gzip=boolean
// Returns Report



public class GetLogEventAnalysisRequest {
    private static Logger log = Logger.getLogger(GetLogEventAnalysisRequest.class);
    private EventContext eventContext;
    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 9
                && "log".equalsIgnoreCase(request.uriParts.get(2))
                && "analysis".equalsIgnoreCase(request.uriParts.get(3))
                && "event".equalsIgnoreCase(request.uriParts.get(4))

        ||

        request.uriParts.size() == 5
                && "log".equalsIgnoreCase(request.uriParts.get(2))
                && "analysis".equalsIgnoreCase(request.uriParts.get(3))
                && "url".equalsIgnoreCase(request.uriParts.get(4));
    }

    public GetLogEventAnalysisRequest(Request request) {
        this.request = request;
    }

    private boolean analysisTargetIsRequest() {
        String requestOrResponse = request.uriParts.get(8);
        return requestOrResponse.equals("request");
    }

    public void run() {
        log.info("GetLogEventAnalysisRequest");

        if (request.uriParts.get(4).equalsIgnoreCase("event")) {
            String testSession = request.uriParts.get(5);
            String channelId = request.uriParts.get(6);
            String eventId = request.uriParts.get(7);
            boolean requestFocus = analysisTargetIsRequest();
            eventContext = new EventContext(testSession, channelId, eventId, requestFocus);

            UIEvent event = request.ec.getEvent(eventContext);
            String requestBodyString = event.getRequestBody();
            Headers requestHeaders = event.getRequestHeader();
            String responseBodyString = event.getResponseBody();
            Headers responseHeaders = event.getResponseHeader();

            String analysisSource = requestFocus ? requestBodyString : responseBodyString;

            boolean runValidation = false;
            String query = request.req.getQueryString();
            if (query != null) {
                if (query.contains("validation=true"))
                    runValidation = true;
            }


            BaseResource baseResource;
            try {
                baseResource = ProxyBase.parse(analysisSource, Format.fromContentType(responseHeaders.getContentType().getValue()));
            } catch (Exception e) {
                returnReport(new Report("No content"));
                return;
            }



            BaseResource requestResource = null;
            if (requestBodyString.length() > 0)
                requestResource = ProxyBase.parse(requestBodyString, Format.fromContentType(requestHeaders.getContentType().getValue()));
            Bundle requestBundle = null;
            if (requestResource instanceof Bundle)
                requestBundle = (Bundle) requestResource;

            if (baseResource instanceof Bundle) {
                Bundle bundle = (Bundle) baseResource;
                String manifestReference = getManifestLocation(bundle);
                boolean isSearchSet = bundle.hasType() && bundle.getType() == Bundle.BundleType.SEARCHSET;
                if (manifestReference != null)
                    runAndReturnReport(new Ref(manifestReference),
                            "reference for Manifest taken from transaction response",
                            false,
                            true,
                            false,
                            runValidation,
                            analysisTargetIsRequest() ? requestBundle : null);
                else if (isSearchSet) {
                    List<Ref> refs = new ArrayList<>();
                    for (Bundle.BundleEntryComponent component : bundle.getEntry()) {
                        String url = component.getFullUrl();
                        if (url != null && !url.equals(""))
                            refs.add(new Ref(url));
                    }
                    runAndReturnReport(bundle, "A Bundle", requestFocus);
                    //returnReport(new Report("Do not understand event"));
                } else {
                    runAndReturnReport(bundle,
                            "Static Bundle",
                            requestFocus,
                            false,
                            false,
                            false,
                            runValidation
                    );
                    //returnReport(new Report("Do not understand event"));
                }
            } else if (responseHeaders.hasHeader("Content-Location")) {
                Ref ref = new Ref(responseHeaders.get("Content-Location").getValue());
                runAndReturnReport(ref, "link taken from response Content-location header", false, false, false, runValidation, requestBundle);
            } else {
                returnReport(new Report("Do not understand event"));
            }
        } else {   // url
            String query = request.req.getQueryString();
            if (query != null) {
                boolean gzip = false;
                boolean useProxy = true;
                boolean ignoreBadRefs = false;
                if (query.contains("gzip=true"))
                    gzip = true;
                if (query.contains("ignoreBadRefs=true"))
                    ignoreBadRefs = true;
                if (query.contains("url=http")){
                    int urlIndex = query.indexOf("url=http") + 4;
                    int urlEndIndex = query.indexOf(";", urlIndex);
                    String url = query.substring(urlIndex, urlEndIndex);
                    Ref ref = new Ref(url);
                    runAndReturnReport(ref, "By Request", gzip, useProxy, ignoreBadRefs, false, null);
                }
//                else if (query.contains("url=urn:uuid")) {
//                    int urlIndex = query.indexOf("url=urn:uuid") + 4;
//                    int urlEndIndex = query.indexOf(";", urlIndex);
//                    String url = query.substring(urlIndex, urlEndIndex);
//                    Ref ref = new Ref(url);
//                    runAndReturnReport(ref, "By Request", gzip, useProxy, baseResource);
//                }
            }
        }
    }

    private void returnReport(Report report) {
        report.getBase().setEventContext(eventContext);
        for (RelatedReport rr : report.getObjects()) {
            rr.setEventContext(eventContext);
        }

        String json = new Gson().toJson(report);
        request.resp.setContentType("application/json");
        try {
            request.resp.getOutputStream().print(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        request.resp.setStatus(request.resp.SC_OK);
    }

    private void runAndReturnReport(Ref ref, String source, boolean gzip, boolean useProxy, boolean ignoreBadRefs, boolean withValidation, BaseResource contextBundle) {
        AnalysisReport analysisReport = new AnalysisReport(ref, source, request.ec)
                .withGzip(gzip)
                .withProxy(useProxy)
                .withValidation(withValidation)
                .withContextResource(contextBundle);
        Report report = analysisReport.run();

        if (ignoreBadRefs) {
            List<String> errors = report.getErrors();
            List<String> newErrors = new ArrayList<>();
            for (String error : errors) {
                if (error.startsWith("Cannot load")) {
                    String[] words = error.split(" ");
                    String url = words[2];
                    if (url.startsWith("http")) {
                        Ref ref1 = new Ref(url);
                        if (!ref1.hasResource())
                            continue;

                    }
                }
                newErrors.add(error);
            }
            report.setErrors(newErrors);
        }

        returnReport(report);
    }

    private void runAndReturnReport(Bundle bundle, String source, boolean isRequest, boolean gzip, boolean useProxy, boolean ignoreBadRefs, boolean withValidation) {
        Ref manifestFullUrl = getManifestFullUrl(bundle);
        AnalysisReport analysisReport = new AnalysisReport(manifestFullUrl, source, request.ec)
                .withGzip(gzip)
                .withProxy(useProxy)
                .withValidation(withValidation)
                .withContextResource(bundle)
                .analyseRequest(isRequest);
        Report report = analysisReport.run();
        returnReport(report);
    }

    private void runAndReturnReport(Bundle bundle, String source, boolean isRequest) {
        Ref manifestFullUrl = getManifestFullUrl(bundle);
        AnalysisReport analysisReport = new AnalysisReport(manifestFullUrl, source, request.ec);
        analysisReport.withContextResource(bundle);
        analysisReport.analyseRequest(isRequest);
        Report report = analysisReport.run();
        returnReport(report);
    }

    private Ref getManifestFullUrl(Bundle bundle) {
        for( Bundle.BundleEntryComponent comp : bundle.getEntry()) {
            if (comp.getResource() instanceof DocumentManifest) {
                return new Ref(comp.getFullUrl());
            }
        }
        return null;
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
