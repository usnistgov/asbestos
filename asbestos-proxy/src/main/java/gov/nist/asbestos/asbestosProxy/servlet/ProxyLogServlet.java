package gov.nist.asbestos.asbestosProxy.servlet;

import com.google.gson.Gson;
import gov.nist.asbestos.asbestosProxy.requests.*;
import gov.nist.asbestos.client.events.Event;
import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.Verb;
import gov.nist.asbestos.http.support.Common;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.simapi.tk.installation.Installation;
import gov.nist.asbestos.testEngine.engine.TestEngine;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.hl7.fhir.r4.model.TestReport;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProxyLogServlet extends HttpServlet {
    private File externalCache = null;
    private static String port = "8081";
    private static String hostname = "localhost";
    private static Logger log = Logger.getLogger(ProxyLogServlet.class);

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        // TODO put EC location in web.xml
        String ec = (String) config.getServletContext().getAttribute("ExternalCache");
        log.info("EC is " + ec);
        setExternalCache(new File(ec));
    }

    public void setExternalCache(File externalCache) {
        this.externalCache = externalCache;
        Installation.instance().setExternalCache(externalCache);
    }

    static String getEventLink(Event event, ChannelConfig channel) {
        String eventId = event.getEventId();
        String testSession = channel.getTestSession();
        String channelId = channel.getChannelId();
        return String.format("http://%s:%s/asbestos/log/%s/%s/null/%s", hostname, port, testSession, channelId, eventId);
    }

    private boolean htmlOk;
    private boolean jsonOk;
    private HttpServletRequest req;
    private HttpServletResponse resp;

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) {
        this.req = req;
        this.resp = resp;
        if (externalCache == null) {
            resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        String uri = req.getRequestURI();

        Request request = new Request(req, resp, externalCache);
        log.info("Log GET " + request.uri);

        Headers headers = Common.getRequestHeaders(req, Verb.GET);
        Header acceptHeader = headers.getAccept();
        htmlOk = acceptHeader.getValue().contains("text/html");
        jsonOk = acceptHeader.getValue().contains("json");

        // uri should be
        // testSession/channelId/resourcetype/event
        //
        // 6 - event
        // 5 - resourceType - may be null
        // 4 - channelId
        // 3 - testSession

        String[] uriParts = uri.split("/");
//        if (uriParts.length == 9) { // part
//            buildJsonListingOfPart(resp, uriParts[3], uriParts[4], uriParts[5], uriParts[6], uriParts[7], uriParts[8]);
//            return;
//        }
//        if (uriParts.length == 8) { // task
//            buildJsonListingOfTask(resp, uriParts[3], uriParts[4], uriParts[5], uriParts[6], uriParts[7]);
//            return;
//        }

        // since event
        // return next event after this one (wait for it if you must)
        // wait may time out and return 404

        // 7 - event
        // 6 - "since"
        // 5 - resourceType - may be null
        // 4 - channelId
        // 3 - testSession
        if (uriParts.length == 8 && uriParts[6].equals("since")) {
//            List<ResourceId> ids = buildListOfEventIdsByResourceType(uriParts[3], uriParts[4]);
//            ids.sort(Comparator.reverseOrder());
//            List<String> results = new ArrayList<>();
//            String event = uriParts[7];
//            for (String id : ids) {
//                if (id.compareTo(event) > 0)
//                    results.add(id);
//                else
//                    break;
//            }
//            returnJsonList(resp, ids);
//            return;
        }

        try {

            if (GetEventRequest.isRequest(request)) new GetEventRequest(request).run();
            else if (GetEventForResourceTypeRequest.isRequest(request)) new GetEventForResourceTypeRequest(request).run();
            else throw new Exception("Invalid request - do not understand URI " + request.uri);

        } catch (RuntimeException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error(ExceptionUtils.getStackTrace(e));
            resp.setStatus(resp.SC_BAD_REQUEST);
        } catch (Throwable e) {
            log.error(ExceptionUtils.getStackTrace(e));
            resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
        }

        if (uriParts.length == 5 && jsonOk) {  // includes channelId
            String query = req.getQueryString();
            if (query != null && query.contains("summaries=true")) {
                buildJsonListingOfEventSummaries(resp, uriParts[3], uriParts[4]);
                return;
            }
            // JSON listing of resourceTypes in channelId
            buildJsonListingOfResourceTypes(resp, uriParts[3], uriParts[4]);
            return;
        }

        resp.setStatus(resp.SC_BAD_REQUEST);
    }

    private void buildJsonListingOfResourceTypes(HttpServletResponse resp, String testSession, String channelId) {
        File fhir = new EC(externalCache).fhirDir(testSession, channelId);

        List<String> resourceTypes = Dirs.dirListingAsStringList(fhir);
        new EC(externalCache).returnJsonList(resp, resourceTypes);
    }

    private class ResourceId {
        String resourceType;
        String id;

        ResourceId(String resourceType, String id) {
            this.resourceType = resourceType;
            this.id = id;
        }
    }

    private List<ResourceId> buildListOfEventIdsByResourceType(String testSession, String channelId) {
        File fhir = new EC(externalCache).fhirDir(testSession, channelId);
        List<File> resourceTypes = dirListing(fhir);
        List<ResourceId> rids = new ArrayList<>();

        for (File resourceType : resourceTypes) {
            List<String> ids = Dirs.dirListingAsStringList(resourceType);
            for (String id : ids) {
                ResourceId rid = new ResourceId(resourceType.getName(), id);
                rids.add(rid);
            }
        }
        return rids;
    }

    private List<File> dirListing(File dir) {
        List<File> contents = new ArrayList<>();

        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.isDirectory()) continue;
                if (file.getName().startsWith(".")) continue;
                if (file.getName().startsWith("_")) continue;
                contents.add(file);
            }
            contents = contents.stream().sorted().collect(Collectors.toList());
        }

        return contents;
    }


}
