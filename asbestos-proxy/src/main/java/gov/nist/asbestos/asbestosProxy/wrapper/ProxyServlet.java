package gov.nist.asbestos.asbestosProxy.wrapper;


import gov.nist.asbestos.asbestosProxy.channel.BaseChannel;
import gov.nist.asbestos.asbestosProxy.events.Event;
import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.http.operations.HttpGet;
import gov.nist.asbestos.http.operations.Verb;
import org.apache.http.client.methods.HttpPost;
import org.apache.log4j.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.net.URI;
import java.util.*;

class ProxyServlet extends HttpServlet {
    static Logger log = Logger.getLogger(ProxyServlet.class);
    File externalCache = null;

    Map<String, BaseChannel> proxyMap = new HashMap<>();
    proxyMap.put("passthrough", new PassthroughChannel());

    public void init(ServletConfig config) {
        super.init(config);
        log.debug("init done");
        externalCache = new File("/home/bill/ec");
        Installation.instance().externalCache = externalCache;
    }

    static URI buildURI(HttpServletRequest req) {
        HttpBase.buildURI(req.getRequestURI(), req.getParameterMap());
//        String params = HttpBase.parameterMapToString(req.getParameterMap())
//        if (params)
//            new URI(req.requestURI + '?' + params)
//        else
//            new URI(req.requestURI)
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp) {

        // typical URI is
        // for FHIR transactions
        // http://host:port/appContext/prox/simId/actor/transaction
        // for general stuff
        // http://host:port/appContext/prox/simId
//        resp.sendError(resp.SC_BAD_GATEWAY,'done')
        try {
            URI uri = buildURI(req);
            //String uri = req.requestURI
            log.debug("doPost " + uri);
            SimStore simStore = parseUri(uri, req, resp, Verb.POST);
            if (simStore == null)
                return;

            if (!simStore.isChannel())
                throw new ServletException("Proxy - POST of configuration data not allowed on " + uri);

            // these should be redundant given what is done in parseUri()
            String channelType = simStore.getConfig().getChannelType();
            if (channelType == null)
                throw new ServletException("Sim " + simStore.getChannelId() + " does not define a Channel Type.");
            BaseChannel channel = (BaseChannel) proxyMap.get(channelType);
            if (channel == null)
                throw new ServletException("Cannot create Channel of type " + channelType);

            HttpPost requestIn = new HttpPost();

            Event event = simStore.newEvent();
            // request from and response to client
            Task clientTask = event.getStore().selectClientTask();

            // log input from client
            logRequestIn(event, requestIn, req, Verb.POST);

            log.info("=> " + simStore.getEndpoint() + " " +  event.getStore().getRequestHeader().getContentType());

            // interaction between proxy and target service
            Task backSideTask = event.getStore().newTask();

            // transform input request for backend service
            HttpBase requestOut = transformRequest(backSideTask, requestIn, channel);
            requestOut.setUri(transformRequestUri(backSideTask, requestIn, channel));

            // send request to backend service
            requestOut.run();

            // log response from backend service
            backSideTask.select();
            backSideTask.getEvent().putResponseHeader(requestOut.getResponseHeaders());
            logResponseBody(backSideTask, requestOut);
            log.info("==> " + requestOut.getStatus() + " " +  (requestOut.getResponse() != null) ? requestOut.getResponseContentType() : "NULL");

            // transform backend service response for client
            HttpBase responseOut = transformResponse(event.getStore().selectTask(Task.CLIENT_TASK), requestOut, channel);
            clientTask.select();

            responseOut.getResponseHeaders().getAll().each { String name, String value ->
                resp.addHeader(name, value)
            }
            if (responseOut.getResponse() != null) {
                resp.outputStream.write(responseOut.response)
            }

            log.info("OK");

        } catch (Throwable t) {
            String msg = t.getMessage();
            log.error(msg);
            resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
            return;
        }
    }

