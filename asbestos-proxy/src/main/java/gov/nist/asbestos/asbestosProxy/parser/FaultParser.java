package gov.nist.asbestos.asbestosProxy.parser;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlsoap.schemas.soap.envelope.Envelope;

import java.io.IOException;

public class FaultParser {

    public static String parse(String in) throws IOException, SAXException {
        DOMParser dp = new DOMParser();
        dp.parse(in);
        Document document = dp.getDocument();
        Element envelope = document.getDocumentElement();
        if ("Envelope".equals(envelope.getLocalName())) {
            NodeList envelopeKids = envelope.getElementsByTagName("Body");
            if (envelopeKids.getLength() == 1) {
                Node body = envelopeKids.item(0);
                NodeList bodyKids = body.getChildNodes();
                for (int i=0; i<bodyKids.getLength(); i++) {
                    Node head = bodyKids.item(i);
                    if ("Fault".equals(head.getLocalName())) {
                        NodeList faultKids = head.getChildNodes();
                        for(int j=0; j<faultKids.getLength(); j++) {
                            Node faultPart = faultKids.item(j);
                            if ("Reason".equals(faultPart.getLocalName())) {
                                NodeList reasonKids = faultPart.getChildNodes();
                                for (int k=0; k<reasonKids.getLength(); k++) {
                                    Node reasonNode = reasonKids.item(k);
                                    String reason = reasonNode.getTextContent();
                                    return reason;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
