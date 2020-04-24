package gov.nist.asbestos.testEngine.engine.fixture;

import org.hl7.fhir.r4.model.BaseResource;

public class UnregisteredFixtureComponent extends FixtureComponent {

    public UnregisteredFixtureComponent(BaseResource baseResource) {
        super(baseResource);
    }

    public UnregisteredFixtureComponent(String id) {
        super(id);
    }
}