    public void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        try {
            URI uri = buildURI(req);
            log.info("doDelete  " + uri);
            parseUri(uri, req, resp, Verb.DELETE);
            resp.setStatus(resp.SC_OK);
            log.info("OK");
        } catch (Throwable t) {
            log.error(t.getMessage());
            resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
            return;
        }
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp) {
        try {
            URI uri = buildURI(req);
            log.info("doGet " + uri);
            SimStore simStore = parseUri(uri, req, resp, Verb.GET);
            if (simStore == null)
                return;

            String channelType = simStore.getConfig().getChannelType();
            if (channelType == null)
                throw new Exception("Sim " + simStore.getChannelId() + " does not define a Channel Type.");
            BaseChannel channel = (BaseChannel) proxyMap.get(channelType);
            if (channel == null)
                throw new Exception("Cannot create Channel of type " + channelType);

            channel.setup(simStore.getConfig());

            // handle non-channel requests
            if (!simStore.isChannel()) {
                Map<String, List<String>> parameters = req.getParameterMap();
                String result = controlRequest(simStore, uri,parameters)
                resp.getOutputStream().print(result);
                return;
            }

            HttpGet requestIn = new HttpGet();

            Event event = simStore.newEvent();
            Task clientTask = event.getStore().selectClientTask();

            // log input request from client
            logRequestIn(event, requestIn, req, Verb.GET);

            // key request headers
            // accept-encoding: gzip
            // accept: *

            log.info("=> " + simStore.getEndpoint() + " " + event.getStore().getRequestHeader().getAccept());

            // create new event task to manage interaction with service behind proxy
            Task backSideTask = event.getStore().newTask();

            // transform input request for backend service
            HttpBase requestOut = transformRequest(backSideTask, requestIn, channel);
            requestOut.uri = transformRequestUri(backSideTask, requestIn, channel);
            requestOut.getRequestHeaders().setPathInfo(requestOut.getUri());

            // send request to backend service
            requestOut.run();

            // log response from backend service
            backSideTask.select();
            backSideTask.getEvent().putResponseHeader(requestOut.getResponseHeaders());
            // TODO make this next line not seem to work
            //backSideTask.event._responseHeaders = requestOut._responseHeaders
            logResponseBody(backSideTask, requestOut);
            log.info("==> " + requestOut.getStatus() + " " + (requestOut.getResponse() != null) ? requestOut.getResponseContentType() : "NULL");

            // transform backend service response for client
            clientTask.select();
            HttpBase responseOut = transformResponse(event.getStore().selectTask(Task.CLIENT_TASK), requestOut, channel);

            for (Header header : responseOut.getResponseHeaders().getAll()) {
                resp.addHeader(header.getName(), header.getValue());
            }
            if (responseOut.getResponse() != null) {
                resp.getOutputStream().write(responseOut.getResponse());
            }

            log.info("OK");
        } catch (Throwable t) {
            log.error(t.getMessage());
            resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
            return;
        }
    }

    static HttpBase logRequestIn(Event event, HttpBase http, HttpServletRequest req, Verb verb) {
        event.getStore().selectRequest();

        // Log headers
        List<String> names = Collections.list(req.getHeaderNames());
        Map<String, List<String>> hdrs = new HashMap<>();
        for (String name : names) {
            List<String> values = Collections.list(req.getHeaders(name));
            hdrs.put(name, values);
        }
        Headers headers = new Headers(hdrs);
        headers.setVerb(verb.toString());
        headers.set = "${verb} ${req.pathInfo} ${HttpBase.parameterMapToString(req.getParameterMap())}"
        Headers headers = HeaderBuilder.parseHeaders(rawHeaders)

        event.store.putRequestHeader(headers)
        http.requestHeaders = headers

        // Log body of POST
        if (verb == Verb.POST) {
            logRequestBody(event, headers, http, req)
        }

        return http;
    }

    static List<String> stringTypes = [
            'application/fhir+json',
            'application/json+fhir'
    ]

    static boolean isStringType(String type) {
        type.startsWith('text') || stringTypes.contains(type)
    }

    static logRequestBody(Event event, Headers headers, HttpBase http, HttpServletRequest req) {
        byte[] bytes = req.inputStream.bytes
        event.store.putRequestBody(bytes)
        http.request = bytes
        String encoding = headers.getContentEncoding()
        if (encoding == 'gzip') {
            String txt = Gzip.unzipWithoutBase64(bytes)
            event.store.putRequestBodyText(txt)
            http.requestText = txt
        } else if (headers.contentType == 'text/html') {
            event.store.putRequestHTMLBody(bytes)
            http.requestText = new String(bytes)
        } else if (isStringType(headers.contentType)) {
            http.requestText = new String(bytes)
        }
    }

    static logResponseBody(Task task, HttpBase http) {
        task.select()
        Headers headers = http.responseHeaders
        byte[] bytes = http.response
        task.event.putResponseBody(bytes)
        String encoding = headers.getContentEncoding()
        if (encoding == 'gzip') {
            String txt = Gzip.unzipWithoutBase64(bytes)
            task.event.putResponseBodyText(txt)
            http.responseText = txt
        } else if (headers.contentType == 'text/html') {
            task.event.putResponseHTMLBody(bytes)
            http.responseText = new String(bytes)
        } else if (isStringType(headers.contentType)) {
            http.responseText = new String(bytes)
        }
    }

    static HttpBase transformRequest(Task task, HttpPost requestIn, BaseChannel channelTransform) {
        HttpPost requestOut = new HttpPost()

        channelTransform.transformRequest(requestIn, requestOut)

        task.select()
        task.event.putRequestHeader(requestOut.requestHeaders)
        task.event.putRequestBody(requestOut.request)

        requestOut
    }

    static HttpBase transformRequest(Task task, HttpGet requestIn, BaseChannel channelTransform) {
        HttpGet requestOut = new HttpGet()

        channelTransform.transformRequest(requestIn, requestOut)

        task.select()
        task.event.putRequestHeader(requestOut.requestHeaders)

        requestOut
    }

    static URI transformRequestUri(Task task, HttpBase requestIn, BaseChannel channelTransform) {

        channelTransform.transformRequestUrl(task.event.simStore.endpoint, requestIn)

    }

    static HttpBase transformResponse(Task task, HttpBase responseIn, BaseChannel channelTransform) {
        HttpBase responseOut = new HttpGet()  // here GET vs POST does not matter

        channelTransform.transformResponse(responseIn, responseOut)

        responseOut.responseHeaders.removeHeader('transfer-encoding')

        task.select()
        //task.event.putResponseHeader(responseIn.responseHeaders)
        task.event.putResponseBody(responseOut.response)
        task.event.putResponseHeader(responseOut.responseHeaders)
        logResponseBody(task, responseOut)

        responseOut
    }

