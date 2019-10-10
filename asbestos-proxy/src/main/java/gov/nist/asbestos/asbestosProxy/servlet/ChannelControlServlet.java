package gov.nist.asbestos.asbestosProxy.servlet;

import gov.nist.asbestos.asbestosProxy.requests.*;
import gov.nist.asbestos.client.log.SimStore;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.simapi.simCommon.SimId;
import gov.nist.asbestos.simapi.simCommon.TestSession;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

public class ChannelControlServlet extends HttpServlet {
    private static Logger log = Logger.getLogger(ChannelControlServlet.class);
    private File externalCache = null;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        log.info("ChannelControlServlet init");
        if (externalCache == null) {
            String ec = (String) config.getServletContext().getAttribute("ExternalCache");
            externalCache = new File(ec);
        }
        // create default TestSession and default channel if they don't exist
        SimId channelId = new SimId(new TestSession("default"), "default", "fhir");
        SimStore simStore = new SimStore(externalCache, channelId);
        simStore.getStore(true);
        if (!simStore.exists()) {
            log.info("Creating default Channel in the default TestSession");
            ChannelConfig cconfig = new ChannelConfig()
                    .setEnvironment("default")
                    .setTestSession("default")
                    .setChannelId("default")
                    .setChannelType("fhir")
                    .setActorType("fhir")
                    .setFhirBase("http://localhost:8080/fhir/fhir");
            simStore.create(cconfig);
        }
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)  {
        Request request = new Request(req, resp, externalCache);
        log.info("Channel Control POST " + request.uri);

        try {
            if (CreateChannelRequest.isRequest(request))        new CreateChannelRequest(request).run();
            else if (EvalRequest.isRequest(request))            new EvalRequest(request).run();
            else if (CancelEvalRequest.isRequest(request))      new CancelEvalRequest(request).run();
            else throw new Exception("Invalid request - do not understand URI " + request.uri);

        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
        } catch (Throwable e) {
            log.error(ExceptionUtils.getStackTrace(e));
            resp.setStatus(resp.SC_BAD_REQUEST);
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)  {
        Request request = new Request(req, resp, externalCache);
        log.info("Channel Control GET " + request.uri);

        try {

            if (GetChannelIdsRequest.isRequest(request))        new GetChannelIdsRequest(request).run();
            else if (GetChannelConfigRequest.isRequest(request)) new GetChannelConfigRequest(request).run();
            else throw new Exception("Invalid request - do not understand URI " + request.uri);

        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error(ExceptionUtils.getStackTrace(e));
            resp.setStatus(resp.SC_BAD_REQUEST);
        }
    }


    @Override
    public void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        Request request = new Request(req, resp, externalCache);
        log.info("Channel Control DELETE " + request.uri);
        try {

            if (DeleteChannelRequest.isRequest(request)) new DeleteChannelRequest(request).run();
            else throw new Exception("Invalid request - do not understand URI " + request.uri);

        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error(ExceptionUtils.getStackTrace(e));
            resp.setStatus(resp.SC_BAD_REQUEST);
        }
    }

    public void setExternalCache(File externalCache) {
        this.externalCache = externalCache;
    }

}
