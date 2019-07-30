package gov.nist.asbestos.asbestosProxy.parser;

import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.utilities.AdhocQueryResponseBuilder;
import gov.nist.asbestos.utilities.RegErrorList;
import jdk.internal.org.objectweb.asm.tree.analysis.Value;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryResponse;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.IdentifiableType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectListType;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType;
import org.apache.commons.lang3.exception.ExceptionUtils;
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

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.List;

public class AhqrSender {
    private RegErrorList errorList = new RegErrorList();

    public boolean send(String body, String toAddr) {
        //String soapString = AdhocQueryWrapper.wrap(toAddr, body);

        try {
            HttpPost poster = new HttpPost();
            poster.setRequestText(body);
            poster.setUri(new URI(toAddr));
            poster.setRequestContentType("application/soap+xml");
            poster.post();
            int status = poster.getStatus();
            if (status == 200) {
                String responseText = poster.getResponseText();
                String ahqr = FaultParser.extractAdhocQueryResponse(responseText);
                if (ahqr == null) {
                    String faultReason = FaultParser.parse(responseText);
                    throw new Error("Send to " + toAddr + " + failed - \n" + faultReason);
                }
                ByteArrayInputStream is = new ByteArrayInputStream(ahqr.getBytes());
                AdhocQueryResponseBuilder builder = new AdhocQueryResponseBuilder();
                AdhocQueryResponse response = builder.fromInputStream(is);
                RegErrorList errorList = AdhocQueryResponseBuilder.asErrorList(response);
                if (errorList.hasErrors()) {
                    this.errorList = errorList;
                    return false;
                } else {
                    RegistryObjectListType rol = AdhocQueryResponseBuilder.getRegistryObjectList(response);
                    List<JAXBElement<? extends IdentifiableType>> eles = rol.getIdentifiable();
                    IdentifiableType value = eles.get(0).getValue();
                    String type = value.getClass().getSimpleName();
                    System.out.println(type);
                }
                return true;
            } else
                throw new ClientProtocolException("Unexpected status: " + status );
        } catch (Exception t) {
            throw new Error("Send to " + toAddr + " + failed - \n" + t.getMessage(), t);
        }
    }
}
