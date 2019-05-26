/**
 *
 */
package gov.nist.asbestos.toolkitApi.toolkitServicesCommon;

import gov.nist.asbestos.toolkitApi.toolkitServicesCommon.resource.RetImgDocSetReqStudyResource;

import java.util.List;

/**
 * RAD-69 Image Request interface
 *
 * @author Ralph Moulton / MIR WUSTL IHE Development Project <a
 * href="mailto:moultonr@mir.wustl.edu">moultonr@mir.wustl.edu</a>
 *
 */
public interface RetImgDocSetReq extends SimId {

   void setRetrieveImageStudyRequests(List<RetImgDocSetReqStudyResource> studyRequests);
   void setTransferSyntaxUIDs(List<String> transferSyntaxUIDs);
   List<RetImgDocSetReqStudyResource> getRetrieveImageStudyRequests();
   List<String> getTransferSyntaxUIDs();
}
