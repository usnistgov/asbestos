package gov.nist.asbestos.testEngine.engine;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.testEngine.engine.translator.AsbestosComponentPath;
import gov.nist.asbestos.testEngine.engine.translator.ComponentPathValue;
import gov.nist.asbestos.testEngine.engine.translator.ComponentReference;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.TestScript;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModularScripts {
    private static Logger log = Logger.getLogger(ModularScripts.class.getName());
    // testId => TestScript json
    // testId/componentId => TestScript.json
    private final Map<String, String> scripts = new LinkedHashMap<>();
    private Map<String, TestScript> testScriptMap = null;
    /*
    Multi use is where a module is reused multiple times within the scope of the main TestScript
    See minimalmetadataonly module and comprehensiveonly module to see how subject, checksubject modules are used.
    operationId => MultiUseScriptId
     */
    private final Map<String, MultiUseScriptId> multiUseScriptOperationIdentifiers = new LinkedHashMap<>();
    private final Set<String> scriptKeySet = new LinkedHashSet<>();
    private String testCollectionName;
    private EC ec;

    public ModularScripts(EC ec, String testCollectionName, File testDef, ModularScriptRunMode runMode) throws IOException {
        if (runMode == ModularScriptRunMode.BACK_END_TEST_RUNNER)
            testScriptMap = new LinkedHashMap<>();
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
            InputStream ins = null;
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
        if (testScriptMap != null)
            testScriptMap.put(testScript.getName(), testScript);
        testActionsHandleImport(testDef, testId, testScript);
        /*
        "operation": {
        "id": "dfaaa2ef-c10a-44ac-b972-32f5032aad59",
        "modifierExtension": [ {
          "url": "https://github.com/usnistgov/asbestos/wiki/TestScript-Import",
          "modifierExtension": [ {
            "url": "component",
            "valueString": "DocumentAddendum/InternalFtkRequest_asbtsFiber1.xml"
          }, {
            "url": "urn:variable-in",
            "valueString": "'ftkLoadFixture'"
          },
         */
        allocateMultiUseScript(scripts, "(\"operation\".*\\s+\"id\": \"%s\",\\s+.*\\s+.*\\s+.*\\s+.*\\s+\"valueString\": \".*)(%s.xml\"$)", "$1%s.xml\"");
        /*
         <operation id="dfaaa2ef-c10a-44ac-b972-32f5032aad59">
            <modifierExtension url="https://github.com/usnistgov/asbestos/wiki/TestScript-Import">
               <extension url="component">
                  <valueString value="InternalFtkRequest.xml" />
         */
//        allocateMultiUseScript(xmlMap, "(<operation\\s+id=\"%s\">\\s+.*\\s+.*\\s+<valueString\\s+value=\".*)(%s.xml\"\\s+\\/>$)", "$1%s.xml\" \\/>");
        return;
    }



    private void allocateMultiUseScript(Map<String, String> scriptMap, String patternFormatString, String replacerFormatString) {
        if (multiUseScriptOperationIdentifiers.size() > 0) {
            for (String opId : multiUseScriptOperationIdentifiers.keySet()) {
                for (String key : scriptMap.keySet()) {
                    String script = scriptMap.get(key);
                    if (script.contains(opId)) {
                        MultiUseScriptId multiUse = multiUseScriptOperationIdentifiers.get(opId);
//                       Pattern p = Pattern.compile(String.format("(^.*/)(%s.xml\"$)",multiUse.getSourceComponentIdPart()) ,Pattern.MULTILINE);
//                       Example: "line1\nline2\nvalueString": "../CheckSubject.xml"\nline\n
                        Pattern p = Pattern.compile(String.format(patternFormatString,
                                opId,
                                multiUse.getSourceComponentIdPart()),
                                Pattern.MULTILINE);
                        Matcher m = p.matcher(script);
                        if (m.find()) {
                            // Now update the TestScript JSON for Vue
                            scriptMap.replace(key, m.replaceFirst(String.format(replacerFormatString, multiUse.getNewScriptId())));
                            // This is needed to overcome hapi fhir bug unmarshalling serialized bytes into object, there is a loss of modifierExtension detail in various places
                            // Now update the TestScript object for test running purpose
                            if (testScriptMap != null) {
                                TestScript componentScript = testScriptMap.get(key);
                                boolean testScriptUpdated = false;
                                TestScript.TestScriptSetupComponent setup = componentScript.getSetup();
                                if (setup != null && setup.hasAction()) {
                                    for (TestScript.SetupActionComponent action : setup.getAction()) {
                                        if (action.hasOperation()) {
                                            TestScript.SetupActionOperationComponent op = action.getOperation();
                                            if (op.getId().equals(opId)) {
                                                testScriptUpdated = updateComponentPath(op, null, multiUse.getNewScriptId());
                                                if (testScriptUpdated) {
                                                    break;
                                                } else
                                                    log.warning("TestScript Setup action operation id in : " + multiUse.getNewScriptId() + " in " + key + " could not be replaced.");
                                            }
                                        }
                                    }
                                }
                                if (!testScriptUpdated) {
                                    for (TestScript.TestScriptTestComponent test : componentScript.getTest()) {
                                        for (TestScript.TestActionComponent action : test.getAction()) {
                                            if (action.hasOperation()) {
                                                TestScript.SetupActionOperationComponent op = action.getOperation();
                                                if (op.getId().equals(opId)) {
                                                    if (updateComponentPath(op, null, multiUse.getNewScriptId())) {
                                                        break;
                                                    } else {
                                                        log.warning("TestScript Test action operation id: " + multiUse.getNewScriptId() + " in " + key + " could not be replaced.");
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Resolves straightforward multiple imports. Example: TestScript imports TestScript2 many times. First import is OrigName. Second import is also OrigName but this method resolves the asbtsFiber call sequence.
     * Assumes the order OrigName, Name1, Name..n, where Name is moduleName. OrigName is used for the first module call. Name..n is used for subsequent call.
     *
     * @param simpleModuleName
     * @return
     */
    String getMultiUseScriptId(String simpleModuleName) {

        String theScriptKey = null;
        boolean asbtsFiber = false;
        for (String scriptKey : scripts.keySet()) {
            boolean isMatch = (asbtsFiber) ? scriptKey.contains(String.format("/%s_asbtsFiber", simpleModuleName)) : scriptKey.endsWith(String.format("/%s", simpleModuleName));
            if (isMatch) {
                if (scriptKeySet.contains(scriptKey)) {
                    asbtsFiber = true;
                    continue;
                } else {
                    theScriptKey = scriptKey;
                    scriptKeySet.add(scriptKey);
                    break;
                }
            }
        }
        return theScriptKey == null ? simpleModuleName : MultiUseScriptId.getComponentPart(theScriptKey);
    }


    private List<ComponentPathValue> testActionsHandleImport(File testDef, String testId, TestScript testScript) throws IOException {
        List<ComponentPathValue> componentPathValues = new ArrayList<>();
        TestScript.TestScriptSetupComponent setup = testScript.getSetup();
        if (setup != null && setup.hasAction()) {
            for (TestScript.SetupActionComponent action : setup.getAction()) {
                List<ComponentPathValue> list = handleImportScripts(testDef, testId, action, testScript.getVariable());
                if (list != null) {
                    componentPathValues.addAll(list);
                }
            }
        }

        for (TestScript.TestScriptTestComponent test : testScript.getTest()) {
            for (TestScript.TestActionComponent action : test.getAction()) {
                List<ComponentPathValue> list = handleImportScripts(testDef, testId, action, testScript.getVariable());
                if (list != null) {
                    componentPathValues.addAll(list);
                }

            }
        }
        return componentPathValues;
    }

    List<ComponentPathValue> handleImportScripts(File testDef, String testId, TestScript.SetupActionComponent action, List<TestScript.TestScriptVariableComponent> variableComponentList) throws IOException {
        if (!action.hasOperation())
            return null;
        TestScript.SetupActionOperationComponent op = action.getOperation();
        if (!op.hasId()) {
            setOperationId(op);
        }
        return handleImportAction(testDef, testId, op, variableComponentList);
    }

    private void setOperationId(TestScript.SetupActionOperationComponent op) {
        log.fine("Assigning operation ID to backtrack and replace component references to make them unique.");
        op.setId(UUID.randomUUID().toString());
    }

    List<ComponentPathValue> handleImportScripts(File testDef, String testId, TestScript.TestActionComponent action, List<TestScript.TestScriptVariableComponent> variableComponentList) throws IOException {
        if (!action.hasOperation())
            return null;
        TestScript.SetupActionOperationComponent op = action.getOperation();
        setOperationId(op);
        return handleImportAction(testDef, testId, op, variableComponentList);
    }

    private List<ComponentPathValue> handleImportAction(File testDef, String testId, TestScript.SetupActionOperationComponent op, List<TestScript.TestScriptVariableComponent> variableComponentList) throws IOException {
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
                        String newFullComponentId = ComponentReference.assignModuleId(scripts.keySet(), fullComponentId);
                        componentScript.setName(fullComponentId);

                        /* Aggregate Test Script
                        locally rebase the testDef to component script module since its references are relative to the module path, not the root testDef
                         */
                        File rebasedTestDef = componentFile.getParentFile();
                        List<ComponentPathValue> replacedComponentPathValues = testActionsHandleImport(rebasedTestDef, testId, componentScript);


                        String componentJson = ParserBase.encode(componentScript, Format.JSON);
                        if (!fullComponentId.equals(newFullComponentId)) {
                            MultiUseScriptId multiUseScriptAllocator = new MultiUseScriptId(fullComponentId, newFullComponentId);
                            multiUseScriptOperationIdentifiers.put(op.getId(), multiUseScriptAllocator);

//                          log.fine("ModularScripts Pattern match failed for " + fullComponentId + " in " + componentFile.toString());
                        }
                        scripts.put(newFullComponentId, componentJson);
                        if (testScriptMap != null)
                            testScriptMap.put(newFullComponentId, componentScript);


                        updateComponentPath(newFullComponentId, replacedComponentPathValues);

                    } else {
                        log.severe(String.format("%s component value does not exist.", testDef.toString()));
                    }
                }
            }
        }
        return componentPathValues;
    }

    private void updateComponentPath(String newFullComponentId, List<ComponentPathValue> replacedComponentPathValues) {
        String componentScript = scripts.get(newFullComponentId);
        if (replacedComponentPathValues != null && !replacedComponentPathValues.isEmpty()) {
            String newValue = componentScript;
            boolean isReplaced = false;
            for (ComponentPathValue c : replacedComponentPathValues) {
                if (c != null && c.isReplaced()) {
                    isReplaced = true;
                    log.fine(() -> "ModularScripts component path traversal before normalization: " + c.getRelativePath());
                    String normalizedPathString = Paths.get(c.getRelativePath()).normalize().toString().replace("\\", "/");
                    newValue = newValue.replaceAll(Pattern.quote(c.getToken()),Matcher.quoteReplacement(normalizedPathString));
                    // Update the TestScript obj by iterating Setup and Test objects, replace the value where modifierExtension of action.operation of both types (Setup,Test) is 'component'
                    updateTestScriptComponentPathValue(newFullComponentId, c.getToken(), normalizedPathString);
                }
            }
            if (isReplaced) {
                scripts.replace(newFullComponentId, newValue);
            }
        }
    }

    private void updateTestScriptComponentPathValue(String newFullComponentId, String toBeReplacedString, String replacementString) {
        if (testScriptMap == null)
            return;

        TestScript componentScript = testScriptMap.get(newFullComponentId);

        TestScript.TestScriptSetupComponent setup = componentScript.getSetup();
        if (setup != null && setup.hasAction()) {
            for (TestScript.SetupActionComponent action : setup.getAction()) {
                if (action.hasOperation()) {
                    TestScript.SetupActionOperationComponent op = action.getOperation();
                    updateComponentPath(op, toBeReplacedString, replacementString);
                }
            }
        }

        for (TestScript.TestScriptTestComponent test : componentScript.getTest()) {
            for (TestScript.TestActionComponent action : test.getAction()) {
                if (action.hasOperation()) {
                    TestScript.SetupActionOperationComponent op = action.getOperation();
                    updateComponentPath(op, toBeReplacedString, replacementString);
                }
            }
        }
    }

    /**
     *
     * @param op
     * @param toBeReplacedString If Null, simply replace the value, otherwise this guards the replace method.
     * @param replacementString
     * @return
     */
    private boolean updateComponentPath(TestScript.SetupActionOperationComponent op, String toBeReplacedString, String replacementString) {
        if (!op.hasModifierExtension())
            return false;
        for (Extension mExtension : op.getModifierExtension()) {
            if (!mExtension.getUrl().equals("https://github.com/usnistgov/asbestos/wiki/TestScript-Import"))
                continue;
            for (Extension componentExtension : mExtension.getExtension()) {
                if (componentExtension.getUrl().equals("component")) {
                    if (componentExtension.hasValue()) {
                        if (toBeReplacedString == null || toBeReplacedString.equals(componentExtension.getValue().toString())) {
                            componentExtension.setValue(new StringType(replacementString));
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public String asJson () {  // returns object : name => TestScript
            JsonObject jsonObject = new JsonObject();

            for (String name : scripts.keySet()) {
                String reportJson = scripts.get(name);
                JsonElement ele = new Gson().fromJson(reportJson, JsonElement.class);
                jsonObject.add(name, ele);
            }
            String str = jsonObject.toString();
            return str;
        }


        private String fileName (File file){
            String name = file.getName();
            int dot = name.indexOf(".");
            if (dot == -1)
                return name;
            return name.substring(0, dot);
        }

        Map<String, TestScript> getTestScriptMap () {
            return testScriptMap;
        }
    }

