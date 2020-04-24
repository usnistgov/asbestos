package gov.nist.asbestos.testEngine.engine.fixture;

import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.testEngine.engine.FhirPathEngineBuilder;
import org.hl7.fhir.r4.model.*;

import java.util.Objects;

public class FixtureSub {
    private  FixtureMgr fixtureMgr;
    private String sourceId;
    private String fhirPath;

    public FixtureSub(FixtureMgr fixtureMgr, String sourceId, String fhirPath) {
        this.fixtureMgr = fixtureMgr;
        this.sourceId = sourceId;
        this.fhirPath = fhirPath;
    }

    public ResourceWrapper get() {
        FixtureComponent fixtureComponent = fixtureMgr.get(sourceId);
        if (fixtureComponent == null)
            throw new RuntimeException("SubFixture " + sourceId + " does not exist in Fixture Manager");
        BaseResource baseResource = fixtureComponent.getResourceResource();
        if (baseResource == null)
            throw new RuntimeException("SubFixture " + sourceId + " does not contain a resource");
        Resource resource;
        if (baseResource instanceof Resource)
            resource = (Resource) baseResource;
        else
            throw new RuntimeException("SubFixture: fixture " + sourceId + " of type " + baseResource.getClass().getName() + " cannot be converted to type Resource");
        if (!(resource instanceof Bundle))
            throw new RuntimeException("SubFixture " + sourceId + " does not contain a Bundle");
        Resource resource1 = FhirPathEngineBuilder.evalForResource(resource, fhirPath);
        ResourceWrapper wrapper = new ResourceWrapper(resource1);
        if (resource1 != null) {
            String fixturePath = fixtureComponent.getResourceWrapper().getRef().toString();
            Ref ref = getRef(resource1.getClass().getSimpleName(), fixturePath, fhirPath);
            wrapper.setRef(ref);
        }
        return wrapper;
    }

    public Ref getRef(String resourceType, String fixturePath, String fhirPath) {
        Objects.requireNonNull(getTestCollectionId());
        Objects.requireNonNull(getTestId());
        return FixtureComponent.generateStaticFixtureRef(resourceType, fixturePath, fhirPath, getTestCollectionId(), getTestId());
//        URI uri = null;
//        try {
//            uri = new URI(ServiceProperties.getInstance().getPropertyOrStop(ServicePropertiesEnum.FHIR_TOOLKIT_BASE) + "/engine/staticFixture/IT_Test_Support/StaticFixture");
//        } catch (URISyntaxException e) {
//            return null;
//        }
//        String searchString = "?url=" + fixturePath;
//        if (fhirPath != null && !fhirPath.equals(""))
//            searchString = searchString + ";fhirPath=" + fhirPath;
//        SearchParms searchParms = new SearchParms();
//        try {
//            searchParms.setParms(searchString, true);
//        } catch (UnsupportedEncodingException e) {
//            return null;
//        }
//        return new Ref(uri, resourceType, searchParms);
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
