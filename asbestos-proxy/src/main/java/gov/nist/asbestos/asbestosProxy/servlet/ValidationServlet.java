package gov.nist.asbestos.asbestosProxy.servlet;

import gov.nist.asbestos.asbestosProxy.requests.GetValidationRequest;
import gov.nist.asbestos.asbestosProxy.requests.Request;
import gov.nist.asbestos.simapi.tk.installation.Installation;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

public class ValidationServlet  extends HttpServlet {
    private File externalCache = null;
    private static Logger log = Logger.getLogger(ValidationServlet.class);

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

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) {
        if (externalCache == null) {
            resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        Request request = new Request(req, resp, externalCache);

        try {
            if (GetValidationRequest.isRequest(request)) new GetValidationRequest(request).run();
        } catch (Throwable t) {
            request.serverError(t);
        }
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) {

        if (externalCache == null) {
            resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        Request request = new Request(req, resp, externalCache);

        try {

        } catch (Throwable t) {
            request.serverError(t);
        }
    }
}
