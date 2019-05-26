/**
 *
 */
package gov.nist.asbestos.toolkitApi.toolkitServicesCommon;

import gov.nist.asbestos.toolkitApi.toolkitServicesCommon.resource.RetImgDocSetReqSeriesResource;

import java.util.List;

/**
 * RAD-69 StudyRequest interface
 *
 * @author Ralph Moulton / MIR WUSTL IHE Development Project <a
 * href="mailto:moultonr@mir.wustl.edu">moultonr@mir.wustl.edu</a>
 *
 */
public interface RetImgDocSetReqStudy {

   void setStudyInstanceUID(String studyInstanceUID);
   void setRetrieveImageSeriesRequests(List<RetImgDocSetReqSeriesResource> retrieveImageSeriesRequests);
   String getStudyInstanceUID();
   List<RetImgDocSetReqSeriesResource> getRetrieveImageSeriesRequests();

}
