package gov.nist.asbestos.mhd.transactionSupport;

import ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;
import ihe.iti.xds_b._2007.RetrieveDocumentSetRequestType;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.io.OutputStream;

public class RetrieveBuilder {
    private static Marshaller marshaller;
    private static Unmarshaller unmarshaller;

    static {
        try {
            JAXBContext context = JAXBContext.newInstance(RetrieveDocumentSetRequestType.class);
            marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

            unmarshaller = context.createUnmarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException("There was a problem creating a JAXBContext object for formatting the RetrieveDocumentSetRequestType object to XML.");
        }
    }

    public RetrieveDocumentSetRequestType fromInputStream(InputStream is) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(RetrieveDocumentSetRequestType.class);
        return (RetrieveDocumentSetRequestType) unmarshaller.unmarshal(is);
    }

    public void toOutputStream(RetrieveDocumentSetRequestType rol, OutputStream os) {
        try {
            marshaller.marshal(rol, os);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }
}
