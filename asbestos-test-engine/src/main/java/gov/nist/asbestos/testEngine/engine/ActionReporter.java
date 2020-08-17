package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.events.UIEvent;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureComponent;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureMgr;
import org.apache.log4j.Logger;
import org.hl7.fhir.r4.model.TestScript;

import java.util.*;

class ActionReporter {
    private static Logger log = Logger.getLogger(ActionReporter.class);

    private String testCollectionId = null;
    private String testId = null;
    private TestEngine testEngine = null;
    private FixtureComponent assertionSource = null;
    private ActionReference actionReference;

    ActionReporter(ActionReference actionReference) {
        this.actionReference = actionReference;
    }

    void reportOperation(ResourceWrapper wrapper, FixtureMgr fixtureMgr, VariableMgr variableMgr, Reporter reporter, TestScript.SetupActionOperationComponent op) {
        String request = wrapper == null ? "" : "### " + wrapper.getHttpBase().getVerb() + " " + wrapper.getHttpBase().getUri() + "\n";

        report(wrapper, fixtureMgr, variableMgr, reporter, request, op);
    }

    void reportAssertion(FixtureMgr fixtureMgr, VariableMgr variableMgr, Reporter reporter, FixtureComponent assertionSource) {
        this.assertionSource = assertionSource;
        String request = "";

        report(null, fixtureMgr, variableMgr, reporter, request, null);
    }

