package gov.nist.asbestos.testEngine.engine;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.Base.ParserBase;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.r4.model.TestReport;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ModularReports {
    private boolean objectModel;
    private Map<String, String> reports = new HashMap<>();  // name => TestReport json string
    private Map<String, TestReport> reportsObject = new HashMap<>();  // name => TestReport object

    public ModularReports(EC ec, String channelId, String testCollection, String testName, boolean objectModel) throws IOException {
        this.objectModel = objectModel;
        File base = ec.getTestLogDir(channelId, testCollection, testName);

        addReport(new File(base, "TestReport.json"));
        File[] modules = base.listFiles();
        if (modules != null) {
            for (File module : modules) {
                if (module.isDirectory())
                    addReport(new File(module, "TestReport.json"));
            }
        }

    }

    public ModularReports(EC ec, String channelId, String testCollection, String testName) throws IOException {
        this(ec, channelId, testCollection, testName, false);
    }

    public ModularReports(Map<String, String> reports) {
        this.reports = reports;
    }

    public ModularReports() {

    }

    private void addReport(File file) throws IOException {
        String json;
        TestReport report;
        try {
            json = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            report = (TestReport) ParserBase.getFhirContext().newJsonParser().parseResource(json);
        } catch (IOException e) {
            return;
        }

        if (this.objectModel) {
            reportsObject.put(report.getName(), report);
        } else {
            reports.put(report.getName(), json);
        }
    }

    public String asJson() {
        JsonObject jsonObject = new JsonObject();

        for (String name : reports.keySet()) {
            String reportJson = reports.get(name);
            JsonElement ele = new Gson().fromJson(reportJson, JsonElement.class);
            jsonObject.add(name, ele);
        }
        String str = jsonObject.toString();
        return str;
    }

    public Map<String, TestReport> getReportsObject() {
        return reportsObject;
    }
}
