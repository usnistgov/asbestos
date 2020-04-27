package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureComponent;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureMgr;
import org.hl7.fhir.r4.model.TestScript;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

class ActionReporter {
    private String testCollectionId = null;
    private String testId = null;
    private TestEngine testEngine = null;
    private FixtureComponent source  = null;

    void reportOperation(ResourceWrapper wrapper, FixtureMgr fixtureMgr, VariableMgr variableMgr, Reporter reporter, TestScript.SetupActionOperationComponent op) {
        String request = "### " + wrapper.getHttpBase().getVerb() + " " + wrapper.getHttpBase().getUri() + "\n";

        report(wrapper, fixtureMgr, variableMgr, reporter, request, op);
    }

    void reportAssertion(FixtureMgr fixtureMgr, VariableMgr variableMgr, Reporter reporter, FixtureComponent source) {
        this.source = source;
        String request = "";

        report(null, fixtureMgr, variableMgr, reporter, request, null);
    }

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
            String additionalKey = null;
            if (key != null && source != null && key.equals(source.getId()))
                additionalKey = "sourceId";
            if (key != null && op != null && op.hasResponseId() && key.equals(op.getResponseId()))
                additionalKey = "responseId";
            FixtureComponent fixtureComponent = fixtureMgr.get(key);
            String value = null;

            HttpBase httpBase = fixtureComponent.getHttpBase();
            ResourceWrapper wrapper1 = fixtureComponent.getResourceWrapper();
            if (httpBase != null) {
                Headers responseHeaders = httpBase.getResponseHeaders();
                String eventUrl = responseHeaders.getProxyEvent();
                if (eventUrl != null)
                    value = EventLinkToUILink.get(eventUrl);
            } else if (wrapper1 != null) {
                Ref ref = wrapper1.getRef();
                if (ref != null) {
                    String refStrEncoded = ref.toString();    // relative static fixture path - URL encoded
                    String refStrRaw = null;   // no URL encoding
                    try {
                        refStrRaw = URLDecoder.decode(refStrEncoded, StandardCharsets.UTF_8.toString());
                    } catch (UnsupportedEncodingException e) {
                        continue;
                    }
                        value = "<a href=\"" +  refStrEncoded + "\"" + " target=\"_blank\">" + refStrRaw + "</a>";
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
            if (additionalKey != null)
                fixtures.put(additionalKey, value);
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
