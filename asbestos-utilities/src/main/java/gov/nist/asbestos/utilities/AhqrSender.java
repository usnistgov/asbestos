package gov.nist.asbestos.utilities;

import gov.nist.asbestos.http.operations.HttpPost;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;

public class AhqrSender {

    public static RegErrorList send(String body, String toAddr) {
        String soapString = AdhocQueryWrapper.wrap(toAddr, body);

        HttpPost poster = new HttpPost();
        poster.setRequestText(body);
        poster.setUri(new URI(toAddr));
        poster.setRequestContentType("application/soap+xml");
        poster.post();
        int status = poster.getStatus();
        if (status == 200)

        } else {
            throw new Error("POST to " + toAddr + " returned " + status);
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            MultipartEntity mp = getMultipartEntity(soapString);

            RequestBuilder builder = RequestBuilder
                    .post()
                    .setUri(toAddr)
                    .setEntity(mp);

            HttpUriRequest request = builder.build();

            request.setHeader("Content-type", getContentType());

            ResponseHandler<String> responseHandler = response -> {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    return (entity != null) ?
                            EntityUtils.toString(entity) : null;
                } else {
                    throw new ClientProtocolException("Unexpected status: " + status );
                }
            };
            String responseBody = httpClient.execute(request, responseHandler);
            String registryResponse = RegistryResponseExtractor.extractRegistryResponse(responseBody);
            System.out.println(registryResponse);
            RegistryResponseType rrt = new RegistryResponseBuilder().fromInputStream(new ByteArrayInputStream(registryResponse.getBytes()));
            return RegistryResponseBuilder.asErrorList(rrt);
        } catch (IOException | JAXBException e) {
            throw new Error("Send to " + toAddr + " + failed - \n" + e.getMessage(), e);
        }
    }

    public static MultipartEntity getMultipartEntity(String soapString) {
        MultipartEntity mp = new MultipartEntity(null, getBoundary(), null);

        ByteArrayBody bab = new ByteArrayBody(soapString.getBytes(), ContentType.parse("application/xop+xml"), "foo");
        FormBodyPart fbp = new FormBodyPart("content", bab);
        fbp.addField("Content-ID", "<0.1.1.1@example.org>");
        mp.addPart(fbp);
        return mp;
    }

    public static String getContentType() {
        return "multipart/related; type=\"application/xop+xml\"; boundary=\"" + getBoundary() + "\"; " +
                "action=\"urn:ihe:iti:2007:ProvideAndRegisterDocumentSet-b\"";
    }

    private static String getBoundary() {
        return "alksdjflkdjfslkdfjslkdjfslkdjf";
    }

}
