package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.events.UIEvent;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureComponent;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureMgr;
import org.checkerframework.checker.units.qual.A;
import org.hl7.fhir.r4.model.TestScript;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

class ActionReporter {
    private String testCollectionId = null;
    private String testId = null;
    private TestEngine testEngine = null;
    private FixtureComponent source  = null;

    void reportOperation(ResourceWrapper wrapper, FixtureMgr fixtureMgr, VariableMgr variableMgr, Reporter reporter, TestScript.SetupActionOperationComponent op) {
        String request = wrapper == null ? "" : "### " + wrapper.getHttpBase().getVerb() + " " + wrapper.getHttpBase().getUri() + "\n";

        report(wrapper, fixtureMgr, variableMgr, reporter, request, op);
    }

    void reportAssertion(FixtureMgr fixtureMgr, VariableMgr variableMgr, Reporter reporter, FixtureComponent source) {
        this.source = source;
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

        if (source != null) {
            HttpBase httpBase = source.getHttpBase();
            if (httpBase != null ) {
                Headers responseHeaders = httpBase.getResponseHeaders();
                String eventUrl = responseHeaders.getProxyEvent();
                if (eventUrl != null) {
                    String value = EventLinkToUILink.get(eventUrl);
                    try {
                        Ref ref = new Ref(value);
                        if (ref.isAbsolute()) {
                            value = "<a href=\"" + value + "\"" + " target=\"_blank\">" + value + "</a>";
                        }
                    } catch (Throwable t) {
                        // ignore
                    }
                    fixtures.put("lastOperation", value);
                }
            }
        }

        for (String key : fixtureMgr.keySet()) {
            Set<String> additionalKey = new HashSet<>();
            if (key != null && source != null && key.equals(source.getId()))
                additionalKey.add("sourceId");
            if (key != null && op != null && op.hasResponseId() && key.equals(op.getResponseId()))
                additionalKey.add("responseId");
            if (key != null && op != null && op.hasSourceId() && key.equals(op.getSourceId()))
                additionalKey.add("sourceId");
            FixtureComponent fixtureComponent = fixtureMgr.get(key);
            String value = null;

            HttpBase httpBase = fixtureComponent.getHttpBase();
            ResourceWrapper wrapper1 = fixtureComponent.getResourceWrapper();
            String refStrEncoded;    // relative static fixture path - URL encoded
            if (httpBase != null) {  // fixtureComponent created by operation
                Headers responseHeaders = httpBase.getResponseHeaders();
                String eventUrl = responseHeaders.getProxyEvent();
                if (eventUrl != null)
                    value = EventLinkToUILink.get(eventUrl);
            } else if (wrapper1 != null) {   // static fixtureComponent
                Ref ref = wrapper1.getRef();
                if (ref != null) {
                    refStrEncoded = ref.toString();
                    String refStrRaw;
                    UIEvent uiEvent = fixtureComponent.getCreatedByUIEvent();
                    if (uiEvent == null)
                        refStrRaw = null;
                    else {
                        refStrRaw = "http://localhost:8082/session/" + testEngine.getTestSession()
                                + "/channel/" + testEngine.getChannelName()
                                + "/lognav/" + uiEvent.getEventName();
                    }
                    value = "<a href=\"" +  refStrRaw + "\"" + " target=\"_blank\">" + refStrRaw + "</a>";
                }
            }

            try {
                Ref ref = new Ref(value);
                if (ref.isAbsolute()) {
                    value = "<a href=\"" + value + "\"" + " target=\"_blank\">" + value + "</a>";
                }
            } catch (Throwable t) {
                // ignore
            }

            fixtures.put(key, value);
            for (String otherKey : additionalKey)
                fixtures.put(otherKey, value);
        }

        Map<String, String> variables = variableMgr.getVariables();

        String path = "## Module\n" + testEngine.getTestEnginePath();

        String markdown = path + "\n" +
                request
                + asMarkdown(fixtures, "Fixtures")
                + "\n"
                + asMarkdown(variables, "Variables");

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
