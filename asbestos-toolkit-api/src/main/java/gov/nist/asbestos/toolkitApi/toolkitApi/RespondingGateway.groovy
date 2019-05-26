package gov.nist.asbestos.toolkitApi.toolkitApi;

import gov.nist.asbestos.simapi.toolkit.toolkitServicesCommon.LeafClassList
import groovy.transform.TypeChecked;

/**
 *
 */
@TypeChecked
 interface RespondingGateway extends AbstractActorInterface {
    LeafClassList FindDocuments(String patientId) throws ToolkitServiceException;
}
