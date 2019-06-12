package gov.nist.asbestos.mhd.resolver;


import gov.nist.asbestos.asbestosProxySupport.Base.IVal;
import gov.nist.asbestos.mhd.transactionSupport.ResourceWrapper;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
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
    private ResourceMgrConfig resourceMgrConfig = new ResourceMgrConfig();

    public ResourceMgr() {

    }

    public void setBundle(Bundle bundle) {
        bundleResources = new HashMap<>();
        parse(bundle);
    }

    public ResourceMgrConfig getResourceMgrConfig() {
        return resourceMgrConfig;
    }

    public ResourceMgr setResourceCacheMgr(ResourceCacheMgr resourceCacheMgr) {
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
        ValE thisVal = new ValE("Load Bundle...");
        val.add(thisVal);
        thisVal.add(new ValE("All objects assigned symbolic IDs")
                .add(new ValE("3.65.4.1.2 Message Semantics").asDoc()));
        bundle.getEntry().forEach(component -> {
            if (component.hasResource()) {
                String id = allocateSymbolicId();
                thisVal.add(new ValE("Assigning " + id + " to " + component.getResource().getClass().getSimpleName() + "(" + component.getResource().getIdElement().getValue() + ")"));
                ResourceWrapper wrapper = new ResourceWrapper(component.getResource())
                        .setId(id)
                        .setUrl(new Ref(component.getFullUrl()));

                thisVal.add(new ValE("..." + component.getFullUrl()));
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
            val.add(new ValE("Duplicate resource found in bundle for URL " + url).asError());
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
        Val thisVal = val.add(new ValE("Resolver: Resolve URL " + referenceUrl + " ... " + config));

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
                    thisVal.add(new ValE("Resolver: ...absolute reference to resource in bundle " + referenceUrl  + " - external reference required ").asError());
                    return Optional.empty();
                }
            } else {
                //
                // External
                //
                return Optional.of(load(new ResourceWrapper(referenceUrl)));
            }
        }
        if (containing == null) {
            thisVal.add(new ValE("Resolver: ... reference is not absolute " + referenceUrl + " but no containing resource is offered").asError());
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
            thisVal.add(new ValE("Resolver: ...reference is to contained resource (" + referenceUrl + " but contained is not acceptable").asError());
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
                    ResourceWrapper resource = new ResourceWrapper(referenceUrl.rebase(containing.getUrl().getBase()));
                    if (resource.getUrl() != null) {
                        if (getFromBundle(resource.getUrl()) != null)
                            resource = getFromBundle(resource.getUrl());  // this includes Resource as well as URL
                    }
                    return Optional.of(resource);
                } else {
                    return Optional.of(res);
                }
            }
            thisVal.add(new ValE("Resolver: ...reference is to relative resource (" + referenceUrl + " but relative is not acceptable").asError());
            return Optional.empty();
        }
        thisVal.add(new ValE("Resolver: ...failed to resolve " + referenceUrl + " in " + containing).asError());
        return Optional.empty();
    }

    private ResourceWrapper load(ResourceWrapper resource) {
        if (resource != null && resource.getUrl() != null) {
            if (resourceCacheMgr != null) {
                ResourceWrapper loaded = resourceCacheMgr.getResource(resource.getUrl());
                if (loaded != null)
                    resource.setResource(loaded.getResource());
            }
            if (!resource.isLoaded() && resourceMgrConfig.isOpen()) {
                val.add(new ValE("ResourceMgr#load: External resource loading is not implemented").asError());
            }
        }
        return resource;
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
