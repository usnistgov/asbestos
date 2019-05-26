/**
 *
 */
package gov.nist.asbestos.toolkitApi.toolkitServicesCommon;

import gov.nist.asbestos.toolkitApi.toolkitServicesCommon.resource.RetImgDocSetRespDocumentResource;

import java.util.List;

/**
 * Retrieve Imaging Document Set Response Resource corresponds to
 * {@code <RetrieveDocumentSetResponse>} message.
 *
 * @author Ralph Moulton / MIR WUSTL IHE Development Project <a
 * href="mailto:moultonr@mir.wustl.edu">moultonr@mir.wustl.edu</a>
 *
 */
public interface RetImgDocSetResp {

   public String getAbbreviatedResponse();
   public void setAbbreviatedResponse(String abbreviatedResponse);

   public List <RetImgDocSetRespDocumentResource> getDocuments();
   public void setDocuments(List<RetImgDocSetRespDocumentResource> documents);
}
