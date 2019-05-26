package gov.nist.asbestos.toolkitApi.toolkitApi


import gov.nist.asbestos.simapi.toolkit.toolkitServicesCommon.LeafClassRegistryResponse
import gov.nist.asbestos.simapi.toolkit.toolkitServicesCommon.RetrieveRequest
import gov.nist.asbestos.simapi.toolkit.toolkitServicesCommon.RetrieveResponse
import gov.nist.asbestos.simapi.toolkit.toolkitServicesCommon.StoredQueryRequest
import groovy.transform.TypeChecked

/**
 *
 */
@TypeChecked
 class XdsDocumentConsumer extends AbstractActor implements DocumentConsumer {
    @Override
     LeafClassRegistryResponse queryForLeafClass(StoredQueryRequest request) throws ToolkitServiceException {
        return engine.queryForLeafClass(request);
    }

//    @Override
//     RefList queryForObjectRef(String queryId, Map<String, List<String>> parameters) {
//        return null;
//    }

    @Override
     RetrieveResponse retrieve(RetrieveRequest request) throws ToolkitServiceException {
        return engine.retrieve(request);
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
