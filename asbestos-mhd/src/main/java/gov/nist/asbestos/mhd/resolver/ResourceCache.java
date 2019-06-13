package gov.nist.asbestos.mhd.resolver;


import gov.nist.asbestos.mhd.transactionSupport.ResourceWrapper;

import java.util.List;

interface ResourceCache {
    ResourceWrapper readResource(Ref url);
    void add(Ref ref, ResourceWrapper resource);
    List<ResourceWrapper> getAll(Ref base, String type);
}
