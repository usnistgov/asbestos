package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.BaseResource;

import java.util.Objects;

public class FixtureComponent {
    private static int idCounter = 1;
    private String id;
    // these are for holding request/resource bodies.  For HTTP operation, see fhirClient
//    private ResourceWrapper request;
    private ResourceWrapper resourceWrapper;
    private boolean is_static = false; // if true then has resource and no request
    private HttpBase httpBase;  //
    private ValE val;
    private FhirClient fhirClient = null;
    private FixtureSub fixtureSub = null;

    FixtureComponent(String id) {
        Objects.requireNonNull(id);
        this.id = id;
    }

    FixtureComponent(BaseResource baseResource) {
        resourceWrapper = new ResourceWrapper(baseResource);
    }

    FixtureComponent(ResourceWrapper resourceWrapper) {
        this.resourceWrapper = resourceWrapper;
    }

    public FixtureComponent setFixtureSub(FixtureSub fixtureSub) {
        this.fixtureSub = fixtureSub;
        return this;
    }

    FixtureComponent load(ResourceWrapper it) {  //  static loads
        Objects.requireNonNull(it);
        Objects.requireNonNull(it.getRef());
        resourceWrapper = it;
        if (isLoaded())
            return this;
        if (fhirClient == null)
            fhirClient = new FhirClient();
        resourceWrapper = fhirClient.readResource(it.getRef());
        is_static = true;
        return this;
    }

    private BaseResource getResource() {
        getResourceWrapper();
        if (resourceWrapper != null)
            return resourceWrapper.getResource();
        return null;
    }

    /**
     *
     * @return -1 if not loaded or HTTP status
     */
    boolean IsOk() {
        return httpBase != null && httpBase.getStatus() == 200;
    }

    public boolean isLoaded() {
        return resourceWrapper != null && resourceWrapper.isLoaded() && resourceWrapper.isOk();
    }

    String getId() {
        return id;
    }

    public FixtureComponent setVal(ValE val) {
        this.val = val;
        return this;
    }

    public BaseResource getResourceResource() {
        ResourceWrapper resourceWrapper = getResourceWrapper();
        if (resourceWrapper != null)
            return resourceWrapper.getResource();
        return null;
    }

    public ResourceWrapper getResourceWrapper() {
        if (resourceWrapper != null)
            return resourceWrapper;
        if (fixtureSub != null) {
            ResourceWrapper wrapper = fixtureSub.get();
            resourceWrapper = wrapper;
            return resourceWrapper;
        }
        return null;
    }

    public String getResponseType() {
        BaseResource resource = getResourceResource();
        if (resource == null)
            return null;
        return resource.getClass().getSimpleName();
    }

    public FixtureComponent setResource(ResourceWrapper resource) {
        this.resourceWrapper = resource;
        return this;
    }


    public boolean hasResource() {
        getResourceWrapper();
        return resourceWrapper != null;
    }

    public HttpBase getHttpBase() {
        return httpBase;
    }

    public FixtureComponent setHttpBase(HttpBase httpBase) {
        this.httpBase = httpBase;
        return this;
    }

    public boolean hasHttpBase() {
        return httpBase != null;
    }

    public static String getNewId() {
        return "ID" + idCounter++;
    }

    public FixtureComponent setFhirClient(FhirClient fhirClient) {
        this.fhirClient = fhirClient;
        return this;
    }

    public boolean isStatic() {
        return is_static;
    }

    public void setId(String id) {
        this.id = id;
    }
}
