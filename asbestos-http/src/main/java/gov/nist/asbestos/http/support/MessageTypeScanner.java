package gov.nist.asbestos.http.support;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Objects;

/**
 * Scan message and determine whether it is a SOAP message (and what its SOAP Action is
 * or a FHIR message
 */
public class MessageTypeScanner {
    String httpHeader;
    String httpBody;

    public MessageTypeScanner(String httpHeader, String httpBody) {
        Objects.requireNonNull(httpHeader);
        Objects.requireNonNull(httpBody);
        this.httpHeader = httpHeader;
        this.httpBody = httpBody.trim();
    }

    public MessageType scan() {
        if (httpBody.startsWith("{"))
            return new MessageType(MessageTechnology.FHIR, null);
        if (httpBody.startsWith("<"))
            return scanEnvelope(httpBody);
        if (httpBody.startsWith("--"))
            return scanMultipart();
        return null;
    }

    private static MessageType scanEnvelope(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setIgnoringElementContentWhitespace(true);
            factory.setIgnoringComments(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream is = new ByteArrayInputStream(xml.getBytes());
            Document doc = builder.parse(is);
            if (!doc.getDocumentElement().getLocalName().equals("Envelope"))
                return null;
            Node headerN = doc.getDocumentElement().getElementsByTagName("Header").item(0);
            String action = headerN.getChildNodes().item(0).getNodeValue();
            if (action != null) {
                return new MessageType(MessageTechnology.SOAP, action);
            }
        } catch (Exception e) {
            // ignored
        }
        return null;
    }

    private MessageType scanMultipart() {
        Multipart multipart = MultipartParser.parse(httpHeader, httpBody);
        Part startPart = multipart.getStartPart().get();

        return scanEnvelope(startPart.body);
    }
}
