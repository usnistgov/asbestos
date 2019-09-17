package gov.nist.asbestos.asbestosProxy.servlet;


import gov.nist.asbestos.asbestosProxy.channel.*;
import gov.nist.asbestos.client.events.Event;
import gov.nist.asbestos.client.log.SimStore;
import gov.nist.asbestos.client.events.Task;
import gov.nist.asbestos.asbestosProxy.util.Gzip;
import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.*;
import gov.nist.asbestos.http.support.Common;
import gov.nist.asbestos.sharedObjects.ChannelConfigFactory;
import gov.nist.asbestos.simapi.simCommon.SimId;
import gov.nist.asbestos.simapi.tk.installation.Installation;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.IntStream;

public class ProxyServlet extends HttpServlet {
    private static Logger log = Logger.getLogger(ProxyServlet.class);
    private File externalCache = null;

    private Map<String, IChannelBuilder> proxyMap = new HashMap<>();

    public ProxyServlet() {
        super();
        proxyMap.put("passthrough", new PassthroughChannelBuilder());
        proxyMap.put("mhd", new MhdChannelBuilder());
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        log.info("ProxyServlet init");
        String ec = config.getInitParameter("ExternalCache");
        setExternalCache(new File(ec));

        // announce location of ExternalCache to other servlets
        config.getServletContext().setAttribute("ExternalCache", ec);
    }

    private static String[] addEventHeader(HttpServletResponse resp, String hostport, Task task) {
        Header header = buildEventHeader(hostport, task);
        resp.setHeader(header.getName(), header.getValue());
        String[] returned = new String[2];
        returned[0] = header.getName();
        returned[1] = header.getValue();
        return returned;
    }


    public static String[] addEventHeader(HttpBase resp, String hostport, Task task) {
        Header header = buildEventHeader(hostport, task);
        resp.getResponseHeaders().add(header);
        String[] returned = new String[2];
        returned[0] = header.getName();
        returned[1] = header.getValue();
        return returned;
    }

    private static Header buildEventHeader(String hostport, Task task) {
        if (hostport == null) return null;
        if (task == null) return null;
        File eventDir = task.getEvent().getEventDir();
        String[] parts = eventDir.toString().split("/");
        int length = parts.length;
        if (length < 6) return null;
        String event = parts[length-1];
        String resource = parts[length-2];
        String channelId = parts[length-4];
        String testSession = parts[length-5];

        String uri = "http://" +
                hostport +
                "/proxy/log/" +
                testSession + "/" +
                channelId + "/" +
                resource + "/" +
                event;
        return new Header("x-proxy-event", uri);
    }

    private static String getHostPort(Headers inHeaders) throws ServletException {
        String hostport = inHeaders.getValue("host");
        if (hostport == null || !hostport.contains(":"))
            throw new ServletException("host header missing or not formatted as host:port");
        return hostport;
    }

    // typical URI is
    // for FHIR translation
    // http://host:port/appContext/prox/simId/actor/transaction
    // for general stuff
    // http://host:port/appContext/prox/simId
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
        Task clientTask = event.getClientTask();
        clientTask.putDescription("PDB from client");
        Headers inHeaders = Common.getRequestHeaders(req, Verb.POST);
        String hostport = inHeaders.getValue("host");
        if (hostport == null || hostport.equals(""))
            hostport = "localhost:8080";

        BaseChannel channel = null;

