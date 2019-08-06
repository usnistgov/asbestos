package gov.nist.asbestos.asbestosProxy.wrapper;

import gov.nist.asbestos.simapi.tk.installation.Installation;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ProxyLogServlet extends HttpServlet {
    private File externalCache = null;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        // TODO put EC location in web.xml
        setExternalCache(new File("/home/bill/ec"));
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
        String uri = req.getRequestURI();

        // uri should be
        // environment/testsession/resourcetype/event

        String[] uriParts = uri.split("/");
        if (uriParts.length != 7) {
            resp.setStatus(resp.SC_BAD_REQUEST);
            return;
        }
        String environment = uriParts[3];
        String testsession = uriParts[4];
        String resourcetype = uriParts[5];
        String event = uriParts[6];

        File psimdb = new File(externalCache, "psimdb");
        File env = new File(psimdb, environment);
        File ts = new File(env, testsession);
        File fhir = new File(ts, "fhir");
        File resourceType = new File(fhir, resourcetype);
        File theEvent = new File(resourceType, event);

        if (!theEvent.exists() || !theEvent.canRead() || !theEvent.isDirectory()) {
            resp.setStatus(resp.SC_NOT_FOUND);
            return;
        }

        //resp.addHeader("Content-Type", "text/html; charset=utf-8");
        StringBuilder b = new StringBuilder();
        b.append("<!DOCTYPE HTML>\n<html><body>");

        b.append("<h1>" + event + "</h1>");

        for (int task=0; ; task++) {
            File taskDir = new File(theEvent, "task" + task);
            if (!taskDir.exists())
                break;
            displayEvent(b, theEvent, "Task" + task);
        }

        b.append("</body></html>");

        try {
            resp.getOutputStream().write(b.toString().getBytes());
        } catch (IOException e) {
            resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void displayEvent(StringBuilder b, File theEvent, String label) {
        String section = label.toLowerCase();
        b.append("<h2>").append(label).append("</h2>");
        b.append("<h3>Request</h3>");
        b.append("<pre>").append(read(theEvent, section, "request_header.txt")).append("</pre>");
        b.append("<br />");
        b.append("<pre>").append(read(theEvent, section, "request_body.txt")).append("</pre>");

        b.append("<h3>Response</h3>");
        b.append("<pre>").append(read(theEvent, section, "response_header.txt")).append("</pre>");
        b.append("<br />");
        b.append("<pre>").append(read(theEvent, section, "response_body.txt")).append("</pre>");
    }

    private String read(File theEvent, String theSection, String thePart) {
        File file = new File(new File(theEvent, theSection), thePart);
        if (!file.exists() || !file.canRead()) {
            String fileSt = file.toString();
            if (fileSt.endsWith(".txt")) {
                fileSt = fileSt.replace(".txt", ".bin");
                file = new File(fileSt);
            }
            if (!file.exists() || !file.canRead()) {
                return "";
            }
        }

        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            content = content.replaceAll("<", "&lt;");
            return content;
        } catch (Exception e) {
            ;
        }
        return "";
    }

}
