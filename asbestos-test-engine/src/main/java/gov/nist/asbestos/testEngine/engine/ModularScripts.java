package gov.nist.asbestos.testEngine.engine;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.Format;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.TestScript;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class ModularScripts {
    // testId => TestScript json
    // testId/componentId => TestScript.json
    private final Map<String, String> scripts = new HashMap<>();

    public ModularScripts(File testDef) {
        // fill the script Map with the base script and all referenced component scripts
        String testId = testDef.getName();
        File testFile = TestEngine.findTestScriptFile(testDef);
        if (!testFile.exists())
            throw new RuntimeException("Not a test definition: " + testDef);
        byte[] bytes;
        try {
            bytes = FileUtils.readFileToByteArray(testFile);
        } catch (IOException e) {
            throw new RuntimeException("Cannot read " + testFile);
        }

        BaseResource resource;
        try {
            resource = ProxyBase.parse(bytes, Format.fromContentType(testFile.getName()));
        } catch (Throwable t) {
            throw new RuntimeException("Cannot parse: " + testFile);
        }
        TestScript testScript = (TestScript) resource;
        testScript.setName(testId);

        File descriptionFile = new File(testDef, "description.md");
        if (descriptionFile.exists()) {
            try {
                InputStream ins = new FileInputStream(descriptionFile);
                String description = org.apache.commons.io.IOUtils.toString(ins, Charset.defaultCharset());
                description = description.replaceAll("\\n", "\\\\n");
                testScript.setDescription(description);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        String json = ProxyBase.encode(testScript, Format.JSON);
        scripts.put(testScript.getName(), json);

        TestScript.TestScriptSetupComponent setup = testScript.getSetup();
        if (setup != null ) {
            for (TestScript.SetupActionComponent action : setup.getAction()) {
                handleImportScripts(testDef, testId, action);
            }
        }


        for (TestScript.TestScriptTestComponent test : testScript.getTest()) {
            for (TestScript.TestActionComponent action: test.getAction()) {
                handleImportScripts(testDef, testId, action);
            }
        }
    }

    void handleImportScripts(File testDef, String testId, TestScript.SetupActionComponent action) {
        if (!action.hasOperation())
            return;
        TestScript.SetupActionOperationComponent op = action.getOperation();
        handleImportAction(testDef, testId, op);
    }

    void handleImportScripts(File testDef, String testId, TestScript.TestActionComponent action) {
        if (!action.hasOperation())
            return;
        TestScript.SetupActionOperationComponent op = action.getOperation();
        handleImportAction(testDef, testId, op);
    }

    private void handleImportAction(File testDef, String testId, TestScript.SetupActionOperationComponent op) {
        if (!op.hasModifierExtension())
            return;
        for (Extension importExtension : op.getModifierExtension()) {
            if (!importExtension.getUrl().equals("https://github.com/usnistgov/asbestos/wiki/TestScript-Import"))
                continue;
            for (Extension componentExtension : importExtension.getExtension()) {
                if (componentExtension.getUrl().equals("component")) {
                    String relativePath = componentExtension.getValue().toString();
                    String componentPath = testDef.getPath() + File.separator + relativePath;
                    File componentFile = new File(componentPath);
                    TestScript componentScript = (TestScript) ProxyBase.parse(componentFile);
                    String componentId = fileName(componentFile);
                    String fullComponentId = testId + '/' + componentId;
                    componentScript.setName(fullComponentId);
                    String componentJson = ProxyBase.encode(componentScript, Format.JSON);
                    scripts.put(fullComponentId, componentJson);
                }
            }
        }
    }

    public String asJson() {  // returns object : name => TestScript
        JsonObject jsonObject = new JsonObject();

        for (String name : scripts.keySet()) {
            String reportJson = scripts.get(name);
            JsonElement ele = new Gson().fromJson(reportJson, JsonElement.class);
            jsonObject.add(name, ele);
        }
        String str = jsonObject.toString();
        return str;
    }


    private String fileName(File file) {
        String name = file.getName();
        int dot = name.indexOf(".");
        if (dot == -1)
            return name;
        return name.substring(0, dot);
    }

}
