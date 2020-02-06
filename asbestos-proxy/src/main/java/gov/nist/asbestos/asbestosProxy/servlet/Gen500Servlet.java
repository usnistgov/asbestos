package gov.nist.asbestos.asbestosProxy.servlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Gen500Servlet extends HttpServlet  {

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)  {
        resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) {
        resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
    }

    @Override
    public void doDelete(HttpServletRequest req, HttpServletResponse resp)  {
        resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
    }
}
