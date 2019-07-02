package gov.nist.asbestos.testEngine;

import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.BaseResource;

import java.util.Objects;

public class FixtureComponent {
    private String id;
    // these are for holding request/response bodies.  For HTTP operation, see fhirClient
    private ResourceWrapper request;
    private ResourceWrapper response;
    private boolean is_static = false; // if true then has response and no request
    private FhirClient fhirClient;
    private ValE val;

    FixtureComponent(String id) {
        Objects.requireNonNull(id);
        this.id = id;
    }

    FixtureComponent load(ResourceWrapper it) {  //  static loads
        Objects.requireNonNull(it);
        Objects.requireNonNull(it.getRef());
        Objects.requireNonNull(fhirClient);
        response = it;
        if (isLoaded())
            return this;
        response = fhirClient.readResource(it.getRef());
        is_static = true;
        return this;
    }

    /**
     *
     * @return -1 if not loaded or HTTP status
     */
    boolean IsOk() {
        return fhirClient.getStatus() == 200;
    }

    public boolean isLoaded() {
        return response.isLoaded() && response.isOk();
    }

    String getId() {
        return id;
    }

    public FixtureComponent withFormat(Format format) {
        fhirClient.setFormat(format);
        return this;
    }

    public FixtureComponent setVal(ValE val) {
        this.val = val;
        return this;
    }

    public BaseResource getRequestResource() {
        return request.getResource();
    }

    public FixtureComponent setRequestResource(ResourceWrapper request) {
        this.request = request;
        return this;
    }

    public BaseResource getResponseResource() {
        return response.getResource();
    }

    public String getResponseType() {
        BaseResource resource = getResponseResource();
        if (resource == null)
            return null;
        return resource.getClass().getSimpleName();
    }

    public FixtureComponent setResponse(ResourceWrapper response) {
        this.response = response;
        return this;
    }

    public boolean hasRequest() {
        return request != null;
    }

    public boolean hasResponse() {
        return response != null;
    }

    public HttpBase getHttpBase() {
        return fhirClient.getHttpBase();
    }

    public FixtureComponent setFhirClient(FhirClient fhirClient) {
        this.fhirClient = fhirClient;
        return this;
    }

    public FhirClient getFhirClient() {
        return fhirClient;
    }
}
