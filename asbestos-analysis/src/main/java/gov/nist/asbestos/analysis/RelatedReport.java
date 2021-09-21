package gov.nist.asbestos.analysis;

import gov.nist.asbestos.client.Base.EventContext;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.operations.HttpGetter;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import org.apache.log4j.Logger;
import org.hl7.fhir.r4.model.OperationOutcome;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RelatedReport {
    private static final Logger log = Logger.getLogger(RelatedReport.class);

    // basics
    String name;
    String relation;
    String url; // link for UI to pull related FHIR object - through proxy
    String nativeUrl;  // FHIR object native location (not through proxy)
    EventContext eventContext;

    // evaluation
    boolean isMinimal;
    boolean isComprehensive;
    List<String> minimalErrors;
    List<String> comprehensiveErrors;
    List<String> codingErrors;
    List<String> minimalChecked;
    List<String> comprehensiveChecked;
    OperationOutcome validationResult;

    Map atts;
    String binaryUrl;

    RelatedReport(ResourceWrapper wrapper, String relation) {
        if (wrapper.hasResource())
            this.name = wrapper.getResource().fhirType();
        else
            this.name = wrapper.getRef().getResourceType();
        this.relation = relation;
        if (wrapper.getRef() == null)
            this.url = "Contained";
        else
            this.url = wrapper.getRef().toString();

        if (this.url.contains("/proxy/")) {
            String proxyLogUrl = ServiceProperties.getInstance().getPropertyOrStop(ServicePropertiesEnum.FHIR_TOOLKIT_BASE)
                    + "/log/native?url=" + this.url;
            HttpGetter getter = new HttpGetter();
            try {
                getter.get(proxyLogUrl);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            if (getter.isSuccess()) {
                this.nativeUrl = getter.getResponseText();
            }
        }

        if (wrapper.hasEvent())
            eventContext = new EventContext(wrapper.getEvent());

        log.info("Related Object url=" + this.url);
    }

    public void setEventContext(EventContext eventContext) {
        this.eventContext = eventContext;
    }

    public String toString() {
        return url;
    }

    public String getName() {
        return name;
    }

    public boolean isMinimal() {
        return isMinimal;
    }

    public boolean isComprehensive() {
        return isComprehensive;
    }

    public List<String> getMinimalChecked() {
        return minimalChecked;
    }

    public List<String> getComprehensiveErrors() {
        return comprehensiveErrors;
    }

    public OperationOutcome getValidationResult() {
        return validationResult;
    }
}
