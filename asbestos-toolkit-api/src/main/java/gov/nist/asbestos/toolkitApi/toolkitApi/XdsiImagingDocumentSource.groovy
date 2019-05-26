/**
 *
 */
package gov.nist.asbestos.toolkitApi.toolkitApi


import gov.nist.asbestos.simapi.toolkit.toolkitServicesCommon.DcmImageSet
import groovy.transform.TypeChecked

/**
 * Implementation class for XDSI Image Document Source
 */
@TypeChecked
 class XdsiImagingDocumentSource extends AbstractActor implements ImagingDocumentSource {

   /* (non-Javadoc)
    * @see gov.nist.toolkit.tookitApi.ImageDocumentSource#retrieveImagingDocumentSet(DcmImageSet)
    */
   @Override
    DcmImageSet retrieveImagingDocumentSet(DcmImageSet request) {
      // TODO Auto-generated method stub
      return null;
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
