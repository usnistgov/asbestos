package gov.nist.asbestos.testEngine.engine;

import com.google.common.base.Strings;
import gov.nist.asbestos.client.events.UIEvent;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.http.operations.HttpGetter;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureComponent;
import org.hl7.fhir.r4.model.TestScript;

import java.util.Objects;

/**
 * The term "raw" used this class means the HAPI FHIR prettified text version of the message, not the real raw text captured by the proxy.
 */
public class FixtureLabels {
    TestDef testDef;
    boolean sourceId = false;
    boolean responseId = false;
    private String rawReference;
    public String referenceLabel;
    String label = null;
    //private String tail = "";
    private static String REQ_DIRECTION_FOR_INSPECTOR = "/req";
    private static String TXT_MODE_REQ_DIRECTION_FOR_INSPECTOR = "/reqmessage";
    private static String RESP_DIRECTION_FOR_INSPECTOR = "/resp";
    private static String TXT_MODE_RESP_DIRECTION_FOR_INSPECTOR = "/respmessage";

    public enum Source { REQUEST, RESPONSE };

    FixtureLabels(TestDef testDef) {
        this.testDef = testDef;
    }

    FixtureLabels(ActionReporter actionReporter, TestScript.SetupActionOperationComponent op, String key) {
        this(actionReporter.getAssertionSource(),
                actionReporter.getAssertionSource() == null ? null : actionReporter.getAssertionSource().getId(),
                key);
        testDef = actionReporter;
    }

    public FixtureLabels referenceWrapper(ResourceWrapper wrapper) {
        HttpBase httpBase = wrapper.getHttpBase();
        if (httpBase != null) {
            operationReport(this, httpBase);
        }
        return this;
    }

    public FixtureLabels(TestDef testDef, FixtureComponent assertionSource, Source source) {
        this.testDef = testDef;
        if (source == Source.REQUEST) {
            sourceId = true;
            label = "source";
        } else if (source == Source.RESPONSE) {
            sourceId = false;
            label = "response";
        }
        responseId = !sourceId;
        assignAttributes(label);

        HttpBase httpBase = assertionSource.getHttpBase();  // http operation of fixtureComponent.wrapper
        ResourceWrapper wrapper1 = assertionSource.getResourceWrapper();

        if (httpBase != null) {  // fixtureComponent created by operation
            operationReport(this, httpBase);
        } else if (wrapper1 != null) {   // static fixtureComponent
            staticFixtureReport(this, assertionSource, wrapper1);
        }
    }

    private void operationReport(FixtureLabels labels, HttpBase httpBase) {
        Headers responseHeaders = httpBase.getResponseHeaders();
        String eventUrl = responseHeaders.getProxyEvent();
        if (eventUrl == null) {
            eventUrl = responseHeaders.getValidationEvent();
        }
        if (eventUrl != null) {
            String refStrRaw = EventLinkToUILink.get(eventUrl, getMessageDirectionForInspector(httpBase, labels.getTail()));
            labels.referenceLabel = (labels.label == null) ? refStrRaw : "Open in Inspector";
            labels.setRawReference(refStrRaw);
        }
    }

    private String getMessageDirectionForInspector(final HttpBase httpBase, String messageDirectionForInspector) {
        if (HttpGetter.GET_VERB.equals(httpBase.getVerb())) {
            if (REQ_DIRECTION_FOR_INSPECTOR.equals(messageDirectionForInspector)) {
                return TXT_MODE_REQ_DIRECTION_FOR_INSPECTOR; // GET request has no FHIR resource for the Inspector so an error will occur if this was not used. Must use raw mode for inspector
            }
        }
        return messageDirectionForInspector;
    }

    private String staticFixtureReport(FixtureLabels labels, FixtureComponent fixtureComponent, ResourceWrapper wrapper1) {
        Objects.requireNonNull(testDef);
        String refStrRaw = null;
        Ref ref = wrapper1.getRef();
        if (ref != null) {
            String base = ServiceProperties.getInstance().getPropertyOrThrow(ServicePropertiesEnum.FHIR_TOOLKIT_UI_HOME_PAGE);
            String server = ServiceProperties.getInstance().getPropertyOrThrow(ServicePropertiesEnum.FHIR_TOOLKIT_BASE);
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
                        + testDef.getTestCollectionId()
                        + "/" + testDef.getTestId()
                        + "/" + ref.asString()).urlEncode();
                refStrRaw = base
                        + "/inspectUrl?dataObject=" + url;
            } else {
                refStrRaw = base + "/session/" + testDef.getTestSessionId()
                        + "/channel/" + testDef.getChannelId()
                        + "/lognav/" + uiEvent.getEventName()
                        + labels.getTail();
            }
        }
        return refStrRaw;
    }


    FixtureLabels(FixtureComponent assertionSource, String opSourceId, String key) {
        if (key != null && assertionSource != null && key.equals(assertionSource.getId())) {
            sourceId = true;
            label = "sourceId (" + key + ")";
        }
        if (key != null && key.equals(opSourceId)) {
            sourceId = true;
            label = "sourceId (" + key + ")";
        }

        if (assertionSource != null) {
            ResourceWrapper wrapper = assertionSource.getResourceWrapper();
            sourceId = wrapper.isRequest();
            responseId = !sourceId;
        }
        assignAttributes(key);
    }

    FixtureLabels(ActionReporter actionReporter, TestScript.SetupActionAssertComponent op, String key) {
        this(actionReporter.getAssertionSource(), actionReporter.getAssertionSource().getId(), key);
    }

    private void assignAttributes(String key) {
        if (key == null)
            key ="";
        if (key.equals("lastOperation")) {
            responseId = true;
            label = key;
        }

        if (label == null) {
            label = key;
            if (key.equals("request"))
                sourceId = true;
            if (key.equals("response"))
                responseId = true;
        }
    }

    public String getReference() {
        // Dynamic
        if (rawReference == null && referenceLabel ==null) {
           return "<span title='A dynamic fixture-out feature is not a registered channel event. Inspector is not available at this point, but future uses of the resulting fixture-out may be inspectable. Check the next Submit Test Action Operation.'>Fixture-out.</span>";  /* operation results in a fixture-out that is added to fixture */
        }

        // Static fixture
        String label = referenceLabel;
        if (Strings.isNullOrEmpty(label))
            label = rawReference;

        return "<a href=\"" +
                rawReference +
                "\"" + " target=\"_blank\">" +
                label  +
                "</a>";
    }

    public FixtureLabels setRawReference(String rawReference) {
        this.rawReference = rawReference;
        return this;
    }

    public void setReference(FixtureComponent fixtureComponent) {
        UIEvent event = fixtureComponent.getCreatedByUIEvent();
        if (event == null) return;
        String uiLink = EventLinkToUILink.get(event, "");
        setRawReference(uiLink);
        referenceLabel = fixtureComponent.getId() == null ? "Open in Inspector" : fixtureComponent.getId();
    }

//    public FixtureLabels setTail(String tail) {
//        this.tail = tail;
//        return this;
//    }

    public String getTail() {
        return responseId ? RESP_DIRECTION_FOR_INSPECTOR : REQ_DIRECTION_FOR_INSPECTOR;
    }

    public String getRawReference() {
        return rawReference;
    }

    public String getLabel() {
        return label;
    }
}
