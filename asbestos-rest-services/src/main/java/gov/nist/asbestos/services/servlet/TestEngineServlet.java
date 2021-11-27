package gov.nist.asbestos.services.servlet;

import gov.nist.asbestos.services.restRequests.*;
import gov.nist.asbestos.client.Base.Request;
import org.apache.log4j.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

public class TestEngineServlet extends HttpServlet {
    private static Logger log = Logger.getLogger(TestEngineServlet.class);
    private File externalCache = null;
    private String port = "8081";

    public TestEngineServlet() {
        super();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        Object ec = config.getServletContext().getAttribute("ExternalCache");
        if (ec != null) {
            log.info("TestEngineServlet - Got External Cache from ProxyServlet");
            externalCache = new File((String) ec);
        } else {
            log.fatal("TestEngineServlet - Proxy not started");
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)  {
        Request request = new Request(req, resp, externalCache);

        try {
            if (GetTestCollectionNamesRequest.isRequest(request)) new GetTestCollectionNamesRequest(request).run();
            else if (RunSelftestRequest.isRequest(request)) new RunSelftestRequest(request).run();
            else if (GetDefaultFhirBaseRequest.isRequest(request)) new GetDefaultFhirBaseRequest(request).run();
            else if (GetEventFixtureRequest.isRequest(request)) new GetEventFixtureRequest(request).run();
            else if (GetTestAssertionsRequest.isRequest(request)) new GetTestAssertionsRequest(request).run();
            else if (GetTestCollectionRequest.isRequest(request)) new GetTestCollectionRequest(request).run();
            else if (GetTestScriptRequest.isRequest(request)) new GetTestScriptRequest(request).run();
            else if (GetTestLogsRequest.isRequest(request)) new GetTestLogsRequest(request).run();
            else if (GetTestReportRequest.isRequest(request)) new GetTestReportRequest(request).run();
            else if (GetClientTestEvalRequest.isRequest(request)) new GetClientTestEvalRequest(request).run();
            else if (GetClientEventEvalRequest.isRequest(request)) new GetClientEventEvalRequest(request).run();
            else if (HapiHeartbeat.isRequest(request)) new HapiHeartbeat(request).run();
            else if (XdsHeartbeat.isRequest(request)) new XdsHeartbeat(request).run();
            else if (GetEventPartRequest.isRequest(request)) new GetEventPartRequest(request).run();
            else if (GetFixtureStringRequest.isRequest(request)) new GetFixtureStringRequest(request).run();
            else request.badRequest();

        } catch (Throwable e) {
            request.serverError(e);
        }
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) {
        Request request = new Request(req, resp, externalCache);

        try {

            if (RunTestRequest.isRequest(request)) new RunTestRequest(request).run();
            else if (EvalRequest.isRequest(request)) new EvalRequest(request).run();
            else request.badRequest();

        } catch (Throwable t) {
            request.serverError(t);
        }

    }
}
