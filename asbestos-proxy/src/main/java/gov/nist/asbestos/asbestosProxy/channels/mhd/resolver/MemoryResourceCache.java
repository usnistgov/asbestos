package gov.nist.asbestos.asbestosProxy.channels.mhd.resolver;


import gov.nist.asbestos.asbestosProxy.channels.mhd.transactionSupport.ResourceWrapper;

import java.util.HashMap;
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
    public String toString() {
        StringBuilder buf = new StringBuilder();

        cache.forEach((Ref ref, ResourceWrapper wrapper) -> {
            buf.append("(MEM ").append(ref.toString()).append(" -> ").append(wrapper.toString()).append("\n");
        });

        return buf.toString();
    }
}
