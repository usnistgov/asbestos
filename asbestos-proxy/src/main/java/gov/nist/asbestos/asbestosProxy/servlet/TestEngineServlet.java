package gov.nist.asbestos.asbestosProxy.servlet;

import com.google.gson.Gson;
import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.http.support.Common;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.hl7.fhir.r4.model.BaseResource;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
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

        List<String> uriParts1 = Arrays.asList(uri.split("/"));
        List<String> uriParts = new ArrayList<>(uriParts1);

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
            log.info("OK");
            return;
        }

        //  collection/name
        //      0        1
        if (uriParts.size() == 2 && uriParts.get(0).equals("collection")) {
            // return list of test names in collection

            String collectionName = uriParts.get(1);
            List<String> names = getTestsInCollection(collectionName);

            returnList(resp, names);
            log.info("OK");
            return;
        }

        //  collection/collectionName/testName
        //      0             1          2
        if (uriParts.size() == 3 && uriParts.get(0).equals("collection")) {
            // return JSON of TestScript

            String collectionName = uriParts.get(1);
            String testName = uriParts.get(2);

            File testDef = getTest(collectionName, testName);
            if (testDef == null) {
                resp.setStatus(resp.SC_NOT_FOUND);
                return;
            }

            byte[] bytes;
            File testFile;
            testFile = new File(testDef, "TestScript.json");
            if (!testFile.exists()) {
                testFile = new File(testDef, "TestScript.xml");
            }
            if (testFile.exists()) {
                try {
                    bytes = FileUtils.readFileToByteArray(testFile);
                } catch (IOException e) {
                    resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
                    return;
                }

                BaseResource resource = ProxyBase.parse(bytes, Format.fromContentType(testFile.getName()));
                String json = ProxyBase.getFhirContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(resource);
                returnString(resp, json);

                log.info("OK");
                return;
            }
        }

        resp.setStatus(resp.SC_NOT_FOUND);
    }

    private List<String> getTestsInCollection(String collectionName) {
        return getTests(collectionName).stream().map(File::getName).collect(Collectors.toList());
    }

    private List<String> getTestCollectionNames() {
        return getTestCollections().stream().map(File::getName).collect(Collectors.toList());
    }

    private void returnString(HttpServletResponse resp, String json) {
        resp.setContentType("application/json");
        try {
            resp.getOutputStream().print(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    private File getTest(String collectionName, String testName) {
        List<File> tests = getTests(collectionName);

        for (File test : tests) {
            if (test.getName().equalsIgnoreCase(testName)) return test;
        }
        return null;
    }

    private List<File> getTests(String collectionName) {
        List<File> files = new ArrayList<>();

        File root;
        root = externalTestCollectionBase(collectionName);
        if (root != null) return listOfDirectories(root);

        root = internalTestCollectionBase(collectionName);
        if (root != null) return listOfDirectories(root);

        return files;
    }

    private List<File> listOfDirectories(File root) {
        List<File> list = new ArrayList<>();

        File[] aList = root.listFiles();
        if (aList != null) {
            for (File file : aList) {
                if (!file.isDirectory()) continue;
                if (file.getName().startsWith(".")) continue;
                if (file.getName().startsWith("_")) continue;
                list.add(file);
            }
        }

        return list;
    }

    private File internalTestCollectionBase(String collectionName) {
        URL aUrl = getClass().getResource("/TestCollections/testCollectionRoot.txt");
        String aFile = aUrl.getFile();
        File internalRoot = new File(aFile).getParentFile();

        File collectionRoot = new File(internalRoot, collectionName);
        if (collectionRoot.exists() && collectionRoot.isDirectory())
            return collectionRoot;

        return null;
    }

    private File externalTestCollectionBase(String collectionName) {
        File externalRoot = new File(externalCache, "TestCollections");
        if (!externalRoot.exists()) return null;
        if (!externalRoot.isDirectory()) return null;
        File collectionRoot = new File(externalRoot, collectionName);
        if (!collectionRoot.exists()) return null;
        if (!collectionRoot.isDirectory()) return null;
        return collectionRoot;
    }

    private List<File> getTestCollections() {
        List<File> collections = new ArrayList<>();

        URL aUrl = getClass().getResource("/TestCollections/testCollectionRoot.txt");
        String aFile = aUrl.getFile();
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
