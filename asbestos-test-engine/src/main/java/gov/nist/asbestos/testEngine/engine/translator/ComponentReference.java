package gov.nist.asbestos.testEngine.engine.translator;

import gov.nist.asbestos.client.Base.ProxyBase;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.TestScript;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ComponentReference {
    private String relativePath = null;
    private File componentRef = null;
    private TestScript component = null;
    private List<Parameter> in = new ArrayList<>();
    private List<Parameter> out = new ArrayList<>();

    public ComponentReference(File testDef, List<Extension> importDeclaration)  {
        for (Extension imd : importDeclaration) {
            for (Extension e : imd.getExtension()) {
                String url = e.getUrl();
                String value = e.getValue().toString();  // value type is Type????
                if (url.equals("component")) {
                    this.relativePath = value;
                } else if (url.equals("urn:fixture-in")) {
                    Parameter p = new Parameter();
                    p.setCallerName(value);
                    in.add(p);
                } else if (url.equals("urn:fixture-out")) {
                    Parameter p = new Parameter();
                    p.setCallerName(value);
                    out.add(p);
                } else {
                    throw new RuntimeException("Do not understand extension " + url + " in " + testDef);
                }
            }
        }
        if (this.relativePath == null)
            throw new RuntimeException("Import does not declare a component reference");
        this.componentRef = new File(testDef, relativePath);
        if (!this.componentRef.exists())
            throw new RuntimeException("Component reference " + this.componentRef + " does not exist");
    }

    public List<Parameter> getIn() {
        return in;
    }

    public List<Parameter> getOut() {
        return out;
    }

    public TestScript getComponent() {
        if (component == null) {
            component = (TestScript) ProxyBase.parse(componentRef);
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
        Extension paramsExtension = getComponent().getExtensionByUrl("urn:component-parameters");
        for (Extension e : paramsExtension.getExtension()) {
            String url = e.getUrl();
            String value = e.getValue().toString();
            if (url.equals("urn:fixture-in")) {
                if (inI < in.size()) {
                    in.get(inI).setLocalName(value);
                    inI++;
                } else {
                    throw new RuntimeException("Component " + relativePath + " was not called with a " + inI + "th in parameter");
                }
            } else if (url.equals("urn:fixture-out")) {
                if (outI < out.size()) {
                    out.get(outI).setLocalName(value);
                    outI++;
                } else {
                    throw new RuntimeException("Component " + relativePath + " was not called with a " + outI + "th out parameter");
                }
            }
        }
    }
}