    /**
     *
     * @param wrapper - for link to inspector
     * @param fixtureMgr
     * @param variableMgr
     * @param reporter
     * @param request - for logging request signature
     * @param op - script operation being reported on
     */
    private void report(ResourceWrapper wrapper, FixtureMgr fixtureMgr, VariableMgr variableMgr, Reporter reporter, String request, TestScript.SetupActionOperationComponent op) {
        Objects.requireNonNull(testCollectionId);
        Objects.requireNonNull(testId);
        Objects.requireNonNull(testEngine);
        Map<String, String> fixtures = new HashMap<>();

        // report assertion source (only for assertions)
        if (assertionSource != null) {
            HttpBase httpBase = assertionSource.getHttpBase();
            if (httpBase != null ) {
                Headers responseHeaders = httpBase.getResponseHeaders();
                String eventUrl = responseHeaders.getProxyEvent();
                if (eventUrl != null) {
                    String value = EventLinkToUILink.get(eventUrl);
                    try {
                        Ref ref = new Ref(value);
                        if (ref.isAbsolute()) {
                            value = "<a href=\"" + value + "/resp" + "\"" + " target=\"_blank\">" + "Open in Inspector" + "</a>";
                        }
                    } catch (Throwable t) {
                        // ignore
                    }
                    fixtures.put("lastOperation", value);
                }
            }
        }

        for (String key : fixtureMgr.keySet()) {

            // Establish additional labels (keys) for this fixture.
            // If it was referenced as either sourceId or responseId for this action
            // it should be displayed that way.
            Set<String> additionalKey = new HashSet<>();
            boolean sourceId = false;
            boolean responseId = false;
            String reference = null;
            String label = null;
            if (key != null && assertionSource != null && key.equals(assertionSource.getId())) {
                sourceId = true;
                label = "sourceId (" + key + ")";
            }
            if (key != null && op != null && op.hasResponseId() && key.equals(op.getResponseId())) {
                responseId = true;
                label = "responseId (" + key + ")";
            }
            if (key != null && op != null && op.hasSourceId() && key.equals(op.getSourceId())) {
                sourceId = true;
                label = "sourceId (" + key + ")";
            }
            String tail = "";
            if (sourceId)
                tail = "/req";
            else if (responseId)
                tail = "/resp";
            else if (key != null && key.equals("lastOperation")) {
                tail = "/resp";
                label = key;
            }

            if (label == null) {
                label = key;
                if (key.equals("request"))
                    tail = "/req";
                if (key.equals("response"))
                    tail = "/resp";
            }

            FixtureComponent fixtureComponent = fixtureMgr.get(key);

            HttpBase httpBase = fixtureComponent.getHttpBase();  // http operation of fixtureComponent.wrapper
            ResourceWrapper wrapper1 = fixtureComponent.getResourceWrapper();
            String refStrRaw = null;
            if (httpBase != null) {  // fixtureComponent created by operation
                Headers responseHeaders = httpBase.getResponseHeaders();
                String eventUrl = responseHeaders.getProxyEvent();
                if (eventUrl != null) {
                    refStrRaw = EventLinkToUILink.get(eventUrl, tail);
                    reference = "<a href=\"" +  refStrRaw + "\"" + " target=\"_blank\">" +
                            ((label == null) ? refStrRaw : "Open in Inspector") +
                            "</a>";
                }
            } else if (wrapper1 != null) {   // static fixtureComponent
                Ref ref = wrapper1.getRef();
                if (ref != null) {
                    String base = ServiceProperties.getInstance().getPropertyOrStop(ServicePropertiesEnum.FHIR_TOOLKIT_UI_HOME_PAGE);
                    String server = ServiceProperties.getInstance().getPropertyOrStop(ServicePropertiesEnum.FHIR_TOOLKIT_BASE);
                    UIEvent uiEvent = fixtureComponent.getCreatedByUIEvent();
                    if (uiEvent == null) {
                        // Use Cases
                        // 1. Cached server object (Patient, Binary), url (dataObject) has form
                        // http://localhost:8081/asbestos/static/staticResource/MHD_DocumentRecipient_minimal/Single_Document_without_Binary/Bundle/doc1.json
                        // 2. Fixture Resource from within a Bundle (PDB), url (dataObject) has form
                        // http://localhost:8081/asbestos/engine/staticResource/MHD_DocumentRecipient_minimal/Single_Document_without_Binary/DocumentReference?dataObject%3DBundle%2Fpdb.xml%3BfhirPath%3DBundle.entry%5B0%5D
                        // This has dataObject param to guide extraction from Bundle so the generated url
                        // (inspectUrl) has two nested params:
                        // The inner references the server and has param to address content within Bundle
                        // The outer references the UI and has inner as its reference to the content.
                        // So there are two reasons for params: server reference and UI display page reference.

                        // if ref contains a query, it was generated by FixtureComponent#generateStaticResourceRef
                        // url points to static resource (fixture) on server
                        // refStrRaw points to UI page to display it
                        String url = ref.isQuery()
                                ? ref.urlEncode()
                                : new Ref(server + "/static/staticResource/"
                                + testCollectionId
                                + "/" + testId
                                + "/" + ref.asString()).urlEncode();
                        refStrRaw = base
                                + "/inspectUrl?dataObject=" + url;
                    } else {
                        refStrRaw = base + "/session/" + testEngine.getTestSession()
                                + "/channel/" + testEngine.getChannelName()
                                + "/lognav/" + uiEvent.getEventName()
                        + tail;
                    }

                    log.info("Fixture Reference: " + label + " => " + refStrRaw);
                    // referenced to UIEvent for display in inspector
                    reference = "<a href=\"" +  refStrRaw + "\"" + " target=\"_blank\">" +
                            ((label == null) ? refStrRaw : "Open in Inspector") +
                            "</a>";
                }
            }
            if (refStrRaw != null && label != null)
                fixtures.put(label, reference);
        }

        Map<String, String> variables = variableMgr.getVariables(true);

        String path = "## Test\n"
                //+ actionReference.toString()
                + testEngine.getTestEnginePath();

        String markdown = path + "\n" +
                "## Action\n" + request
                + asMarkdown(fixtures, "Fixtures")
                + "\n"
                + asMarkdown(variables, "Variables (evaluated after action)");

        reporter.report(markdown, wrapper);
    }


    private String asMarkdown(Map<String, String> table, String title) {
        StringBuilder buf = new StringBuilder();
        buf.append("### ").append(title).append("\n");
        boolean first = true;
        for (String key : table.keySet()) {
            if (!first)
                buf.append("\n");
            first = false;
            String value = table.get(key);
            buf.append("**").append(key).append("**: ").append(value);
        }
        return buf.toString();
    }

    public ActionReporter setTestCollectionId(String testCollectionId) {
        this.testCollectionId = testCollectionId;
        return this;
    }

    public ActionReporter setTestId(String testId) {
        this.testId = testId;
        return this;
    }

    public ActionReporter setTestEngine(TestEngine testEngine) {
        this.testEngine = testEngine;
        return this;
    }
}
