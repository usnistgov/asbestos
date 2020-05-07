package gov.nist.asbestos.analysis;

import gov.nist.asbestos.client.resolver.ResourceWrapper;

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

    Related(ResourceWrapper wrapper, String howRelated) {
        this.wrapper = wrapper;
        this.howRelated = howRelated;
    }

    Related contained() {
        contained = true;
        return this;
    }


}
