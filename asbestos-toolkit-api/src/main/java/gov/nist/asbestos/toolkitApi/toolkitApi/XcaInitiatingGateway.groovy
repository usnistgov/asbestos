package gov.nist.asbestos.toolkitApi.toolkitApi


import gov.nist.asbestos.simapi.toolkit.toolkitServicesCommon.RefList
import gov.nist.asbestos.simapi.toolkit.toolkitServicesCommon.resource.RefListResource
import groovy.transform.TypeChecked

import javax.ws.rs.core.Response

/**
 *
 */
@TypeChecked
 class XcaInitiatingGateway  extends AbstractActor implements InitiatingGateway {
    @Override
     RefList FindDocuments(String patientID) throws ToolkitServiceException {
        Response response = engine.getTarget()
                .path(String.format("simulators/%s/xds/GetAllDocs/%s", getConfig().getFullId(), patientID))
                .request().get();
        if (response.getStatus() != 200)
            throw new ToolkitServiceException(response);
        RefList rl = response.readEntity(RefListResource.class);
        return rl;
//        return response.readEntity(LeafClassListResource.class);
    }

   /* (non-Javadoc)
    * @see ChannelConfig#setPatientErrorMap(PatientErrorMap)
    */
   @Override
    void setPatientErrorMap(gov.nist.asbestos.toolkitApi.configDatatypes.client.PatientErrorMap errorMap) throws IOException {
      // TODO Auto-generated method stub

   }

   /* (non-Javadoc)
    * @see ChannelConfig#getPatientErrorMap()
    */
   @Override
    gov.nist.asbestos.toolkitApi.configDatatypes.client.PatientErrorMap getPatientErrorMap() throws IOException {
      // TODO Auto-generated method stub
      return null;
   }

}
