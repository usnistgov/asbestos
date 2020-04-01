package gov.nist.asbestos.testEngine.engine.translator;

import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.TestScript;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ComponentDefinition {
    private List<Parameter> fixturesIn = new ArrayList<>();
    private List<Parameter> fixturesOut = new ArrayList<>();
    private List<Parameter> variablesIn = new ArrayList<>();
    private List<Parameter> variablesOut = new ArrayList<>();

    private File testScriptFile;

    public ComponentDefinition(File testScriptFile, TestScript module) {
        this.testScriptFile = testScriptFile;
        List<Extension> moduleDefinition = null;
        List<Extension> allExtensions = module.getModifierExtension();
        for (Extension e : allExtensions) {
            if (e.hasUrl() && e.getUrl().equals("urn:module")) {
                moduleDefinition = e.getExtension();
                break;
            }
        }
        if (moduleDefinition == null)
            throw new RuntimeException("TestScript module " + testScriptFile + " has no module header");

        for (Extension e : moduleDefinition) {
            String url = e.getUrl();
            String value = e.getValue().toString();  // value type is Type????
            if (url.equals("urn:fixture-in")) {
                Parameter p = new Parameter();
                p.setLocalName(value);
                fixturesIn.add(p);
            } else if (url.equals("urn:fixture-out")) {
                Parameter p = new Parameter();
                p.setLocalName(value);
                fixturesOut.add(p);
            } else if (url.equals("urn:variable-in")) {
                Parameter p = new Parameter().setVariable(true);
                p.setLocalName(value);
                variablesIn.add(p);
            } else if (url.equals("urn:variable-out")) {
                Parameter p = new Parameter().setVariable(true);
                p.setLocalName(value);
                variablesOut.add(p);
            } else {
                throw new RuntimeException("Do not understand extension " + url + " in ComponentDefinition " + testScriptFile);
            }
        }
    }

    public void loadTranslation(ComponentReference componentReference) {
        if (fixturesIn.size() !=  componentReference.getFixturesIn().size())
            throw new RuntimeException("Calling module " + testScriptFile +
                    " - call has " + componentReference.getFixturesIn().size() +
                    " inbound parameters but module definition declares " +
                    fixturesIn.size());

        for (int i = 0; i< fixturesIn.size(); i++) {
            componentReference.getFixturesIn().get(i).setLocalName(fixturesIn.get(i).getLocalName());
        }

        if (variablesIn.size() !=  componentReference.getVariablesIn().size())
            throw new RuntimeException("Calling module " + testScriptFile +
                    " - call has " + componentReference.getVariablesIn().size() +
                    " inbound variables but module definition declares " +
                    variablesIn.size());

        for (int i = 0; i< variablesIn.size(); i++) {
            componentReference.getVariablesIn().get(i).setLocalName(variablesIn.get(i).getLocalName());
        }

        if (fixturesOut.size() !=  componentReference.getFixturesOut().size())
            throw new RuntimeException("Calling module " + testScriptFile +
                    " - call has " + componentReference.getFixturesOut().size() +
                    " outbound parameters but module definition declares " +
                    fixturesOut.size());

        for (int i = 0; i< fixturesOut.size(); i++) {
            componentReference.getFixturesOut().get(i).setLocalName(fixturesOut.get(i).getLocalName());
        }

        if (variablesOut.size() !=  componentReference.getVariablesOut().size())
            throw new RuntimeException("Calling module " + testScriptFile +
                    " - call has " + componentReference.getVariablesOut().size() +
                    " outbound variables but module definition declares " +
                    variablesOut.size());

        for (int i = 0; i< variablesOut.size(); i++) {
            componentReference.getVariablesOut().get(i).setLocalName(variablesOut.get(i).getLocalName());
        }

    }

    public List<Parameter> getFixturesIn() {
        return fixturesIn;
    }

    public List<Parameter> getFixturesOut() {
        return fixturesOut;
    }

    public String getInnerName(int i) {
        if (i >= fixturesIn.size())
            return null;
        return fixturesIn.get(i).getLocalName();
    }
}
