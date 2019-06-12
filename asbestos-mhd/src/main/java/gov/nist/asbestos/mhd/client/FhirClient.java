package gov.nist.asbestos.mhd.client;

import gov.nist.asbestos.http.operations.HttpGet;
import gov.nist.asbestos.mhd.resolver.Ref;
import gov.nist.asbestos.mhd.transactionSupport.ResourceWrapper;
import gov.nist.asbestos.utilities.TheFhirContext;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FhirClient {

    public static Optional<ResourceWrapper> readResource(URI uri, boolean xml) {
        HttpGet getter = new HttpGet();
        String contentType = (xml) ? "application/fhir+xml" : "application/fhir+json";
            getter.get(uri, contentType);
        String resourceText = getter.getResponseText();
        if (resourceText == null)
            return Optional.empty();
        IBaseResource resource;
        if (xml)
            resource = TheFhirContext.get().newXmlParser().parseResource(resourceText);
        else
            resource = TheFhirContext.get().newJsonParser().parseResource(resourceText);
        ResourceWrapper wrapper = new ResourceWrapper();
        wrapper.setUrl(new Ref(uri));
        wrapper.setResource(resource);
        return Optional.of(wrapper);
    }

    public static Optional<ResourceWrapper> readResource(URI uri) {
        return readResource(uri, false);
    }

    public static List<ResourceWrapper> search(Ref base, Class<?> resourceType, List<String> params) {
        URI query = QueryBuilder.buildUrl(base, resourceType, params);
        Optional<ResourceWrapper> wrapper = readResource(query);
        if (!wrapper.isPresent())
            return new ArrayList<>();
        assert wrapper.get().getResource() instanceof Bundle;
        Bundle bundle = (Bundle) wrapper.get().getResource();
        List<ResourceWrapper> list = new ArrayList<>();

        for (Bundle.BundleEntryComponent comp : bundle.getEntry()) {
            String fullUrl = comp.getFullUrl();
            IBaseResource resource = comp.getResource();
            ResourceWrapper wrapper1 = new ResourceWrapper();
            wrapper1.setResource(resource);
            wrapper1.setUrl(new Ref(fullUrl));
            list.add(wrapper1);
        }

        return list;
    }
}
