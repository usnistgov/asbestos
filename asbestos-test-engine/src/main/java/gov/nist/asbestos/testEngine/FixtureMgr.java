package gov.nist.asbestos.testEngine;

import gov.nist.asbestos.client.Base.IVal;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.resolver.ResourceCacheMgr;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.simapi.validation.Val;

import java.util.Objects;

public class FixtureMgr implements IVal {
    private String id;
    private ResourceWrapper resourceWrapper;
    private FhirClient fhirClient;
    private Val val;

    public FixtureMgr(String id, ResourceWrapper resourceWrapper, ResourceCacheMgr resourceCacheMgr) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(resourceWrapper);
        Objects.requireNonNull(resourceCacheMgr);
        this.id = id;
        this.resourceWrapper = resourceWrapper;
        fhirClient = new FhirClient().setResourceCacheMgr(resourceCacheMgr);
    }

    public FixtureMgr load() {
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
    public int getStatus() {
        return resourceWrapper.getStatus();
    }

    public boolean isLoaded() {
        return resourceWrapper.isLoaded() && resourceWrapper.isOk();
    }

    public String getId() {
        return id;
    }

    public ResourceWrapper getResourceWrapper() {
        return resourceWrapper;
    }

    public FixtureMgr withFormat(Format format) {
        fhirClient.setFormat(format);
        return this;
    }


    @Override
    public void setVal(Val val) {
        this.val = val;
    }
}
