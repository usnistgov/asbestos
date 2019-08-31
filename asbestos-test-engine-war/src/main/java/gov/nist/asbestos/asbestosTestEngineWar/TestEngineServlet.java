package gov.nist.asbestos.asbestosTestEngineWar;

import gov.nist.asbestos.http.support.Common;
import org.apache.log4j.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestEngineServlet extends HttpServlet {
    private static Logger log = Logger.getLogger(TestEngineServlet.class);
    private File externalCache = null;

    public TestEngineServlet() {
        super();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        log.info("TestEngineServlet init");
        // announce location of ExternalCache to other servlets
        Object ec = config.getServletContext().getAttribute("ExternalCache");
        if (ec != null) {
            externalCache = new File((String) ec);
            initializeTestCollections();
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)  {
        URI uri = Common.buildURI(req);
        log.info("doGet " + uri);
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