        try {
            if (!simStore.isChannel())
                throw new Error("Proxy - POST of configuration data not allowed on " + uri);

            // these should be redundant given what is done in parseUri()
            String channelType = simStore.getChannelConfig().getChannelType();
            if (channelType == null)
                throw new Error("Sim " + simStore.getChannelId() + " does not define a Channel Type.");
            IChannelBuilder channelBuilder = proxyMap.get(channelType);
            channel = channelBuilder.build();

            channel.setup(simStore.getChannelConfig());

            channel.setReturnFormatType(Format.resultContentType(inHeaders));

            byte[] inBody = getRequestBody(req);

            HttpPost requestIn = (HttpPost) logClientRequestIn(clientTask, inHeaders, inBody, Verb.POST);

            log.info("=> " + simStore.getEndpoint() + " " + clientTask.getRequestHeader().getContentType());

            // interaction between proxy and target service
            Task backSideTask = clientTask.newTask();
            backSideTask.putDescription("PNR to target");

            String proxyBase = new Ref(uri).getBase().withHostPort(hostport).toString();
            String fhirBase = new Ref(requestIn.getRequestHeaders().getPathInfo()).getBase().toString();
            channel.setProxyBase(proxyBase);
            channel.setServerBase(fhirBase);

            URI outURI = transformRequestUri(requestIn, channel);
            // transform input request for backend service
            HttpBase requestOut = transformRequest(backSideTask, requestIn, outURI, channel);
            requestOut.setUri(outURI);

            // send request to backend service
            requestOut.run();

            // log response from backend service
            logResponse(backSideTask, requestOut);

            // transform backend service response for client
            HttpBase responseOut = transformResponse(clientTask, requestOut, channel, hostport);

            respond(resp, responseOut, inHeaders, clientTask);
        } catch (TransformException e) {
            respond(resp, e.getResponse(), inHeaders, clientTask);
            resp.setStatus(resp.SC_OK);
        } catch (Throwable t) {
            respondWithError(req, resp, t, inHeaders, clientTask);
            resp.setStatus(resp.SC_OK);
        } finally {
            if (channel != null)
                ChannelRelay.postEvent(channel.getChannelId(), event.getEventDir());
        }
    }

    @Override
    public void doDelete(HttpServletRequest req, HttpServletResponse resp)  {
        URI uri = Common.buildURI(req);
        log.info("doDelete  " + uri);
        doGetDelete(req, resp, uri, Verb.DELETE);
    }

    private void returnOperationOutcome(HttpServletRequest req, HttpServletResponse resp, Task task, Throwable t) throws IOException, ServletException {
        Headers headers = Common.getRequestHeaders(req, Verb.GET);  // verb not used
        Header acceptHeader = headers.getAccept();
        Headers responseHeaders = new Headers();

        OperationOutcome oo = wrapInOutcome(t);
        if (acceptHeader != null && acceptHeader.getValue().contains("json")) {
            String ooString = ProxyBase.encode(oo, Format.JSON);
            task.putResponseBodyText(ooString);
            resp.getWriter().print(ooString);
            resp.addHeader("Content-Type", Format.JSON.getContentType());
            responseHeaders.add(new Header("Content-Type", Format.JSON.getContentType()));
        } else {
            String ooString = ProxyBase.encode(oo, Format.XML);
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
    public void doGet(HttpServletRequest req, HttpServletResponse resp)  {
        URI uri = Common.buildURI(req);
        log.info("doGet " + uri);
        doGetDelete(req, resp, uri, Verb.GET);
    }

    private void doGetDelete(HttpServletRequest req, HttpServletResponse resp, URI uri, Verb verb)  {
        SimStore simStore;
        try {
            simStore = parseUri(uri, req, resp, verb);
            if (simStore == null) {
                return;
            }
        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        Event event = simStore.newEvent();
        Task clientTask = event.getClientTask();
        Headers inHeaders = Common.getRequestHeaders(req, verb);
        String hostport = inHeaders.getValue("host");
        if (hostport == null || hostport.equals(""))
            hostport = "localhost:8080";

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

            byte[] inBody = getRequestBody(req);

            HttpBase requestIn = logClientRequestIn(clientTask, inHeaders, inBody, verb);

            log.info("=> " + simStore.getEndpoint() + " " + clientTask.getRequestHeader().getAccept());

            Task backSideTask = clientTask.newTask();


            URI outURI = transformRequestUri(requestIn, channel);
            // transform input request for backend service
            HttpBase requestOut;
            channel.setTask(backSideTask);
            if (requestIn instanceof HttpGet)
                requestOut = transformRequest(backSideTask, (HttpGet) requestIn, outURI, channel);
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
                HttpBase responseOut = transformResponse(backSideTask, requestOut, channel, hostport);
                respond(resp, responseOut, inHeaders, clientTask);
            } else {
                respondWithError(req, resp, "backend call failed", inHeaders, clientTask);
            }
            resp.setStatus(resp.SC_OK);
        } catch (TransformException e) {
            respond(resp, e.getResponse(), inHeaders, clientTask);
            resp.setStatus(resp.SC_OK);
        } catch (Throwable t) {
            respondWithError(req, resp, t, inHeaders, clientTask);
            resp.setStatus(resp.SC_OK);
        }
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

    private void respondWithError(HttpServletRequest req, HttpServletResponse resp, Throwable t, Headers inHeaders, Task clientTask) {
        log.error(ExceptionUtils.getStackTrace(t));
        if (new Ref(Common.buildURI(req)).isQuery()) {
            Bundle bundle = wrapInBundle(wrapInOutcome(t));
            respond(resp, bundle, inHeaders, clientTask);
        } else {
            OperationOutcome oo = wrapInOutcome(t);
            respond(resp, oo, inHeaders, clientTask);
        }
    }

    private void respondWithError(HttpServletRequest req, HttpServletResponse resp, String msg, Headers inHeaders, Task clientTask) {
        if (new Ref(Common.buildURI(req)).isQuery()) {
            Bundle bundle = wrapInBundle(wrapInOutcome(msg));
            respond(resp, bundle, inHeaders, clientTask);
        } else {
            OperationOutcome oo = wrapInOutcome(msg);
            respond(resp, oo, inHeaders, clientTask);
        }
    }

    private void respond(HttpServletResponse resp, BaseResource resource, Headers inHeaders, Task clientTask) {
        String resourceString = "";
        if (resource != null)
            resourceString = ProxyBase.encode(resource, Format.resultContentType(inHeaders));
        respond(resp, resourceString.getBytes(), inHeaders, clientTask);
    }

    private void respond(HttpServletResponse resp, byte[] content, Headers inHeaders, Task clientTask) {
        HttpBase responseOut = new HttpGet();
        Format format = Format.resultContentType(inHeaders);
        responseOut.getResponseHeaders().add(new Header("Content-Type", format.getContentType()));
        responseOut.setResponse(content);

        respond(resp, responseOut, inHeaders, clientTask);
    }

    // responseOut is final response to return to client
    private void respond(HttpServletResponse resp, HttpBase responseOut, Headers inHeaders, Task clientTask) {
        try {
            responseOut.setStatus(200);
            addEventHeader(responseOut, getHostPort(inHeaders), clientTask);
            logResponse(clientTask, responseOut);

            transferHeaders(responseOut.getResponseHeaders(), resp);
            if (responseOut.getResponse() != null && responseOut.getResponse().length != 0)
                resp.getOutputStream().write(responseOut.getResponse());
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
    }

    private static void logResponse(Task task, HttpBase requestOut) {
        Headers responseHeaders = requestOut.getResponseHeaders();
        if (requestOut.getStatus() != 0)
            responseHeaders.setStatus(requestOut.getStatus());
        task.putResponseHeader(responseHeaders);
        logResponseBody(task, requestOut);
        log.info("==> " + requestOut.getStatus() + " " + ((requestOut.getResponse() != null) ? requestOut.getResponseContentType() + " " + requestOut.getResponse().length + " bytes" : "NULL"));
    }

    static HttpBase logClientRequestIn(Task task, Headers headers, byte[] body, Verb verb) {
        HttpBase base = (verb == Verb.GET) ? new HttpGet() : (verb == Verb.DELETE ? new HttpDelete() : new HttpPost());
        task.putRequestHeader(headers);
        base.setRequestHeaders(headers);

        task.putRequestBody(body);
        base.setRequest(body);
        String encoding = (headers.getContentEncoding().getAllValues().isEmpty()) ? "" : headers.getContentEncoding().getAllValues().get(0);
        if (encoding.equalsIgnoreCase("gzip")) {
            String txt = Gzip.decompressGZIP(body);
            task.putRequestBodyText(txt);
            base.setRequestText(txt);
        } else if (headers.getContentType().getAllValues().get(0).equalsIgnoreCase("text/html")) {
            task.putRequestHTMLBody(body);
            base.setRequestText(new String(body));
        } else if (isStringType(headers.getContentType().getAllValues().get(0))) {
            task.putRequestBodyText(new String(body));
            base.setRequestText(new String(body));
        } else {
            task.putRequestBodyText(new String(body));
            base.setRequestText(new String(body));
        }

        try {
            // try to get input formatted
            Format format = Format.fromContentType(headers.getContentType().getValue());
            BaseResource resource = ProxyBase.parse(body, format);
            String text = ProxyBase.encode(resource, format);
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
        return bytes;
    }

    static void logRequestBody(Task task, Headers headers, HttpBase http, HttpServletRequest req) {
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
            String txt = Gzip.decompressGZIP(bytes);
            task.putRequestBodyText(txt);
            http.setRequest(txt.getBytes());
        } else if (headers.getContentType().getAllValues().get(0).equalsIgnoreCase("text/html")) {
            task.putRequestHTMLBody(bytes);
            http.setRequestText(new String(bytes));
        } else if (isStringType(headers.getContentType().getAllValues().get(0))) {
            http.setRequestText(new String(bytes));
        }
    }

    static void logResponseBody(Task task, HttpBase http) {
        Headers headers = http.getResponseHeaders();
        byte[] bytes = http.getResponse();
        task.putResponseBody(bytes);
        if (headers == null) {
            String txt = new String(bytes);
            http.setResponseText(txt);
            task.putResponseBodyText(txt);
        } else {
            List<String> encodings = headers.getContentEncoding().getAllValues();
            if (encodings.isEmpty()) {
                String contentType = headers.getContentType().getAllValues().get(0);
                if (isStringType(contentType)) {
                    String txt = new String(bytes);
                    http.setResponseText(txt);
                    task.putResponseBodyText(txt);
                }
            } else {
                String encoding = encodings.get(0);
                if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
                    String txt = Gzip.decompressGZIP(bytes);
                    task.putResponseBodyText(txt);
                    http.setResponseText(txt);
                } else if (headers.getContentType().getAllValues().get(0).equalsIgnoreCase("text/html")) {
                    task.putResponseHTMLBody(bytes);
                    http.setResponseText(new String(bytes));
                } else if (isStringType(headers.getContentType().getAllValues().get(0))) {
                    http.setResponseText(new String(bytes));
                }
            }
        }
    }

    static void logRequest(Task task, HttpBase http) {
        Headers headers = http.getRequestHeaders();
        task.putRequestHeader(headers);
        byte[] bytes = http.getRequest();
        task.putRequestBody(bytes);
        List<String> encodings = headers.getContentEncoding().getAllValues();
        if (encodings.isEmpty()) {
            String contentType = headers.getContentType().getAllValues().get(0);
            if (isStringType(contentType) || "".equals(contentType)) {
                String txt = new String(bytes);
                http.setRequestText(txt);
                task.putRequestBodyText(txt);
            }
        }
        else {
            String encoding = encodings.get(0);
            if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
                String txt = Gzip.decompressGZIP(bytes);
                task.putRequestBodyText(txt);
                http.setRequestText(txt);
            } else if (headers.getContentType().getAllValues().get(0).equalsIgnoreCase("text/html")) {
                task.putRequestHTMLBody(bytes);
                http.setRequestText(new String(bytes));
            } else if (isStringType(headers.getContentType().getAllValues().get(0))) {
                http.setRequestText(new String(bytes));
            }
        }
    }

    static HttpBase transformRequest(Task task, HttpPost requestIn, URI newURI, IBaseChannel channelTransform) {
        HttpPost requestOut = new HttpPost();
        channelTransform.setTask(task);
        channelTransform.transformRequest(requestIn, requestOut);

        requestOut.setUri(newURI);
        requestOut.getRequestHeaders().setPathInfo(requestOut.getUri());

        logRequest(task, requestOut);

        return requestOut;
    }

    static HttpBase transformRequest(Task task, HttpGet requestIn, URI newURI, IBaseChannel channelTransform) {
        HttpGet requestOut = new HttpGet();

        channelTransform.transformRequest(requestIn, requestOut);

        requestOut.setUri(newURI);
        requestOut.getRequestHeaders().setPathInfo(requestOut.getUri());

        //task.putRequestHeader(requestOut.getRequestHeaders());

        return requestOut;
    }

    static HttpBase transformRequest(Task task, HttpDelete requestIn, URI newURI, IBaseChannel channelTransform) {
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

    static HttpBase transformResponse(Task task, HttpBase responseIn, IBaseChannel channelTransform, String proxyHostPort) {
        HttpBase responseOut = new HttpGet();  // here GET vs POST does not matter
        channelTransform.transformResponse(responseIn, responseOut, proxyHostPort);
        responseOut.setStatus(responseIn.getStatus());
        return responseOut;
    }

    SimStore parseUri(URI uri, HttpServletRequest req, HttpServletResponse resp, Verb verb) throws IOException {
        List<String> uriParts1 = Arrays.asList(uri.getPath().split("/"));
        List<String> uriParts = new ArrayList<>(uriParts1);  // so parts are deletable
        SimStore simStore;

        if (uriParts.size() == 3 && uriParts.get(2).equals("prox") && verb != Verb.DELETE) {
            // CREATE
            // /appContext/prox
            // control channel - request to create proxy channel
            // can be done with GET or POST

            String parmameterString = uri.getQuery();

//            if (verb == Verb.POST) {
//                String rawRequest = IOUtils.toString(req.getInputStream(), Charset.defaultCharset());   // json
//                log.debug("CREATESIM " + rawRequest);
//                ChannelConfig channelConfig = ChannelConfigFactory.convert(rawRequest);
//                simStore = new SimStore(externalCache,
//                        new SimId(new TestSession(channelConfig.getTestSession()),
//                                channelConfig.getChannelId(),
//                                channelConfig.getActorType(),
//                                channelConfig.getEnvironment(),
//                                true));
//
//                simStore.create(channelConfig);
//                log.info("Channel " + simStore.getChannelId().toString() + " created (type " + simStore.getActorType() + ")" );
//
//                resp.setContentType("application/json");
//                resp.getOutputStream().print(rawRequest);
//
//
//                resp.setStatus((simStore.isNewlyCreated() ? resp.SC_CREATED : resp.SC_OK));
//                log.info("OK");
//                return null;  // trigger - we are done - exit now
//            } else  if (parmameterString != null) {  // GET with parameters - also CREATE SIM
//                Map<String, List<String>> queryMap = HttpBase.mapFromQuery(parmameterString);
//                String json = new ObjectMapper().writeValueAsString(HttpBase.flattenQueryMap(queryMap));
//                ChannelConfig channelConfig = ChannelConfigFactory.convert(json);
//                SimId simId = new SimId(new TestSession(channelConfig.getTestSession()), channelConfig.getChannelId());
//                simStore = new SimStore(externalCache, simId);
//
//                resp.setContentType("application/json");
//                resp.getOutputStream().print(json);
//
//
//                resp.setStatus((simStore.isNewlyCreated() ? resp.SC_CREATED : resp.SC_OK));
//                log.info("OK");
//                return null;
//            }
        }

        SimId simId = null;

        if (uriParts.size() >= 4) {
            // /appContext/prox/channelId
            if (uriParts.get(0).equals("") && uriParts.get(2).equals("proxy")) { // no appContext
                simId = SimId.buildFromRawId(uriParts.get(3));
                simStore = new SimStore(externalCache, simId);
                if (!simStore.exists()) {
                    resp.setStatus(resp.SC_NOT_FOUND);
                    return null;
                }
                simStore.open();

                uriParts.remove(0);  // leading empty string
                uriParts.remove(0);  // appContext
                uriParts.remove(0);  // prox
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

//        if (verb == Verb.DELETE) {
//            Ref ref = new Ref(uri);
//            if (!ref.hasResource()) {
//                simStore.deleteSim();
//                return null;  // delete channel
//            }
//        }

        //
        // everything above this is handling control operations
        // starting with this load of simStore, normal channel operations begin
        //


        // ChannelId has been established - from now all errors result in TaskStore logging

        // the request targets a Channel - maybe a control message or a pass through.
        // pass through have Channel/ as the next element of the URI


        if (!uriParts.isEmpty()) {
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
