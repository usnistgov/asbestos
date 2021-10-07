package gov.nist.asbestos.services.servlet;


import gov.nist.asbestos.client.channel.BaseChannel;
import gov.nist.asbestos.client.channel.IBaseChannel;
import gov.nist.asbestos.asbestosProxy.channel.IChannelBuilder;
import gov.nist.asbestos.asbestosProxy.channel.PassthroughChannelBuilder;
import gov.nist.asbestos.asbestosProxy.channel.XdsOnFhirChannelBuilder;
import gov.nist.asbestos.asbestosProxy.channels.capabilitystatement.FhirToolkitCapabilityStatement;
import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.events.*;
import gov.nist.asbestos.client.log.SimStore;
import gov.nist.asbestos.client.resolver.ChannelUrl;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.http.operations.HttpDelete;
import gov.nist.asbestos.http.operations.HttpGetter;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.http.operations.Verb;
import gov.nist.asbestos.http.support.Common;
import gov.nist.asbestos.http.util.Gzip;
import gov.nist.asbestos.mhd.exceptions.TransformException;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.channel.ChannelConfigFactory;
import gov.nist.asbestos.simapi.simCommon.SimId;
import gov.nist.asbestos.simapi.tk.installation.Installation;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.OperationOutcome;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class ProxyServlet extends HttpServlet {
    private static Logger log = Logger.getLogger(ProxyServlet.class);
    private static File externalCache = null;
    private Map<String, IChannelBuilder> proxyMap = new HashMap<>();

    public ProxyServlet() {
        super();
        proxyMap.put("fhir", new PassthroughChannelBuilder());
        proxyMap.put("mhd", new XdsOnFhirChannelBuilder());
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        log.info("ProxyServlet init");

        if (externalCache == null) {
            String ec = (String) config.getServletContext().getAttribute("ExternalCache");
            externalCache = new File(ec);
            Ref.setEC(new EC(externalCache));
        }

    }

    private static String[] addEventHeader(HttpServletResponse resp, String hostport, ITask task) {
        Header header = buildEventHeader(hostport, task);
        resp.setHeader(header.getName(), header.getValue());
        String[] returned = new String[2];
        returned[0] = header.getName();
        returned[1] = header.getValue();
        return returned;
    }


    public static String[] addEventHeader(HttpBase resp, String hostport, ITask task) {
        Header header = buildEventHeader(hostport, task);
        resp.getResponseHeaders().add(header);
        String[] returned = new String[2];
        returned[0] = header.getName();
        returned[1] = header.getValue();
        return returned;
    }

    private static Header buildEventHeader(String hostport, ITask task) {
        if (hostport == null) return null;
        if (task == null) return null;
        File eventDir = task.getEvent().getEventDir();
        String[] parts = eventDir.toString().split(Pattern.quote(File.separator));
        int length = parts.length;
        if (length < 6) return null;
        String event = parts[length-1];
        String resource = parts[length-2];
        String channelId = parts[length-4];
        String testSession = parts[length-5];

        UIEvent uiEvent = new UIEvent(new EC(externalCache));
        uiEvent.setHostPort(hostport);
        uiEvent.setTestSession(testSession);
        uiEvent.setChannelId(channelId);
        uiEvent.setResourceType(resource);
        uiEvent.setEventName(event);

//        String uri = "http://" +
//                hostport +
//                "/asbestos/log/" +
//                testSession + "/" +
//                channelId + "/" +
//                resource + "/" +
//                event;
        return new Header("x-proxy-event", uiEvent.getURI().toString());
    }

    private static String getHostPort(Headers inHeaders) throws ServletException {
        String hostport = inHeaders.getValue("host");
        if (hostport == null || !hostport.contains(":")) {
            //throw new ServletException("host header missing or not formatted as host:port");
            String defaultPort = ":80";
            log.info("Port is not present. Adding default port " + hostport + defaultPort);
            hostport += defaultPort; //
        }
        return hostport;
    }

    // http://host:port/asbestos/proxy/testSession__channelId
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)  {
        URI uri = Common.buildURI(req);
        log.info("doPost " + uri);
        SimStore simStore;
        try {
            simStore = parseUri(uri, req, resp, Verb.POST);
            if (simStore == null)
                return;
        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
            return;
        } catch (Throwable t) {
            log.error(ExceptionUtils.getStackTrace(t));
            resp.setStatus(resp.SC_BAD_REQUEST);
            return;
        }

        Event event = simStore.newEvent();
        ITask clientTask = event.getClientTask();
        clientTask.putDescription("POST");
        Headers inHeaders = Common.getRequestHeaders(req, Verb.POST);
        String hostport = inHeaders.getValue("host");
        if (hostport == null || hostport.equals(""))
            hostport = "localhost:8080";

        inHeaders.add(new Header("x-client-addr", req.getRemoteAddr()));

        BaseChannel channel = null;

        try {
            if (!simStore.isChannel())
                throw new Error("Proxy - POST of configuration data not allowed on " + uri);

            // these should be redundant given what is done in parseUri()
            ChannelConfig channelConfig = simStore.getChannelConfig();
            String channelType = channelConfig.getChannelType();
            if (channelType == null)
                throw new Error("Sim " + simStore.getChannelId() + " does not define a Channel Type.");
            IChannelBuilder channelBuilder = proxyMap.get(channelType);
            channel = channelBuilder.build();

            channel.setup(simStore.getChannelConfig());

            channel.setReturnFormatType(Format.resultContentType(inHeaders));

            byte[] inBody = getRequestBody(req);
            if (inHeaders.isZipped())
                inBody = Gzip.decompressGZIP(inBody);
            String inBodyStr = new String(inBody);

            HttpPost requestIn = (HttpPost) logClientRequestIn(clientTask, inHeaders, inBody, Verb.POST);

            log.info("=> " + uri.toString() + " ContentType: " + clientTask.getRequestHeader().getContentType());

            // interaction between proxy and target service
            ITask backSideTask = clientTask.newTask();
            backSideTask.putDescription("PNR to target");

            String proxyBase = new Ref(uri).getBase().withHostPort(hostport).toString();
            String fhirBase = new Ref(requestIn.getRequestHeaders().getPathInfo()).getBase().toString();
            channel.setProxyBase(proxyBase);
            channel.setServerBase(fhirBase);



            URI outURI = transformRequestUri(requestIn, channel);
            // transform input request for backend service
            HttpBase requestOut = transformRequest(backSideTask, requestIn, outURI, channel);
            requestOut.setUri(outURI);

            // send request to backend service, if this fails probably the proxy target (fhir base or mhd channel name is not configured properly in the channel config)
            requestOut.run();

            // log response from backend service
            logResponse(backSideTask, requestOut);

            // transform backend service response for client
            HttpBase responseOut = transformResponse(clientTask, requestOut, channel, hostport, null, null);


            respond(resp, responseOut, inHeaders, clientTask, responseOut.getStatus());
        } catch (TransformException e) {
            respond(resp, e.getResponse(), inHeaders, clientTask, 400);
            //resp.setStatus(resp.SC_OK);
        } catch (Throwable t) {
            respondWithError(req, resp, t, inHeaders, clientTask);
            //resp.setStatus(resp.SC_OK);
        } finally {
        }
    }

    @Override
    public void doDelete(HttpServletRequest req, HttpServletResponse resp)  {
        URI uri = Common.buildURI(req);
        log.info("doDelete  " + uri);
        Verb verb = Verb.DELETE;
        SimStore simStore = getSimStore(req, resp, uri, verb);
        doGetOrDelete(req, resp, simStore, uri, verb);
    }

    private void returnOperationOutcome(HttpServletRequest req, HttpServletResponse resp, ITask task, Throwable t) throws IOException, ServletException {
        Headers headers = Common.getRequestHeaders(req, Verb.GET);  // verb not used
        Header acceptHeader = headers.getAccept();
        Headers responseHeaders = new Headers();

        OperationOutcome oo = wrapInOutcome(t);
        if (acceptHeader != null && acceptHeader.getValue().contains("json")) {
            String ooString = ParserBase.encode(oo, Format.JSON);
            task.putResponseBodyText(ooString);
            resp.getWriter().print(ooString);
            resp.addHeader("Content-Type", Format.JSON.getContentType());
            responseHeaders.add(new Header("Content-Type", Format.JSON.getContentType()));
        } else {
            String ooString = ParserBase.encode(oo, Format.XML);
            task.putResponseBodyText(ooString);
            resp.getWriter().print(ooString);
            resp.addHeader("Content-Type", Format.XML.getContentType());
            responseHeaders.add(new Header("Content-Type", Format.XML.getContentType()));
        }
        Headers inHeaders = Common.getRequestHeaders(req, Verb.POST);
        String hostport = getHostPort(inHeaders);
        String[] eventHeader = addEventHeader(resp, hostport, task);
        if (eventHeader != null)
            responseHeaders.add(new Header(eventHeader[0], eventHeader[1]));
        task.putResponseHeader(responseHeaders);
    }

    private OperationOutcome wrapInOutcome(Throwable t) {
        return wrapInOutcome(ExceptionUtils.getStackTrace(t));
    }

    private OperationOutcome wrapInOutcome(String msg) {
        OperationOutcome oo = new OperationOutcome();
        OperationOutcome.OperationOutcomeIssueComponent issue = oo.addIssue();
        issue.setSeverity(OperationOutcome.IssueSeverity.ERROR);
        issue.setCode(OperationOutcome.IssueType.EXCEPTION);
        issue.setDiagnostics(msg);
        return oo;
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) {
        URI uri = Common.buildURI(req);
        log.info("doGet " + uri);

        Verb verb = Verb.GET;
        SimStore simStore = getSimStore(req, resp, uri, verb);
        String channelType = simStore.getChannelConfig().getChannelType();

        // Only the MHD capability statement
        if ("mhd".equals(channelType)) {
            try {
                Optional<URI> proxyBaseURI = getProxyBase(uri);
                 if (proxyBaseURI.isPresent()) {
                     Headers inHeaders = Common.getRequestHeaders(req, verb);
                    if (FhirToolkitCapabilityStatement.isCapabilityStatementRequest(proxyBaseURI.get(), inHeaders.getPathInfo())) {
                        doGetCapabilityStatement(req, resp, simStore, uri, verb, inHeaders, simStore.getChannelConfig().asChannelId());
                        return; // EXIT
                    }
                }
            } catch (URISyntaxException uriEx) {
                 log.error(ExceptionUtils.getStackTrace(uriEx));
            }
        }

        // All other requests including passthrough channel's capability statement, and other MHD requests
        doGetOrDelete(req, resp, simStore, uri, verb);
    }

    private void doGetCapabilityStatement(HttpServletRequest req, HttpServletResponse resp, SimStore simStore, URI uri, Verb verb, Headers inHeaders, String channelId) {
        if (simStore == null) return;

        boolean isLoggingEnabled = simStore.getChannelConfig().isLogMhdCapabilityStatementRequest();

        ITask clientTask = (isLoggingEnabled ? simStore.newEvent().getClientTask() : new NoOpTask());
        try {
            String hostport = inHeaders.getValue("host");
            if (hostport == null || hostport.equals(""))
                hostport = "localhost:8080";

            String channelType = simStore.getChannelConfig().getChannelType();
            if (channelType == null)
                throw new Exception("Sim " + simStore.getChannelId() + " does not define a Channel Type.");

            log.info("Metadata Request => " + uri.toString() + " accept: " + inHeaders.getAccept());

            byte[] inBody = getRequestBody(req);
            HttpBase requestIn = logClientRequestIn(clientTask, inHeaders, inBody, verb);
            String enumFindKey = String.format("%sChannelCapabilityStatementFile", channelId);
            Optional<ServicePropertiesEnum> spEnum = ServicePropertiesEnum.find(enumFindKey);
            ServicePropertiesEnum capabilityStatementFile;
            if (! spEnum.isPresent()) {
                capabilityStatementFile = ServicePropertiesEnum.EMPTY_CAPABILITY_STATEMENT_FILE;
            } else {
                capabilityStatementFile = spEnum.get();
            }
            BaseResource baseResource = FhirToolkitCapabilityStatement.getCapabilityStatement(capabilityStatementFile, channelId);
            String versionId = ((CapabilityStatement)baseResource).getVersion();
            resp.addHeader("ETag", String.format("W/\"%s\"", versionId.hashCode()));
            respond(resp, baseResource, inHeaders, clientTask, 200);
        } catch (Exception ex) {
            // This did not work in IntelliJ Jetty runner without any Jetty XML config:
            // resp.sendError(500, ex.toString());.
            // This worked in Tomcat but not Jetty without any Jetty XML config. Works with the following accept-headers: fhir+xml and fhir+json.
            log.error(ExceptionUtils.getStackTrace(ex));
            resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
            respondWithError(req, resp, ex.toString(), inHeaders, clientTask);
        }
    }

    private void doGetOrDelete(HttpServletRequest req, HttpServletResponse resp, SimStore simStore, URI uri, Verb verb)  {
        if (simStore == null) return;

        Event event = simStore.newEvent();
        ITask clientTask = event.getClientTask();
        Headers inHeaders = Common.getRequestHeaders(req, verb);
        String hostport = inHeaders.getValue("host");
        if (hostport == null || hostport.equals(""))
            hostport = "localhost:8080";

        inHeaders.add(new Header("x-client-addr", req.getRemoteAddr()));

        boolean isSearch = !new Ref(uri).hasId();

        try {

            String channelType = simStore.getChannelConfig().getChannelType();
            if (channelType == null)
                throw new Exception("Sim " + simStore.getChannelId() + " does not define a Channel Type.");
            IChannelBuilder channelBuilder = proxyMap.get(channelType);
            BaseChannel channel = channelBuilder.build();

            channel.setup(simStore.getChannelConfig());
            channel.setReturnFormatType(Format.resultContentType(inHeaders));
            channel.setHostport(hostport);
            channel.setTask(clientTask);

            // handle non-channel requests
            if (!simStore.isChannel()) {
                Map<String, List<String>> parameters = req.getParameterMap();
                String result = controlRequest(simStore, uri, parameters);
                resp.getOutputStream().print(result);
                return;
            }

            log.info("ProxyServlet => " + uri.toString() + " accept: " + clientTask.getRequestHeader().getAccept());

            byte[] inBody = getRequestBody(req);
            HttpBase requestIn = logClientRequestIn(clientTask, inHeaders, inBody, verb);

            ITask backSideTask = clientTask.newTask();

            URI outURI = transformRequestUri(requestIn, channel);
            // transform input request for backend service
            HttpBase requestOut;
            channel.setTask(backSideTask);
            if (requestIn instanceof HttpGetter)
                requestOut = transformRequest(backSideTask, (HttpGetter) requestIn, outURI, channel);
            else if (requestIn instanceof HttpPost)
                requestOut = transformRequest(backSideTask, (HttpPost) requestIn, outURI, channel);
            else
                requestOut = transformRequest(backSideTask, (HttpDelete) requestIn, outURI, channel);


            // send request to backend service
            if (!backSideTask.hasRun()) {
                requestOut.run();

                logRequest(backSideTask, requestOut);
                // log response from backend service
                logResponse(backSideTask, requestOut);
            } else {
                backSideTask.fromTask(requestOut);  // load requestOut
            }

            // transform backend service response for client
            if (requestOut.isSuccess()) {
                Ref ref = new Ref(uri);
                String requestedType;
                requestedType = ref.getResourceType();
                HttpBase responseOut = transformResponse(backSideTask, requestOut, channel, hostport, requestedType, uri.toString());
                respond(resp, responseOut, inHeaders, clientTask, 200);
                resp.setStatus(resp.SC_OK);
            } else {
                respondWithError(req, resp, "backend call failed", inHeaders, clientTask);
            }
        } catch (TransformException e) {
            respond(resp, e.getResponse(), inHeaders, clientTask, 400);
            resp.setStatus(resp.SC_OK);
        } catch (Throwable t) {
            respondWithError(req, resp, t, inHeaders, clientTask);
            //resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private SimStore getSimStore(HttpServletRequest req, HttpServletResponse resp, URI uri, Verb verb) {
        SimStore simStore;
        try {
            simStore = parseUri(uri, req, resp, verb);
            if (simStore == null) {
                return null;
            }
        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
        return simStore;
    }

    private Optional<URI> getProxyBase(URI pathInfo) throws URISyntaxException {
        // '/proxy' is where the ProxyServlet is mapped to in the web.xml
        // http://localhost:8081/asbestos/proxy/default__mhdchannel/metadata
        // proxy base = all segments up to the first occurrence of 'proxy' + testsession + '__" + channelId
        URI proxyBase = null;
        List<String> pathSegments = Arrays.asList(pathInfo.getPath().split("/"));
        int proxyIndex = pathSegments.indexOf("proxy");
        int channelSegmentIndex = proxyIndex + 1;
        if (proxyIndex > -1 && (pathSegments.size() >= channelSegmentIndex)) {
           List<String> proxyBaseSegments = pathSegments.subList(0, channelSegmentIndex+1/* +1 because To is exclusive of the provided index number */);
           proxyBase = new URI(String.join("/", proxyBaseSegments));
        }
        return Optional.ofNullable(proxyBase);
    }

    private Bundle wrapInBundle(OperationOutcome oo) {
        Bundle bundle = new Bundle();
        Bundle.BundleEntryComponent bundleEntryComponent = bundle.addEntry();
        Bundle.BundleEntrySearchComponent search = bundleEntryComponent.getSearch();
        search.setMode(Bundle.SearchEntryMode.OUTCOME);
        Bundle.BundleEntryResponseComponent resp1 = bundleEntryComponent.getResponse();
        resp1.setOutcome(oo);
        return bundle;
    }

    private void respondWithError(HttpServletRequest req, HttpServletResponse resp, Throwable t, Headers inHeaders, ITask
        clientTask) {
        log.error(ExceptionUtils.getStackTrace(t));
        Ref ref = new Ref(Common.buildURI(req));
//        if (ref.isQuery()) {
//            respond(resp, wrapInOutcome(t), inHeaders, clientTask, 403);
//        } else {
            OperationOutcome oo = wrapInOutcome(t);
            respond(resp, oo, inHeaders, clientTask, 400);
//        }
    }

    private void respondWithError(HttpServletRequest req, HttpServletResponse resp, String msg, Headers inHeaders, ITask
        clientTask) {
//        if (new Ref(Common.buildURI(req)).isQuery()) {
//            Bundle bundle = wrapInBundle(wrapInOutcome(msg));
//            respond(resp, bundle, inHeaders, clientTask, 200);
//        } else {
            OperationOutcome oo = wrapInOutcome(msg);
            respond(resp, oo, inHeaders, clientTask, 400);
//        }
    }

    private void respond(HttpServletResponse resp, BaseResource resource, Headers inHeaders, ITask clientTask, int status) {
        String resourceString = "";
        if (resource != null)
            resourceString = ParserBase.encode(resource, Format.resultContentType(inHeaders));
        respond(resp, resourceString.getBytes(), inHeaders, clientTask, status);
    }

    private void respond(HttpServletResponse resp, byte[] content, Headers inHeaders, ITask clientTask, int status) {
        HttpBase responseOut = new HttpGetter();
        Format format = Format.resultContentType(inHeaders);
        responseOut.getResponseHeaders().add(new Header("Content-Type", format.getContentType()));
        responseOut.setResponse(content);

        respond(resp, responseOut, inHeaders, clientTask, status);
    }

    // responseOut is final response to return to client
    private void respond(HttpServletResponse resp, HttpBase responseOut, Headers inHeaders, ITask clientTask, int status) {
        try {
//            if (responseOut.getStatus() == 0)
                responseOut.setStatus(status);
            if (clientTask.getEvent() != null)
                addEventHeader(responseOut, getHostPort(inHeaders), clientTask);
            logResponse(clientTask, responseOut);

            if (inHeaders.requestsZip() && !responseOut.isResponseGzipEncoded())
                responseOut.getResponseHeaders().add(new Header("Content-Encoding", "gzip"));
            transferHeaders(responseOut.getResponseHeaders(), resp);
            if (responseOut.getResponse() != null && responseOut.getResponse().length != 0) {
                byte[] content = responseOut.getResponse();
                if (inHeaders.requestsZip()) {
                    content = Gzip.compressGZIP(content);
                }
                resp.getOutputStream().write(content);
            }
        } catch (Exception e) {
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }


    private void transferHeaders(Headers headers, HttpServletResponse resp) {
        List<String> exclude = Arrays.asList("transfer-encoding", "x-powered-by", "content-length");
        for (Header header : headers.getHeaders()) {
            if (exclude.contains(header.getName().toLowerCase()))
                continue;
            resp.setHeader(header.getName(), header.getValue());
        }
        resp.setStatus(headers.getStatus());
    }

    private static void logResponse(ITask task, HttpBase requestOut) {
        Headers responseHeaders = requestOut.getResponseHeaders();
        if (requestOut.getStatus() != 0)
            responseHeaders.setStatus(requestOut.getStatus());
        task.putResponseHeader(responseHeaders);
        logResponseBody(task, requestOut);
        log.info("==> " + requestOut.getStatus() + " " + ((requestOut.getResponse() != null) ? requestOut.getResponseContentType() + " " + requestOut.getResponse().length + " bytes" : "NULL"));
    }

    static HttpBase logClientRequestIn(ITask task, Headers headers, byte[] body, Verb verb) {
        HttpBase base = (verb == Verb.GET) ? new HttpGetter() : (verb == Verb.DELETE ? new HttpDelete() : new HttpPost());
        task.putRequestHeader(headers);
        base.setRequestHeaders(headers);

        task.putRequestBody(body);
        base.setRequest(body);
//        String encoding = (headers.getContentEncoding().getAllValues().isEmpty()) ? "" : headers.getContentEncoding().getAllValues().get(0);
//        if (encoding.equalsIgnoreCase("gzip")) {
//            String txt = Gzip.decompressGZIPToString(body);
//            task.putRequestBodyText(txt);
//            base.setRequestText(txt);
//        }
//        else
        if (body != null && headers.getContentType().getAllValues().get(0).equalsIgnoreCase("text/html")) {
            task.putRequestHTMLBody(body);
            base.setRequestText(new String(body));
        } else if (body != null) {
            task.putRequestBodyText(new String(body));
            base.setRequestText(new String(body));
        }

        try {
            // try to get input formatted
            Format format = Format.fromContentType(headers.getContentType().getValue());
            BaseResource resource = ParserBase.parse(task.getRequestBodyAsString(), format);
            String text = ParserBase.encode(resource, format);
            task.putRequestBodyText(text);
        } catch (Throwable t) {

        }
        return base;
    }

    private static List<String> stringTypes = Arrays.asList(
            "application/fhir+json",
            "application/json+fhir",
            "application/soap+xml",
            "multipart/related",
            "application/fhir+xml",
            "application/fhir+json"
    );

    static boolean isStringType(String type) {
        return type.startsWith("text") || type.startsWith("multipart") || type.contains("xml") || type.contains("json");
    }

    static byte[] getRequestBody(HttpServletRequest req) {
        byte[] bytes;
        try {
            bytes = IOUtils.toByteArray(req.getInputStream());
        } catch (Exception e) {
            throw new  RuntimeException(e);
        }
        if (bytes.length == 0)
            return null;
        return bytes;
    }

    static void logRequestBody(ITask task, Headers headers, HttpBase http, HttpServletRequest req) {
        byte[] bytes;
        try {
            bytes = IOUtils.toByteArray(req.getInputStream());
        } catch (Exception e) {
            throw new  RuntimeException(e);
        }
        task.putRequestBody(bytes);
        http.setRequest(bytes);
        String encoding = headers.getContentEncoding().getAllValues().get(0);
        if (encoding.equalsIgnoreCase("gzip")) {
            String txt = Gzip.decompressGZIPToString(bytes);
            task.putRequestBodyText(txt);
            http.setRequest(txt.getBytes());
        } else if (headers.getContentType().getAllValues().get(0).equalsIgnoreCase("text/html")) {
            task.putRequestHTMLBody(bytes);
            http.setRequestText(new String(bytes));
        } else if (isStringType(headers.getContentType().getAllValues().get(0))) {
            http.setRequestText(new String(bytes));
        }
    }

    static void logResponseBody(ITask task, HttpBase http) {
        Headers headers = http.getResponseHeaders();
        byte[] bytes = http.getResponse();
        task.putResponseBody(bytes);
        if (headers == null) {
            String txt = new String(bytes);
            //http.setResponseText(txt);
            task.putResponseBodyText(txt);
        } else {
            List<String> encodings = headers.getContentEncoding().getAllValues();
            if (encodings.isEmpty()) {
                String contentType = headers.getContentType().getAllValues().get(0);
                if (isStringType(contentType)) {
                    if (bytes != null) {
                        String txt = new String(bytes);
                        //http.setResponseText(txt);
                        task.putResponseBodyText(txt);
                    }
                }
            } else if (!encodings.isEmpty()){
                String encoding = encodings.get(0);
                if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
                    if (bytes != null) {
                        String txt = http.getResponseText(); //Gzip.decompressGZIP(bytes);
                        task.putResponseBodyText(txt);
                        //http.setResponseText(txt);
                    }
                } else if (headers.getContentType().getAllValues().get(0).equalsIgnoreCase("text/html")) {
                    task.putResponseHTMLBody(bytes);
                    //if (bytes != null)
                       // http.setResponseText(new String(bytes));
                } else if (isStringType(headers.getContentType().getAllValues().get(0))) {
                    //if (bytes != null)
                     //   http.setResponseText(new String(bytes));
                }
            }
        }
    }

    static void logRequest(ITask task, HttpBase http) {
        Headers headers = http.getRequestHeaders();
        task.putRequestHeader(headers);
        byte[] bytes = http.getRequest();
        task.putRequestBody(bytes);
        List<String> encodings = headers.getContentEncoding().getAllValues();
        if (encodings.isEmpty()) {
            String contentType = headers.getContentType().getAllValues().get(0);
            if (isStringType(contentType) || "".equals(contentType) && bytes != null && bytes.length != 0) {
                String txt = new String(bytes);
                http.setRequestText(txt);
                task.putRequestBodyText(txt);
            }
        }
        else {
//            String encoding = encodings.get(0);
//            if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
//                String txt = Gzip.decompressGZIPToString(bytes);
//                task.putRequestBodyText(txt);
//                http.setRequestText(txt);
//            } else
                if (headers.getContentType().getAllValues().get(0).equalsIgnoreCase("text/html")) {
                task.putRequestHTMLBody(bytes);
                http.setRequestText(new String(bytes));
            } else if (isStringType(headers.getContentType().getAllValues().get(0))) {
                http.setRequestText(new String(bytes));
            }
        }
    }

    static HttpBase transformRequest(ITask task, HttpPost requestIn, URI newURI, IBaseChannel channelTransform) {
        HttpPost requestOut = new HttpPost();
        channelTransform.setTask(task);
        channelTransform.transformRequest(requestIn, requestOut);

        requestOut.setUri(newURI);
        requestOut.getRequestHeaders().setPathInfo(requestOut.getUri());

        logRequest(task, requestOut);

        return requestOut;
    }

    static HttpBase transformRequest(ITask task, HttpGetter requestIn, URI newURI, IBaseChannel channelTransform) {
        HttpGetter requestOut = new HttpGetter();

        channelTransform.transformRequest(requestIn, requestOut);

        requestOut.setUri(newURI);
        requestOut.getRequestHeaders().setPathInfo(requestOut.getUri());

        //task.putRequestHeader(requestOut.getRequestHeaders());

        return requestOut;
    }

    static HttpBase transformRequest(ITask task, HttpDelete requestIn, URI newURI, IBaseChannel channelTransform) {
        HttpDelete requestOut = new HttpDelete();

        channelTransform.transformRequest(requestIn, requestOut);

        requestOut.setUri(newURI);
        requestOut.getRequestHeaders().setPathInfo(requestOut.getUri());

        task.putRequestHeader(requestOut.getRequestHeaders());

        return requestOut;
    }

    static URI transformRequestUri(HttpBase requestIn, IBaseChannel channelTransform) {
        Headers headers = requestIn.getRequestHeaders();
        return channelTransform.transformRequestUrl(headers.getPathInfo().getPath(), requestIn);
    }

    static HttpBase transformResponse(ITask task, HttpBase responseIn, IBaseChannel channelTransform, String proxyHostPort, String requestedType, String search) {
        HttpBase responseOut = new HttpGetter();  // here GET vs POST does not matter
        channelTransform.transformResponse(responseIn, responseOut, proxyHostPort, requestedType, search);
        //responseOut.setStatus(responseIn.getStatus());
        return responseOut;
    }

    // format of Channel URI is also found in class ChannelUrl
    SimStore parseUri(URI uri, HttpServletRequest req, HttpServletResponse resp, Verb verb) throws IOException {
        List<String> uriParts = ChannelUrl.uriParts(uri);  // so parts are deletable
        SimStore simStore;

        SimId simId = null;

        if (uriParts.size() >= 4) {
            // /appContext/proxy/channelId
            if (uriParts.get(0).equals("") && uriParts.get(2).equals("proxy")) {
                simId = ChannelUrl.getSimId(uri);    //  SimId.buildFromRawId(uriParts.get(3));
                simStore = new SimStore(externalCache, simId);
                if (!simStore.exists()) {
                    resp.setStatus(resp.SC_NOT_FOUND);
                    return null;
                }
                simStore.open();

                uriParts.remove(0);  // leading empty string
                uriParts.remove(0);  // appContext
                uriParts.remove(0);  // proxy
                uriParts.remove(0);  // channelId

                if (!simStore.exists()) {
                    resp.setStatus(resp.SC_NOT_FOUND);
                    return null;
                }
                if (uriParts.isEmpty() && verb == Verb.GET) {
                    // return channel config
                    String json = ChannelConfigFactory.convert(simStore.getChannelConfig());
                    resp.setContentType("application/json");
                    resp.getOutputStream().print(json);
                    return null;
                }
            }
            else
                return null;
        }  else
            return null;

        simStore = new SimStore(externalCache, simId);

        String uriString = uri.toString();
        if (uriString.contains("?")) {
            // This is a search - should be resource type Bundle
            simStore.setResource("Bundle");
        } else if (!uriParts.isEmpty()) {
            simStore.setResource(uriParts.get(0));
            uriParts.remove(0);
        } else {
            // this only happens with transaction or batch
            simStore.setResource("Bundle");
        }

        // verify that proxy exists - only if this is a channel to a backend system
        if (simStore.isChannel())
            simStore.getStore();  // exception if proxy does not exist
        simStore.open();

        log.debug("Sim " + simStore.getChannelId() + " " +  simStore.getActorType() + " " + simStore.getResource());

        return simStore; // expect content

    }

    // /appContext/prox/channelId/?
    static String controlRequest(SimStore simStore, URI uri, Map<String, List<String>> parameters) {
        List<String> uriParts = Arrays.asList(uri.getPath().split("/"));
        if (uriParts.size() <= 4)
            throw new RuntimeException("Proxy control request - do not understand URI " + uri);
        IntStream.rangeClosed(1, 4)
                .forEach(x -> uriParts.remove(0));

        String type = uriParts.get(0);
        uriParts.remove(0);

        if (type.equals("TaskStore")) {
            return EventRequestHandler.eventRequest(simStore, uriParts, parameters);
        }
        throw new RuntimeException("Proxy: Do not understand control request type " + type + " of " + uri);
    }

    public void setExternalCache(File externalCache) {
        this.externalCache = externalCache;
        Installation.instance().setExternalCache(externalCache);
        log.debug("Asbestos Proxy init EC is " + externalCache.getPath());
    }
}
