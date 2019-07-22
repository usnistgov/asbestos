package gov.nist.asbestos.utilities;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectListType;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryError;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryErrorList;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class RegistryResponseBuilder {
    private static Marshaller marshaller;
    private static Unmarshaller unmarshaller;

    static {
        try {
            JAXBContext context = JAXBContext.newInstance(RegistryResponseType.class);
            marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

            unmarshaller = context.createUnmarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException("There was a problem creating a JAXBContext object for formatting the RegistryObjectListType object to XML.");
        }
    }
    public RegistryResponseType fromInputStream(InputStream is) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(RegistryResponseType.class);
        return (RegistryResponseType) unmarshaller.unmarshal(is);
    }

    public void toOutputStream(RegistryResponseType rol, OutputStream os) {
        try {
            marshaller.marshal(rol, os);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public static RegErrorList asErrorList(RegistryResponseType registryResponseType) {
        RegErrorList list = new RegErrorList();

        RegistryErrorList registryErrorList = registryResponseType.getRegistryErrorList();
        if (registryErrorList != null) {
            List<RegistryError> registryErrors = registryErrorList.getRegistryError();
            if (registryErrors != null) {
                for (RegistryError rError : registryErrors) {
                    list.getList().add(new RegError(rError.getCodeContext(), rError.getSeverity().endsWith("Warning") ? ErrorType.Warning : ErrorType.Error));
                }
            }
        }

        return list;
    }
}
