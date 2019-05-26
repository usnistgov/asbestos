package gov.nist.asbestos.asbestosProxy.channels.mhd.transactionSupport

import gov.nist.asbestos.fproxy.channels.mhd.resolver.Ref
import gov.nist.asbestos.simapi.validation.Val
import groovy.transform.TypeChecked
import org.hl7.fhir.instance.model.api.IBaseResource


@TypeChecked
class ResourceWrapper {
    IBaseResource resource    // basic content of the resource
    String assignedId         // assigned symbolic id - used in XDS submissionm
    Ref url               // FHIR URL - used when available - read from server
    // String is the fragment without the leading #
    // https://www.hl7.org/fhir/references.html#contained
    // lists the rules for contained resources
    // also relevant is
    // https://www.hl7.org/fhir/resource.html#id
    Map<Ref, ResourceWrapper> contained = [:]


    ResourceWrapper(IBaseResource resource) {
        this.resource = resource
    }

    ResourceWrapper(IBaseResource resource, Ref url) {
        this.resource = resource
        this.url = url
    }

    ResourceWrapper setId(String id) {
        assignedId = id
        this
    }

    ResourceWrapper setUrl(Ref url) {
        this.url = url
        this
    }

    String getId() {
        if (url.id) return url.id
        if (assignedId) return assignedId
        assert false : "Cannot retreive id for ${resource}"
    }

    private boolean addContainedResource(ResourceWrapper resource, Val val) {
        Ref id = new Ref(resource.id)
        boolean duplicate = contained.containsKey(id)
        if (!duplicate) contained[id] = resource
        if (duplicate)
            val.err(new Val().msg("Contained resource ${id} is a duplicate within ${resource.id}"))
        return duplicate
    }

    @Override
    String toString() {
        StringBuilder buf = new  StringBuilder()

        buf << "RW[${assignedId}, ${url}] => ${resource.class.simpleName}"

        buf.toString()
    }
}
