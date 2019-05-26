package gov.nist.asbestos.toolkitApi.toolkitApi;

import gov.nist.asbestos.simapi.toolkit.toolkitServicesCommon.RefList
import groovy.transform.TypeChecked;

/**
 *
 */
@TypeChecked
 interface InitiatingGateway extends AbstractActorInterface {
    RefList FindDocuments(String patientId) throws ToolkitServiceException;
}
