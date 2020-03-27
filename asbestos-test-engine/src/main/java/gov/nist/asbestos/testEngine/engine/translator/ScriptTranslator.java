package gov.nist.asbestos.testEngine.engine.translator;

import gov.nist.asbestos.client.Base.ProxyBase;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.TestScript;
import org.jaxen.util.SingletonList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ScriptTranslator {
    private File testDef;
    private TestScript script;
    private List<String> errors = new ArrayList<>();


    public ScriptTranslator(File testDef) {
        this.testDef = testDef;
        this.script = (TestScript) ProxyBase.parse(new File(testDef, "script.xml"));
    }

    // Compiles script and returns resulting TestScript
    public TestScript run() {
        TestScript result = script;
        // scan all tests for imports and load them
        if (script.hasTest()) {
            for (TestScript.TestScriptTestComponent test : script.getTest()) {
                if (test.hasModifierExtension()) {
                    ComponentReferences refs = extractComponentReferences(test);
                    if (validate(refs))
                        resolve(refs, test);
                }
            }
        }
        if (script.hasSetup()) {
            TestScript.TestScriptSetupComponent setup = script.getSetup();
            if (setup.hasModifierExtension()) {
                ComponentReferences refs = extractComponentReferences(setup);
                if (validate(refs))
                    resolve(refs, setup);
            }
        }

        return result;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<String> getErrors() {
        return errors;
    }

    ComponentReferences extractComponentReferences(TestScript.TestScriptTestComponent test) {
        List<ComponentReference> rawRefs = new ArrayList<>();
        for (Extension e : test.getModifierExtension()) {
            if (e.getUrl().equals("urn:import")) {
                ComponentReference ref = new ComponentReference(testDef, new SingletonList(e));
                ref.loadComponentHeader();
                rawRefs.add(ref);
            }
        }
        return new ComponentReferences(rawRefs);
    }

    ComponentReferences extractComponentReferences(TestScript.TestScriptSetupComponent setup) {
        List<ComponentReference> rawRefs = new ArrayList<>();
        for (Extension e : setup.getModifierExtension()) {
            if (e.getUrl().equals("urn:import")) {
                ComponentReference ref = new ComponentReference(testDef, new SingletonList(e));
                ref.loadComponentHeader();
                rawRefs.add(ref);
            }
        }
        return new ComponentReferences(rawRefs);
    }

    // all references contain one or more Test elements
    private boolean validate(ComponentReferences refs) {
        for (ComponentReference ref : refs.getReferences()) {
            TestScript component = ref.getComponent();
//            if (component.getTest().size() == 0) {
//                error("No tests defined in component " + ref.getRelativePath());
//            }
            if (component.hasFixture()) {
                error("Fixtures not allowed in component: " + ref.getRelativePath());
            }
//            if (component.hasSetup()) {
//                error("Setup not allowed in component: " + ref.getRelativePath());
//            }
            if (component.hasTeardown()) {
                error("Teardown not allowed in component: " + ref.getRelativePath());
            }
            if (component.hasVariable()) {
                error("Variable not allowed in component: " + ref.getRelativePath());
            }
        }
        return errors.isEmpty();
    }

    private void error(String msg) {
        errors.add(msg);
    }

    private void resolve(ComponentReferences refs, TestScript.TestScriptTestComponent scriptTest) {
        // within test, update
        // requestId, sourceId, targetId from parameter-in
        // responseId from parameter-out

        for (ComponentReference ref : refs.getReferences()) {
            TestScript component = ref.getComponent();
            for (TestScript.TestScriptTestComponent componentTest : component.getTest()) {
                List<TestScript.TestActionComponent> actions = updateTestComponent(componentTest, ref);
                for (TestScript.TestActionComponent action : actions) {
                    scriptTest.addAction(action);
                }
            }
        }
    }

    private void resolve(ComponentReferences refs, TestScript.TestScriptSetupComponent scriptSetup) {
        // within test, update
        // requestId, sourceId, targetId from parameter-in
        // responseId from parameter-out

        for (ComponentReference ref : refs.getReferences()) {
            TestScript component = ref.getComponent();
            TestScript.TestScriptSetupComponent componentSetup = component.getSetup();
            List<TestScript.SetupActionComponent> actions = updateSetupComponent(componentSetup, ref);
            for (TestScript.SetupActionComponent action : actions) {
                scriptSetup.addAction(action);
            }
        }

    }


    private List<TestScript.TestActionComponent> updateTestComponent(TestScript.TestScriptTestComponent componentTest, ComponentReference ref) {
        List<Parameter> inUpdates = ref.getIn();
        List<Parameter> outUpdates = ref.getOut();
        for (TestScript.TestActionComponent action : componentTest.getAction()) {
            if (action.hasOperation()) {
                TestScript.SetupActionOperationComponent op = action.getOperation();
                updateOperation(op, ref);
            }
            if (action.hasAssert()) {
                TestScript.SetupActionAssertComponent as = action.getAssert();
                updateAssert(as, ref);
            }
        }
        return componentTest.getAction();
    }

    private List<TestScript.SetupActionComponent> updateSetupComponent(TestScript.TestScriptSetupComponent componentTest, ComponentReference ref) {
        List<Parameter> inUpdates = ref.getIn();
        List<Parameter> outUpdates = ref.getOut();
        for (TestScript.SetupActionComponent action : componentTest.getAction()) {
            if (action.hasOperation()) {
                TestScript.SetupActionOperationComponent op = action.getOperation();
                updateOperation(op, ref);
            }
            if (action.hasAssert()) {
                TestScript.SetupActionAssertComponent as = action.getAssert();
                updateAssert(as, ref);
            }
        }
        return componentTest.getAction();
    }

    private void updateOperation(TestScript.SetupActionOperationComponent op, ComponentReference ref) {
        List<Parameter> inUpdates = ref.getIn();
        List<Parameter> outUpdates = ref.getOut();
        if (op.hasRequestId()) {
            Parameter p = Parameter.findByLocalName(inUpdates, op.getRequestId());
            if (p != null) op.setRequestId(p.getCallerName());
        }
        if (op.hasResponseId()) {
            Parameter p = Parameter.findByLocalName(outUpdates, op.getResponseId());
            if (p != null) op.setResponseId(p.getCallerName());
        }
        if (op.hasSourceId()) {
            Parameter p = Parameter.findByLocalName(inUpdates, op.getSourceId());
            if (p != null) op.setSourceId(p.getCallerName());
        }
        if (op.hasTargetId()) {
            Parameter p = Parameter.findByLocalName(inUpdates, op.getTargetId());
            if (p != null) op.setTargetId(p.getCallerName());
        }
    }

    private void updateAssert(TestScript.SetupActionAssertComponent as, ComponentReference ref) {
        List<Parameter> inUpdates = ref.getIn();
        List<Parameter> outUpdates = ref.getOut();
        if (as.hasSourceId()) {
            Parameter p = Parameter.findByLocalName(inUpdates, as.getSourceId());
            if (p != null) as.setSourceId(p.getCallerName());
        }
        if (as.hasValue()) {
            Parameter p = Parameter.findByLocalName(inUpdates, as.getValue());
            if (p != null) as.setValue(p.getCallerName());
        }
    }
}
