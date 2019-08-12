package gov.nist.asbestos.client.resolver;


import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MemoryResourceCache implements ResourceCache {
    private Map<Ref, ResourceWrapper> cache = new HashMap<>();

    @Override
    public ResourceWrapper readResource(Ref url) {
        return cache.get(url);
    }

    public boolean hasResource(Ref url) {
        return cache.containsKey(url);
    }

    @Override
    public void add(Ref ref, ResourceWrapper resource) {
        if (!resource.hasRef())
            resource.setRef(ref);
        cache.put(ref, resource);
    }

    @Override
    public List<ResourceWrapper> getAll(Ref base, String type) {
        List<ResourceWrapper> results = new ArrayList<>();
        for (ResourceWrapper wrapper : cache.values()) {
            if (wrapper.hasResource() && type.equals(wrapper.getResource().getClass().getSimpleName())) {
                if (wrapper.hasRef()) {
                    Ref aBase = wrapper.getRef().getBase();
                    if (aBase.equals(base))
                        results.add(wrapper);
                }
                results.add(wrapper);
            }
        }
        return results;
    }

    public List<ResourceWrapper> getAll(String type) {
        List<ResourceWrapper> results = new ArrayList<>();
        for (ResourceWrapper wrapper : cache.values()) {
            if (wrapper.hasResource() && type.equals(wrapper.getResource().getClass().getSimpleName())) {
                results.add(wrapper);
            }
        }
        return results;
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
