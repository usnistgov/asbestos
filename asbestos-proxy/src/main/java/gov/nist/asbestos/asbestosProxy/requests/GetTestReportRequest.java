package gov.nist.asbestos.asbestosProxy.requests;

import com.google.gson.Gson;
import gov.nist.asbestos.asbestosProxy.servlet.ChannelConnector;
import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.testEngine.engine.ModularLogs;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

// 0 - empty
// 1 - appContext
// 2 - "engine"
// 3 - "testReport"
// 4 - channelName   testSession__channelId
// 5 - testCollectionId
// 6 - testId
// return List of TestReports - main report first

public class GetTestReportRequest {
    private static Logger log = Logger.getLogger(GetTestReportRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 7 && request.uriParts.get(3).equals("testReport");
    }

    public GetTestReportRequest(Request request) {
        this.request = request;
    }

//    static class NoReport {
//        String value = "No Report";
//    }

    public void run() {
        log.info("GetTestReportRequest");
        String channelId = request.uriParts.get(4);
        String testCollection = request.uriParts.get(5);
        String testName = request.uriParts.get(6);

        ChannelConfig channelConfig = ChannelConnector.getChannelConfig(request.resp, request.externalCache, channelId);
        if (channelConfig == null) throw new Error("Channel does not exist");

        try {
            String json = new ModularLogs(request.ec, channelId, testCollection, testName).asJson();
            Returns.returnString(request.resp, json);
            log.info("ModuleLogs:");
            log.info(json);
        } catch (IOException e) {
            Returns.returnString(request.resp, new ModularLogs().asJson());
        }

//        //File testLog = request.ec.getTestLog(channelId, testCollection, testName);
//        List<String> moduleNames = request.ec.getTestLogModules(channelId, testCollection, testName);
//        for (String moduleName : moduleNames) {
//            File moduleReportFile = request.ec.getTestLog(channelId, testCollection, testName, moduleName);
//
//        }
//
//        byte[] bytes;
//        try {
//            bytes = FileUtils.readFileToByteArray(testLog);
//        } catch (IOException e) {
//            String json = new Gson().toJson(new NoReport());
//            Returns.returnString(request.resp, json);
//            return;
//        }
//
//        BaseResource resource = ProxyBase.parse(bytes, Format.fromContentType(testLog.getName()));
//        TestReport testReport = (TestReport) resource;
//
//        List<TestReport.TestReportTestComponent> testComponents = testReport.getTest();
//        int index = 0;
//        int testComponentCount = testComponents.size();
//
//        for (; index < testComponentCount; ) {
//            TestReport.TestReportTestComponent test = testComponents.get(index);
//            if (test.hasModifierExtension() && test.getModifierExtension().get(0).hasValue()) {
//                TestReport containedTestReport = getContainedTestReport(testReport, test.getModifierExtension().get(0).getValue().toString());
//                if (containedTestReport != null) {
//                    if (!containedTestReport.hasName() && containedTestReport.hasId())
//                        containedTestReport.setName(containedTestReport.getId());
//                }
//            }
//            index++;
//        }
//
//        testReport.setName(testName);
//
//        String json = ProxyBase.getFhirContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(testReport);
//        Returns.returnString(request.resp, json);
    }

//    private TestReport getContainedTestReport(TestReport testReport, String id) {
//        List<Resource> containeds = testReport.getContained();
//        for (Resource contained : containeds) {
//            if (contained instanceof TestReport) {
//                TestReport containedTestReport = (TestReport) contained;
//                if (contained.hasId() && contained.getId().equals(id)) {
//                    return containedTestReport;
//                }
//            }
//        }
//        return null;
//    }

}
