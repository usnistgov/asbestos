package gov.nist.asbestos.toolkitApi.toolkitServicesCommon.resource;

import gov.nist.asbestos.toolkitApi.toolkitServicesCommon.RetrieveRequest;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 */
@XmlRootElement
public class RetrieveRequestResource  extends SimIdResource implements RetrieveRequest {
    String repositoryUniqueId = null;
    String documentUniqueId = null;
    String homeCommunityId = null;
    RequestFlavorResource flavor = new RequestFlavorResource();

    public RetrieveRequestResource() {}

    @Override
   public String getRepositoryUniqueId() {
        return repositoryUniqueId;
    }

    @Override
   public void setRepositoryUniqueId(String repositoryUniqueId) {
        this.repositoryUniqueId = repositoryUniqueId;
    }

    @Override
   public String getDocumentUniqueId() {
        return documentUniqueId;
    }

    @Override
   public void setDocumentUniqueId(String documentUniqueId) {
        this.documentUniqueId = documentUniqueId;
    }

    @Override
   public String getHomeCommunityId() {
        return homeCommunityId;
    }

    @Override
   public void setHomeCommunityId(String homeCommunityId) {
        this.homeCommunityId = homeCommunityId;
    }

    public boolean isTls() {
        return flavor.isTls();
    }

    public void setTls(boolean tls) {
        flavor.setTls(tls);
    }
}
