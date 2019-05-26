/**
 *
 */
package gov.nist.asbestos.toolkitApi.toolkitServicesCommon;

import gov.nist.asbestos.toolkitApi.toolkitServicesCommon.resource.RetImgDocSetReqDocumentResource;

import java.util.List;

/**
 * RAD-69 SeriesRequest interface
 *
 * @author Ralph Moulton / MIR WUSTL IHE Development Project <a
 * href="mailto:moultonr@mir.wustl.edu">moultonr@mir.wustl.edu</a>
 *
 */
public interface RetImgDocSetReqSeries {

   void setSeriesInstanceUID(String seriesIntanceUID);
   void setRetrieveImageDocumentRequests(List<RetImgDocSetReqDocumentResource> retrieveRequests);
   String getSeriesInstanceUID();
   List<RetImgDocSetReqDocumentResource> getRetrieveImageDocumentRequests();
}
