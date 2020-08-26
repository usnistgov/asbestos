package gov.nist.asbestos.analysis;

import gov.nist.asbestos.client.resolver.ResourceWrapper;
import org.hl7.fhir.r4.model.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class Related {
    ResourceWrapper wrapper;
    String howRelated;
    List<String> minimalErrors;
    List<String> comprehensiveErrors;
    List<String> codingErrors = new ArrayList<>();
    Checked comprehensiveChecked;
    Checked minimalChecked;
    Map atts;
    boolean contained = false;
    String binaryUrl;
    Bundle contextResource = null;

    Related(ResourceWrapper wrapper, String howRelated) {
        this.wrapper = wrapper;
        this.howRelated = howRelated;
    }

    Related(ResourceWrapper wrapper, Bundle contextResource, String howRelated) {
        this.wrapper = wrapper;
        this.howRelated = howRelated;
        this.contextResource = contextResource;
    }

    Related contained() {
        contained = true;
        return this;
    }

    public String toString() {
        return wrapper.toString();
    }


}
