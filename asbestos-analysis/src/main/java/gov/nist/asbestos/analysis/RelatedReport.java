package gov.nist.asbestos.analysis;

import gov.nist.asbestos.client.resolver.ResourceWrapper;

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
    String minimalChecked;
    String comprehensiveChecked;
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
}
