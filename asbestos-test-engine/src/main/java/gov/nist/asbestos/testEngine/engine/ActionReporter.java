package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpBase;

import java.util.HashMap;
import java.util.Map;

class ActionReporter {

    void reportOperation(ResourceWrapper wrapper, FixtureMgr fixtureMgr, VariableMgr variableMgr, Reporter reporter) {
        String request = "### " + wrapper.getHttpBase().getVerb() + " " + wrapper.getHttpBase().getUri() + "\n";

        report(wrapper, fixtureMgr, variableMgr, reporter, request);
    }

    void reportAssertion(FixtureMgr fixtureMgr, VariableMgr variableMgr, Reporter reporter) {
        String request = "";

        report(null, fixtureMgr, variableMgr, reporter, request);
    }

    private void report(ResourceWrapper wrapper, FixtureMgr fixtureMgr, VariableMgr variableMgr, Reporter reporter, String request) {
        Map<String, String> fixtures = new HashMap<>();
        for (String key : fixtureMgr.keySet()) {
            FixtureComponent comp = fixtureMgr.get(key);
            String value = null;

            HttpBase httpBase = comp.getHttpBase();
            if (httpBase != null) {
                Headers responseHeaders = httpBase.getResponseHeaders();
                String eventUrl = responseHeaders.getProxyEvent();
                value = EventLinkToUILink.get(eventUrl);
            } else if (comp.isLoaded()){
                ResourceWrapper wrapper1 = comp.getResourceWrapper();
                if (wrapper1 != null) {
                    Ref ref = wrapper1.getRef();
                    if (ref != null)
                        value = ref.toString() + " (static)";
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
        }

        Map<String, String> variables = variableMgr.getVariables();

        String markdown = request
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
}
