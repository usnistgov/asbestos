package gov.nist.asbestos.proxyTest;

import gov.nist.asbestos.mhd.transactionSupport.PnrWrapper;
import gov.nist.asbestos.mhd.transactionSupport.ProvideAndRegisterBuilder;
import gov.nist.asbestos.mhd.transactionSupport.RegistryObjectListTypeBuilder;
import ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;
import oasis.names.tc.ebxml_regrep.xsd.lcm._3.SubmitObjectsRequest;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.AssociationType1;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectListType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

class MultipartIT {
    private static String fhirPort;
    private static String proxyPort;

    @BeforeAll
    static void beforeAll() {
        fhirPort = System.getProperty("fhir.port", "8080");
        proxyPort = System.getProperty("proxy.port", "8081");
    }

    @Test
    void sendPnr() throws IOException {
        RegistryObjectListType registryObjectListType = new RegistryObjectListType();
        ExtrinsicObjectType extrinsicObjectType = new ExtrinsicObjectType();
        extrinsicObjectType.setId("Doc1");
        RegistryPackageType registryPackageType = new RegistryPackageType();
        registryPackageType.setId("SS");
        AssociationType1 associationType1 = new AssociationType1();
        associationType1.setAssociationType("HasMember");
        associationType1.setSourceObject("SS");
        associationType1.setTargetObject("Doc1");

        registryObjectListType.getIdentifiable().add(new JAXBElement<>(
                new QName("urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0", "ExtrinsicObject"),
                ExtrinsicObjectType.class,
                extrinsicObjectType));
        registryObjectListType.getIdentifiable().add(new JAXBElement<>(
                new QName("urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0", "RegistryPackage"),
                RegistryPackageType.class,
                registryPackageType));
        registryObjectListType.getIdentifiable().add(new JAXBElement<>(
                new QName("urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0", "Association"),
                AssociationType1.class,
                associationType1));

        ProvideAndRegisterDocumentSetRequestType pnr = new ProvideAndRegisterDocumentSetRequestType();
        SubmitObjectsRequest sor = new SubmitObjectsRequest();
        sor.setRegistryObjectList(registryObjectListType);
        pnr.setSubmitObjectsRequest(sor);

        byte[] document1Content = "Hello World!".getBytes();
        ProvideAndRegisterDocumentSetRequestType.Document document1 = new ProvideAndRegisterDocumentSetRequestType.Document();
        document1.setValue(document1Content);
        document1.setId("Doc1");
        pnr.getDocument().add(document1);

        ByteArrayOutputStream pnrStream = new ByteArrayOutputStream();
        new ProvideAndRegisterBuilder().toOutputStream(pnr, pnrStream);

        String toAddr = "http://localhost:8080/xdstools/sim/default__rr/rep/prb";

        String pnrString = deleteXMLInstruction(new String(pnrStream.toByteArray()));
        String soapString = PnrWrapper.wrap(toAddr, pnrString);

        //System.out.println(soapString);

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
        }
    }

    private String extractRegistryResponse(String in) {
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

    private String deleteXMLInstruction(String in) {
        StringBuilder buf = new StringBuilder();
        Scanner scanner = new Scanner(in);
        while(scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (!line.startsWith("<?xml"))
                buf.append(line).append(("\n"));
        }
        scanner.close();
        return  buf.toString();
    }
}
