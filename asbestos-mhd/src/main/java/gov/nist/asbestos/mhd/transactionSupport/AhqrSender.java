package gov.nist.asbestos.mhd.transactionSupport;

import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.http.operations.Verb;
import gov.nist.asbestos.utilities.*;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryRequest;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryResponse;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.IdentifiableType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectListType;
import org.apache.http.client.ClientProtocolException;

import javax.xml.bind.JAXBElement;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class AhqrSender {
    private RegErrorList errorList = new RegErrorList();
    private List<IdentifiableType> contents = new ArrayList<>();
    private String requestBody = null;
    private Headers requestHeaders = new Headers();
    private String responseText = null;

    public void send(AdhocQueryRequest adhocQueryRequest, URI toAddr) {
        ByteArrayOutputStream queryStream = new ByteArrayOutputStream();
        new AdhocQueryBuilder().toOutputStream(adhocQueryRequest, queryStream);

        String queryString1 = XmlTools.deleteXMLInstruction(new String(queryStream.toByteArray()));
        String queryString = XmlTools.deleteQueryExpression(queryString1);
        String soapString = AdhocQueryWrapper.wrap(toAddr.toString(), queryString);
        send(soapString, toAddr.toString());
    }

    public void send(String body, String toAddr) {
        this.requestBody = body;
        try {
            HttpPost poster = new HttpPost();
            poster.setRequestText(body);
            poster.setUri(new URI(toAddr));
            poster.setRequestContentType("application/soap+xml");
            requestHeaders.add(new Header("Content-Type", "application/soap+xml"));
            poster.post();
            int status = poster.getStatus();
            requestHeaders.setVerb(Verb.POST.name());
            requestHeaders.setPathInfo(new URI(toAddr));
            if (status == 200) {
                responseText = poster.getResponseText();
                String ahqr = FaultParser.extractAdhocQueryResponse(responseText);
                if (ahqr == null) {
                    String faultReason = FaultParser.parse(responseText);
                    errorList.getList().add(new RegError(faultReason, ErrorType.Error));
                    return;
                }
                ByteArrayInputStream is = new ByteArrayInputStream(ahqr.getBytes());
                AdhocQueryResponseBuilder builder = new AdhocQueryResponseBuilder();
                AdhocQueryResponse response = builder.fromInputStream(is);
                RegErrorList errorList = AdhocQueryResponseBuilder.asErrorList(response);
                if (errorList.hasErrors()) {
                    this.errorList = errorList;
                    return;
                }
                RegistryObjectListType rol = AdhocQueryResponseBuilder.getRegistryObjectList(response);
                List<JAXBElement<? extends IdentifiableType>> eles = rol.getIdentifiable();
                for (JAXBElement ele : eles) {
                    contents.add((IdentifiableType) ele.getValue());
                }
                return;

            } else
                throw new ClientProtocolException("Unexpected status: " + status );
        } catch (Exception t) {
            throw new Error("Send to " + toAddr + " + failed - \n" + t.getMessage(), t);
        }
    }

    public RegErrorList getErrorList() {
        return errorList;
    }

    public boolean hasErrors() {
        return !errorList.getList().isEmpty();
    }

    public List<IdentifiableType> getContents() {
        return contents;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public Headers getRequestHeaders() {
        return requestHeaders;
    }

    public String getResponseText() {
        return responseText;
    }
}
