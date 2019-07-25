package gov.nist.asbestos.asbestosProxy.parser;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlsoap.schemas.soap.envelope.Envelope;

import java.io.IOException;
import java.io.StringReader;

public class FaultParser {

    public static String parse(String xml) throws IOException, SAXException {
        DOMParser dp = new DOMParser();
        dp.parse(new InputSource(new StringReader(xml)));
        Document document = dp.getDocument();
        //Element envelope = document.getDocumentElement();
        NodeList top = document.getChildNodes();
        if (top.getLength() == 1 ) {
            Node envelope = top.item(0);
            if ("Envelope".equals(envelope.getLocalName())) {
                Node body;
                for (body = envelope.getFirstChild(); !"Body".equals(body.getLocalName()); body = body.getNextSibling()) {
                }
                if (body == null)
                    return null;
                Node fault;
                for (fault=body.getFirstChild(); !"Fault".equals(fault.getLocalName()); fault=fault.getNextSibling()) {
                }
                if (fault == null)
                    return null;
                Node reason;
                for (reason=fault.getFirstChild(); !"Reason".equals(reason.getLocalName()); reason=reason.getNextSibling()) {
                }
                if (reason == null)
                    return null;
                return reason.getTextContent();
            }
        }
        return null;
    }
}
