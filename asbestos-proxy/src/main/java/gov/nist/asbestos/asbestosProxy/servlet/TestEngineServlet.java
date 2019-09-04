package gov.nist.asbestos.asbestosProxy.servlet;

import com.google.gson.Gson;
import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.log.SimStore;
import gov.nist.asbestos.http.support.Common;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.simapi.simCommon.SimId;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.testEngine.engine.TestEngine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.TestReport;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

        //   testlog/channelName/testCollection
        //      0         1           2
        if (uriParts.size() == 3 && uriParts.get(0).equals("testlog")) {
            String channelId = uriParts.get(1);
            String testCollection = uriParts.get(2);

            ChannelConfig channelConfig = getChannelConfig(resp, channelId);
            if (channelConfig == null) return;
            String testSession = channelConfig.getTestSession();

            StringBuilder buf = new StringBuilder();
            buf.append("{\n");
            List<File> testLogs = getTestLogs(testSession, testCollection);
            boolean first = true;
            for (File testLog : testLogs) {
                String name = testLog.getName();
                name = name.split("\\.")[0];
                String json;
                try {
                    if (!first)
                        buf.append(",\n");
                    json = new String(Files.readAllBytes(Paths.get(testLog.toString())));
//                    json = "\"results\"";
                    buf.append("\"").append(name).append("\": ").append(json);
                    first = false;
                } catch (IOException e) {
                    log.error(ExceptionUtils.getStackTrace(e));
                    throw new RuntimeException(e);
                }
            }
            buf.append("\n}");
            String theString = buf.toString();
            returnString(resp, theString);
            log.info("OK");
            return;
        }

        //   testlog/channelName/testCollection/testName
        //      0         1           2            3
        if (uriParts.size() == 4 && uriParts.get(0).equals("testlog")) {
            String channelId = uriParts.get(1);
            String testCollection = uriParts.get(2);
            String testName = uriParts.get(3);

            ChannelConfig channelConfig = getChannelConfig(resp, channelId);
            if (channelConfig == null) return;
            String testSession = channelConfig.getTestSession();

            File testLog = getTestLog(testSession, testCollection, testName);
            String json;
            try {
                json = new String(Files.readAllBytes(Paths.get(testLog.toString())));
            } catch (IOException e) {
                log.error(ExceptionUtils.getStackTrace(e));
                throw new RuntimeException(e);
            }
            returnString(resp, json);
            log.info("OK");
            return;
        }

        resp.setStatus(resp.SC_NOT_FOUND);
    }

    private ChannelConfig getChannelConfig(HttpServletResponse resp, String channelId) {
        try {
            return ChannelControlServlet.channelConfigFromChannelId(externalCache, channelId);
        } catch (Throwable e) {
            resp.setStatus(resp.SC_NOT_FOUND);
            return null;
        }
    }

    private String returnResource(HttpServletResponse resp, BaseResource resource) {
        String json = ProxyBase.getFhirContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(resource);
        returnString(resp, json);
        return json;
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

    private void initializeTestCollections() {
        File collections = new File(externalCache, "TestCollections");
        new File(collections, "default").mkdirs();
    }

    private File getTestLog(String testSession, String collectionName, String testName) {
        File testLogs = new File(externalCache, "FhirTestLogs");
        File forTestSession = new File(testLogs, testSession);
        File forCollection = new File(forTestSession, collectionName);
        forCollection.mkdirs();
        return new File(forCollection, testName + ".json");
    }

    private List<File> getTestLogs(String testSession, String collectionName) {
        File testLogs = new File(externalCache, "FhirTestLogs");
        File forTestSession = new File(testLogs, testSession);
        File forCollection = new File(forTestSession, collectionName);

        List<File> testLogList = new ArrayList<>();
        File[] tests = forCollection.listFiles();
        if (tests != null) {
            for (File test : tests) {
                if (!test.toString().endsWith(".json")) continue;
                if (test.toString().startsWith(".")) continue;
                testLogList.add(test);
            }
        }
        return testLogList;
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

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) {
        String uri = req.getRequestURI();
        log.info("doPost " + uri);

        List<String> uriParts1 = Arrays.asList(uri.split("/"));
        List<String> uriParts = new ArrayList<>(uriParts1);

        //  RUN TEST
        //   /appContext/engine/testrun
        //  0     1         2      3

        uriParts.remove(0);
        uriParts.remove(0);
        uriParts.remove(0);

        //   testrun/channelName/testCollection/testName
        //      0         1           2            3
        if (uriParts.size() == 4 && uriParts.get(0).equals("testrun")) {
            String channelId = uriParts.get(1);
            String testCollection = uriParts.get(2);
            String testName = uriParts.get(3);

            ChannelConfig channelConfig;
            try {
                channelConfig = ChannelControlServlet.channelConfigFromChannelId(externalCache, channelId);
            } catch (Throwable e) {
                resp.setStatus(resp.SC_NOT_FOUND);
                return;
            }
            String testSession = channelConfig.getTestSession();
            String sutStr = channelConfig.getFhirBase();
            URI sut = null;
            try {
                sut = new URI(sutStr);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            File testDir = getTest(testCollection, testName);

            TestReport report;
            try {
                report = new TestEngine(testDir, sut)
                        .setTestSession(testSession)
                        .setExternalCache(externalCache)
                        .setVal(new Val())
                        .setFhirClient(new FhirClient())
                        .run()
                        .getTestReport();
            } catch (Throwable t) {
                log.error(ExceptionUtils.getStackTrace(t));
                throw t;
            }
            String json = returnResource(resp, report);
            Path path = getTestLog(testSession, testCollection, testName).toPath();
            try (BufferedWriter writer = Files.newBufferedWriter(path))
            {
                writer.write(json);
            } catch (IOException e) {
                log.error(ExceptionUtils.getStackTrace(e));
                throw new RuntimeException(e);
            }

        }
    }
}
