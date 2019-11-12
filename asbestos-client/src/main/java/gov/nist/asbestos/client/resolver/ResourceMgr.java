package gov.nist.asbestos.client.resolver;


import gov.nist.asbestos.client.Base.IVal;
import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.events.ITask;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Resource;

import java.util.*;

/**
 *
 */
public class ResourceMgr implements IVal {
    //    static private final Logger logger = Logger.getLogger(ResourceMgr.class);
    private Map<Ref, ResourceWrapper> bundleResources = new HashMap<>();   // url -> resource; for contents of bundle
    private Val val;
    private ResourceMgrConfig resourceMgrConfig = new ResourceMgrConfig();
    private FhirClient fhirClient = null;

    private List<ResourceWrapper> bundleResourceList = new ArrayList<>();
    private ITask theTask = null;

    public ResourceMgr() {

    }

    public void setBundle(Bundle bundle) {
        bundleResources = new HashMap<>();
        parse(bundle);
    }

    public ResourceMgrConfig getResourceMgrConfig() {
        return resourceMgrConfig;
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
    private void parse(Bundle bundle) {
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
                        .setAssignedId(id)
                        .setRef(new Ref(component.getFullUrl()));

                thisVal.add(new ValE("..." + component.getFullUrl()));
                addResource(new Ref(component.getFullUrl()), wrapper);
                bundleResourceList.add(wrapper);
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
        if (ref == null || !ref.startsWith("#"))
            return null;
        //ref = ref.substring(1);
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

    public Optional<ResourceWrapper> resolveReference(ResourceWrapper containing, Ref referenceUrl, ResolverConfig config) {
        return resolveReference(containing, referenceUrl, config, new ValE(val));
    }
    /**
     * Return resource if internal or reference if external.
     * @param containing
     * @param referenceUrl
     * @param config
     * @return
     */
    public Optional<ResourceWrapper> resolveReference(ResourceWrapper containing, Ref referenceUrl, ResolverConfig config, ValE val) {
        Objects.requireNonNull(val);
        Objects.requireNonNull(referenceUrl);
        Objects.requireNonNull(config);
        ValE thisVal = new ValE(val);
        thisVal.add(new ValE("Resolver: Resolve URL " + referenceUrl + " ... " + config));

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
                thisVal.setMsg("Resolver: ...contained");
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
                    if (containing.getRef() == null)
                        return Optional.empty();
                    // relative/external
                    ResourceWrapper resource = new ResourceWrapper(referenceUrl.rebase(containing.getRef().getBase()));
                    if (resource.getRef() != null) {
                        if (getFromBundle(resource.getRef()) != null)
                            resource = getFromBundle(resource.getRef());  // this includes Resource as well as URL
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
        Objects.requireNonNull(resource);
        Objects.requireNonNull(fhirClient);
        if (resource.isLoaded())
            return resource;
        if (resource.getRef() == null)
            return resource;
        if (resourceMgrConfig.isInternalOnly()) {
            Optional<ResourceWrapper> cached = fhirClient.readCachedResource(resource.getRef());
            if (cached.isPresent() && cached.get().isLoaded()) {
                resource.setResource(cached.get().getResource());
                logResourceWrapper(resource, "Read from cache");
            }
            return resource;
        }
        ResourceWrapper wrapper = fhirClient.readResource(resource.getRef());
        String msg = wrapper.getRef().isRelative() ? "loaded from test definition" : "";
        wrapper.getHttpBase().getRequestHeaders().setVerb("GET").setPathInfo(wrapper.getHttpBase().getUri());
        logResourceWrapper(wrapper, msg);
        resource.setResource(wrapper.getResource());
        return resource;
    }

    private void logResourceWrapper(ResourceWrapper wrapper, String msg) {
        Objects.requireNonNull(theTask);

        ITask task = theTask.newTask();
        task.putDescription(msg);
        logResourceWrapper(wrapper, task);
    }

    private void logResourceWrapper(ResourceWrapper wrapper, ITask task) {
        HttpBase base = wrapper.getHttpBase();
        if (base == null) {
            task.putRequestHeader(new Headers().withVerb("GET").withPathInfo(wrapper.getRef().getUri()));
            BaseResource resource = wrapper.getResource();
            String txt = ProxyBase.encode(resource, Format.XML);
            task.putResponseBodyText(txt);
        } else {
            task.putRequestHeader(base.getRequestHeaders());
            task.putRequestBody(base.getRequest());
            task.putResponseHeader(base.getResponseHeaders());
            task.putResponseBody(base.getResponse());
        }
    }

    private SymbolicIdBuilder symbolicIdBuilder = new SymbolicIdBuilder();

    public String allocateSymbolicId() {
        return symbolicIdBuilder.allocate();
    }


    @Override
    public void setVal(Val val) {
        this.val = val;
    }

    public ResourceMgr setFhirClient(FhirClient fhirClient) {
        this.fhirClient = fhirClient;
        return this;
    }



    private static final List<Integer> ignores = Arrays.asList(8,13,18,23);
    public static boolean isUUID(String u) {
        if (u.startsWith("urn:uuid:")) u = u.substring(9);
        u = u.toLowerCase();
        if (u.length() != 36)
            return false;
        for (Integer i : ignores) {
            if (u.charAt(i) != '-')
                return false;
        }
        for (int i=0; i<36; i++) {
            if (ignores.contains(i))
                continue;
            String hexChars = "0123456789abcdef";
            if (hexChars.indexOf(u.charAt(i)) == -1)
                return false;
        }
        return true;
    }

    public List<ResourceWrapper> getBundleResourceList() {
        return bundleResourceList;
    }

    public ResourceMgr setTask(ITask task) {
        this.theTask = task;
        return this;
    }
}
