package gov.nist.asbestos.asbestosProxy.servlet;

import gov.nist.asbestos.asbestosProxy.requests.*;
import gov.nist.asbestos.client.Base.EC;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

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
            //initializeTestCollections();
        } else {
            log.fatal("TestEngineServlet - Proxy not started");
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)  {
        Request request = new Request(req, resp, externalCache);
        log.info("Test Engine GET " + request.uri);

        try {
            if (GetTestCollectionNamesRequest.isRequest(request)) new GetTestCollectionNamesRequest(request).run();
            else if (GetTestCollectionRequest.isRequest(request)) new GetTestCollectionRequest(request).run();
            else if (GetTestDefinitionRequest.isRequest(request)) new GetTestDefinitionRequest(request).run();
            else if (GetTestLogsRequest.isRequest(request)) new GetTestLogsRequest(request).run();
            else if (GetTestLogRequest.isRequest(request)) new GetTestLogRequest(request).run();
            else if (GetClientTestEvalRequest.isRequest(request)) new GetClientTestEvalRequest(request).run();
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
        Request request = new Request(req, resp, externalCache);
        log.info("Test Engine POST " + request.uri);

        try {

            if (RunTestRequest.isRequest(request)) new RunTestRequest(request).run();
            else if (EvalRequest.isRequest(request)) new EvalRequest(request).run();
            else throw new Exception("Invalid request - do not understand URI " + request.uri);

        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
        } catch (Throwable e) {
            log.error(ExceptionUtils.getStackTrace(e));
            resp.setStatus(resp.SC_BAD_REQUEST);
        }

    }
}
