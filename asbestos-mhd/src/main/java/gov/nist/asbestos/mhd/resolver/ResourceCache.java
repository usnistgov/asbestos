package gov.nist.asbestos.mhd.resolver;


import gov.nist.asbestos.mhd.transactionSupport.ResourceWrapper;

interface ResourceCache {
    ResourceWrapper readResource(Ref url);
    void add(Ref ref, ResourceWrapper resource);
}
