package gov.nist.asbestos.asbestosProxy.requests;

import com.google.gson.Gson;
import gov.nist.asbestos.analysis.AnalysisReport;
import gov.nist.asbestos.analysis.RelatedReport;
import gov.nist.asbestos.analysis.Report;
import gov.nist.asbestos.client.Base.EventContext;
import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.events.ProxyEvent;
import gov.nist.asbestos.client.events.UIEvent;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.http.headers.Headers;
import org.apache.log4j.Logger;
import org.checkerframework.checker.units.qual.A;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentManifest;
import org.hl7.fhir.r4.model.Procedure;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
// focusUrl=url (may be null) - for private bundles this is the internal UUID
//             for contained it is ID#anchor
// Returns Report

// OR

// 0 - empty
// 1 - app context  (asbestos)
// 2 - "log"
// 3 - "analysis"
// 4 - "url"
// ?url=TheUrlOfAFHIRResource;gzip=boolean
// Returns Report

// OR

// 0 - empty
// 1 - app context  (asbestos)
// 2 - "log"
// 3 - "analysis"
// 4 - "static"
// 5 - testCollectionId
// 6 - testId
// ?url=reference
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
                && "url".equalsIgnoreCase(request.uriParts.get(4))

                ||

                request.uriParts.size() == 7
                        && "log".equalsIgnoreCase(request.uriParts.get(2))
                        && "analysis".equalsIgnoreCase(request.uriParts.get(3))
                        && "static".equalsIgnoreCase(request.uriParts.get(4));



    }

    public GetLogEventAnalysisRequest(Request request) {
        this.request = request;
    }

    public GetLogEventAnalysisRequest setEventContext(EventContext eventContext) {
        this.eventContext = eventContext;
        return this;
    }

    private boolean analysisTargetIsRequest() {
        String requestOrResponse = request.uriParts.get(8);
        return requestOrResponse.equals("request");
    }

