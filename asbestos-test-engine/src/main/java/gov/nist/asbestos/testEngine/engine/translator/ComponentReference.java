package gov.nist.asbestos.testEngine.engine.translator;

import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.testEngine.engine.ExtensionDef;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.TestScript;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class ComponentReference {
    private String relativePath = null;
    private File componentRef = null;
    private TestScript component = null;
    private List<Parameter> fixturesIn = new ArrayList<>();
    private List<Parameter> fixturesOut = new ArrayList<>();
    private List<Parameter> variablesIn = new ArrayList<>();
    private List<Parameter> variablesInNoTranslation = new ArrayList<>();
    private List<Parameter> variablesOut = new ArrayList<>();

    public ComponentReference(Properties propertiesMap, List<TestScript.TestScriptVariableComponent> variableComponentList, File testDef, List<Extension> importDeclaration)  {
        for (Extension imd : importDeclaration) {
            for (Extension e : imd.getExtension()) {
                String url = e.getUrl();
                String value = e.getValue().toString();  // value type is Type????
                if (url.equals(ExtensionDef.component)) {
                    ComponentPathValue componentPathValue = AsbestosComponentPath.getRelativeComponentPath(propertiesMap, variableComponentList, value);
                    this.relativePath = componentPathValue.getRelativePath();
                } else if (url.equals(ExtensionDef.fixtureIn)) {
                    Parameter p = new Parameter();
                    p.setCallerName(value);
                    fixturesIn.add(p);
                } else if (url.equals(ExtensionDef.fixtureOut)) {
                    Parameter p = new Parameter();
                    p.setCallerName(value);
                    fixturesOut.add(p);
                } else if (url.equals(ExtensionDef.variableIn)) {
                    Parameter p = new Parameter();
                    p.setCallerName(AsbestosPropertyReference.getValue(propertiesMap, value));
                    variablesIn.add(p);
                } else if (url.equals(ExtensionDef.variableInNoTranslation)) {
                    Parameter p = new Parameter().setVariable(true);
                    p.setLocalName(value);
                    p.setCallerName(value);
                    variablesInNoTranslation.add(p);
                } else if (url.equals(ExtensionDef.variableOut)) {
                    Parameter p = new Parameter();
                    p.setCallerName(value);
                    variablesOut.add(p);
                } else {
                    throw new RuntimeException("Do not understand extension " + url + " in ComponentReference " + testDef);
                }
            }
        }
        if (this.relativePath == null)
            throw new RuntimeException("Import does not declare a component reference");
        this.componentRef = new File(testDef, relativePath);
        if (!this.componentRef.exists())
            throw new RuntimeException("Modular TestScript error: Component reference " + this.componentRef + " does not exist");
    }

    @Override
    public String toString() {
        return relativePath;
    }

    public List<Parameter> getFixturesIn() {
        return fixturesIn;
    }

    public List<Parameter> getFixturesOut() {
        return fixturesOut;
    }

    public List<Parameter> getVariablesIn() {
        return variablesIn;
    }

    public List<Parameter> getVariablesOut() {
        return variablesOut;
    }

    public TestScript getComponent() {
        if (component == null) {
            component = (TestScript) ParserBase.parse(componentRef);
        }
        return component;
    }

    public File getComponentRef() {
        return componentRef;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void loadComponentHeader() {
        int inI = 0;
        int outI = 0;
        Extension paramsExtension = getComponent().getExtensionByUrl(ExtensionDef.componentParameters);
        for (Extension e : paramsExtension.getExtension()) {
            String url = e.getUrl();
            String value = e.getValue().toString();
            if (url.equals(ExtensionDef.fixtureIn)) {
                if (inI < fixturesIn.size()) {
                    fixturesIn.get(inI).setLocalName(value);
                    inI++;
                } else {
                    throw new RuntimeException("Component " + relativePath + " was not called with a " + inI + "th in parameter");
                }
            } else if (url.equals(ExtensionDef.fixtureOut)) {
                if (outI < fixturesOut.size()) {
                    fixturesOut.get(outI).setLocalName(value);
                    outI++;
                } else {
                    throw new RuntimeException("Component " + relativePath + " was not called with a " + outI + "th out parameter");
                }
            }
        }
    }

    public List<Parameter> getVariablesInNoTranslation() {
        return variablesInNoTranslation;
    }

    /**
     *
     *  Assign moduleName and moduleId in caller's TestReport.
     *  moduleName is the name of the module.
     *  moduleId is the same name with a numeric suffix to make this call unique.
     *  The module call is identified as moduleName/moduleId in the logs
     *
     * @param candidate
     * @return
     */
    public static String assignModuleId(Set<String> moduleIds, String candidate) {
        String real = candidate;
        int i = 1;
        while (moduleIds.contains(real)) {
            real = candidate + "_asbtsFiber" + i;
            i++;
        }
        return real;
    }

}
