package gov.nist.asbestos.asbestosProxy.channels.mhd.resolver

import gov.nist.asbestos.fproxy.channels.mhd.transactionSupport.ResourceWrapper
import groovy.transform.TypeChecked

@TypeChecked
class MemoryResourceCache implements ResourceCache {
    Map<Ref, ResourceWrapper> cache = [:]

    @Override
    ResourceWrapper readResource(Ref url) {
        cache[url]
    }

    @Override
    void add(Ref ref, ResourceWrapper resource) {
        cache[ref] = resource
    }

    @Override
    String toString() {
        StringBuilder buf = new StringBuilder()

        cache.each { Ref key, ResourceWrapper wrapper ->
            buf.append("(MEM ${key} -> ${wrapper})\n")
        }

        buf.toString()
    }
}
