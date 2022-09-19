package gov.nist.asbestos.client.Base;

import com.google.gson.Gson;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.events.UIEvent;
import gov.nist.asbestos.client.log.SimStore;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.simapi.simCommon.SimId;
import gov.nist.asbestos.simapi.simCommon.TestSession;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.utils.FHIRPathEngine;

import javax.xml.bind.JAXBException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static gov.nist.asbestos.client.Base.Dirs.listOfDirectories;
import static gov.nist.asbestos.client.Base.Dirs.listOfFiles;

public class EC {
    private static Logger log = Logger.getLogger(EC.class.getName());
    public File externalCache;

    public static final String MarkerType = "Marker";
    public static final String TEST_COLLECTIONS_DIR = "FhirTestCollections";
    public static final String SESSIONS_DIR = "FhirSessions";
    public static final String TEST_ASSERTIONS_DIR = "FhirTestAssertions";
    public static final String TEST_ASSERTIONS_FILE = "assertions.json";
    public static final String CHANNELS_DIR = "FhirChannels";
    public static final String DOCUMENT_CACHE = "FhirDocCache";
    public static final String TRASH_CAN = "trashcan";
    public static final String TEST_COLLECTION_PROPERTIES = "TestCollection.properties";
    public static final String TEST_PROPERTIES = "Test.properties";
    public static final String HIDDEN_KEY_NAME = "Hidden"; // TODO: make an enum
    public static final String DEFAULT_CHANNEL_NAME = "default";
    public static final String EXTERNAL_PATIENT_SERVER_CHANNEL_NAME = "external_patient";
    public static final SimId DEFAULT = new SimId(TestSession.DEFAULT_TEST_SESSION, DEFAULT_CHANNEL_NAME);
    public static final SimId EXTERNAL_PATIENT_SERVER = new SimId(TestSession.DEFAULT_TEST_SESSION, EXTERNAL_PATIENT_SERVER_CHANNEL_NAME);


    public EC(File externalCache) {
        this.externalCache = externalCache;
    }


    // channelId is testSession__channelName
    public File getCache(String channelId, String resourceType) {
        File cacheBase = new File(new File(new File(externalCache, "FhirTestLogs"), channelId), "cache");
        File typeBase = new File(cacheBase, resourceType);
        typeBase.mkdirs();
        return  typeBase;
    }

     public List<String> getTestCollectionNames() {
        return getTestCollections().stream().map(File::getName).collect(Collectors.toList());
    }

     List<File> getTestCollections() {
//        URL aUrl = EC.class.getClassLoader().getResource("/TestCollections/testCollectionRoot.txt");
//        String aFile = aUrl.getFile();
//        File internalRoot = new File(aFile).getParentFile();
//        List<File> intList = listOfFiles(internalRoot);
         List<File> collections = new ArrayList<>();

        File externalRoot = new File(externalCache, EC.TEST_COLLECTIONS_DIR);
        List<File> extList = listOfFiles(externalRoot);
        collections.addAll(extList);

        return collections;
    }

    public boolean isClientTestCollection(String testCollectionId) {
        Properties props  = getTestCollectionProperties(testCollectionId);
        return props.getProperty("TestType") != null && (props.getProperty("TestType").equalsIgnoreCase("Client"));
    }

    public List<String> getTestsInCollection(String collectionName) {
        List<String> list = getTests(collectionName).stream().map(File::getName).collect(Collectors.toList());
        if (list != null)
            Collections.sort(list);
        return list;
    }

    public File getTest(String collectionName, String testName) {
        List<File> tests = getTests(collectionName);

        for (File test : tests) {
            if (test.getName().equalsIgnoreCase(testName)) return test;
        }
        return null;
    }

     public List<File> getTests(String collectionName) {
        File root = getTestCollectionBase(collectionName);
        if (root == null)
            return new ArrayList<>();
        List<File> testDirs = listOfDirectories(root);
        return testDirs
                .stream()
                .filter(s -> !isHiddenTest(s))
                .collect(Collectors.toList());
    }

    public boolean isHiddenTest(File testDir) {
        Properties props = getTestProperties(testDir);
        if (props != null) {
            String hiddenValue = props.getProperty(EC.HIDDEN_KEY_NAME);
            return "true".equals(hiddenValue);
        }
        return false;
    }

    private static Properties defaultProperties = new Properties();
    static {
        defaultProperties.setProperty("TestType", "server");
    }

    public Properties getTestCollectionProperties(String collectionName) {
        Properties props = new Properties();
        File root = getTestCollectionBase(collectionName);
        if (root == null)
            return props;
        File file = new File(root, TEST_COLLECTION_PROPERTIES);
        try (final FileInputStream fis = new FileInputStream(file)) {
            props.load(fis);
        } catch (IOException e) {
            return defaultProperties;
        }
        return props;
    }

