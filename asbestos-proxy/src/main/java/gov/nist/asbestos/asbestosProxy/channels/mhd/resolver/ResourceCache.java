package gov.nist.asbestos.asbestosProxy.channels.mhd.resolver;

import gov.nist.asbestos.fproxy.channels.mhd.transactionSupport.ResourceWrapper;

interface ResourceCache {
    ResourceWrapper readResource(Ref url);
    void add(Ref ref, ResourceWrapper resource);
}
