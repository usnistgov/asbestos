package gov.nist.asbestos.mhd.transactionSupport;

import gov.nist.asbestos.client.events.ITask;
import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.http.operations.Verb;
import gov.nist.asbestos.utilities.*;
import ihe.iti.xds_b._2007.RetrieveDocumentSetRequestType;
import ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.IdentifiableType;
import org.apache.http.client.ClientProtocolException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class RetrieveSender {
    private String requestBody = null;
    private ITask task;
    private Headers requestHeaders = new Headers();
    private String responseText = null;
    private RegErrorList errorList = new RegErrorList();
    private List<IdentifiableType> contents = new ArrayList<>();

    private String contentType = null;
    private String content = null;

    public String getContent() {
        return content;
    }

    public String getContentType() {
        return contentType;
    }

    public RetrieveContent send(RetrieveDocumentSetRequestType requestType, URI toAddr, ITask task) {
        ByteArrayOutputStream queryStream = new ByteArrayOutputStream();
        new RetrieveBuilder().toOutputStream(requestType, queryStream);

        String queryString1 = XmlTools.deleteXMLInstruction(new String(queryStream.toByteArray()));
        String queryString = XmlTools.deleteQueryExpression(queryString1);
        String soapString = RetWrapper.wrap(toAddr.toString(), queryString);
        return send(soapString, toAddr.toString(), task);
    }

    private static String getBoundary() {
        return "alksdjflkdjfslkdfjslkdjfslkdjf";
    }

    private static String getMTOMContentType() {
        return "multipart/related; type=\"application/xop+xml\"; boundary=\"" + getBoundary() + "\"; " +
                "action=\"urn:ihe:iti:2007:RetrieveDocumentSet\"";
    }

    private RetrieveContent send(String body, String toAddr, ITask task) {
        this.requestBody = body;
        this.task = task;
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try {
                MultipartSender.getMultipartEntity(body).writeTo(os);
            } catch (IOException e) {
                //
            }

            String output = os.toString();
            HttpPost poster = new HttpPost();
            poster.setRequestText(output);

            task.putRequestBodyText(output);
            poster.setUri(new URI(toAddr));
            requestHeaders.add(new Header("Content-Type", getMTOMContentType()));
            requestHeaders.setVerb(Verb.POST.name());
            requestHeaders.setPathInfo(new URI(toAddr));
            poster.setRequestHeaders(requestHeaders);
//            poster.setRequestContentType("application/soap+xml"); // won't override
            task.putRequestHeader(requestHeaders);
            poster.post();
            int status = poster.getStatus();
            responseText = poster.getResponseText();
            task.putResponseBodyText(responseText);
            task.putResponseHeader(poster.getResponseHeaders());
            if (status == 200) {
//                String ahqr = FaultParser.extractAdhocQueryResponse(responseText);
//                if (ahqr == null) {
//                    String faultReason = FaultParser.parse(responseText);
//                    errorList.getList().add(new RegError(faultReason, ErrorType.Error));
//                    return;
//                }
                List<String> parts = FaultParser.unwrapParts(responseText);

                if (parts.size() == 3) {
                    // 0 - Envelope
                    // 1 - content
                    // 2 - content type
                    content = parts.get(1);
                    contentType = parts.get(2);
                } else if (parts.size() == 2) {
                    contentType = parts.get(1);
                }

//                ByteArrayInputStream is = new ByteArrayInputStream(responseText.getBytes());
//                RetrieveDocumentSetResponseType response = new RetrieveResponseBuilder().fromInputStream(is);
                return new RetrieveContent(contentType, content == null ? "".getBytes() : content.getBytes());
//                AdhocQueryResponseBuilder builder = new AdhocQueryResponseBuilder();
//                AdhocQueryResponse response = builder.fromInputStream(is);
//                RegErrorList errorList = AdhocQueryResponseBuilder.asErrorList(response);
//                if (errorList.hasErrors()) {
//                    this.errorList = errorList;
//                    return;
//                }
//                RegistryObjectListType rol = AdhocQueryResponseBuilder.getRegistryObjectList(response);
//                List<JAXBElement<? extends IdentifiableType>> eles = rol.getIdentifiable();
//                for (JAXBElement ele : eles) {
//                    contents.add((IdentifiableType) ele.getValue());
//                }
//                return;
//
            } else
                throw new ClientProtocolException("Unexpected status: " + status );
        } catch (Exception t) {
            throw new Error("Send to " + toAddr + " + failed - \n" + t.getMessage(), t);
        }
    }

}