/**
 *
 * @param uri
 * @param req
 * @param resp
 * @param isDelete
 * @return SimStore
 */
    SimStore parseUri(URI uri, HttpServletRequest req, HttpServletResponse resp, Verb verb) {
        List<String> uriParts = uri.path.split('/') as List<String>
        SimStore simStore = new SimStore(externalCache)  // temp - will be overwritten

        if (uriParts.size() == 3 && uriParts[2] == 'prox' && verb != Verb.DELETE) {
            // CREATE
            // /appContext/prox
            // control channel - request to create proxy channel
            // can be done with GET or POST

            String parmameterString = uri.getQuery()

            if (verb == Verb.POST) {
                String rawRequest = req.inputStream.text   // json
                log.debug "CREATESIM ${rawRequest}"
                simStore = SimStoreBuilder.builder(externalCache, SimStoreBuilder.buildSimConfig(rawRequest))

                resp.contentType = 'application/json'
                resp.outputStream.print(rawRequest)


                resp.setStatus((simStore.newlyCreated ? resp.SC_CREATED : resp.SC_OK))
                log.info 'OK'
                return null  // trigger - we are done - exit now
            } else  if (parmameterString) {  // GET with parameters - also CREATE SIM
                Map<String, List<String>> queryMap = HttpBase.mapFromQuery(parmameterString)
                String json = JsonOutput.toJson(HttpBase.flattenQueryMap(queryMap))
                simStore = SimStoreBuilder.builder(externalCache, SimStoreBuilder.buildSimConfig(json))

                resp.contentType = 'application/json'
                resp.outputStream.print(json)


                resp.setStatus((simStore.newlyCreated ? resp.SC_CREATED : resp.SC_OK))
                log.info 'OK'
                return null
            }
        }

        SimId simId = null

        if (uriParts.size() >= 4) {
            // /appContext/prox/channelId
            if (uriParts[0] == '' && uriParts[2] == 'prox') { // no appContext
                simId = SimId.buildFromRawId(uriParts[3])

                uriParts.remove(0)  // leasing empty string
                uriParts.remove(0)  // appContext
                uriParts.remove(0)  // prox
                uriParts.remove(0)  // channelId

                try {
                    SimStoreBuilder.sense(externalCache, simId.testSession, simId.id)
                } catch (Throwable t) {
                    if (verb == Verb.DELETE)
                        resp.setStatus(resp.SC_OK)
                    else
                        resp.setStatus(resp.SC_NOT_FOUND)
                    return
                }
                if (uriParts.empty && verb == Verb.GET) {
                    // return channel config
                    ChannelConfig config = simStore.config
                    String json = new JsonBuilder(config).toPrettyString()
                    resp.contentType = 'application/json'
                    resp.outputStream.print(json)
                    return
                }
            }
        }

        //
        // everything above this is handling control operations
        // starting with this build of simStore, normal channel operations begin
        //

        simStore = SimStoreBuilder.sense(externalCache, simId.testSession, simId.id)

        if (verb == Verb.DELETE) {
            boolean status = simStore.deleteSim()
            assert status : "Proxy: Delete of ${simId} failed"
            return null
        }

        simStore = SimStoreBuilder.loader(externalCache, simId.testSession, simId.id)

        assert simStore.channelId : "ProxyServlet: request to ${uri} - ChannelId must be present in URI\n"

        // ChannelId has been established - from now all errors result in Event logging



        // the request targets a Channel - maybe a control message or a pass through.
        // pass through have Channel/ as the next element of the URI


        if (!uriParts.empty) {
            simStore.channel = uriParts[0] == 'Channel'   // Channel -> message passes through to backend system
            uriParts.remove(0)
        }

//        assert simStore.isChannel() : "Proxy: Bad URL ${uri} - Channel/ must be present"

        if (!uriParts.empty) {
            simStore.resource = uriParts[0]
            uriParts.remove(0)
        }

//        assert simStore.resource : "Proxy: no resource specified ${uri}"

        // verify that proxy exists - only if this is a channel to a backend system
        if (simStore.isChannel())
            simStore.getStore()  // exception if proxy does not exist

        log.debug "Sim ${simStore.channelId} ${simStore.actor} ${simStore.resource}"

        return simStore // expect content

    }

    // /appContext/prox/channelId/?
    static String controlRequest(SimStore simStore, URI uri, Map<String, List<String>> parameters) {
        List<String> uriParts = uri.path.split('/') as List<String>
        assert uriParts.size() > 4 : "Proxy control request - do not understand URI ${uri}\n"
        (1..4).each { uriParts.remove(0) }

        String type = uriParts[0]
        uriParts.remove(0)

        if (type == 'Event') {
            return EventRequestHandler.eventRequest(simStore, uriParts, parameters)
        }
        assert true : "Proxy: Do not understand control request type ${type} of ${uri}\n"
    }

}