    public String getTestCollectionDescription(String collectionName) {
        File root = getTestCollectionBase(collectionName);
        if (root == null)
            throw new Error("Test Collection " + collectionName + " not found");
        File file = new File(root, "description.txt");
        if (!file.exists())
            return null;
        try {
            return new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public Properties getTestProperties(String collectionName, String testName) {
        File testDir = getTest(collectionName, testName);
        return getTestProperties(testDir);
    }

    static Properties getTestProperties(File testDir) {
        Properties props = new Properties();
        if (testDir != null ) {
            File testProps = new File(testDir, EC.TEST_PROPERTIES);
            if (testProps.exists()) {
                try (final FileInputStream fis = new FileInputStream(testProps)) {
                    props.load(fis);
                    return props;
                } catch (IOException ex) {}
            }
        }
        return null;
    }

    public File getTestAssertionsFile() {
        return new File(new File(externalCache, TEST_ASSERTIONS_DIR), TEST_ASSERTIONS_FILE);
    }

    File getTestCollectionBase(String collectionName) {
        return externalTestCollectionBase(collectionName);
    }

     File externalTestCollectionBase(String collectionName) {
        File externalRoot = new File(externalCache, TEST_COLLECTIONS_DIR);
        if (!externalRoot.exists()) return null;
        if (!externalRoot.isDirectory()) return null;
        File collectionRoot = new File(externalRoot, collectionName);
        if (!collectionRoot.exists()) return null;
        if (!collectionRoot.isDirectory()) return null;
        return collectionRoot;
    }

    public static File ftkTrashCan(File ecDir) {
        return new File(ecDir, TRASH_CAN);
    }

    public File getFtkSessions() {
        return ftkSessionsDir(externalCache);
    }

    public static File ftkSessionsDir(File ecDir) {
        return new File(ecDir, SESSIONS_DIR);
    }

    public static File ftkChannelsDir(File ecDir) {
        return new File(ecDir, CHANNELS_DIR);
    }

    public static File ftkSessionDir(File ecDir, String sessionName) {
        return new File(ftkSessionsDir(ecDir), sessionName);
    }

    public File getSessionDir(String sessionId) {
        return new File(getFtkSessions(), sessionId);
    }

    public File getTestCollectionsBase() {
        return new File(externalCache, TEST_COLLECTIONS_DIR);
    }

    public File getTestLog(String channelId, String collectionName, String testName) {
        return getTestLog(channelId, collectionName, testName, null);
    }

    public List<String> getTestLogModules(String channelId, String collectionName, String testName) {
        List<String> names = new ArrayList<>();
        File collectionDir = getTestLogCollectionDir(channelId, collectionName);
        File testDir = new File(collectionDir, testName);
        if (!testDir.isDirectory())
            return names;
        File[] files = testDir.listFiles();
        if (files == null)
            return names;
        for (File file : files) {
            if (file.isDirectory())
                names.add(file.getName());
        }
        return names;
    }

    public File getTestLogDir(String channelId, String collectionName, String testName) {
        File collectionDir = getTestLogCollectionDir(channelId, collectionName);
        File testDir = new File(collectionDir, testName);
        return testDir;
    }

     public File getTestLog(String channelId, String collectionName, String testName, String moduleName) {
        File collectionDir = getTestLogCollectionDir(channelId, collectionName);
        File testDir = new File(collectionDir, testName);
        if (moduleName == null) {
            testDir.mkdirs();
            return new File(testDir, "TestReport.json");
        }
        File moduleDir = new File(testDir, moduleName);
        moduleDir.mkdirs();
        return new File(moduleDir, "TestReport.json");
    }

    // channelId is testSession__channel
    public File getTestLogCollectionDir(String channelId, String collectionName) {
        Objects.requireNonNull(channelId);
        File testLogs = new File(externalCache, "FhirTestLogs");
        File forChannelId = new File(testLogs, channelId);
        File forCollection = (collectionName == null) ? forChannelId : new File(forChannelId, collectionName);
        forCollection.mkdirs();
        return forCollection;
    }

    public File getTestLogCacheDir(String channelId) {
        return new File(getTestLogCollectionDir(channelId, null), "cache");
    }

    // channelId is testSession__channel
    public List<File> getTestLogs(String channelId, String collectionName) {
//        File testLogs = new File(externalCache, "FhirTestLogs");
//        File forTestSession = new File(testLogs, channelId);
        File forCollection = getTestLogCollectionDir(channelId, collectionName); //new File(forTestSession, collectionName);

        List<File> testLogList = new ArrayList<>();
        File[] tests = forCollection.listFiles();
        if (tests != null) {
            for (File test : tests) {
                if (test.isDirectory()) {
                    File report = new File(test, "TestReport.json");
                    if (report.exists())
                        testLogList.add(report);
                } else {
                    String name = test.toString();
                    if (!name.endsWith(".json")) continue;
                    if (name.startsWith(".")) continue;
                    if (name.startsWith("_")) continue;
                    testLogList.add(test);
                }
            }
        }
//        log.info("got " + testLogList.size() + " test logs from " + testLogs.toString());
        return testLogList;
    }

    public File getResourceType(String testSession, String channelId, String resourceType) {
        File fhir = fhirDir(testSession, channelId);
        return new File(fhir, resourceType);
    }

    public void buildEventJson(Request request, String testSession, String channelId, String resourceType, String eventName) {
        UIEvent uiEvent = getEvent(testSession, channelId, resourceType, eventName);
        if (uiEvent == null) {
            request.notFound();
            return;
        }

        String json = new Gson().toJson(uiEvent);
        request.resp.setContentType("application/json");
        try {
            request.resp.getOutputStream().write(json.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        request.ok();
    }

    public UIEvent eventFromJson(String json) {
        return new Gson().fromJson(json, UIEvent.class);
    }

    public UIEvent getEvent(EventContext eventContext) {
        return getEvent(
            eventContext.getTestSession(),
            eventContext.getChannelId(),
            "null",
            eventContext.getEventId()
        );
    }

    public UIEvent getEvent(String testSession, String channelId, String resourceType, String eventName) {
        if (channelId.contains("__")) {
            String[] parts = channelId.split("__");
            if (parts.length == 2)
                channelId = parts[1];
        }
        File fhir = fhirDir(testSession, channelId);
        if (resourceType.equals("null")) {
            resourceType = resourceTypeForEvent(fhir, eventName);
            if (resourceType == null) {
                return null;
            }
        }
        File resourceTypeFile = new File(fhir, resourceType);
        if (eventName == null)
            return null;
        File eventDir = new File(resourceTypeFile, eventName);

        UIEvent uiEvent = new UIEvent(new EC(externalCache)).fromEventDir(eventDir);
        uiEvent.setEventName(eventName);
        uiEvent.setResourceType(resourceType);
        uiEvent.setTestSession(testSession);
        uiEvent.setChannelId(channelId);
        return uiEvent;
    }

    public File fhirDir(String testSession, String channelId) {
        File psimdb = new File(externalCache, SimStore.PSIMDB);
        File testSessionFile = new File(psimdb, testSession);
        File channelFile = new File(testSessionFile, channelId);
        return new File(channelFile, "fhir");
    }

    public String resourceTypeForEvent(File fhir, String eventName) {
        File[] resourceTypeFiles = fhir.listFiles();
        if (resourceTypeFiles != null) {
            for (File resourceTypeDir : resourceTypeFiles) {
                File[] eventFiles = resourceTypeDir.listFiles();
                if (eventFiles != null) {
                    for (File eventFile : eventFiles) {
                        if (eventFile.getName().equals(eventName)) {
                            return resourceTypeDir.getName();
                        }
                    }
                }
            }
        }
        return null;
    }


    public String getLastMarker(String testSession, String channelId) {
        File markerDir = getResourceType(testSession, channelId, MarkerType);
        List<String> markers = Dirs.dirListingAsStringList(markerDir);
        if (markers.size() == 0) {
            return null;
        } else if (markers.size() == 1) {
            return markers.get(0);
        } else {
            markers.sort(String.CASE_INSENSITIVE_ORDER);
            return markers.get(markers.size() - 1);
        }
    }

    public List<File> getEventsSince(SimId simId, String marker) {
        List<File> eventsList = new ArrayList<>();
        SimStore simStore = new SimStore(externalCache, simId);
        List<File> resourceTypeDirs = simStore.getResourceTypeDirs();
        for (File resourceTypeDir : resourceTypeDirs) {
            List<File> events = Dirs.listOfDirectories(resourceTypeDir);
            for (File event : events) {
                String eventName = event.getName();
                if (marker == null)
                    eventsList.add(event);
                else if (eventName.compareTo(marker) > 0)
                    eventsList.add(event);
            }
        }
        return eventsList;
    }

    public File getEvent(SimId simId, String eventId) {
        SimStore simStore = new SimStore(externalCache, simId);
        List<File> resourceTypeDirs = simStore.getResourceTypeDirs();
        for (File resourceTypeDir : resourceTypeDirs) {
            List<File> events = Dirs.listOfDirectories(resourceTypeDir);
            for (File event : events) {
                String eventName = event.getName();
                if (eventName.equals(eventId))
                    return event;
            }
        }
        return null;
    }

    public File getDocumentCache() {
        return new File(externalCache, DOCUMENT_CACHE);
    }


    public File getCodesFile(String environment) {
        return new File(new File(new File(externalCache, "environment"), environment), "codes.xml");
    }

    // these utilities duplicated from FhirPathEngineBuilder
    private FHIRPathEngine build() {
        return new FHIRPathEngine(new HapiWorkerContext(ParserBase.getFhirContext(), new PrePopulatedValidationSupport(ParserBase.getFhirContext())));
    }

    private Resource evalForResource(Resource resourceIn, String expression) {
        List<Base> results = build().evaluate(resourceIn, expression);
        if (results.isEmpty())
            return null;
        if (results.size() > 1)
            return null;
        Base result = results.get(0);
        if (result instanceof Bundle.BundleEntryComponent) {
            Bundle.BundleEntryComponent comp = (Bundle.BundleEntryComponent) result;
            return comp.getResource();
        }
        return null;
    }


    public ResourceWrapper getStaticFixture(String testCollectionId, String testId, String channelId, String fixturePath, String fhirPath, URL url) {
        if (fixturePath == null || fixturePath.startsWith("/") || fixturePath.startsWith("."))
            return null;
        File testDir = getTest(testCollectionId, testId);
        if (testDir == null || !testDir.exists() || !testDir.isDirectory())
            return null;
        if (fixturePath.contains("?")) {
            String[] parts = fixturePath.split("\\?");
            String query = parts[1];
            if (query.startsWith("url")) {
                try {
                    fixturePath = URLDecoder.decode(query, StandardCharsets.UTF_8.toString());
                } catch (UnsupportedEncodingException e) {
                    return null;
                }
                parts = fixturePath.split("=");
                if (parts.length == 2)
                    fixturePath = parts[1];
            }
        }


        File file = new File(testDir, fixturePath);
        BaseResource resource = null;
        ResourceWrapper wrapper = null;

        // look in test definition
        if (file.exists() && file.isFile()) {
            try {
                resource = ParserBase.parse(file);
                URI uri = url.toURI();
                Ref ref = new Ref(uri);
                wrapper = new ResourceWrapper();
                wrapper.setResource(resource);
                wrapper.setRef(ref);
            } catch (Exception e) {
                // ignore and try next approach
                wrapper = null;
            }
        }

        // look in caches
        if (wrapper == null) {
            FhirClient fhirClient = FhirSearchPath.getFhirClient(this, testDir, channelId);
            Optional<ResourceWrapper> owrapper = fhirClient.readCachedResource(new Ref(fixturePath));
            if (!owrapper.isPresent())
                return null;
            wrapper = owrapper.get();
            resource = wrapper.getResource();
        }

        if (resource instanceof Bundle && fhirPath != null) {
            Bundle bundle = (Bundle) resource;
            Resource resource1 = evalForResource(bundle, fhirPath);
            if (resource1 == null)  // resource not found in bundle
                return null;
            wrapper.setResource(resource1);
        }
        return wrapper;
    }

    public boolean mhdValueSetsNeedBuilding(String environment) {
        return new MhdValueSets(externalCache, environment).needsBuilding();
    }

    /**
     * Build ValueSet(s) from codes.xml and save into ec/environment/<environment>/valuesets
     * @param environment
     */
    public void buildMhdValueSets(String environment) {
        try {
            new MhdValueSets(externalCache, environment).build(this);
        } catch (FileNotFoundException | JAXBException e) {
            log.log(Level.SEVERE, e.toString(), e);
            throw new RuntimeException(e);
        }
    }

    public List<ValueSet> getMhdValueSets(String environment) {
        return new MhdValueSets(externalCache, environment).load();
    }

    public Map<String, ValueSet> getMhdValueSetsAsMap(String environment) {
        Map<String, ValueSet> map = new HashMap<String, ValueSet>();

        for (ValueSet vs : getMhdValueSets(environment)) {
            map.put(vs.getUrl(), vs);
        }

        return map;
    }

    public void writeToFile(File where, String content) {
        Path path = where.toPath();
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(content);
        } catch (IOException e) {
            log.log(Level.SEVERE, e.toString(), e);
            throw new RuntimeException(e);
        }
    }

    public static String readFromFile(File file) {
        byte[] data;
        try {
            data = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            log.log(Level.SEVERE, "", e);
            throw new RuntimeException(e);
        }
        return new String(data);
    }
}
