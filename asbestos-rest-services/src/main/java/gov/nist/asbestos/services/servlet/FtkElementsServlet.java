package gov.nist.asbestos.services.servlet;

import gov.nist.asbestos.services.restRequests.*;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.log.SimStore;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.simapi.simCommon.SimId;
import gov.nist.asbestos.simapi.simCommon.TestSession;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

public class FtkElementsServlet extends HttpServlet {
    private static Logger log = Logger.getLogger(FtkElementsServlet.class.getName());
    private File externalCache = null;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        log.info("FtkElementsServlet init");
        if (externalCache == null) {
            String ec = (String) config.getServletContext().getAttribute("ExternalCache");
            externalCache = new File(ec);
        }
        // create default TestSession and default channel if they don't exist
        SimId channelId = new SimId(new TestSession("default"), "default", "fhir");
        SimStore simStore = new SimStore(externalCache, channelId);
        simStore.getStore(true);
        if (!simStore.exists()) {
            //log.info("Creating default Channel in the default TestSession");
            String hapiFhirBase;
            ServicePropertiesEnum key = ServicePropertiesEnum.HAPI_FHIR_BASE;
            hapiFhirBase = ServiceProperties.getInstance().getPropertyOrStop(key);
            ChannelConfig cconfig = new ChannelConfig()
                    .setEnvironment("default")
                    .setTestSession("default")
                    .setChannelName("default")
                    .setChannelType("fhir")
                    .setActorType("fhir")
                    .setFhirBase(hapiFhirBase);
            simStore.create(cconfig);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Request request = new Request(req, resp, externalCache);

        try {
            if (ReplaceChannelRequest.isRequest(request))        new ReplaceChannelRequest(request).run();
            else  request.badRequest();

        } catch (Throwable t) {
            request.serverError(t);
        }

    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)  {
        Request request = new Request(req, resp, externalCache);
        //log.info("Channel Control POST " + request.uri);

        try {
            if (CreateChannelRequest.isRequest(request))        new CreateChannelRequest(request).run();
            else if (EvalRequest.isRequest(request))            new EvalRequest(request).run();
            else if (CancelEvalRequest.isRequest(request))      new CancelEvalRequest(request).run();
            else if (LockChannelRequest.isRequest(request))      new LockChannelRequest(request).run();
            else if (AddSessionRequest.isRequest(request))      new AddSessionRequest(request).run();
            else  request.badRequest();

        } catch (Throwable t) {
            request.serverError(t);
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)  {
        Request request = new Request(req, resp, externalCache);
        //log.info("Channel Control GET " + request.uri);

        try {
            if (GetChannelIdsRequest.isRequest(request))        new GetChannelIdsRequest(request).run();
            else if (GetChannelIdAndURLRequest.isRequest(request)) new GetChannelIdAndURLRequest(request).run();
            else if (GetChannelConfigRequest.isRequest(request)) new GetChannelConfigRequest(request).run();
            else if (GetSessionNamesRequest.isRequest(request)) new GetSessionNamesRequest(request).run();
            else if (GetSessionConfigRequest.isRequest(request)) new GetSessionConfigRequest(request).run();
            else if (GetSignInRequest.isRequest(request)) new GetSignInRequest(request).run();
            else request.badRequest();

        } catch (Throwable t) {
            request.serverError(t);
        }
    }


    @Override
    public void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        Request request = new Request(req, resp, externalCache);
        log.info("Channel Control DELETE " + request.uri);
        try {

            if (DeleteChannelRequest.isRequest(request)) new DeleteChannelRequest(request).run();
            else if (DelSessionRequest.isRequest(request))  new DelSessionRequest(request).run();
            else request.badRequest();

        } catch (Throwable t) {
            request.serverError(t);
        }
    }

    public void setExternalCache(File externalCache) {
        this.externalCache = externalCache;
    }

}
