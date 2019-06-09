package gov.nist.asbestos.mhd.transactions;


import gov.nist.asbestos.mhd.resolver.ResourceMgr;
import oasis.names.tc.ebxml_regrep.xsd.lcm._3.SubmitObjectsRequest;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectListType;
import org.junit.jupiter.api.Test;

import javax.xml.bind.*;
import javax.xml.namespace.QName;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AttributesTest {

    @Test
    void addSlot() throws JAXBException {
        ExtrinsicObjectType eo = new ExtrinsicObjectType();
        BundleToRegistryObjectList brol = new BundleToRegistryObjectList();

        brol.addSlot(eo,"foo", "bar");

        String eoString = toXml(eo);
        System.out.println(eoString);

        ExtrinsicObjectType eo2 = toEo(eoString);

        assertEquals("foo", eo2.getSlot().get(0).getName());
        assertEquals(1, eo.getSlot().get(0).getValueList().getValue().size());
        assertEquals("bar", eo2.getSlot().get(0).getValueList().getValue().get(0));

    }

    @Test
    void addSlot2() throws JAXBException {
        ExtrinsicObjectType eo = new ExtrinsicObjectType();
        BundleToRegistryObjectList brol = new BundleToRegistryObjectList();

        brol.addSlot(eo,"foo", Arrays.asList("bar", "xuy"));

        String eoString = toXml(eo);
        System.out.println(eoString);

        ExtrinsicObjectType eo2 = toEo(eoString);

        assertEquals("foo", eo2.getSlot().get(0).getName());
        assertEquals(2, eo.getSlot().get(0).getValueList().getValue().size());
        assertEquals("bar", eo2.getSlot().get(0).getValueList().getValue().get(0));
        assertEquals("xuy", eo2.getSlot().get(0).getValueList().getValue().get(1));
    }

    @Test
    void addName() throws JAXBException {
        ExtrinsicObjectType eo = new ExtrinsicObjectType();
        BundleToRegistryObjectList brol = new BundleToRegistryObjectList();

        brol.addName(eo, "MyName");

        String eoString = toXml(eo);
        System.out.println(eoString);

        ExtrinsicObjectType eo2 = toEo(eoString);

        assertEquals("MyName", eo2.getName().getLocalizedString().get(0).getValue());
    }

    @Test
    void createAssociation() throws JAXBException {
        ResourceMgr rMgr = new ResourceMgr();
        BundleToRegistryObjectList brol = new BundleToRegistryObjectList();
        ExtrinsicObjectType eo = new ExtrinsicObjectType();

        brol.createAssociation("HasMember", "id1", "id2", "name", Collections.singletonList("foo"));

        String eoString = toXml(eo);
        System.out.println(eoString);

        ExtrinsicObjectType eo2 = toEo(eoString);

        assertEquals("MyName", eo2.getName().getLocalizedString().get(0).getValue());

    }

    ExtrinsicObjectType toEo(String xml) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(SubmitObjectsRequest.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        SubmitObjectsRequest sor2 = (SubmitObjectsRequest) unmarshaller.unmarshal(new StringReader(xml));
        return ((ExtrinsicObjectType)((JAXBElement) sor2.getRegistryObjectList().getIdentifiable().get(0)).getValue());
    }


    String toXml(ExtrinsicObjectType eo) throws JAXBException {
        RegistryObjectListType rol = new RegistryObjectListType();
        rol.getIdentifiable().add(new JAXBElement<>(new QName("urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0", "ExtrinsicObject"), ExtrinsicObjectType.class, eo));

        SubmitObjectsRequest sor = new SubmitObjectsRequest();
        sor.setRegistryObjectList(rol);

        JAXBContext jaxbContext = JAXBContext.newInstance(SubmitObjectsRequest.class);
        Marshaller jaxbMarshaller   = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        StringWriter sw = new StringWriter();
        jaxbMarshaller.marshal(sor, sw);
        return sw.toString();
    }

}
