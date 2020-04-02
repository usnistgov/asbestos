package gov.nist.asbestos.testEngine.engine;

import com.google.gson.Gson;
import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.Base.ProxyBase;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.TestReport;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ModularLogs {
    public List<TestReport> reports = new ArrayList<>();

    public ModularLogs(EC ec, String channelId, String testCollection, String testName) throws IOException {
        File base = ec.getTestLog(channelId, testCollection, testName);

        addReport(new File(base, "TestReport.json"));
        File[] modules = base.listFiles();
        if (modules != null) {
            for (File module : modules) {
                addReport(new File(module, "TestReport.json"));
            }
        }
    }

    public ModularLogs() {

    }

    private void addReport(File file) throws IOException {
        String contents = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        IBaseResource resource = ProxyBase.getFhirContext().newJsonParser().parseResource(contents);
        reports.add((TestReport) resource);
    }

    public String asJson() {
        return new Gson().toJson(this);
    }
}
