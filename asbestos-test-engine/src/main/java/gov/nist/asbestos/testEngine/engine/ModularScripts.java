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
import org.hl7.fhir.r4.model.TestScript;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    /*
    Multi use is where a module is reused multiple times within the scope of the main TestScript
    See minimalmetadataonly module and comprehensiveonly module to see how subject, checksubject modules are used.
    operationId => MultiUseScriptId
     */
    private final Map<String, MultiUseScriptId> multiUseScriptOperationIdentifiers = new LinkedHashMap<>();
    private final Set<String> scriptKeySet = new HashSet<>();
    private String testCollectionName;
    private EC ec;

    public ModularScripts(EC ec, String testCollectionName, File testDef) throws IOException, ModularScriptCircularReferenceException {
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
        testActionsHandleImport(testFile, testId, testScript);
        allocateMultiUseScript();
        return;
    }

    private void allocateMultiUseScript() {
        if (multiUseScriptOperationIdentifiers.size() > 0) {
            for (String opId : multiUseScriptOperationIdentifiers.keySet()) {
               for (String key: scripts.keySet()) {
                   String script = scripts.get(key);
                   if (script.contains(opId)) {
                       MultiUseScriptId multiUse = multiUseScriptOperationIdentifiers.get(opId);
//                       Pattern p = Pattern.compile(String.format("(^.*/)(%s.xml\"$)",multiUse.getSourceComponentIdPart()) ,Pattern.MULTILINE);// Example: "line1\nline2\nvalueString": "../CheckSubject.xml"\nline\n
                       Pattern p = Pattern.compile(String.format("(\"operation\".*\\s+\"id\": \"%s\",\\s+.*\\s+.*\\s+.*\\s+.*\\s+\"valueString\": \".*)(%s.xml\"$)" ,
                               opId,
                               multiUse.getSourceComponentIdPart()),
                               Pattern.MULTILINE);
                       Matcher m = p.matcher(script);
                       if (m.find()) {
                           scripts.replace(key, m.replaceFirst(String.format("$1%s.xml\"", multiUse.getNewScriptId())));
                       }
                   }
               }
            }
        }
    }

    /**
     * Resolves straightforward multiple imports. Example: TestScript imports TestScript2 many times. First import is OrigName. Second import is also OrigName but this method resolves the asbtsFiber call sequence.
     * Assumes the order OrigName, Name1, Name..n, where Name is moduleName. OrigName is used for the first module call. Name..n is used for subsequent call.
     * @param simpleModuleName
     * @return
     */
    String getMultiUseScriptId(String simpleModuleName) {

        String theScriptKey = null;
        boolean asbtsFiber = false;
        for ( String scriptKey : scripts.keySet()) {
            boolean isMatch = (asbtsFiber) ? scriptKey.contains(String.format("/%s_asbtsFiber", simpleModuleName)) : scriptKey.endsWith(String.format("/%s",simpleModuleName)) ;
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

    /**
     * Useful in the case where module is imported many times but only executed 1 time due to all others being conditional action executions.
     * @param moduleName
     * @param parentSimpleName
     * @return
     */
    String getMultiUseScriptIdByParent(String moduleName, String parentSimpleName) {
        if (multiUseScriptOperationIdentifiers.size() > 0) {
            for (String opId : multiUseScriptOperationIdentifiers.keySet()) {
                if (moduleName.equals(multiUseScriptOperationIdentifiers.get(opId).getSourceComponentIdPart())) {
                    // Check if same parent
                    for (String scriptKey : scripts.keySet()) {
                        if (scripts.get(scriptKey).contains(opId)) {
                           if (parentSimpleName.equals(MultiUseScriptId.getComponentPart(scriptKey))) {
                               return multiUseScriptOperationIdentifiers.get(opId).getNewComponentIdPart();
                           }
                        }
                    }
                }
            }
        }
        return moduleName;
    }


    private List<ComponentPathValue> testActionsHandleImport(File testFile, String testId, TestScript testScript ) throws IOException, ModularScriptCircularReferenceException {
        List<ComponentPathValue> componentPathValues = new ArrayList<>();
        TestScript.TestScriptSetupComponent setup = testScript.getSetup();
        if (setup != null ) {
            for (TestScript.SetupActionComponent action : setup.getAction()) {
                List<ComponentPathValue> list = handleImportScripts(testFile, testId, action, testScript.getVariable() );
                if (list != null) {
                    componentPathValues.addAll(list);
                }
            }
        }

        for (TestScript.TestScriptTestComponent test : testScript.getTest()) {
            for (TestScript.TestActionComponent action: test.getAction()) {
                List<ComponentPathValue> list = handleImportScripts(testFile, testId, action, testScript.getVariable() );
                if (list != null) {
                    componentPathValues.addAll(list);
                }

            }
        }
        return componentPathValues;
    }

    List<ComponentPathValue> handleImportScripts(File testFile, String testId, TestScript.SetupActionComponent action, List<TestScript.TestScriptVariableComponent> variableComponentList ) throws IOException, ModularScriptCircularReferenceException {
        if (!action.hasOperation())
            return null;
        TestScript.SetupActionOperationComponent op = action.getOperation();
        if (! op.hasId()) {
            setOperationId(op);
        }
        return handleImportAction(testFile, testId, op, variableComponentList );
    }

    private void setOperationId(TestScript.SetupActionOperationComponent op) {
        log.fine("Assigning operation ID to backtrack and replace component references to make them unique.");
        op.setId(UUID.randomUUID().toString());
    }

    List<ComponentPathValue> handleImportScripts(File testFile, String testId, TestScript.TestActionComponent action, List<TestScript.TestScriptVariableComponent> variableComponentList) throws IOException, ModularScriptCircularReferenceException {
        if (!action.hasOperation())
            return null;
        TestScript.SetupActionOperationComponent op = action.getOperation();
        setOperationId(op);
        return handleImportAction(testFile, testId, op, variableComponentList);
    }

    private List<ComponentPathValue> handleImportAction(File testFile, String testId, TestScript.SetupActionOperationComponent op, List<TestScript.TestScriptVariableComponent> variableComponentList) throws IOException, ModularScriptCircularReferenceException {
        File testDef = testFile.getParentFile();
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
                        if (componentFile.getCanonicalFile().equals(testFile.getCanonicalFile())) {
                            String errorStr = String.format("TestScript Import circular reference detected in %s.", testFile);
                            throw new ModularScriptCircularReferenceException(errorStr);
                        }
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
                        if (! fullComponentId.equals(newFullComponentId)) {
                            MultiUseScriptId multiUseScriptAllocator = new MultiUseScriptId(fullComponentId, newFullComponentId);
                            multiUseScriptOperationIdentifiers.put(op.getId(), multiUseScriptAllocator);

//                          log.fine("ModularScripts Pattern match failed for " + fullComponentId + " in " + componentFile.toString());
                        }
                        scripts.put(newFullComponentId, componentJson);


                        if (replacedComponentPathValues != null && !replacedComponentPathValues.isEmpty()) {
                            String newValue = componentJson;
                            boolean isReplaced = false;
                            for (ComponentPathValue c : replacedComponentPathValues) {
                                if (c != null && c.isReplaced()) {
                                    isReplaced = true;
                                    log.fine(() -> "ModularScripts component path traversal before normalization: " + c.getRelativePath());
                                    String normalizedPathString = Paths.get(c.getRelativePath()).normalize().toString().replace("\\","/");
                                    newValue = newValue.replaceAll(Pattern.quote(c.getToken()), Matcher.quoteReplacement(normalizedPathString));
                                }
                            }
                            if (isReplaced) {
                                scripts.replace(newFullComponentId, newValue);
                            }
                        }

                    } else {
                        log.severe(String.format("%s component value does not exist.", testDef.toString()));
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
