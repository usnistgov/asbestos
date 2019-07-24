package gov.nist.asbestos.asbestosProxy.channels.mhd;

import gov.nist.asbestos.asbestosProxy.channel.IBaseChannel;
import gov.nist.asbestos.asbestosProxy.events.EventStore;
import gov.nist.asbestos.asbestosProxy.parser.FaultParser;
import gov.nist.asbestos.asbestosProxy.util.XdsActorMapper;
import gov.nist.asbestos.asbestosProxy.wrapper.TransformException;
import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.resolver.IdBuilder;
import gov.nist.asbestos.client.resolver.ResourceCacheMgr;
import gov.nist.asbestos.client.resolver.ResourceMgr;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.http.operations.HttpDelete;
import gov.nist.asbestos.http.operations.HttpGet;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.mhd.transactionSupport.AssigningAuthorities;
import gov.nist.asbestos.mhd.transactionSupport.CodeTranslator;
import gov.nist.asbestos.mhd.transactionSupport.ProvideAndRegisterBuilder;
import gov.nist.asbestos.mhd.translation.BundleToRegistryObjectList;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.simapi.tk.installation.Installation;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
import gov.nist.asbestos.simapi.validation.ValErrors;
import gov.nist.asbestos.simapi.validation.ValWarnings;
import gov.nist.asbestos.utilities.*;
import ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;
import oasis.names.tc.ebxml_regrep.xsd.lcm._3.SubmitObjectsRequest;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectListType;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class MhdChannel implements IBaseChannel {
    private ChannelConfig channelConfig = null;
    private String serverBase;
    private String proxyBase;
    OperationOutcome oo = new OperationOutcome();

    private String transformPDBToPNR(Bundle bundle) {
        Val val = new Val();
        FhirClient fhirClient = new FhirClient();
        fhirClient.setResourceCacheMgr(new ResourceCacheMgr(getExternalCache()));

        ResourceMgr rMgr = new ResourceMgr();
        rMgr.setVal(val);
        rMgr.setFhirClient(fhirClient);

        CodeTranslator codeTranslator;
        try {
            codeTranslator = new CodeTranslator(getCodesFile());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        BundleToRegistryObjectList bundleToRegistryObjectList = new BundleToRegistryObjectList();
        bundleToRegistryObjectList.setVal(val);
        bundleToRegistryObjectList.setCodeTranslator(codeTranslator);
        bundleToRegistryObjectList.setResourceMgr(rMgr);
        bundleToRegistryObjectList.setAssigningAuthorities(AssigningAuthorities.allowAny());
        bundleToRegistryObjectList.setIdBuilder(new IdBuilder(true));

        // perform translation
        rMgr.setBundle(bundle);

        RegistryObjectListType registryObjectListType = bundleToRegistryObjectList.build(bundle);
        if (bundleToRegistryObjectList.isResponseHasError()) {
            throw new TransformException(ProxyBase
                    .getFhirContext()
                    .newXmlParser()
                    .setPrettyPrint(true)
                    .encodeResourceToString(
                            bundleToRegistryObjectList.getResponseBundle())
                            , Format.XML);
        }

//        if (val.hasWarnings()) {
//            for (ValE valE : new ValWarnings(val).getWarnings()) {
//                OperationOutcome.OperationOutcomeIssueComponent issue = oo.addIssue();
//                issue.setSeverity(OperationOutcome.IssueSeverity.WARNING);
//                issue.setCode(OperationOutcome.IssueType.UNKNOWN);
//                issue.setDiagnostics(valE.getMsg());
//            }
//        }
//
//        if (val.hasErrors()) {
//            for (ValE valE : new ValErrors(val).getErrors()) {
//                OperationOutcome.OperationOutcomeIssueComponent issue = oo.addIssue();
//                issue.setSeverity(OperationOutcome.IssueSeverity.ERROR);
//                issue.setCode(OperationOutcome.IssueType.UNKNOWN);
//                issue.setDiagnostics(valE.getMsg());
//            }
//
//            return null;
//        }


        ProvideAndRegisterDocumentSetRequestType pnr = new ProvideAndRegisterDocumentSetRequestType();
        SubmitObjectsRequest sor = new SubmitObjectsRequest();
        sor.setRegistryObjectList(registryObjectListType);
        pnr.setSubmitObjectsRequest(sor);

        for (String id : bundleToRegistryObjectList.getDocumentContents().keySet()) {
            byte[] contents = bundleToRegistryObjectList.getDocumentContents(id);
            ProvideAndRegisterDocumentSetRequestType.Document document1 = new ProvideAndRegisterDocumentSetRequestType.Document();
            document1.setValue(contents);
            document1.setId(id);
            pnr.getDocument().add(document1);
        }

        ByteArrayOutputStream pnrStream = new ByteArrayOutputStream();
        new ProvideAndRegisterBuilder().toOutputStream(pnr, pnrStream);

        return deleteXMLInstruction(new String(pnrStream.toByteArray()));
    }

    @Override
    public void transformRequest(HttpPost requestIn, HttpPost requestOut)  {
        Objects.requireNonNull(channelConfig);
        Headers headers  = requestIn.getRequestHeaders();
        byte[] request = requestIn.getRequest();
        String contentType = requestIn.getRequestContentType();
        IBaseResource resource;
        boolean isXml = true;
        if (contentType == null)
            throw new RuntimeException("No Content Type");
        if (contentType.startsWith("application/fhir+json")) {
            isXml = false;
            resource = ProxyBase.getFhirContext().newJsonParser().parseResource(new String(request));
        } else if (contentType.startsWith("application/fhir+xml")) {
            resource = ProxyBase.getFhirContext().newXmlParser().parseResource(new String(request));
        } else
            throw new RuntimeException("Do not understand Content-Type " + contentType);
        if (!(resource instanceof Bundle) )
            throw new RuntimeException("Expecting Bundle - got " + resource.getClass().getSimpleName());
        Bundle bundle = (Bundle) resource;

        String pnrString = transformPDBToPNR(bundle);
        if (pnrString == null) {
            // OperationOutcome is loaded with errors to return
            String response;
            if (isXml)
                response = ProxyBase.getFhirContext().newXmlParser().setPrettyPrint(true).encodeResourceToString(oo);
            else
                response = ProxyBase.getFhirContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(oo);
            throw new TransformException(response, isXml ? Format.XML : Format.JSON);
        }
        URI toAddr;
        try {
            toAddr = transformRequestUrl(null, requestIn); //channelConfig.translateEndpointToFhirBase(requestIn.getRequestHeaders().getPathInfo());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String soapString = PnrWrapper.wrap(toAddr.toString(), pnrString);

        requestOut.setRequestText(soapString);
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

    private static File getCodesFile() {
        return Installation.instance().getCodesFile("default");
    }

    private static File getExternalCache() {
        return Installation.instance().externalCache();
    }

    @Override
    public void transformRequest(HttpGet requestIn, HttpGet requestOut) {
        throw new RuntimeException("GET not supported on this channel");
    }

    @Override
    public void transformRequest(HttpDelete requestIn, HttpDelete requestOut) {
        throw new RuntimeException("DELETE not supported on this channel");
    }

    @Override
    public URI transformRequestUrl(String endpoint, HttpBase requestIn) {
        Objects.requireNonNull(channelConfig);
        if (channelConfig.getXdsSiteName() == null || channelConfig.getXdsSiteName().equals(""))
            throw new RuntimeException("ChannelConfig does not have XdsSiteName");
        String addr =  new XdsActorMapper().getEndpoint(channelConfig.getXdsSiteName(), "rep", "pnr", false);
        if (addr == null || addr.equals(""))
            throw new RuntimeException("XdsActorMapper cannot map site=" + channelConfig.getXdsSiteName() + " actorType=rep transactionType=pnr isTls=false");
        try {
            return new URI(addr);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void transformResponse(HttpBase responseIn, HttpBase responseOut, String proxyHostPort) {
        String responseBody = responseIn.getResponseText();
        String registryResponse = RegistryResponseExtractor.extractRegistryResponse(responseBody);
        if (registryResponse == null) {
            String faultReason = null;
            try {
                faultReason = FaultParser.parse(responseBody);
            } catch (Exception e) {
                OperationOutcome.OperationOutcomeIssueComponent issue = oo.addIssue();
                issue.setCode(OperationOutcome.IssueType.EXCEPTION);
                issue.setDiagnostics(ExceptionUtils.getStackTrace(e));
                return;
            }
            if (faultReason != null) {
                OperationOutcome.OperationOutcomeIssueComponent issue = oo.addIssue();
                issue.setCode(OperationOutcome.IssueType.EXCEPTION);
                issue.setDiagnostics(faultReason);
                return;
            }
            throw new RuntimeException("Registry Response does not parse");
        }
        RegistryResponseType rrt;
        try {
            rrt = new RegistryResponseBuilder().fromInputStream(new ByteArrayInputStream(registryResponse.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        RegErrorList regErrorList = RegistryResponseBuilder.asErrorList(rrt);

        for (RegError re : regErrorList.getList()) {
            System.out.println(re.getSeverity() + " - " + re.getMsg());
        }
        List<RegError> lst = regErrorList.getList();

        for (RegError re : lst) {
            ErrorType errorType = re.getSeverity();
            String msg = re.getMsg();
            OperationOutcome.OperationOutcomeIssueComponent issue = oo.addIssue();
            issue.setCode(OperationOutcome.IssueType.UNKNOWN);
            issue.setDiagnostics(msg);
            issue.setSeverity(errorType == ErrorType.Error ? OperationOutcome.IssueSeverity.ERROR : OperationOutcome.IssueSeverity.WARNING);
        }
    }

    @Override
    public void setServerBase(String serverBase) {
        this.serverBase = serverBase;
    }

    @Override
    public void setProxyBase(String proxyBase) {
        this.proxyBase = proxyBase;
    }

    @Override
    public void setup(ChannelConfig simConfig) {
        this.channelConfig = simConfig;
    }

    @Override
    public void teardown() {

    }

    @Override
    public void validateConfig(ChannelConfig simConfig) {

    }

    @Override
    public void handle(EventStore event) {

    }
}
