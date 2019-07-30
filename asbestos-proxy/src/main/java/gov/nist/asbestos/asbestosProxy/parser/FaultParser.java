package gov.nist.asbestos.asbestosProxy.parser;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlsoap.schemas.soap.envelope.Envelope;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.CharBuffer;
import java.util.Scanner;

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
                for (fault=body.getFirstChild(); fault != null && !"Fault".equals(fault.getLocalName()); fault=fault.getNextSibling()) {
                }
                if (fault == null)
                    return null;
                Node reason;
                for (reason=fault.getFirstChild(); reason != null && !"Reason".equals(reason.getLocalName()); reason=reason.getNextSibling()) {
                }
                if (reason == null)
                    return null;
                return reason.getTextContent();
            }
        }
        return null;
    }

    public static String extractRegistryResponse(String xml) throws IOException, SAXException {
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
                Node contents;
                for (contents = body.getFirstChild(); !"RegistryResponse".equals(contents.getLocalName()); contents = contents.getNextSibling()) {
                }
                if (contents != null)
                    return nodeToString(contents);
            }
        }
        return null;

    }

    public static String extractAdhocQueryResponse(String xml) throws IOException, SAXException {
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
                Node contents;
                for (contents = body.getFirstChild(); contents != null  && !"AdhocQueryResponse".equals(contents.getLocalName()); contents = contents.getNextSibling()) {
                }
                if (contents != null)
                    return nodeToString(contents);
            }
        }
        return null;

    }

    private static String nodeToString(Node node) {
        StringWriter sw = new StringWriter();
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.transform(new DOMSource(node), new StreamResult(sw));
        } catch (TransformerException te) {
            System.out.println("nodeToString Transformer Exception");
        }
        return sw.toString();
    }

    private enum State {
        BOUNDARY, HEADER, BODY, ENDBOUNDARY
    }

    public static String unwrapPart(String part) {
        part = part.trim();
        if (!part.startsWith("--"))
            return part;
        State state = State.BOUNDARY;
        String output = "";
        Scanner scanner = new Scanner(part);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (state == State.BOUNDARY && line.startsWith("--")) {
                state = State.HEADER;
                continue;
            }
            if (state == State.HEADER && line.equals("")) {
                state = State.BODY;
            }
            if (state == State.BODY && line.startsWith("--")) {
                state = State.ENDBOUNDARY;
            }
            if (state == State.BODY) {
                output += "\n" + line;
            }
            if (state == State.ENDBOUNDARY)
                break;
        }
        return trim(output);
    }

    static private String trimable = " \n";

    static public String trim(String in) {
        if (in.length() == 0) return in;
        while ( in.length() > 0 && trimable.indexOf(in.charAt(0)) != -1) {
            in = in.substring(1);
        }
        while (in.length() > 0 && trimable.indexOf(in.charAt(in.length()-1)) != -1) {
            in = in.substring(0, in.length()-1);
        }
        return in;
    }
}
