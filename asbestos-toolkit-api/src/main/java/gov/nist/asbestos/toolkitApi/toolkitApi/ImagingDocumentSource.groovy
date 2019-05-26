/**
 *
 */
package gov.nist.asbestos.toolkitApi.toolkitApi;

import gov.nist.asbestos.simapi.toolkit.toolkitServicesCommon.DcmImageSet
import groovy.transform.TypeChecked;

/**
 * XDSI Image Document Source API
 */
@TypeChecked
 interface ImagingDocumentSource extends AbstractActorInterface {

   DcmImageSet retrieveImagingDocumentSet(DcmImageSet request);
}
