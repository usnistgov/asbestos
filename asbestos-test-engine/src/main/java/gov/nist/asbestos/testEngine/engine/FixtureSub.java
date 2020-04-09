package gov.nist.asbestos.testEngine.engine;

import gov.nist.asbestos.client.resolver.ResourceWrapper;
import org.hl7.fhir.r4.model.*;

public class FixtureSub {
    private  FixtureMgr fixtureMgr;
    private String fixtureName;
    private String fhirPath;

    public FixtureSub(FixtureMgr fixtureMgr, String fixtureName, String fhirPath) {
        this.fixtureMgr = fixtureMgr;
        this.fixtureName = fixtureName;
        this.fhirPath = fhirPath;
    }

    public ResourceWrapper get() {
        FixtureComponent fixtureComponent = fixtureMgr.get(fixtureName);
        if (fixtureComponent == null)
            throw new RuntimeException("SubFixture " + fixtureName + " does not exist in Fixture Manager");
        BaseResource baseResource = fixtureComponent.getResourceResource();
        if (baseResource == null)
            throw new RuntimeException("SubFixture " + fixtureName + " does not contain a resource");
        Resource resource;
        if (baseResource instanceof Resource)
            resource = (Resource) baseResource;
        else
            throw new RuntimeException("SubFixture: fixture " + fixtureName + " of type " + baseResource.getClass().getName() + " cannot be converted to type Resource");
        if (!(resource instanceof Bundle))
            throw new RuntimeException("SubFixture " + fixtureName + " does not contain a Bundle");
        Resource resource1 = FhirPathEngineBuilder.evalForResource(resource, fhirPath);
        return new ResourceWrapper(resource1);
    }

}
