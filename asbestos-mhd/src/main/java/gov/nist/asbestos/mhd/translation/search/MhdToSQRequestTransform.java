package gov.nist.asbestos.mhd.translation.search

import org.apache.http.HttpRequest
import org.apache.http.RequestLine
import org.apache.http.entity.BasicHttpEntity
import org.apache.http.message.BasicHttpEntityEnclosingRequest

import java.net.URI;
import java.util.Map;

// the endpoint transform must be run before this one
class MhdToSQRequestTransform {
    @Override
    HttpRequest run(SimProxyBase base, BasicHttpEntityEnclosingRequest request) {
        logger.info('Running MhdToSQRequestTransform')
        URI uri = new URI(base.uri)
        def query = uri.query
        if (!query)
            throw new SimProxyTransformException("Query is null")
        Map sqModel = new FhirSq().fhirQueryToSQModel(query)
        sqAsHttpRequest(base, request, sqModel)
    }

    static HttpRequest sqAsHttpRequest(URI uri, HttpRequest request, Map sqModel) {
        RequestLine requestLine = request.requestLine
        BasicHttpEntityEnclosingRequest newRequest = new BasicHttpEntityEnclosingRequest(requestLine.method, requestLine.uri, requestLine.protocolVersion)
        String endpoint = base.targetEndpoint
        String service = uri.path
        String host = uri.host
        if (host == null)
            host = 'localhost'
        String port = Integer.toString(uri.port)
        if (port == '-1')
            port = 80  // hope nobody checks!


        String sq = FhirSq.toXml(sqModel, true)
        String action = "urn:ihe:iti:2007:RegistryStoredQuery";
        String body = Query.metadataInSoapWrapper(endpoint, action, sq);
        String headers = Query.header(service, host, port, action);
        BasicHttpEntity entity = new BasicHttpEntity()
        entity.setContent(Io.bytesToInputStream(body.bytes))
        headers.split('\n').each { String hdr ->
            if (hdr.contains(':')) {
                def (name, value) = hdr.split(':', 2)
                newRequest.addHeader(name, value)
            }
        }
        newRequest.setEntity(entity)
        return newRequest
    }

}
