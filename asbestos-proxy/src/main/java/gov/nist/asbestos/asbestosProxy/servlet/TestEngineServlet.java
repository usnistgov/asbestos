package gov.nist.asbestos.asbestosProxy.servlet;

import com.google.gson.Gson;
import gov.nist.asbestos.http.support.Common;
import org.apache.log4j.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TestEngineServlet extends HttpServlet {
    private static Logger log = Logger.getLogger(TestEngineServlet.class);
    private File externalCache = null;

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
            initializeTestCollections();
        } else {
            log.fatal("TestEngineServlet - Proxy not started");
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)  {
        String uri = req.getRequestURI();
        log.info("doGet " + uri);

        List<String> uriParts = Arrays.asList(uri.split("/"));

        //  /appContext/engine/collections
        // 0    1         2        3

        if (uriParts.size() < 4) {
            resp.setStatus(resp.SC_NOT_FOUND);
            return;
        }

        uriParts.remove(0);
        uriParts.remove(0);
        uriParts.remove(0);

        //  collections
        //      0
        if (uriParts.size() == 1 && uriParts.get(0).equals("collections")) {
            // return list of test collection names

            List<String> names = getTestCollectionNames();
            returnList(resp, names);
            return;
        }




        resp.setStatus(resp.SC_NOT_FOUND);
    }

    private List<String> getTestCollectionNames() {
        return getTestCollections().stream().map(File::getName).collect(Collectors.toList());
    }

    private void returnList(HttpServletResponse resp, List<String> values) {
        String json = new Gson().toJson(values);
        resp.setContentType("application/json");
        try {
            resp.getOutputStream().print(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) {
        URI uri = Common.buildURI(req);
        log.info("doPost " + uri);
    }

    private void initializeTestCollections() {
        File collections = new File(externalCache, "TestCollections");
        new File(collections, "default").mkdirs();
    }

    private List<File> getTestCollections() {
        List<File> collections = new ArrayList<>();

        String aFile = getClass().getResource("testCollectionRoot.txt").getFile();
        File internalRoot = new File(aFile).getParentFile();
        File[] intList = internalRoot.listFiles();
        if (intList != null)
            collections.addAll(Arrays.asList(intList));

        File externalRoot = new File(externalCache, "TestCollections");
        File[] extList = externalRoot.listFiles();
        if (extList != null)
            collections.addAll(Arrays.asList(extList));

        return collections;
    }

}
