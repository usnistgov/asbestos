package gov.nist.asbestos.utilities;

import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryError;
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

public class MultipartSender {

    public static RegErrorList send(String body, String toAddr) {
        String soapString = PnrWrapper.wrap(toAddr, body);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String boundary = "alksdjflkdjfslkdfjslkdjfslkdjf";
            MultipartEntity mp = new MultipartEntity(null, boundary, null);

            ByteArrayBody bab = new ByteArrayBody(soapString.getBytes(), ContentType.parse("application/xop+xml"), "foo");
            FormBodyPart fbp = new FormBodyPart("content", bab);
            fbp.addField("Content-ID", "<0.1.1.1@example.org>");
            mp.addPart(fbp);

            RequestBuilder builder = RequestBuilder
                    .post()
                    .setUri(toAddr)
                    .setEntity(mp);

            String contentType = "multipart/related; type=\"application/xop+xml\"; boundary=\"" + boundary + "\";" +
                    "action=\"urn:ihe:iti:2007:ProvideAndRegisterDocumentSet-b\"";

            HttpUriRequest request = builder.build();

            request.setHeader("Content-type", contentType);

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
            String registryResponse = extractRegistryResponse(responseBody);
            System.out.println(registryResponse);
            RegistryResponseType rrt = new RegistryResponseBuilder().fromInputStream(new ByteArrayInputStream(registryResponse.getBytes()));
            return asErrorList(rrt);
        } catch (IOException | JAXBException e) {
            throw new Error("Send to " + toAddr + " + failed - \n" + e.getMessage(), e);
        }
    }

    private static RegErrorList asErrorList(RegistryResponseType registryResponseType) {
        RegErrorList list = new RegErrorList();

        for (RegistryError rError: registryResponseType.getRegistryErrorList().getRegistryError()) {
            list.getList().add(new RegError(rError.getCodeContext(), rError.getSeverity().endsWith("Warning") ? ErrorType.Warning : ErrorType.Error));
        }

        return list;
    }

    private static String extractRegistryResponse(String in) {
        int start = in.indexOf("RegistryResponse");
        if (start == -1)
            return null;
        while (in.charAt(start) != '<') start--;
        int end = in.indexOf("RegistryResponse", start+20);
        if (end == -1)
            return null;
        while(in.charAt(end) != '>') end++;
        return in.substring(start, end+1);
    }



}
