package gov.nist.asbestos.mhd.translation.it;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.events.NoOpTask;
import gov.nist.asbestos.client.resolver.IdBuilder;
import gov.nist.asbestos.client.resolver.ResourceCacheMgr;
import gov.nist.asbestos.client.resolver.ResourceMgr;
import gov.nist.asbestos.mhd.channel.CanonicalUriCodeEnum;
import gov.nist.asbestos.mhd.channel.MhdImplFactory;
import gov.nist.asbestos.mhd.channel.MhdProfileVersionInterface;
import gov.nist.asbestos.mhd.channel.MhdVersionEnum;
import gov.nist.asbestos.mhd.channel.UriCodeTypeEnum;
import gov.nist.asbestos.mhd.transactionSupport.AssigningAuthorities;
import gov.nist.asbestos.mhd.transactionSupport.CodeTranslator;
import gov.nist.asbestos.mhd.transactionSupport.ProvideAndRegisterBuilder;
import gov.nist.asbestos.mhd.transforms.BundleToRegistryObjectList;
import gov.nist.asbestos.mhd.transforms.MhdTransforms;
import gov.nist.asbestos.mhd.transforms.MhdV3xCanonicalUriCodes;
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
import oasis.names.tc.ebxml_regrep.xsd.rim._3.AssociationType1;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.IdentifiableType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectListType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
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
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
        fhirContext = ParserBase.getFhirContext();
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

        Map<CanonicalUriCodeEnum, String> m = new MhdV3xCanonicalUriCodes().getUriCodesByType(UriCodeTypeEnum.PROFILE);
        Map<CanonicalUriCodeEnum, String> myMap = m.entrySet().stream().filter(me -> me.getKey().equals(CanonicalUriCodeEnum.COMPREHENSIVE)).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        bundleToRegistryObjectList = new BundleToRegistryObjectList(null, myMap.entrySet().iterator().next());
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
        IBaseResource resource = ParserBase.getFhirContext().newXmlParser().parseResource(is);
        assertTrue(resource instanceof Bundle);
        Bundle bundle = (Bundle) resource;

        rMgr.setBundle(bundle);

        MhdTransforms mhdTransforms = new MhdTransforms(rMgr, val, new NoOpTask());

        MhdProfileVersionInterface mhdVersionSpecificImpl = MhdImplFactory.getImplementation(MhdVersionEnum.MHDv3x);

        RegistryObjectListType registryObjectListType = bundleToRegistryObjectList.build(mhdVersionSpecificImpl,  bundle);

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
