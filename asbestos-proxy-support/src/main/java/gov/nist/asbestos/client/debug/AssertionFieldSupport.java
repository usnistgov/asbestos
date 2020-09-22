package gov.nist.asbestos.client.debug;

import java.util.List;

public class AssertionFieldSupport {
    private AssertionFieldDescription fixtureId;
    private List<AssertionFieldDescription> overrideFieldTypes;
    private List<AssertionFieldDescription> fhirEnumerationTypes;


    public AssertionFieldDescription getFixtureId() {
        return fixtureId;
    }

    public void setFixtureId(AssertionFieldDescription fixtureId) {
        this.fixtureId = fixtureId;
    }

    public List<AssertionFieldDescription> getOverrideFieldTypes() {
        return overrideFieldTypes;
    }

    public void setOverrideFieldTypes(List<AssertionFieldDescription> overrideFieldTypes) {
        this.overrideFieldTypes = overrideFieldTypes;
    }

    public List<AssertionFieldDescription> getFhirEnumerationTypes() {
        return fhirEnumerationTypes;
    }

    public void setFhirEnumerationTypes(List<AssertionFieldDescription> fhirEnumerationTypes) {
        this.fhirEnumerationTypes = fhirEnumerationTypes;
    }
}
