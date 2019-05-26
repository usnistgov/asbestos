package gov.nist.asbestos.toolkitApi.toolkitApi

import groovy.transform.TypeChecked;

/**
 *
 */
@TypeChecked
 class FhirServer extends AbstractActor implements IFhirServer {
    @Override
     boolean isFhir() {
        return true;
    }

}
