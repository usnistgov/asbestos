package gov.nist.asbestos.proxyWar;

import gov.nist.asbestos.utilities.MultipartSender;
import gov.nist.asbestos.utilities.PnrWrapper;
import gov.nist.asbestos.mhd.transactionSupport.ProvideAndRegisterBuilder;
import gov.nist.asbestos.utilities.RegError;
import gov.nist.asbestos.utilities.RegErrorList;
import ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;
import oasis.names.tc.ebxml_regrep.xsd.lcm._3.SubmitObjectsRequest;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.AssociationType1;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectListType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MultipartIT {
    private static String fhirPort;
    private static String proxyPort;

    @BeforeAll
    static void beforeAll() {
        fhirPort = ITConfig.getFhirPort();
        proxyPort = ITConfig.getProxyPort();
    }

    @Test
    void sendPnr() throws IOException {
        RegistryObjectListType registryObjectListType = new RegistryObjectListType();
        ExtrinsicObjectType extrinsicObjectType = new ExtrinsicObjectType();
        extrinsicObjectType.setId("Doc1");
        RegistryPackageType registryPackageType = new RegistryPackageType();
        registryPackageType.setId("SS");
        AssociationType1 associationType1 = new AssociationType1();
        associationType1.setAssociationType("HasMember");
        associationType1.setSourceObject("SS");
        associationType1.setTargetObject("Doc1");

        registryObjectListType.getIdentifiable().add(new JAXBElement<>(
                new QName("urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0", "ExtrinsicObject"),
                ExtrinsicObjectType.class,
                extrinsicObjectType));
        registryObjectListType.getIdentifiable().add(new JAXBElement<>(
                new QName("urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0", "RegistryPackage"),
                RegistryPackageType.class,
                registryPackageType));
        registryObjectListType.getIdentifiable().add(new JAXBElement<>(
                new QName("urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0", "Association"),
                AssociationType1.class,
                associationType1));

        ProvideAndRegisterDocumentSetRequestType pnr = new ProvideAndRegisterDocumentSetRequestType();
        SubmitObjectsRequest sor = new SubmitObjectsRequest();
        sor.setRegistryObjectList(registryObjectListType);
        pnr.setSubmitObjectsRequest(sor);

        byte[] document1Content = "Hello World!".getBytes();
        ProvideAndRegisterDocumentSetRequestType.Document document1 = new ProvideAndRegisterDocumentSetRequestType.Document();
        document1.setValue(document1Content);
        document1.setId("Doc1");
        pnr.getDocument().add(document1);

        ByteArrayOutputStream pnrStream = new ByteArrayOutputStream();
        new ProvideAndRegisterBuilder().toOutputStream(pnr, pnrStream);

        String toAddr = "http://localhost:8080/xdstools/sim/default__asbtsrr/rep/prb";

        String pnrString = deleteXMLInstruction(new String(pnrStream.toByteArray()));
        String soapString = PnrWrapper.wrap(toAddr, pnrString);
        System.out.println(soapString);

        RegErrorList regErrorList = MultipartSender.send(pnrString, toAddr);
        for (RegError re : regErrorList.getList()) {
            System.out.println(re.getSeverity() + " - " + re.getMsg());
        }
        //assertTrue(regErrorList.getList().isEmpty());
    }

    private static String deleteXMLInstruction(String in) {
        StringBuilder buf = new StringBuilder();
        Scanner scanner = new Scanner(in);
        while(scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (!line.startsWith("<?xml"))
                buf.append(line).append(("\n"));
        }
        scanner.close();
        return  buf.toString();
    }
}
