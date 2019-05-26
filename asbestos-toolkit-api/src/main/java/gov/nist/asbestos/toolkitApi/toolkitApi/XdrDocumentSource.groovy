package gov.nist.asbestos.toolkitApi.toolkitApi


import gov.nist.asbestos.simapi.toolkit.toolkitServicesCommon.RawSendRequest
import gov.nist.asbestos.simapi.toolkit.toolkitServicesCommon.RawSendResponse
import gov.nist.asbestos.simapi.toolkit.toolkitServicesCommon.resource.RawSendRequestResource
import groovy.transform.TypeChecked

/**
 *
 */
@TypeChecked
class XdrDocumentSource extends AbstractActor implements DocumentSource {

    /**
     * Send a raw Provide and Register request.
     * @param request raw request
     * @return raw response
     * @throws ToolkitServiceException if something goes wrong
     */
    @Override
     RawSendResponse sendProvideAndRegister(RawSendRequest request) throws ToolkitServiceException {
        return engine.sendXdr(request);
    }

    /**
     * Create empty raw send request for this actor. This request can be filled in and then sent to the actor.
     * @return the empty request
     */
     RawSendRequest newRawSendRequest() { return new RawSendRequestResource(config); }

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
