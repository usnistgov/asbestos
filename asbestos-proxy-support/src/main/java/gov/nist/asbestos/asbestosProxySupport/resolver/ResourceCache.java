package gov.nist.asbestos.asbestosProxySupport.resolver;


import java.util.List;

interface ResourceCache {
    ResourceWrapper readResource(Ref url);
    void add(Ref ref, ResourceWrapper resource);
    List<ResourceWrapper> getAll(Ref base, String type);
}
