package gov.nist.asbestos.mhd.resolver;


import gov.nist.asbestos.mhd.transactionSupport.ResourceWrapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MemoryResourceCache implements ResourceCache {
    private Map<Ref, ResourceWrapper> cache = new HashMap<>();

    @Override
    public ResourceWrapper readResource(Ref url) {
        return cache.get(url);
    }

    @Override
    public void add(Ref ref, ResourceWrapper resource) {
        cache.put(ref, resource);
    }

    @Override
    public List<ResourceWrapper> getAll(Ref base, String type) {
        return null;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        cache.forEach((Ref ref, ResourceWrapper wrapper) -> {
            buf.append("(MEM ").append(ref.toString()).append(" -> ").append(wrapper.toString()).append("\n");
        });

        return buf.toString();
    }
}
