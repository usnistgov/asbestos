package gov.nist.asbestos.toolkitApi.toolkitApi

import gov.nist.asbestos.simapi.toolkit.toolkitServicesCommon.DocumentContent
import gov.nist.asbestos.simapi.toolkit.toolkitServicesCommon.resource.DocumentContentResource
import groovy.transform.TypeChecked

import javax.ws.rs.core.Response

/**
 *
 */
@TypeChecked
 class XdsDocumentRepository extends AbstractActor implements DocumentRepository {

    @Override
     DocumentContent getDocument(String uniqueId) throws ToolkitServiceException {
        Response response = engine.getTarget()
                .path(String.format("simulators/%s/document/%s", getConfig().getFullId(), uniqueId))
                .request().get();
        if (response.getStatus() != 200)
            throw new ToolkitServiceException(response);
        return response.readEntity(DocumentContentResource.class);
    }


}
