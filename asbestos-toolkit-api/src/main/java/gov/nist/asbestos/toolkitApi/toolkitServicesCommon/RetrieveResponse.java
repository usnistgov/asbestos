package gov.nist.asbestos.toolkitApi.toolkitServicesCommon;

/**
 *
 */
public interface RetrieveResponse {
   String getDocumentUid();
   String getRepositoryUid();
   String getHomeCommunityUid();
    String getMimeType();
    byte[] getDocumentContents();
}
