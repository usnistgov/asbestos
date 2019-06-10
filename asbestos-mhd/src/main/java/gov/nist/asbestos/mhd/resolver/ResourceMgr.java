package gov.nist.asbestos.mhd.resolver;


import gov.nist.asbestos.asbestosProxySupport.Base.IVal;
import gov.nist.asbestos.mhd.transactionSupport.ResourceWrapper;
import gov.nist.asbestos.simapi.validation.Val;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Resource;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
public class ResourceMgr implements IVal {
//    static private final Logger logger = Logger.getLogger(ResourceMgr.class);
    private Map<Ref, ResourceWrapper> bundleResources = new HashMap<>();   // url -> resource; for contents of bundle
    private ResourceCacheMgr resourceCacheMgr = null;
    private Val val;

    public ResourceMgr() {

    }

    public void setBundle(Bundle bundle) {
        bundleResources = new HashMap<>();
        parse(bundle);
    }

    public ResourceMgr addResourceCacheMgr(ResourceCacheMgr resourceCacheMgr) {
        this.resourceCacheMgr = resourceCacheMgr;
        return this;
    }

    private boolean inBundle(Ref ref) {
        return bundleResources.containsKey(ref);
    }

    private ResourceWrapper getFromBundle(Ref ref) {
        return bundleResources.get(ref);
    }

    public List<ResourceWrapper> getBundleResources() {
        return new ArrayList<>(bundleResources.values());
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

        for (Ref ref : bundleResources.keySet()) {
            ResourceWrapper resource = bundleResources.get(ref);
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
    private void addResource(Ref url, ResourceWrapper resource) {
        Objects.requireNonNull(val);
        Objects.requireNonNull(url);
        Objects.requireNonNull(resource);
        boolean duplicate = bundleResources.containsKey(url);
        if (duplicate)
            val.err(new Val()
                    .msg("Duplicate resource found in bundle for URL ${url}"));
        else
            bundleResources.put(url, resource);
    }

    private ResourceWrapper getContains(ResourceWrapper resource, Ref refUrl) {
        Objects.requireNonNull(resource);
        Objects.requireNonNull(resource.getResource());
        Objects.requireNonNull(refUrl);
        String ref = refUrl.toString();
        if (resource.getResource() instanceof DomainResource) {
            DomainResource res = (DomainResource) resource.getResource();
            for (Resource r : res.getContained()) {
                if (r.hasId() && r.getId().equals(ref))
                    return new ResourceWrapper(r, refUrl);
            }
        }
        return null;
    }

    private ResourceWrapper getRelative(ResourceWrapper resource, Ref refUrl) {
        Objects.requireNonNull(resource);
        Objects.requireNonNull(refUrl);
        if (refUrl.isRelative())
            return new ResourceWrapper(refUrl).relativeTo(resource);
        return new ResourceWrapper(refUrl);
    }

    /**
     * Return resource if internal or reference if external.
     * @param containing
     * @param referenceUrl
     * @param config
     * @return
     */
    public Optional<ResourceWrapper> resolveReference(ResourceWrapper containing, Ref referenceUrl, ResolverConfig config) {
        Objects.requireNonNull(val);
        Objects.requireNonNull(referenceUrl);
        Objects.requireNonNull(config);
        Val thisVal = val.addSection("Resolver: Resolve URL " + referenceUrl + " ... " + config);

        //
        // Absolute
        //
        if (referenceUrl.isAbsolute()) {
            if (inBundle(referenceUrl)) {
                //
                // In bundle ok?
                //
                if (config.isInBundleOk()) {
                    return Optional.of(getFromBundle(referenceUrl));
                } else {
                    thisVal.err(new Val("Resolver: ...absolute reference to resource in bundle " + referenceUrl  + " - external reference required "));
                    return Optional.empty();
                }
            } else {
                //
                // External
                //
                return Optional.of(new ResourceWrapper(referenceUrl));
            }
        }
        if (containing == null) {
            thisVal.err(new Val("Resolver: ... reference is not absolute " + referenceUrl + " but no containing resource is offered"));
            return Optional.empty();
        }
        //
        // Contained
        //
        if (referenceUrl.isContained()) {
            if (config.isContainedOk()) {
                thisVal.msg("Resolver: ...contained");
                return Optional.ofNullable(getContains(containing, referenceUrl));
            }
            thisVal.err(new Val("Resolver: ...reference is to contained resource (" + referenceUrl + " but contained is not acceptable"));
            return Optional.empty();
        }
        //
        // Relative to containing resource or found in bundle
        //   if in bundle must be fullUrl but temp labels (urn:...) look like local -
        //   don't start with http or file
        //
        if (referenceUrl.isRelative()) {
            if (config.isRelativeOk()) {
                ResourceWrapper res = getFromBundle(referenceUrl);
                if (res == null) {
                    if (containing.getUrl() == null)
                        return Optional.empty();
                    // relative/external
                    return Optional.of(new ResourceWrapper(referenceUrl.rebase(containing.getUrl().getBase())));
                } else {
                    return Optional.of(res);
                }
            }
            thisVal.err(new Val("Resolver: ...reference is to relative resource (" + referenceUrl + " but relative is not acceptable"));
            return Optional.empty();
        }
        thisVal.err(new Val().msg("Resolver: ...failed to resolve " + referenceUrl + " in " + containing));
        return Optional.empty();
    }

    private SymbolicIdBuilder symbolicIdBuilder = new SymbolicIdBuilder();

    public String allocateSymbolicId() {
        return symbolicIdBuilder.allocate();
    }


    @Override
    public void setVal(Val val) {
        this.val = val;
    }
}
