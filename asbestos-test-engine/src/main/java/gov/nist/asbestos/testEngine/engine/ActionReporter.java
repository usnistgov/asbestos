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
import org.hl7.fhir.r4.model.TestReport;
import org.hl7.fhir.r4.model.TestScript;

import java.util.*;

class ActionReporter implements TestDef {
    private static Logger log = Logger.getLogger(ActionReporter.class);

    private String testCollectionId = null;
    private String testId = null;
    private String parentTestCollectionId = null;
    private String parentTestId = null;
    private TestEngine parentTestEngine = null;
    private TestEngine testEngine = null;
    private FixtureComponent assertionSource = null;
    //private ActionReference actionReference;
    private String prefixMarkdown = "";
    private String postfixMarkdown = "";
    private boolean imAParent = false;
    private boolean isModule = false;

    ActionReporter() {

    }

    ActionReporter reportOperation(ResourceWrapper wrapper, FixtureMgr fixtureMgr, VariableMgr variableMgr,
                                   Reporter reporter, TestScript.SetupActionOperationComponent op) {
        String request = "";
        if (wrapper != null) {
            String url = wrapper.getHttpBase().getUri().toString();
            request = "### " + wrapper.getHttpBase().getVerb() + " [" + url + "](" + url + ")";
        }

        if (!variableMgr.hasReporter())
            variableMgr.setReporter(reporter);

        report(wrapper, fixtureMgr, variableMgr, reporter, request, op);
        return this;
    }

    ActionReporter reportAssertion(FixtureMgr fixtureMgr, VariableMgr variableMgr, Reporter reporter,
                                   FixtureComponent assertionSource,
                                   TestScript.SetupActionAssertComponent assertReport) {
        this.assertionSource = assertionSource;
        String request = "";

        if (!variableMgr.hasReporter())
            variableMgr.setReporter(reporter);

        report((ResourceWrapper) null, fixtureMgr, variableMgr, reporter, request, assertReport);
        return this;
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

        log.info("Report request: " + request.trim());

        // report assertion source (only for assertions)
        reportAssertionSource(fixtures);

        FixtureLabels requestLabels = null;
        FixtureLabels responseLabels = null;
        for (String key : fixtureMgr.keySet()) {
            FixtureLabels labels = new FixtureLabels(this, op, key);

            FixtureComponent fixtureComponent = fixtureMgr.get(key);
            HttpBase httpBase = fixtureComponent.getHttpBase();  // http operation of fixtureComponent.wrapper
            ResourceWrapper wrapper1 = fixtureComponent.getResourceWrapper();
            String refStrRaw = null;

            if (httpBase != null) {  // fixtureComponent created by operation
                refStrRaw = operationReport(labels, httpBase, refStrRaw);
            } else if (wrapper1 != null) {   // static fixtureComponent
                refStrRaw = staticFixtureReport(labels, fixtureComponent, wrapper1, refStrRaw);
            }
            if (refStrRaw != null && labels.label != null) {
                // referenced to UIEvent for display in inspector
                labels.rawReference = refStrRaw;
                labels.referenceLabel = "Open in Inspector";
                // log.info("Fixture Reference: " + labels.label + " => " + refStrRaw);
                fixtures.put(labels.label, labels.getReference());

                if (labels.sourceId) {
                    requestLabels = labels;
                    requestLabels.referenceLabel = "Request";
                }
                if (labels.responseId) {
                    responseLabels = labels;
                    responseLabels.referenceLabel = "Response";
                }
            }
        }

        String markdown = "## " + testNotation()
                + testEngine.getTestEnginePath() + "\n"
                + "\n"
                + (imAParent ? "" : request
                + " (  " + (requestLabels == null ? "--" : requestLabels.getReference()) + " ) => "
                + (responseLabels == null ? "--" : responseLabels.getReference())
        )
                + "\n"
                + errorDisplay(reporter.getOpReport())
                + asMarkdown(fixtures, "TestScript Fixtures")
                + "\n"
                + asMarkdown(variableMgr.getVariables(true), "TestScript Variables (evaluated after action)")
                + "\n"
                + (imAParent ? "## Call " : "")
                + "\n"
                + (isModule && !testEngine.getCallFixtureMap().isEmpty() ? asMarkdown(testEngine.getCallFixtureMap(), "Fixture Translation", "Name in caller", "Name in module") + "\n" : "")
                + (isModule && !testEngine.getCallVariableMap().isEmpty() ? asMarkdown(testEngine.getCallVariableMap(), "Variable Translation", "Name in caller", "Name in module") + "\n" : "")

                ;

        if (isModule)
            reporter.setModuleActionContext(markdown, wrapper);
        else
            reporter.setActionContext(markdown, wrapper);
    }

