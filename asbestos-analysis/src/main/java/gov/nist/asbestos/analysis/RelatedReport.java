package gov.nist.asbestos.analysis;

import gov.nist.asbestos.client.Base.EventContext;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import org.hl7.fhir.r4.model.OperationOutcome;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RelatedReport {
    String name;
    String relation;
    String url;
    boolean isMinimal;
    boolean isComprehensive;
    List<String> minimalErrors;
    List<String> comprehensiveErrors;
    List<String> codingErrors;
    List<String> minimalChecked;
    List<String> comprehensiveChecked;
    OperationOutcome validationResult;
    EventContext eventContext;

    Map atts;
    String binaryUrl;

    RelatedReport(ResourceWrapper wrapper, String relation) {
        if (wrapper.hasResource())
            this.name = wrapper.getResource().getClass().getSimpleName();
        else
            this.name = wrapper.getRef().getResourceType();
        this.relation = relation;
        if (wrapper.getRef() == null)
            this.url = "Contained";
        else
            this.url = wrapper.getRef().toString();
    }

    public void setEventContext(EventContext eventContext) {
        this.eventContext = eventContext;
    }

    public String toString() {
        return url;
    }
}
