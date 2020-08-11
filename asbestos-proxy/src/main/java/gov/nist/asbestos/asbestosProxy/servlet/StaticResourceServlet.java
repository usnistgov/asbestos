package gov.nist.asbestos.asbestosProxy.servlet;

import gov.nist.asbestos.asbestosProxy.requests.GetStaticResourceRequest;
import gov.nist.asbestos.asbestosProxy.requests.Request;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

public class StaticResourceServlet extends HttpServlet {
    private static Logger log = Logger.getLogger(StaticResourceServlet.class);
    private File externalCache = null;

    public StaticResourceServlet() {
        super();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        Object ec = config.getServletContext().getAttribute("ExternalCache");
        if (ec != null) {
            log.info("StaticResourceServlet - Got External Cache from ProxyServlet");
            externalCache = new File((String) ec);
        } else {
            log.fatal("StaticResourceServlet - Proxy not started");
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) {
        Request request = new Request(req, resp, externalCache);
        log.info("Static resource GET " + request.uri);

        try {
            if (GetStaticResourceRequest.isRequest(request)) new GetStaticResourceRequest(request).run();
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
}
