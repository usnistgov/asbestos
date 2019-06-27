package gov.nist.asbestos.client.resolver;


import java.util.List;

interface ResourceCache {
    ResourceWrapper readResource(Ref url);
    void add(Ref ref, ResourceWrapper resource);
    List<ResourceWrapper> getAll(Ref base, String type);
}
