package gov.nist.asbestos.asbestosProxy.requests;

import com.fasterxml.jackson.databind.annotation.JsonAppend;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static gov.nist.asbestos.asbestosProxy.requests.Dirs.listOfDirectories;
import static gov.nist.asbestos.asbestosProxy.requests.Dirs.listOfFiles;

public class EC {
    File externalCache;

    public EC(File externalCache) {
        this.externalCache = externalCache;
    }

     List<String> getTestCollectionNames() {
        return getTestCollections().stream().map(File::getName).collect(Collectors.toList());
    }

     List<File> getTestCollections() {
        List<File> collections = new ArrayList<>();

        URL aUrl = EC.class.getResource("/TestCollections/testCollectionRoot.txt");
        String aFile = aUrl.getFile();
        File internalRoot = new File(aFile).getParentFile();
        List<File> intList = listOfFiles(internalRoot);
        collections.addAll(intList);

        File externalRoot = new File(externalCache, "TestCollections");
        List<File> extList = listOfFiles(externalRoot);
        collections.addAll(extList);

        return collections;
    }

     List<String> getTestsInCollection(String collectionName) {
        return getTests(collectionName).stream().map(File::getName).collect(Collectors.toList());
    }

    public File getTest(String collectionName, String testName) {
        List<File> tests = getTests(collectionName);

        for (File test : tests) {
            if (test.getName().equalsIgnoreCase(testName)) return test;
        }
        return null;
    }

     List<File> getTests(String collectionName) {
        File root = getTestCollectionBase(collectionName);
        if (root == null)
            return new ArrayList<>();
        return listOfDirectories(root);
    }

    private static Properties defaultProperties = new Properties();
    static {
        defaultProperties.setProperty("TestType", "server");
    }

    Properties getTestCollectionProperties(String collectionName) {
        Properties props = new Properties();
        File root = getTestCollectionBase(collectionName);
        if (root == null)
            return props;
        File file = new File(root, "TestCollection.properties");
        try {
            props.load(new FileInputStream(file));
        } catch (IOException e) {
            return defaultProperties;
        }
        return props;
    }

    File getTestCollectionBase(String collectionName) {
        File base = externalTestCollectionBase(collectionName);
        if (base != null)
            return base;
        return internalTestCollectionBase(collectionName);
    }

     File externalTestCollectionBase(String collectionName) {
        File externalRoot = new File(externalCache, "TestCollections");
        if (!externalRoot.exists()) return null;
        if (!externalRoot.isDirectory()) return null;
        File collectionRoot = new File(externalRoot, collectionName);
        if (!collectionRoot.exists()) return null;
        if (!collectionRoot.isDirectory()) return null;
        return collectionRoot;
    }

     File internalTestCollectionBase(String collectionName) {
        URL aUrl = getClass().getResource("/TestCollections/testCollectionRoot.txt");
        String aFile = aUrl.getFile();
        File internalRoot = new File(aFile).getParentFile();

        File collectionRoot = new File(internalRoot, collectionName);
        if (collectionRoot.exists() && collectionRoot.isDirectory())
            return collectionRoot;

        return null;
    }

     File getTestLog(String channelId, String collectionName, String testName) {
        File testLogs = new File(externalCache, "FhirTestLogs");
        File forChannelId = new File(testLogs, channelId);
        File forCollection = new File(forChannelId, collectionName);
        forCollection.mkdirs();
        return new File(forCollection, testName + ".json");
    }

    List<File> getTestLogs(String testSession, String collectionName) {
        File testLogs = new File(externalCache, "FhirTestLogs");
        File forTestSession = new File(testLogs, testSession);
        File forCollection = new File(forTestSession, collectionName);

        List<File> testLogList = new ArrayList<>();
        File[] tests = forCollection.listFiles();
        if (tests != null) {
            for (File test : tests) {
                String name = test.toString();
                if (!name.endsWith(".json")) continue;
                if (name.startsWith(".")) continue;
                if (name.startsWith("_")) continue;
                testLogList.add(test);
            }
        }
        return testLogList;
    }

}
