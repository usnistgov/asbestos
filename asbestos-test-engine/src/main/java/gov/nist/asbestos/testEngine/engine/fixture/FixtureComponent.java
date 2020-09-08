package gov.nist.asbestos.testEngine.engine.fixture;

import com.google.common.base.Strings;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.events.UIEvent;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.client.resolver.SearchParms;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import gov.nist.asbestos.simapi.validation.ValE;
import gov.nist.asbestos.testEngine.engine.ActionReference;
import org.hl7.fhir.r4.model.BaseResource;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
    private FixtureMgr fixtureMgr = null;
    private Ref staticRef = null;
    private ActionReference createdByActionReference = null;  // script action that created this fixture
    private List<ActionReference> referencedByActionReference = new ArrayList<>();
    private UIEvent createdByUIEvent = null;

    // constructors only available to FixtureMgr
    FixtureComponent(String id) {
        Objects.requireNonNull(id);
        this.id = id;
    }

    FixtureComponent(BaseResource baseResource) {
        setResource(new ResourceWrapper(baseResource));
    }

    public FixtureComponent() {
        //setResource(resourceWrapper);
    }

    public FixtureSub getFixtureSub() {
        return fixtureSub;
    }

    public FixtureComponent setResourceSimple(ResourceWrapper resource) {
        resourceWrapper = resource;
        return this;
    }

    public FixtureComponent setFixtureSub(FixtureSub fixtureSub) {
        this.fixtureSub = fixtureSub;
        return this;
    }

    @Override
    public String toString() {
        return id
        + (createdByUIEvent == null ? "" : ": " + createdByUIEvent.getEventName())
                + (resourceWrapper == null ? "" : ": " + resourceWrapper.toString());
    }

    public FixtureComponent load(ResourceWrapper it) {  //  static loads
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
    public boolean IsOk() {
        return httpBase != null && httpBase.getStatus() == 200;
    }

    public boolean isLoaded() {
        return resourceWrapper != null && resourceWrapper.isLoaded() && resourceWrapper.isOk();
    }

    public String getId() {
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

    private FhirClient getFhirClient() {
        Objects.requireNonNull(fixtureMgr);
        FhirClient fhirClient = fixtureMgr.getFhirClient();
        Objects.requireNonNull(fhirClient);
        return fhirClient;
    }

    private void loadStatic() {
        Optional<ResourceWrapper> optWrapper;
        FhirClient fhirClient = getFhirClient();
        try {
            // with optional return this should not throw an exception - but it does
            optWrapper = fhirClient.readCachedResource(staticRef);
        } catch (Throwable t) {
            optWrapper = Optional.empty();
        }

        if (!optWrapper.isPresent())
            throw new Error("Static Fixture " + staticRef + " cannot be loaded");
        resourceWrapper = optWrapper.get();
    }

    public ResourceWrapper getResourceWrapper() {
        try {
            if (resourceWrapper == null && staticRef != null) {
                loadStatic();
            }
            if (resourceWrapper != null)
                return resourceWrapper;
            if (fixtureSub != null) {
                ResourceWrapper wrapper = fixtureSub.get();
                resourceWrapper = wrapper;
                return resourceWrapper;
            }
        } catch (Throwable t) {
            // ignore
        }
        return null;
    }

    public static Ref generateStaticResourceRef(ResourceWrapper wrapper, String fixturePath, String fhirPath, String testCollectionId, String testId) {
        URI uri;
        try {
            uri = new URI(ServiceProperties.getInstance().getPropertyOrStop(ServicePropertiesEnum.FHIR_TOOLKIT_BASE)
                    + "/static/staticResource/" + testCollectionId + "/" + testId);
        } catch (URISyntaxException e) {
            return null;
        }
        String searchString = "?fixturePath=" + fixturePath;
        if (!Strings.isNullOrEmpty(fhirPath))
            searchString = searchString + ";fhirPath=" + fhirPath;
        SearchParms searchParms = new SearchParms();
        try {
            searchParms.setParms(searchString, true);  // encoding needed to escape bundle references
        } catch (UnsupportedEncodingException e) {
            return null;
        }
        return new Ref(uri, wrapper.getResourceType(), searchParms);
    }

    public String getResponseType() {
        BaseResource resource = getResourceResource();
        if (resource == null)
            return null;
        return resource.getClass().getSimpleName();
    }

    public FixtureComponent setStaticRef(Ref ref) {
        this.staticRef = ref;
        this.is_static = true;
        return this;
    }

    public FixtureComponent setResource(ResourceWrapper resource) {
        Objects.requireNonNull(getTestCollectionId());
        Objects.requireNonNull(getTestId());
        this.resourceWrapper = resource;
        if (resource.hasRef() && resource.getRef().isRelative()) {
            Ref ref = generateStaticResourceRef(
                    resource,
                    resource.getRef().toString(),  // fixturePath
                    null,
                    getTestCollectionId(),
                    getTestId());
            resource.setRef(ref);
        }
        if (resource.getEvent() == null && resource.hasRef() && resource.getRef().isEvent()) {
            createdByUIEvent = new UIEvent(Ref.getEC()).fromResource(resourceWrapper);
        }
        return this;
    }


    public boolean hasResource() {
        getResourceWrapper();
        return resourceWrapper != null;
    }

    public HttpBase getHttpBase() {
        if (httpBase != null)
            return httpBase;
        if (resourceWrapper != null) {
            if (resourceWrapper.getHttpBase() != null)
                return resourceWrapper.getHttpBase();
        }
        return null;
    }

    public FixtureComponent setHttpBase(HttpBase httpBase) {
        this.httpBase = httpBase;
        return this;
    }

    public boolean hasHttpBase() {
        return getHttpBase() != null;
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

    public FixtureComponent setFixtureMgr(FixtureMgr mgr) {
        fixtureMgr = mgr;
        return this;
    }

    private String getTestCollectionId() {
        Objects.requireNonNull(fixtureMgr);
        return fixtureMgr.getTestCollectionId();
    }

    private String getTestId() {
        Objects.requireNonNull(fixtureMgr);
        return fixtureMgr.getTestId();
    }

    public FixtureComponent setCreatedByActionReference(ActionReference createdByActionReference) {
        this.createdByActionReference = createdByActionReference;
        return this;
    }

    public ActionReference getCreatedByActionReference() {
        return createdByActionReference;
    }

    public FixtureComponent setReferencedByActionReference(ActionReference actionReference) {
        referencedByActionReference.add(actionReference);
        return this;
    }

    public List<ActionReference> getReferencedByActionReference() {
        return referencedByActionReference;
    }

    public UIEvent getCreatedByUIEvent() {
        return createdByUIEvent;
    }

    public FixtureComponent setCreatedByUIEvent(UIEvent createdByUIEvent) {
        this.createdByUIEvent = createdByUIEvent;
        return this;
    }
}