    private void report(ResourceWrapper wrapper, FixtureMgr fixtureMgr, VariableMgr variableMgr, Reporter reporter, String request, TestScript.SetupActionAssertComponent assrt) {
        Objects.requireNonNull(testCollectionId);
        Objects.requireNonNull(testId);
        Objects.requireNonNull(testEngine);
        Map<String, String> fixtures = new HashMap<>();

        // log.info("Report request: " + request.trim());

        // report assertion source (only for assertions)
        reportAssertionSource(fixtures);

        FixtureLabels requestLabels = null;
        FixtureLabels responseLabels = null;
        for (String key : fixtureMgr.keySet()) {
            FixtureLabels labels = new FixtureLabels(this, assrt, key);

            FixtureComponent fixtureComponent = fixtureMgr.get(key);
            HttpBase httpBase = fixtureComponent.getHttpBase();  // http operation of fixtureComponent.wrapper
            ResourceWrapper wrapper1 = fixtureComponent.getResourceWrapper();
            String refStrRaw = null;

            if (httpBase != null) {  // fixtureComponent created by operation
                refStrRaw = operationReport(labels, httpBase, refStrRaw);
            } else if (wrapper1 != null) {   // static fixtureComponent
                refStrRaw = staticFixtureReport(labels, fixtureComponent, wrapper1, refStrRaw);
            }
            if (refStrRaw != null && labels.label != null) {
                // referenced to UIEvent for display in inspector
                labels.rawReference = refStrRaw;
                labels.referenceLabel = "Open in Inspector";
                // log.info("Fixture Reference: " + labels.label + " => " + refStrRaw);
                fixtures.put(labels.label, labels.getReference());

                if (labels.sourceId) {
                    requestLabels = labels;
                    requestLabels.referenceLabel = "Request";
                }
                if (labels.responseId) {
                    responseLabels = labels;
                    responseLabels.referenceLabel = "Response";
                }
            }
        }

        String markdown = "## " + testNotation()
                + testEngine.getTestEnginePath() + "\n"
                + "\n"
                + (imAParent ? "" : request
                + callGraph(requestLabels, responseLabels)
        )
                + "\n"
                + errorDisplay(reporter.getOpReport())
                + asMarkdown(fixtures, "TestScript Fixtures")
                + "\n"
                + asMarkdown(variableMgr.getVariables(true), "TestScript Variables (evaluated after action)")
                + "\n"
                + (imAParent ? "## Call " : "")
                + "\n"
                + (isModule && !testEngine.getCallFixtureMap().isEmpty() ? asMarkdown(testEngine.getCallFixtureMap(), "Fixture Translation", "Name in caller", "Name in module") + "\n" : "")
                + (isModule && !testEngine.getCallVariableMap().isEmpty() ? asMarkdown(testEngine.getCallVariableMap(), "Variable Translation", "Name in caller", "Name in module") + "\n" : "")

                ;

        if (isModule)
            reporter.setModuleActionContext(markdown, wrapper);
        else
            reporter.setActionContext(markdown, wrapper);
    }

    String callGraph(FixtureLabels requestLabels, FixtureLabels responseLabels) {
        return "";
//        return " (  " + (requestLabels == null ? "--" : requestLabels.getReference()) + " ) => "
//                + (responseLabels == null ? "--" : responseLabels.getReference());
    }

    private String testNotation() {
        if (!isModule)
            return "Test:  ";
        return "Called Module:  ";
    }

