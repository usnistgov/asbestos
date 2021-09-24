package gov.nist.asbestos.testEngine.engine.fixture;

import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.testEngine.engine.FhirPathEngineBuilder;
import gov.nist.asbestos.testEngine.engine.NotABundleException;
import org.hl7.fhir.r4.model.*;

import java.util.Objects;

public class FixtureSub {
    private final FixtureMgr fixtureMgr;
    private final String sourceId;
    private final String fhirPath;

    public FixtureSub(FixtureMgr fixtureMgr, String sourceId, String fhirPath) {
        this.fixtureMgr = fixtureMgr;
        this.sourceId = sourceId;
        this.fhirPath = fhirPath;
    }

    @Override
    public String toString() {
        return sourceId + ": " + fhirPath;
    }

    // create temporary/unregistered FixtureComponent for SubFixture
    // do dereferencing now.  Life span is only duration of module call
    public FixtureComponent getSubFixture(FixtureComponent fixtureComponent) {
        FixtureComponent fc = new FixtureComponent(fixtureComponent.getId());
        FixtureComponent referencedFixtureComponent = fixtureMgr.get(sourceId);
        fc.setCreatedByUIEvent(referencedFixtureComponent.getCreatedByUIEvent());
        fc.setResourceSimple(get());
        fc.setHttpBase(null);
        return fc;
    }

    public ResourceWrapper get() {
        FixtureComponent fixtureComponent = fixtureMgr.get(sourceId);
        if (fixtureComponent == null)
            throw new RuntimeException("Fixture " + sourceId + " does not exist in Fixture Manager");
        BaseResource baseResource = fixtureComponent.getResourceResource();
        if (baseResource == null)
            throw new RuntimeException("Fixture " + sourceId + " does not contain a resource");
        Resource resource;
        if (baseResource instanceof Resource)
            resource = (Resource) baseResource;
        else
            throw new RuntimeException("SubFixture: fixture " + sourceId + " of type " + baseResource.getClass().getName() + " cannot be converted to type Resource");
        if (!(resource instanceof Bundle))
            throw new NotABundleException("Fixture " + sourceId + " does not contain a Bundle");
        Resource resource1 = FhirPathEngineBuilder.evalForResource(resource, fhirPath);
        ResourceWrapper wrapper = new ResourceWrapper(resource1);
        if (resource1 != null) {
            Ref fixturePathRef = fixtureComponent.getResourceWrapper().getRef();
            String fixturePath = fixturePathRef == null ? null : fixturePathRef.toString();
            Ref ref = getRef(wrapper, fixturePath, fhirPath);
            wrapper.setRef(ref);
        }
        return wrapper;
    }

    public Ref getRef(ResourceWrapper wrapper, String fixturePath, String fhirPath) {
        Objects.requireNonNull(getTestCollectionId());
        Objects.requireNonNull(getTestId());
        return FixtureComponent.generateStaticResourceRef(wrapper, fixturePath, fhirPath, getTestCollectionId(), getTestId());
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getFhirPath() {
        return fhirPath;
    }

    private String getTestCollectionId() {
        if (fixtureMgr == null)
            return null;
        return fixtureMgr.getTestCollectionId();
    }

    private String getTestId() {
        if (fixtureMgr == null)
            return null;
        return fixtureMgr.getTestId();
    }
}
