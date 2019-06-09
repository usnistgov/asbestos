package gov.nist.asbestos.asbestosProxySupport.Base;


import gov.nist.asbestos.http.operations.HttpGet;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.io.IOException;
import java.net.URI;

public class FhirClient {

    public static IBaseResource readResource(URI uri) throws IOException {
        return readResource(uri, FhirContentType.JSON);
    }

    public static IBaseResource readResource(URI uri, FhirContentType fhirContentType) throws IOException {
        HttpGet getter = new HttpGet();
        getter.get(uri, fhirContentType.contentType);
        return parse(getter.getResponseText(), fhirContentType);
    }

    public static IBaseResource parse(String resourceText, FhirContentType fhirContentType) {
        if(resourceText == null) return null;
        return (fhirContentType.isJson) ?
                Base.getFhirContext().newJsonParser().parseResource(resourceText) :
                Base.getFhirContext().newXmlParser().parseResource(resourceText);
    }
}
