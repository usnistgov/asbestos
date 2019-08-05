package gov.nist.asbestos.mhd.translation.it;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.resolver.IdBuilder;
import gov.nist.asbestos.client.resolver.ResourceCacheMgr;
import gov.nist.asbestos.client.resolver.ResourceMgr;
import gov.nist.asbestos.mhd.transactionSupport.AssigningAuthorities;
import gov.nist.asbestos.mhd.transactionSupport.CodeTranslator;
import gov.nist.asbestos.mhd.transactionSupport.ProvideAndRegisterBuilder;
import gov.nist.asbestos.mhd.transforms.BundleToRegistryObjectList;
import gov.nist.asbestos.mhd.translation.ContainedIdAllocator;
import gov.nist.asbestos.simapi.tk.installation.Installation;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValErrors;
import gov.nist.asbestos.simapi.validation.ValFactory;
import gov.nist.asbestos.simapi.validation.ValWarnings;
import gov.nist.asbestos.utilities.MultipartSender;
import gov.nist.asbestos.utilities.PnrWrapper;
import gov.nist.asbestos.utilities.RegError;
import gov.nist.asbestos.utilities.RegErrorList;
import ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;
import oasis.names.tc.ebxml_regrep.xsd.lcm._3.SubmitObjectsRequest;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.*;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class Pnr1IT {
    private static Val val;
    private static FhirContext fhirContext;
    private static ObjectMapper objectMapper;
    private static JsonFactory jsonFactory;
    private static File externalCache;
    private static ResourceMgr rMgr;
    private static FhirClient fhirClient;
    private static CodeTranslator codeTranslator;
    private BundleToRegistryObjectList bundleToRegistryObjectList;
    private ContainedIdAllocator containedIdAllocator;

    @BeforeAll
    static void beforeAll() throws URISyntaxException, FileNotFoundException, JAXBException {
        externalCache = Paths.get(Pnr1IT.class.getResource("/external_cache/findme.txt").toURI()).getParent().toFile();
        fhirContext = ProxyBase.getFhirContext();
        objectMapper = new  ObjectMapper();
        jsonFactory = objectMapper.getFactory();
        Installation.instance().setExternalCache(externalCache);
        codeTranslator = new CodeTranslator(getCodesFile());
    }

    @BeforeEach
    void beforeEach() {
        val = new Val();
        fhirClient = new FhirClient();
        fhirClient.setResourceCacheMgr(new ResourceCacheMgr(externalCache));

        containedIdAllocator = new ContainedIdAllocator();

        rMgr = new ResourceMgr();
        rMgr.setVal(val);
        rMgr.setFhirClient(fhirClient);

        bundleToRegistryObjectList = new BundleToRegistryObjectList();
        bundleToRegistryObjectList.setVal(val);
        bundleToRegistryObjectList.setCodeTranslator(codeTranslator);
        bundleToRegistryObjectList.setResourceMgr(rMgr);
        bundleToRegistryObjectList.setAssigningAuthorities(AssigningAuthorities.allowAny());
        bundleToRegistryObjectList.setIdBuilder(new IdBuilder(true));
    }

    private static File getCodesFile() {
        return Installation.instance().getCodesFile("default");
    }

    @Test
    void test1() {
        InputStream is = Pnr1IT.class.getResourceAsStream("/gov/nist/asbestos/mhd/translation/pnr1/bundle.xml");
        IBaseResource resource = ProxyBase.getFhirContext().newXmlParser().parseResource(is);
        assertTrue(resource instanceof Bundle);
        Bundle bundle = (Bundle) resource;

        rMgr.setBundle(bundle);

        RegistryObjectListType registryObjectListType = bundleToRegistryObjectList.build(bundle);

        if (val.hasErrors())
            fail(ValFactory.toJson(new ValErrors(val)));
        if (val.hasWarnings())
            fail(ValFactory.toJson(new ValWarnings(val)));

        boolean hasSubmissionSet = false;
        boolean hasDocumentEntry = false;
        boolean hasHasMember = false;
        String eoId = null;

        for (JAXBElement obj : registryObjectListType.getIdentifiable()) {
            IdentifiableType identifiableType = (IdentifiableType) obj.getValue();
            Class<?> theClass = identifiableType.getClass();
            if (theClass.equals(RegistryPackageType.class)) {
                hasSubmissionSet = true;
            }
            if (theClass.equals(ExtrinsicObjectType.class)) {
                hasDocumentEntry = true;
                ExtrinsicObjectType extrinsicObjectType = (ExtrinsicObjectType) identifiableType;
                String id = extrinsicObjectType.getId();
                eoId = id;
                byte[] content = bundleToRegistryObjectList.getDocumentContents(id);
                assertNotNull(content);
                assertEquals("foobar", new String(content));
            }
            if (theClass.equals(AssociationType1.class)) {
                hasHasMember = true;
                AssociationType1 associationType1 = (AssociationType1) identifiableType;
                assertTrue(associationType1.getAssociationType().endsWith("HasMember"));
            }
        }
        assertTrue(hasDocumentEntry);
        assertTrue(hasSubmissionSet);
        assertTrue(hasHasMember);

        ProvideAndRegisterDocumentSetRequestType pnr = new ProvideAndRegisterDocumentSetRequestType();
        SubmitObjectsRequest sor = new SubmitObjectsRequest();
        sor.setRegistryObjectList(registryObjectListType);
        pnr.setSubmitObjectsRequest(sor);

        byte[] document1Content = "Hello World!".getBytes();
        ProvideAndRegisterDocumentSetRequestType.Document document1 = new ProvideAndRegisterDocumentSetRequestType.Document();
        document1.setValue(document1Content);
        document1.setId(eoId);
        pnr.getDocument().add(document1);

        ByteArrayOutputStream pnrStream = new ByteArrayOutputStream();
        new ProvideAndRegisterBuilder().toOutputStream(pnr, pnrStream);

        String toAddr = "http://localhost:8080/xdstools/sim/default__rr/rep/prb";

        String pnrString = deleteXMLInstruction(new String(pnrStream.toByteArray()));
        String soapString = PnrWrapper.wrap(toAddr, pnrString);
        //System.out.println(soapString);

        RegErrorList regErrorList = MultipartSender.send(pnrString, toAddr);
        for (RegError re : regErrorList.getList()) {
            System.out.println(re.getSeverity() + " - " + re.getMsg());
        }
        List<RegError> lst = regErrorList.getList();
        assertTrue(lst.isEmpty());
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
