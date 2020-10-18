package gov.nist.asbestos.services.servlet;

import gov.nist.asbestos.services.restRequests.*;
import gov.nist.asbestos.client.Base.Dirs;
import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.events.Event;
import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.simapi.tk.installation.Installation;
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
        String channelId = channel.asChannelId();
        return String.format("http://%s:%s/asbestos/log/%s/%s/null/%s", hostname, port, testSession, channelId, eventId);
    }


    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) {

        Request request = new Request(req, resp, externalCache);

        try {
            if (externalCache == null)
                throw new Exception("External Cache not set");

            if (GetLogEventAnalysisByEventRequest.isRequest(request)) new GetLogEventAnalysisByEventRequest(request).run();
            else if (GetNativeUrlRequest.isRequest(request)) new GetNativeUrlRequest(request).run();
            else if (GetLogEventAnalysisByURLRequest.isRequest(request)) new GetLogEventAnalysisByURLRequest(request).run();
            else if (GetEventRequest.isRequest(request)) new GetEventRequest(request).run();
            else if (GetDocumentRequest.isRequest(request)) new GetDocumentRequest(request).run();
            else if (GetProxyBaseRequest.isRequest(request)) new GetProxyBaseRequest(request).run();
            else if (GetValidationServerRequest.isRequest(request)) new GetValidationServerRequest(request).run();
            else if (GetEventForResourceTypeRequest.isRequest(request)) new GetEventForResourceTypeRequest(request).run();
            else if (GetEventsForChannelRequest.isRequest(request)) new GetEventsForChannelRequest(request).run();
            else if (GetEcRequest.isRequest(request)) new GetEcRequest(request).run();
            else if (GetStartupSessionRequest.isRequest(request)) new GetStartupSessionRequest(request).run();
            else request.badRequest();

        } catch (Throwable t) {
            request.serverError(t);
        }
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) {

        Request request = new Request(req, resp, externalCache);

        try {
            if (externalCache == null)
                throw new Exception("External Cache not set");
            if (AnalyseResourceRequest.isRequest(request)) new AnalyseResourceRequest(request).run();
            else
                request.badRequest();

        } catch (Throwable t) {
            request.serverError(t);
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