    private String staticFixtureReport(FixtureLabels labels, FixtureComponent fixtureComponent, ResourceWrapper wrapper1, String refStrRaw) {
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
                + labels.tail;
            }
        }
        return refStrRaw;
    }

    private String operationReport(FixtureLabels labels, HttpBase httpBase, String refStrRaw) {
        Headers responseHeaders = httpBase.getResponseHeaders();
        String eventUrl = responseHeaders.getProxyEvent();
        if (eventUrl != null) {
            refStrRaw = EventLinkToUILink.get(eventUrl, labels.tail);
            labels.referenceLabel = (labels.label == null) ? refStrRaw : "Open in Inspector";
            labels.rawReference = refStrRaw;
        }
        return refStrRaw;
    }

    public String uiLinkToEvent(String eventUrl, String urlTail) {
        FixtureLabels labels = new FixtureLabels(this);
        labels.tail = urlTail;
        String refStrRaw = EventLinkToUILink.get(eventUrl, labels.tail);
        labels.referenceLabel = (labels.label == null) ? refStrRaw : "Open in Inspector";
        labels.rawReference = refStrRaw;
        return labels.getReference();
    }

    private void reportAssertionSource(Map<String, String> fixtures) {
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
    }

    private String errorDisplay(TestReport.SetupActionOperationComponent report) {
        String errors = errorsToReport(report);
        if (errors.equals(""))
            return "";
        return "## Errors\n**" + errors + "**\n";
    }

    private String errorsToReport(TestReport.SetupActionOperationComponent report) {
        if (report != null && report.hasResult() && report.getResult().equals(TestReport.TestReportActionResult.ERROR)) {
            return report.getMessage();
        }
        return "";
    }

    private String asMarkdown(Map<String, String> table, String title) {
        StringBuilder buf = new StringBuilder();
        buf.append("### ").append(title).append("\n");
        boolean first = true;
        for (String key : table.keySet()) {
            //if (!first)
                buf.append("\n");
            first = false;
            String value = table.get(key);
            buf.append("* **").append(key).append("**: ").append(value);
        }
        return buf.toString();
    }

    private String asMarkdown(Map<String, String> table, String title, String header1, String header2) {
        StringBuilder buf = new StringBuilder();

        //buf.append("<div class=\"indent2\">");
        buf.append("\n**").append(title).append("**\n");
        buf.append("<table class=\"indent2\">\n");
        buf.append("\n<tr>");
        buf.append("\n<th>").append(header1).append("</th>");
        buf.append("\n<th>").append(header2).append("</th>");
        buf.append("\n</tr>");
        for (String name : table.keySet()) {
            String value = table.get(name);
            buf.append("\n<tr>");
            buf.append("\n<td>").append(name).append("</td>");
            buf.append("\n<td>").append(value).append("</td>");
            buf.append("\n</tr>");
        }
        buf.append("\n</table>\n");
        //buf.append("</div>");

        return buf.toString();
    }

    private int columnWidth = 40;
    private String center(String in) {
        int size = in.length();
        int border = size / 2;
        return spaces(border) + border;
    }
    private String spaces(int count) {
        StringBuilder val = new StringBuilder();
        while (count > 0) {
            val.append(" ");
            count--;
        }
        return val.toString();
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

    public ActionReporter setParentTestCollectionId(String parentTestCollectionId) {
        this.parentTestCollectionId = parentTestCollectionId;
        return this;
    }

    public ActionReporter setParentTestId(String parentTestId) {
        this.parentTestId = parentTestId;
        return this;
    }

    public ActionReporter setParentTestEngine(TestEngine parentTestEngine) {
        this.parentTestEngine = parentTestEngine;
        return this;
    }

    public ActionReporter setModule(boolean module) {
        isModule = module;
        return this;
    }

    public ActionReporter setImAParent(boolean imAParent) {
        this.imAParent = imAParent;
        return this;
    }

    public FixtureComponent getAssertionSource() {
        return assertionSource;
    }

    @Override
    public String getTestId() {
        return testId;
    }

    @Override
    public String getTestCollectionId() {
        return testCollectionId;
    }

    @Override
    public String getTestSessionId() {
        return testEngine.getTestSession();
    }

    @Override
    public String getChannelId() {
        return testEngine.getChannelId();
    }
}
