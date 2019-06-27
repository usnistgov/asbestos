package gov.nist.asbestos.testEngine;

import gov.nist.asbestos.client.Base.IVal;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;

import java.util.Objects;

public class FixtureComponent {
    private String id;
    private ResourceWrapper resourceWrapper;
    private FhirClient fhirClient;
    private ValE val;

    FixtureComponent(String id, ResourceWrapper resourceWrapper, FhirClient fhirClient) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(resourceWrapper);
        Objects.requireNonNull(fhirClient);
        this.id = id;
        this.resourceWrapper = resourceWrapper;
        this.fhirClient = fhirClient;
    }

    FixtureComponent load() {
        Objects.requireNonNull(resourceWrapper);
        Objects.requireNonNull(resourceWrapper.getUrl());
        if (isLoaded())
            return this;
        resourceWrapper = fhirClient.readResource(resourceWrapper.getUrl());
        return this;
    }

    /**
     *
     * @return -1 if not loaded or HTTP status
     */
    boolean IsOk() {
        return resourceWrapper.getStatus() == 200;
    }

    public boolean isLoaded() {
        return resourceWrapper.isLoaded() && resourceWrapper.isOk();
    }

    String getId() {
        return id;
    }

    ResourceWrapper getResourceWrapper() {
        return resourceWrapper;
    }

    public FixtureComponent withFormat(Format format) {
        fhirClient.setFormat(format);
        return this;
    }

    public FixtureComponent setVal(ValE val) {
        this.val = val;
        return this;
    }
}
