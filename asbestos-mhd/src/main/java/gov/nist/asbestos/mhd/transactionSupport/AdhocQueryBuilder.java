package gov.nist.asbestos.mhd.transactionSupport;

import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryRequest;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.io.OutputStream;

public class AdhocQueryBuilder {
    private static Marshaller marshaller;
    private static Unmarshaller unmarshaller;

    static {
        try {
            JAXBContext context = JAXBContext.newInstance(AdhocQueryRequest.class);
            marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

            unmarshaller = context.createUnmarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException("There was a problem creating a JAXBContext object for formatting the AdhocQueryRequest object to XML.");
        }
    }
    public AdhocQueryRequest fromInputStream(InputStream is) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(AdhocQueryRequest.class);
        return (AdhocQueryRequest) unmarshaller.unmarshal(is);
    }

    public void toOutputStream(AdhocQueryRequest rol, OutputStream os) {
        try {
            marshaller.marshal(rol, os);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }
}
