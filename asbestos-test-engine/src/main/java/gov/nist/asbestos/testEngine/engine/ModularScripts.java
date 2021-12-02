package gov.nist.asbestos.testEngine.engine;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.testEngine.engine.translator.AsbestosComponentPath;
import gov.nist.asbestos.testEngine.engine.translator.ComponentPathValue;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.TestScript;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModularScripts {
    private static Logger log = Logger.getLogger(ModularScripts.class);
    // testId => TestScript json
    // testId/componentId => TestScript.json
    private final Map<String, String> scripts = new LinkedHashMap<>();
    private String testCollectionName;
    private EC ec;

    public ModularScripts(EC ec, String testCollectionName, File testDef) throws IOException {
        this.ec = ec;
        this.testCollectionName = testCollectionName;
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
            resource = ParserBase.parse(bytes, Format.fromContentType(testFile.getName()));
        } catch (Throwable t) {
            throw new RuntimeException("Cannot parse: " + testFile);
        }
        TestScript testScript = (TestScript) resource;
        testScript.setName(testId);

        File descriptionFile = new File(testDef, "description.md");
        if (descriptionFile.exists()) {
            InputStream ins =  null;
            try {
                ins = new FileInputStream(descriptionFile);
                String description = org.apache.commons.io.IOUtils.toString(ins, Charset.defaultCharset());
                description = description.replaceAll("\\n", "\\\\n");
                testScript.setDescription(description);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
               if (ins != null)
                   ins.close();
            }
        }

        String json = ParserBase.encode(testScript, Format.JSON);
        scripts.put(testScript.getName(), json);


        testActionsHandleImport(testDef, testId, testScript);
    }

    private List<ComponentPathValue> testActionsHandleImport(File testDef, String testId, TestScript testScript ) {
        List<ComponentPathValue> componentPathValues = new ArrayList<>();
        TestScript.TestScriptSetupComponent setup = testScript.getSetup();
        if (setup != null ) {
            for (TestScript.SetupActionComponent action : setup.getAction()) {
                List<ComponentPathValue> list = handleImportScripts(testDef, testId, action, testScript.getVariable() );
                if (list != null) {
                    componentPathValues.addAll(list);
                }
            }
        }

        for (TestScript.TestScriptTestComponent test : testScript.getTest()) {
            for (TestScript.TestActionComponent action: test.getAction()) {
                List<ComponentPathValue> list = handleImportScripts(testDef, testId, action, testScript.getVariable() );
                if (list != null) {
                    componentPathValues.addAll(list);
                }

            }
        }
        return componentPathValues;
    }

    List<ComponentPathValue> handleImportScripts(File testDef, String testId, TestScript.SetupActionComponent action, List<TestScript.TestScriptVariableComponent> variableComponentList ) {
        if (!action.hasOperation())
            return null;
        TestScript.SetupActionOperationComponent op = action.getOperation();
        return handleImportAction(testDef, testId, op, variableComponentList );
    }

    List<ComponentPathValue> handleImportScripts(File testDef, String testId, TestScript.TestActionComponent action, List<TestScript.TestScriptVariableComponent> variableComponentList) {
        if (!action.hasOperation())
            return null;
        TestScript.SetupActionOperationComponent op = action.getOperation();
        return handleImportAction(testDef, testId, op, variableComponentList);
    }

    private List<ComponentPathValue> handleImportAction(File testDef, String testId, TestScript.SetupActionOperationComponent op, List<TestScript.TestScriptVariableComponent> variableComponentList) {
        List<ComponentPathValue> componentPathValues = new ArrayList<>();
        if (!op.hasModifierExtension())
            return null;
        for (Extension importExtension : op.getModifierExtension()) {
            if (!importExtension.getUrl().equals("https://github.com/usnistgov/asbestos/wiki/TestScript-Import"))
                continue;
            for (Extension componentExtension : importExtension.getExtension()) {
                if (componentExtension.getUrl().equals("component")) {
                    if (componentExtension.hasValue()) {
                        String componentExtensionValue = componentExtension.getValue().toString();
                        ComponentPathValue componentPathValue = AsbestosComponentPath.getRelativeComponentPath(ec.getTestCollectionProperties(testCollectionName), variableComponentList, componentExtensionValue);
                        String relativePath = componentPathValue.getRelativePath();
                        if (componentPathValue.isReplaced()) {
                            componentPathValues.add(componentPathValue);
                        }
                        String componentPath = testDef.getPath() + File.separator + relativePath;
                        File componentFile = new File(componentPath);
                        TestScript componentScript = (TestScript) ParserBase.parse(componentFile);
                        String componentId = fileName(componentFile);
                        String fullComponentId = testId + '/' + componentId;
                        componentScript.setName(fullComponentId);
                        String componentJson = ParserBase.encode(componentScript, Format.JSON);
                        scripts.put(fullComponentId, componentJson);

                        /* Aggregate Test Script
                        locally rebase the testDef to component script module since its references are relative to the module path, not the root testDef
                         */
                        File rebasedTestDef = componentFile.getParentFile();
                        List<ComponentPathValue> replacedComponentPathValues = testActionsHandleImport(rebasedTestDef, testId, componentScript);
                        if (replacedComponentPathValues != null && !replacedComponentPathValues.isEmpty()) {
                            String newValue = componentJson;
                            boolean isReplaced = false;
                            for (ComponentPathValue c : replacedComponentPathValues) {
                                if (c != null && c.isReplaced()) {
                                    isReplaced = true;
                                    newValue = newValue.replaceAll(Pattern.quote(c.getToken()), Matcher.quoteReplacement(c.getRelativePath()));
                                }
                            }
                            if (isReplaced) {
                                scripts.replace(fullComponentId, newValue);
                            }
                        }

                    } else {
                        log.error(String.format("%s component value does not exist.", testDef.toString()));
                    }
                }
            }
        }
        return componentPathValues;
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
