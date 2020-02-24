package gov.nist.asbestos.asbestosProxy.servlet;

import gov.nist.asbestos.asbestosProxy.requests.*;
import gov.nist.asbestos.client.Base.Dirs;
import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.events.Event;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.simapi.tk.installation.Installation;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
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


    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) {

        if (externalCache == null) {
            resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        Request request = new Request(req, resp, externalCache);
        log.info("Log GET " + request.uri);

        try {

            if (GetLogEventAnalysis.isRequest(request)) new GetLogEventAnalysis(request).run();
            else if (GetEventRequest.isRequest(request)) new GetEventRequest(request).run();
            else if (GetDocumentRequest.isRequest(request)) new GetDocumentRequest(request).run();
            else if (GetProxyBaseRequest.isRequest(request)) new GetProxyBaseRequest(request).run();
            else if (GetValidationServerRequest.isRequest(request)) new GetValidationServerRequest(request).run();
            else if (GetValidationRequest.isRequest(request)) new GetValidationRequest(request).run();
            else if (GetChannelMarkerRequest.isRequest(request)) new GetChannelMarkerRequest(request).run();
            else if (GetEventForResourceTypeRequest.isRequest(request)) new GetEventForResourceTypeRequest(request).run();
            else if (GetEventsForChannelRequest.isRequest(request)) new GetEventsForChannelRequest(request).run();
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
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) {

        if (externalCache == null) {
            resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        Request request = new Request(req, resp, externalCache);
        log.info("Log POST " + request.uri);

        try {

            if (CreateChannelMarkerRequest.isRequest(request)) new CreateChannelMarkerRequest(request).run();
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