//    private String getParm(String name) {
//        String query = request.req.getQueryString();
//        if (query == null)
//            return null;
//        int parmi = query.indexOf(name + "=");
//        if (parmi == -1)
//            return null;
//        int parmend = query.indexOf(";", parmi);
//        if (parmend == -1)
//            parmend = query.length();
//        int parmstart = query.indexOf("=", parmi);
//        if (parmstart == -1)
//            return null;
//        parmstart++;
//        if (parmend <= parmstart)
//            return null;
//        return query.substring(parmstart, parmend);
//
//    }

    public void run() {
        log.info("GetLogEventAnalysisRequest");

        if (request.uriParts.get(4).equalsIgnoreCase("static")) {
            // load static fixture from test definition
            String testCollectionId = request.uriParts.get(5);
            String testId = request.uriParts.get(6);
            String ref = request.getParm("url");
            if (ref == null) {
                request.resp.setStatus(request.resp.SC_BAD_REQUEST);
                return;
            }
            File testDir = request.ec.getTest(testCollectionId, testId);
            if (!testDir.exists() || !testDir.isDirectory()) {
                request.resp.setStatus(request.resp.SC_NOT_FOUND);
                return;
            }
            File file = new File(testDir, ref);
            if (!file.exists() || !file.isFile()) {
                request.resp.setStatus(request.resp.SC_NOT_FOUND);
                return;
            }
            BaseResource resource;
            try {
                resource = ProxyBase.parse(file);
            } catch (Exception e) {
                returnReport(new Report("Not found or not a resource: " + file));
                return;
            }
            analyseResource(resource, null, true);

        } else if (request.uriParts.get(4).equalsIgnoreCase("event")) {
            String testSession = request.uriParts.get(5);
            String channelId = request.uriParts.get(6);
            String eventId = request.uriParts.get(7);
            String focusUrl = request.getParm("focusUrl");
            String focusAnchor = request.getParm("focusAnchor");
            if (focusUrl != null && focusAnchor != null)
                focusUrl = focusUrl + "#" + focusAnchor;
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
                returnReport(new Report("No content in " + (requestFocus ? "Request" : "Response") + " message"));
                return;
            }



            BaseResource requestResource = null;
            if (requestBodyString.length() > 0)
                requestResource = ProxyBase.parse(requestBodyString, Format.fromContentType(requestHeaders.getContentType().getValue()));
            Bundle requestBundle = null;
            if (requestResource instanceof Bundle)
                requestBundle = (Bundle) requestResource;

            if (baseResource instanceof Bundle) {
                analyseBundle(focusUrl, requestFocus, runValidation, (Bundle) baseResource /*, requestBundle */);
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
                String eventId = null;
                if (query.contains("gzip=true"))
                    gzip = true;
                if (query.contains("ignoreBadRefs=true"))
                    ignoreBadRefs = true;
                if (query.contains("eventId=")) {
                    int index = query.indexOf("eventId=");
                    index = query.indexOf("=", index);
                    index++;
                    int index2 = query.indexOf(";", index);
                    if (index2 == -1)
                        index2 = query.length() -1;
                    if (index2 > index)  // event=  is a possibility
                        eventId = query.substring(index, index2);
                }
                if (query.contains("url=http")){
                    int urlIndex = query.indexOf("url=http") + 4;
                    int urlEndIndex = query.indexOf(";", urlIndex);
                    String url = query.substring(urlIndex, urlEndIndex);
                    Ref ref = new Ref(url);
                    if (eventId != null) {
                        try {
                            eventContext = new EventContext(ProxyEvent.eventFromEventURI(new URI(url)));
                        } catch (Exception e) {
                            throw new RuntimeException("URI " + url + " cannot be translated into an event");
                        }
                    }
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

    void analyseBundle(String focusUrl, boolean requestFocus, boolean runValidation, Bundle bundle /*, Bundle requestBundle */) {
        String focusReference = (focusUrl == null || focusUrl.equals("") || focusUrl.equals("null")) ? getManifestLocation(bundle) : focusUrl;
        boolean isSearchSet = bundle.hasType() && bundle.getType() == Bundle.BundleType.SEARCHSET;
        if (focusReference != null)
            runAndReturnReport(new Ref(focusReference),
                    "focus reference",
                    false,
                    true,
                    false,
                    runValidation,
                    analysisTargetIsRequest() ? bundle : null);
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
    }

    void analyseResource(BaseResource baseResource, EventContext eventContext, boolean runValidation) {
        Report report = new AnalysisReport((Ref)null, "User", request.ec)
                .withContextResource(baseResource)
                .withValidation(runValidation)
                .run();
        returnReport(report, request, eventContext);
    }

    void returnReport(Report report) {
        returnReport(report, request, eventContext);
    }

    public void returnReport(Report report, Request request, EventContext eventContext) {
        Objects.requireNonNull(report);
        if (!report.hasErrors()) {
            Objects.requireNonNull(report.getBase());
            report.getBase().setEventContext(eventContext);
            for (RelatedReport rr : report.getObjects()) {
                rr.setEventContext(eventContext);
            }
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

        Report report;
        try {
            report = analysisReport.run();
        } catch (Throwable t) {
            report = new Report(t.getMessage());
            returnReport(report);
            return;
        }

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

        Report report;
        try {
            report = analysisReport.run();
        } catch (Throwable t) {
            report = new Report(t.getMessage());
            returnReport(report);
            return;
        }

        returnReport(report);
    }

    private void runAndReturnReport(Bundle bundle, String source, boolean isRequest) {
        Ref manifestFullUrl = getManifestFullUrl(bundle);
        AnalysisReport analysisReport = new AnalysisReport(manifestFullUrl, source, request.ec);
        analysisReport.withContextResource(bundle);
        analysisReport.analyseRequest(isRequest);

        Report report;
        try {
            report = analysisReport.run();
        } catch (Throwable t) {
            report = new Report(t.getMessage());
            returnReport(report);
            return;
        }

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
