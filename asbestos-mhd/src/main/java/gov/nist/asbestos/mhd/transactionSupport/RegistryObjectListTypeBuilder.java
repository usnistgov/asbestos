package gov.nist.asbestos.mhd.transactionSupport;

import gov.nist.asbestos.asbestorCodesJaxb.Codes;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectListType;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.io.OutputStream;

public class RegistryObjectListTypeBuilder {
    private static Marshaller marshaller = null;
    private static Unmarshaller unmarshaller = null;

    static {
        try {
            JAXBContext context = JAXBContext.newInstance("path.to.package");
            marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

            unmarshaller = context.createUnmarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException("There was a problem creating a JAXBContext object for formatting the object to XML.");
        }
    }
    public RegistryObjectListType fromInputStream(InputStream is) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(RegistryObjectListType.class);
        return (RegistryObjectListType) unmarshaller.unmarshal(is);
    }

    public void toOutputStream(RegistryObjectListType rol, OutputStream os) {
        try {
            marshaller.marshal(rol, os);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }
}
