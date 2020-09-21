package gov.nist.asbestos.asbestosProxy.requests;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import gov.nist.asbestos.analysis.AnalysisReport;
import gov.nist.asbestos.analysis.RelatedReport;
import gov.nist.asbestos.analysis.Report;
import gov.nist.asbestos.client.Base.EventContext;
import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.events.UIEvent;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.headers.Headers;
import org.apache.log4j.Logger;
import org.hl7.fhir.r4.model.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GetLogEventAnalysisRequest {
    private static Logger log = Logger.getLogger(GetLogEventAnalysisRequest.class);
    private EventContext eventContext;
    private gov.nist.asbestos.client.Base.Request request;

    public static boolean isRequest(gov.nist.asbestos.client.Base.Request request) {
        return (request.uriParts.size() == 9
                && "log".equalsIgnoreCase(request.uriParts.get(2))
                && "analysis".equalsIgnoreCase(request.uriParts.get(3))
                && "event".equalsIgnoreCase(request.uriParts.get(4))
                ||
                ("log".equalsIgnoreCase(request.uriParts.get(2))
                && "analysis".equalsIgnoreCase(request.uriParts.get(3))
                && "url".equalsIgnoreCase(request.uriParts.get(4)))
                );
    }

    public GetLogEventAnalysisRequest(gov.nist.asbestos.client.Base.Request request) {
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

    private File testDir = null;

    private File getTestDir(String testCollectionId, String testId) {
        File testDir = null;
        if (testCollectionId != null && testId != null) {
            testDir = request.ec.getTest(testCollectionId, testId);
            if (testDir == null || !testDir.exists() || !testDir.isDirectory()) {
                log.info(testDir + " does not exist or is not directory");
                request.notFound();
                return null;
            }
        }
        return testDir;
    }

    public void run() throws IOException {
        request.announce("GetLogEventAnalysisRequest");

        if ("event".equalsIgnoreCase(request.uriParts.get(4))) {
            fromEventURL();
        } else if ("url".equalsIgnoreCase(request.uriParts.get(4))) {
            fromPassedURL();
        } else {
            request.badRequest();
        }
    }

    private void oldRun() throws IOException {
        if (request.uriParts.get(4).equalsIgnoreCase("static")) {
            testDir = getTestDir(request.segment(5), request.segment(6));
            // load static fixture from test definition
            String ref = request.getParm("url");
            ResourceWrapper resource = getResourceFromTestDefinition(testDir, ref);
            if (resource == null)
                throw new RuntimeException("Resource not found");
            analyseResource(resource, null, true);

        } else if (request.uriParts.get(4).equalsIgnoreCase("event")) {
            String testSession = request.uriParts.get(5);
            String channelId = request.uriParts.get(6);
            String eventId = request.uriParts.get(7);
            String focusUrl = request.getParm("focusUrl");
            String focusAnchor = request.getParm("focusAnchor");
            if (focusUrl != null && focusAnchor != null)
                focusUrl = focusUrl + "#" + focusAnchor;
            boolean requestFocus = analysisTargetIsRequest();  // focus on request (or response)?
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
                baseResource = ParserBase.parse(analysisSource, Format.fromContentType(responseHeaders.getContentType().getValue()));
                if (!(baseResource instanceof Bundle) &&  baseResource.getId() == null) {
                    Ref ref = new Ref(requestHeaders.getPathInfo());
                    baseResource.setId(ref.getId());
                }
            } catch (Exception e) {
                returnReport(new Report("No content in " + (requestFocus ? "Request" : "Response") + " message"));
                return;
            }

            ResourceWrapper sourceWrapper = new ResourceWrapper(baseResource)
                    .setEvent(event, requestFocus);

            BaseResource requestResource = null;
            if (requestBodyString.length() > 0)
                requestResource = ParserBase.parse(requestBodyString, Format.fromContentType(requestHeaders.getContentType().getValue()));
            ResourceWrapper requestBundle = null;
            if (requestResource instanceof Bundle)
                requestBundle = new ResourceWrapper(requestResource);

            if (baseResource instanceof Bundle) {
                analyseBundle(focusUrl, requestFocus, runValidation, sourceWrapper /*, requestBundle */);
            } else if (responseHeaders.hasHeader("Content-Location")) {
                Ref ref = new Ref(responseHeaders.get("Content-Location").getValue());
                runAndReturnReport(
                        ref,
                        "link taken from response Content-location header",
                        false,
                        false,
                        false,
                        runValidation,
                        requestBundle);
            } else {
                Ref ref = new Ref("http://" + requestHeaders.getHeaderValue("host") + requestHeaders.getPathInfo());
                runAndReturnReport(new ResourceWrapper(baseResource).setRef(ref));
            }
        } else if (request.uriParts.get(4).equalsIgnoreCase("url")) {   // url
            String query = request.req.getQueryString();
            if (Strings.isNullOrEmpty(query)) {
                request.badRequest();
                return;
            }
            if (!query.startsWith("url=")) {
                request.badRequest();
                return;
            }
            String url = query.substring(4);
            if (!url.contains("?") && url.contains(";")) {
                url = url.substring(0, url.indexOf(';'));
            }
            Map<String, String> queryParams = Ref.parseParameters(query);
            boolean gzip = queryParams.containsKey("gzip") && "true".equals(queryParams.get("gzip"));
            boolean useProxy = queryParams.containsKey("useProxy") && "true".equals(queryParams.get("useProxy"));;
            boolean ignoreBadRefs = queryParams.containsKey("ignoreBadRefs") && "true".equals(queryParams.get("ignoreBadRefs"));
            String fixturePath = queryParams.get("fixturePath");
            Ref ref = new Ref(url);
            UIEvent uiEvent;
            gov.nist.asbestos.client.Base.Request target;
            try {
                target = new gov.nist.asbestos.client.Base.Request(url, request.externalCache);
                uiEvent = new UIEvent(target.ec).fromURI(target.uri);
            } catch (URISyntaxException e) {
                request.badRequest();
                return;
            }
            File testDir = getTestDir(target.segment(4), target.segment(5));  // testCollectionId, testId

            ResourceWrapper resource = null;
            if (testDir != null && !Strings.isNullOrEmpty(fixturePath)) {
                resource = getResourceFromTestDefinition(testDir, fixturePath);
                if (resource == null) {
                    request.badRequest();
                    return;
                }
                runAndReturnReport(
                        ref,
                        "By Request",
                        gzip,
                        useProxy,
                        ignoreBadRefs,
                        false,
                        resource);
                return;
            }

            runAndReturnReport(
                    ref,
                    "By Request",
                    gzip,
                    useProxy,
                    ignoreBadRefs,
                    false,
                    resource    // contextBundle
            );
       }
    }

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

    void fromPassedURL() throws IOException {
        Map<String, String> queryParams = request.getParametersMap();
        String url = queryParams.get("url");
        if (Strings.isNullOrEmpty(url)) {
            request.badRequest();
            return;
        }
        boolean gzip = queryParams.containsKey("gzip") && "true".equals(queryParams.get("gzip"));
        boolean ignoreBadRefs = queryParams.containsKey("ignoreBadRefs") && "true".equals(queryParams.get("ignoreBadRefs"));
        Ref ref = new Ref(url);
        runAndReturnReport(
                ref,
                "By Request",
                gzip,
                false,
                ignoreBadRefs,
                false,
                null    // contextBundle
        );

    }

    // 0 - empty
    // 1 - app context  (asbestos)
    // 2 - "log"
    // 3 - "analysis"
    // 4 - "event"
    // 5 - testSession
    // 6 - channelId
    // 7 - eventId
    // 8 - "request" or "response"

    // optional parameters
    // validation (true|false (default)) (accepted but ignored)
    // focusUrl (used with Bundles to focus on resource within)
    // gzip (true|false) - use gzip when issuing queries
    // useProxy (true|false) - issue queries through proxy (or bypass and go directly to server)

    void fromEventURL() throws IOException {
        Map<String, String> queryParams = request.getParametersMap();
        boolean gzip = queryParams.containsKey("gzip") && "true".equals(queryParams.get("gzip"));
        boolean useProxy = queryParams.containsKey("useProxy") && "true".equals(queryParams.get("useProxy"));;
        boolean ignoreBadRefs = queryParams.containsKey("ignoreBadRefs") && "true".equals(queryParams.get("ignoreBadRefs"));
        request.testSession = request.uriParts.get(5);
        request.channelId = request.uriParts.get(6);
        String eventId = request.uriParts.get(7);
        String resourceType = request.ec.resourceTypeForEvent(
                request.ec.fhirDir(request.testSession, request.channelId),
                eventId);
        UIEvent uiEvent = new UIEvent(request.ec).fromParms(
                request.testSession,
                request.channelId,
                resourceType,
                eventId);

        if (uiEvent == null) {
            request.badRequest();
            return;
        }

        boolean focusOnRequest = "request".equals(request.uriParts.get(8));

        ResourceWrapper wrapper = new ResourceWrapper();
        wrapper.setEvent(uiEvent, focusOnRequest);
        wrapper.getRef().addParameters(queryParams);
        runAndReturnReport(wrapper);
//
//        // this is for the log object.  The request (above) has its own syntax that is different
//        Ref uiEventRef = new Ref(uiEvent.getURI());
//
//        if (queryParams.containsKey(Ref.FOCUSURL))
//            uiEventRef.setFocusUrl(queryParams.get(Ref.FOCUSURL));
//
//        ResourceWrapper contextBundle = getResourceFromEvent(uiEvent, focusOnRequest);
//
//        runAndReturnReport(
//                uiEventRef,
//                "By Request",
//                gzip,
//                useProxy,
//                ignoreBadRefs,
//                false,
//                contextBundle    // contextBundle
//        );
//
    }


    ResourceWrapper getResourceFromEvent(UIEvent event, boolean focusOnRequest) {
        String requestBodyString = event.getRequestBody();
        Headers requestHeaders = event.getRequestHeader();
        String responseBodyString = event.getResponseBody();
        Headers responseHeaders = event.getResponseHeader();
        String analysisSource = focusOnRequest ? requestBodyString : responseBodyString;

        BaseResource baseResource;
        try {
            baseResource = ParserBase.parse(analysisSource, Format.fromContentType(responseHeaders.getContentType().getValue()));
            if (!(baseResource instanceof Bundle) &&  baseResource.getId() == null) {
                Ref ref = new Ref(requestHeaders.getPathInfo());
                baseResource.setId(ref.getId());
            }
        } catch (Exception e) {
            //returnReport(new Report("No content in " + (focusOnRequest ? "Request" : "Response") + " message"));
            return null;
        }

        ResourceWrapper wrapper = new ResourceWrapper(baseResource)
                .setEvent(event, focusOnRequest);
        return wrapper;
    }

    ResourceWrapper getResourceFromTestDefinition(File testDir, String ref) throws IOException {
        if (Strings.isNullOrEmpty(ref))
            throw new RuntimeException("Bad path to fixture");

        if (testDir == null)
            throw new RuntimeException("Test not found");
        File file = new File(testDir, ref);
        if (!file.exists() || !file.isFile())
            throw new RuntimeException("File within fixture not found");
        BaseResource resource;
        try {
            resource = ParserBase.parse(file);
        } catch (Exception e) {
            returnReport(new Report("Not found or not a resource: " + file));
            return null;
        }
        ResourceWrapper wrapper = new ResourceWrapper(resource);
        wrapper.setFile(file);
        return wrapper;
    }

    void analyseBundle(String focusUrl, boolean requestFocus, boolean runValidation, ResourceWrapper bundleWrapper /*, Bundle requestBundle */) throws IOException {
        if (!"Bundle".equals(bundleWrapper.getResourceType()))
            throw new RuntimeException("Not a Bundle");
        Bundle bundle = (Bundle) bundleWrapper.getResource();
        String focusReference = (focusUrl == null || focusUrl.equals("") || focusUrl.equals("null")) ? getManifestLocation(bundle) : focusUrl;
        boolean isSearchSet = bundle.hasType() && bundle.getType() == Bundle.BundleType.SEARCHSET;
        if (focusReference != null)
            runAndReturnReport(new Ref(focusReference),
                    "focus reference",
                    false,
                    true,
                    false,
                    runValidation,
                    analysisTargetIsRequest() ? bundleWrapper : null);
        else if (isSearchSet) {
            runAndReturnReport(bundleWrapper, "A Bundle", requestFocus);
        } else {
            runAndReturnReport(bundleWrapper,
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

    void analyseResource(ResourceWrapper baseResource, EventContext eventContext, boolean runValidation) throws IOException {
        Report report = new AnalysisReport((Ref)null, "User", request.ec)
                .withContextResource(baseResource)
                .withValidation(runValidation)
                .run();
        returnReport(report, request, eventContext);
    }

    void returnReport(Report report) throws IOException {
        returnReport(report, request, eventContext);
    }

    public void returnReport(Report report, Request request, EventContext eventContext) throws IOException {
        Objects.requireNonNull(report);
        if (!report.hasErrors()) {
            if (report.getBase() != null) {
                report.getBase().setEventContext(eventContext);
                for (RelatedReport rr : report.getObjects()) {
                    rr.setEventContext(eventContext);
                }
            }
        }
        String json = new Gson().toJson(report);
        request.resp.setContentType("application/json");
        request.resp.getOutputStream().print(json);
        request.ok();
    }

    private void runAndReturnReport(Ref ref, String source, boolean gzip, boolean useProxy, boolean ignoreBadRefs, boolean withValidation, ResourceWrapper contextBundle) throws IOException {
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

    private void runAndReturnReport(ResourceWrapper bundleWrapper, String source, boolean isRequest, boolean gzip, boolean useProxy, boolean ignoreBadRefs, boolean withValidation) throws IOException {
        if (!"Bundle".equals(bundleWrapper.getResourceType()))
            throw new RuntimeException("Not a Bundle");
        Ref focusRef = AnalysisReport.isPDBRequest(bundleWrapper)
                ? getManifestFullUrl(bundleWrapper) : bundleWrapper.getRef();
        AnalysisReport analysisReport = new AnalysisReport(focusRef, source, request.ec)
                .withGzip(gzip)
                .withProxy(useProxy)
                .withValidation(withValidation)
                .withContextResource(bundleWrapper)
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

    private void runAndReturnReport(ResourceWrapper wrapper) throws IOException {
        Report report;
        try {
            wrapper.getResource();  // maybe force pulling of resource from logs
            report = new AnalysisReport(request.ec, wrapper).run();
        } catch (Throwable t) {
            report = new Report(t.getMessage());
            returnReport(report);
            return;
        }
        returnReport(report);
    }

    private void runAndReturnReport(ResourceWrapper bundleWrapper, String source, boolean isRequest) throws IOException {
        Ref manifestFullUrl = getManifestFullUrl(bundleWrapper);
        AnalysisReport analysisReport = new AnalysisReport(manifestFullUrl, source, request.ec);
        analysisReport.withContextResource(bundleWrapper);
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

    private Ref getManifestFullUrl(ResourceWrapper wrapper) {
        Bundle bundle = (Bundle) wrapper.getResource();
        for( Bundle.BundleEntryComponent comp : bundle.getEntry()) {
            if (comp.getResource() instanceof DocumentManifest) {
                String url = comp.getFullUrl();
                if (url != null && url.startsWith("urn:uuid") && wrapper.getRef().asString().startsWith("http")) {
                    Ref ref = wrapper.getRef().copy();
                    ref.addParameter("focusUrl", url);
                    return ref;
                }
                return new Ref(url);
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
