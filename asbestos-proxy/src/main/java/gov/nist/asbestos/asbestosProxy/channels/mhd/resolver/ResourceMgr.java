package gov.nist.asbestos.asbestosProxy.channels.mhd.resolver;


import gov.nist.asbestos.asbestosProxy.Base.IVal;
import gov.nist.asbestos.asbestosProxy.channels.mhd.transactionSupport.ResourceWrapper;
import gov.nist.asbestos.simapi.validation.Val;
import org.hl7.fhir.r4.model.Bundle;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 *
 */
public class ResourceMgr implements IVal {
//    static private final Logger logger = Logger.getLogger(ResourceMgr.class);
    private Map<Ref, ResourceWrapper> resources = new HashMap<>();   // url -> resource
    private ResourceCacheMgr resourceCacheMgr = null;
    private Val val;

    public ResourceMgr(Bundle bundle) {
        if (bundle != null)
            parse(bundle);
    }

    public ResourceMgr addResourceCacheMgr(ResourceCacheMgr resourceCacheMgr) {
        this.resourceCacheMgr = resourceCacheMgr;
        return this;
    }

    // Load bundle and assign symbolic ids
    public void parse(Bundle bundle) {
        Objects.requireNonNull(bundle);
        Objects.requireNonNull(val);
        Val thisVal = val.addSection("Load Bundle...");
        thisVal.add(new Val()
                .msg("All objects assigned symbolic IDs")
                .frameworkDoc("3.65.4.1.2 Message Semantics"));
        bundle.getEntry().forEach(component -> {
            if (component.hasResource()) {
                String id = allocateSymbolicId();
                thisVal.msg("Assigning ${id} to ${component.resource.class.simpleName}/${component.resource.idElement.value}");
                ResourceWrapper wrapper = new ResourceWrapper(component.getResource())
                        .setId(id)
                        .setUrl(new Ref(component.getFullUrl()));

                thisVal.add("..." + component.getFullUrl());
                addResource(new Ref(component.getFullUrl()), wrapper);
            }
        });
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("Resources:\n");

        for (Ref ref : resources.keySet()) {
            ResourceWrapper resource = resources.get(ref);
            buf.append(ref).append("   ").append(resource.getClass().getSimpleName()).append('\n');
        }
        return buf.toString();
    }

    /**
     *
     * @param url
     * @param resource
     * @return already present
     */
    void addResource(Ref url, ResourceWrapper resource) {
        Objects.requireNonNull(val);
        Objects.requireNonNull(url);
        Objects.requireNonNull(resource);
        boolean duplicate = resources.containsKey(url);
        if (duplicate)
            val.err(new Val()
                    .msg("Duplicate resource found in bundle for URL ${url}"));
        else
            resources.put(url, resource);
    }

    /**
     *
     * @param containing  (fullUrl)
     * @param referenceUrl   (reference)
     * @return [url, Resource]
     */
    // TODO - needs toughening - containingURL could be null if referenceURL is absolute
    ResourceWrapper resolveReference(ResourceWrapper containing, Ref referenceUrl, ResolverConfig config) {
        Objects.requireNonNull(val);
        Objects.requireNonNull(containing);
        Objects.requireNonNull(referenceUrl);
        Objects.requireNonNull(config);
        Val thisVal = val.addSection("Resolver: Resolve URL " + referenceUrl + " ... " + config);

        if (config.containedRequired || (config.containedOk && referenceUrl.getId().startsWith("#"))) {
            if (config.relativeReferenceOk && referenceUrl.toString().startsWith("#") && config.containedOk) {
                thisVal.msg("Resolver: ...contained");
                return new ResourceWrapper(containing.getResource(), referenceUrl);
            }
            return new ResourceWrapper(null, null);
        }
        if (!config.externalRequired) {
            if (config.relativeReferenceOk && referenceUrl.toString().startsWith("#") && config.containedOk) {
                ResourceWrapper res = containing.getContained().get(referenceUrl);
                res.setUrl(referenceUrl);
                thisVal.msg("Resolver: ...contained");
                return res;
            }
            if (resources.containsKey(referenceUrl)) {
                thisVal.msg("Resolver: ...in bundle");
                return resources.get(referenceUrl);
            }
            boolean isRelativeReference = !referenceUrl.isAbsolute();
            if (config.relativeReferenceRequired && !isRelativeReference) {
                thisVal.msg("Resolver: ...relative reference required - not relative");
                return new ResourceWrapper(null, null);
            }
            String resourceType = referenceUrl.getResourceType();
            // TODO - isAbsolute does an assert on containing... here is why... if we have gotten to this point...
            // Resource.fullUrl (containing) is a uuid (not a real reference) then it is not absolute
            // if it is not absolute then this refernceUrl cannot be relative (relative to what???).
            // this is a correct validation but needs a lot more on the error message (now a Groovy assert)
            if (!containing.getUrl().isAbsolute() && !referenceUrl.isAbsolute()) {
                Map<Ref, ResourceWrapper> x = resources.entrySet().stream()
//                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
//                        .entrySet().stream()
                        .filter(ref -> !("Patient".equals(resourceType) && isRelativeReference && !config.relativeReferenceOk))
                        .filter(ref -> ref.toString().endsWith(referenceUrl.toString()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                // TODO should check for multiples
                if (!x.isEmpty()) {
                    return x.entrySet().iterator().next().getValue();
                }
            }
            if (containing.getUrl().isAbsolute() && !referenceUrl.isAbsolute()) {
                Ref url = containing.getUrl().rebase(referenceUrl);
                if (resources.containsKey(url)) {
                    thisVal.msg("Resolver: ...found in bundle");
                    return resources.get(url);
                }
                if (resourceCacheMgr != null) {
                    thisVal.msg("Resolver: ...looking in Resource Cache");
                    ResourceWrapper resource = resourceCacheMgr.getResource(url);
                    if (resource != null) {
                        thisVal.msg("Resolver: ...returned from cache");
                        return resource;
                    }
                } else
                    thisVal.msg("Resource Cache not configured");
            }
        }

        // external
        if (!config.internalRequired && referenceUrl.isAbsolute()) {
            if (resourceCacheMgr != null) {
                thisVal.msg("Resolver: ...looking in Resource Cache");
                ResourceWrapper resource = resourceCacheMgr.getResource(referenceUrl);
                if (resource != null) {
                    thisVal.msg("Resolver: ...returned from cache");
                    return resource;
                }
            } else {
                thisVal.msg("Resource Cache not configured");
            }
            ResourceWrapper res = referenceUrl.load();
            if (res != null) {
                thisVal.msg("Resolver: ...found");
                return res;
            } else {
                thisVal.msg("Resolver: " + referenceUrl + " ...not available");
                return new ResourceWrapper(null, null);
            }
        }

        thisVal.err(new Val().msg("Resolver: ...failed"));
        new ResourceWrapper(null, null);
    }

    private int symbolicIdCounter = 1;
    String allocateSymbolicId() {
        return "ID" + Integer.toString(symbolicIdCounter++);
    }


    @Override
    public void setVal(Val val) {
        this.val = val;
    }
}
