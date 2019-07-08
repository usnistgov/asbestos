package gov.nist.asbestos.testEngine;

import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
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
    private ResourceWrapper resource;
    private boolean is_static = false; // if true then has resource and no request
    private HttpBase httpBase;  //
    private ValE val;
    private FhirClient fhirClient = null;

    FixtureComponent(String id) {
        Objects.requireNonNull(id);
        this.id = id;
    }

    FixtureComponent load(ResourceWrapper it) {  //  static loads
        Objects.requireNonNull(it);
        Objects.requireNonNull(it.getRef());
        resource = it;
        if (isLoaded())
            return this;
        if (fhirClient == null)
            fhirClient = new FhirClient();
        resource = fhirClient.readResource(it.getRef());
        is_static = true;
        return this;
    }

    /**
     *
     * @return -1 if not loaded or HTTP status
     */
    boolean IsOk() {
        return httpBase.getStatus() == 200;
    }

    public boolean isLoaded() {
        return resource.isLoaded() && resource.isOk();
    }

    String getId() {
        return id;
    }

    public FixtureComponent setVal(ValE val) {
        this.val = val;
        return this;
    }

    public BaseResource getResourceResource() {
        if (resource != null)
            return resource.getResource();
        return null;
    }

    public String getResponseType() {
        BaseResource resource = getResourceResource();
        if (resource == null)
            return null;
        return resource.getClass().getSimpleName();
    }

    public FixtureComponent setResource(ResourceWrapper resource) {
        this.resource = resource;
        return this;
    }


    public boolean hasResource() {
        return resource != null;
    }

    public HttpBase getHttpBase() {
        return httpBase;
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
}
