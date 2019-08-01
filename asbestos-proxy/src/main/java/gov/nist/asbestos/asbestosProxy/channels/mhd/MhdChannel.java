package gov.nist.asbestos.asbestosProxy.channels.mhd;

import gov.nist.asbestos.asbestosProxy.channel.BaseChannel;
import gov.nist.asbestos.client.events.Event;
import gov.nist.asbestos.client.events.EventStore;
import gov.nist.asbestos.asbestosProxy.parser.AhqrSender;
import gov.nist.asbestos.asbestosProxy.parser.FaultParser;
import gov.nist.asbestos.asbestosProxy.util.XdsActorMapper;
import gov.nist.asbestos.asbestosProxy.wrapper.TransformException;
import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.resolver.IdBuilder;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceCacheMgr;
import gov.nist.asbestos.client.resolver.ResourceMgr;
import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.*;
import gov.nist.asbestos.mhd.SubmittedObject;
import gov.nist.asbestos.mhd.transactionSupport.AdhocQueryBuilder;
import gov.nist.asbestos.mhd.transactionSupport.AssigningAuthorities;
import gov.nist.asbestos.mhd.transactionSupport.CodeTranslator;
import gov.nist.asbestos.mhd.transactionSupport.ProvideAndRegisterBuilder;
import gov.nist.asbestos.mhd.translation.BundleToRegistryObjectList;
import gov.nist.asbestos.mhd.translation.ContainedIdAllocator;
import gov.nist.asbestos.mhd.translation.DocumentEntryToDocumentReference;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.simapi.tk.installation.Installation;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.utilities.*;
import ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;
import oasis.names.tc.ebxml_regrep.xsd.lcm._3.SubmitObjectsRequest;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryRequest;
import oasis.names.tc.ebxml_regrep.xsd.query._3.ResponseOptionType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.*;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class MhdChannel extends BaseChannel /*implements IBaseChannel*/ {
    private ChannelConfig channelConfig = null;
    private Bundle requestBundle = null;
    private String serverBase;
    private String proxyBase;
    private BundleToRegistryObjectList bundleToRegistryObjectList = new BundleToRegistryObjectList();
    private AhqrSender sender = null;

    public MhdChannel() {}

    private String transformPDBToPNR(Bundle bundle, URI toAddr) {
        Objects.requireNonNull(getEvent());
        Val val = new Val();
        FhirClient fhirClient = new FhirClient();
        fhirClient.setResourceCacheMgr(new ResourceCacheMgr(getExternalCache()));

        ResourceMgr rMgr = new ResourceMgr();
        rMgr.setVal(val);
        rMgr.setFhirClient(fhirClient);
        rMgr.setEvent(getEvent());

        CodeTranslator codeTranslator;
        try {
            codeTranslator = new CodeTranslator(getCodesFile());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

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

        String pnrString = deleteXMLInstruction(new String(pnrStream.toByteArray()));
        String soapString = PnrWrapper.wrap(toAddr.toString(), pnrString);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            MultipartSender.getMultipartEntity(soapString).writeTo(os);
        } catch (IOException e) {
            //
        }
        return os.toString();
    }


    private AhqrSender documentEntryByUidQuery(String uid, URI toAddr)  {
        AdhocQueryRequest adhocQueryRequest = new AdhocQueryRequest();
        ResponseOptionType responseOptionType = new ResponseOptionType();
        responseOptionType.setReturnType("LeafClass");
        responseOptionType.setReturnComposedObjects(true);
        adhocQueryRequest.setResponseOption(responseOptionType);
        AdhocQueryType adhocQueryType = new AdhocQueryType();
        adhocQueryType.setId("urn:uuid:5c4f972b-d56b-40ac-a5fc-c8ca9b40b9d4");
        QueryExpressionType queryExpressionType = new QueryExpressionType();
        adhocQueryType.setQueryExpression(queryExpressionType);
        SlotType1 slot = new SlotType1();
        slot.setName("$XDSDocumentEntryUniqueId");
        ValueListType valueList = new ValueListType();
        valueList.getValue().add("('" + uid + "')");
        slot.setValueList(valueList);
        adhocQueryType.getSlot().add(slot);
        adhocQueryRequest.setAdhocQuery(adhocQueryType);

        ByteArrayOutputStream queryStream = new ByteArrayOutputStream();
        new AdhocQueryBuilder().toOutputStream(adhocQueryRequest, queryStream);

        String queryString1 = deleteXMLInstruction(new String(queryStream.toByteArray()));
        String queryString = deleteQueryExpression(queryString1);
        String soapString = AdhocQueryWrapper.wrap(toAddr.toString(), queryString);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        AhqrSender sender = new AhqrSender();
        sender.send(soapString, toAddr.toString());
        return sender;
    }

    private OperationOutcome regErrorListAsOperationOutcome(RegErrorList regErrorList) {
        OperationOutcome oo = new OperationOutcome();

        for (RegError regError : regErrorList.getList()) {
            OperationOutcome.OperationOutcomeIssueComponent issue = new OperationOutcome.OperationOutcomeIssueComponent();
            oo.addIssue(issue);
            issue.setDiagnostics(regError.getMsg());
            issue.setSeverity(OperationOutcome.IssueSeverity.ERROR);
        }

        return oo;
    }

    @Override
    public void transformRequest(HttpPost requestIn, HttpPost requestOut)  {
        Objects.requireNonNull(channelConfig);
        OperationOutcome oo = new OperationOutcome();
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
        requestBundle = bundle;

        URI toAddr = transformRequestUrl(null, requestIn);

        String soapString = transformPDBToPNR(bundle, toAddr);
        if (soapString == null) {
            // OperationOutcome is loaded with errors to return
            String response;
            if (isXml)
                response = ProxyBase.getFhirContext().newXmlParser().setPrettyPrint(true).encodeResourceToString(oo);
            else
                response = ProxyBase.getFhirContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(oo);
            throw new TransformException(response, isXml ? Format.XML : Format.JSON);
        }
        requestOut.setRequestText(soapString);
        requestOut.setRequestHeaders(new Headers().withContentType(MultipartSender.getContentType()));
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

    private static String deleteQueryExpression(String in) {
        StringBuilder buf = new StringBuilder();
        Scanner scanner = new Scanner(in);
        while(scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (!line.contains("<QueryExpression/>"))
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
        Ref ref = new Ref(requestIn.getRequestHeaders().getPathInfo());
        String resourceType = ref.getResourceType();
        if (resourceType == null)
            throw new Error("Cannot retrieve XDS contents for resource type " + resourceType);

        URI toAddr = transformRequestUrl(null, requestIn);

        String uid = ref.getId();
        if (resourceType.equals("DocumentReference")) {
            sender = documentEntryByUidQuery(uid, toAddr);
            requestOut.setRequestHeaders(sender.getRequestHeaders());
            requestOut.setRequestText(sender.getRequestBody());
            getEvent().getStore().putRequestHeader(sender.getRequestHeaders());
            getEvent().getStore().putRequestBodyText(sender.getRequestBody());
//        } else if (resourceType.equals("DocumentManifest")) {

        } else {
            throw new RuntimeException("GET " + resourceType + " not supported");
        }
    }

    private void returnOperationOutcome(HttpBase resp, Event event, OperationOutcome oo) {
        Headers responseHeaders = new Headers();
        event.getStore().putResponseHeader(responseHeaders);
        Format format = getReturnFormatType();
        String encoded = ProxyBase.encode(oo, format);
        resp.setResponseText(encoded);
        resp.getResponseHeaders().add(new Header("Content-Type", format.getContentType()));
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
        if (requestIn instanceof HttpPost) {
            String actorType;
            String transType;
            actorType = "rep";
            transType = "prb";

            return new XdsActorMapper().getEndpoint(channelConfig.getXdsSiteName(), actorType, transType, false);
        } else if (requestIn instanceof  HttpGet) {
            Ref ref = new Ref(requestIn.getRequestHeaders().getPathInfo());
            String actorType;
            String transType;
            String resourceType = ref.getResourceType();
            if (resourceType == null)
                throw new Error("Cannot retrieve XDS contents for resource type " + resourceType);

            if (resourceType.equals("DocumentReference") || resourceType.equals("DocumentManifest")) {
                actorType = "reg";
                transType = "sq";
            } else if (resourceType.equals("Binary")) {
                actorType = "rep";
                transType = "ret";
            } else {
                throw new Error("Cannot retrieve XDS contents for resource type " + resourceType);
            }

            return  new XdsActorMapper().getEndpoint(channelConfig.getXdsSiteName(), actorType, transType, false);
        }
        return null;
    }

    private void transformError(String msg, Throwable t, HttpBase responseOut) {
        transformError(msg + "\n" + ExceptionUtils.getStackTrace(t), responseOut);
    }

    private OperationOutcome wrapErrorInOperationOutcome(String msg) {
        OperationOutcome oo = new OperationOutcome();
        addErrorToOperationOutcome(oo, msg);
        return oo;
    }

    private OperationOutcome addErrorToOperationOutcome(OperationOutcome oo, String msg) {
        OperationOutcome.OperationOutcomeIssueComponent issue = oo.addIssue();
        issue.setCode(OperationOutcome.IssueType.UNKNOWN);
        issue.setSeverity(OperationOutcome.IssueSeverity.ERROR);
        issue.setDiagnostics(msg);
        return oo;
    }

    private void transformError(String message, HttpBase responseOut) {
        OperationOutcome oo = wrapErrorInOperationOutcome(message);
        packageResponse(responseOut, oo);
    }

    private Bundle bundleWith(List<Resource> in) {
        Bundle bundle = new Bundle();

        for (Resource resource : in) {
            Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
            bundle.addEntry(entry);
            if (resource instanceof OperationOutcome) {
                Bundle.BundleEntryResponseComponent resp = new Bundle.BundleEntryResponseComponent();
                resp.setStatus("500");
                resp.setOutcome(resource);
            } else {
                entry.setResource(resource);
                Bundle.BundleEntryResponseComponent resp = new Bundle.BundleEntryResponseComponent();
                entry.setResponse(resp);
                resp.setStatus("200");
            }
        }

        return bundle;
    }

    private String logResponse(String text) {
        getEvent().getStore().putResponseBodyText(text);
        getEvent().getStore().putResponseHeader(new Headers().withContentType(this.getReturnFormatType().getContentType()));
        return text;
    }

    @Override
    public void transformResponse(HttpBase responseIn, HttpBase responseOut, String proxyHostPort) {
        if (sender != null) {
            getEvent().getStore().putResponseBodyText(sender.getResponseText());
            if (responseOut.getVerb().equals(Verb.GET.toString())) {  // FHIR READ
                if (sender.hasErrors()) {
                    logResponse(sender.getResponseText());
                    OperationOutcome oo = regErrorListAsOperationOutcome(sender.getErrorList());
                    returnOperationOutcome(responseOut, getEvent(), oo);
                } else if (sender.getContents().size() == 1) {
                    Val val = new Val();
                    CodeTranslator codeTranslator;
                    try {
                        codeTranslator = new CodeTranslator(Installation.instance().getCodesFile(channelConfig.getEnvironment()));
                    } catch (Exception e) {
                        throw new RuntimeException("Cannot load codes file for environment " + channelConfig.getEnvironment(), e);
                    }
                    DocumentEntryToDocumentReference trans = new DocumentEntryToDocumentReference();
                    trans
                            .setContainedIdAllocator(new ContainedIdAllocator())
                            .setResourceMgr(new ResourceMgr().setFhirClient(new FhirClient()))
                            .setCodeTranslator(codeTranslator)
                            .setVal(val);
                    DocumentReference dr = trans.getDocumentReference((ExtrinsicObjectType) sender.getContents().get(0));
                    responseOut.setResponseText(logResponse(ProxyBase.encode(dr, returnFormatType)));
                    responseOut.setResponseContentType(returnFormatType.getContentType());
                } else if (sender.getContents().size() > 1){
                    OperationOutcome oo = wrapErrorInOperationOutcome("XDS Query returned " + sender.getContents().size() + " objects");
                    responseOut.setResponseText(logResponse(ProxyBase.encode(oo, returnFormatType)));
                    responseOut.setResponseContentType(returnFormatType.getContentType());
                } else { // no contents
                    responseOut.setResponseContentType(returnFormatType.getContentType());
                    responseOut.setStatus(404);
                }
            } else {
                if (sender.hasErrors()) {
                    OperationOutcome oo = regErrorListAsOperationOutcome(sender.getErrorList());
                    returnOperationOutcome(responseOut, getEvent(), oo);
                } else {
                    List<IdentifiableType> inContents = sender.getContents();
                    List<Resource> outContents = new ArrayList<>();
                    for (IdentifiableType it : inContents) {
                        if (it instanceof ExtrinsicObjectType) {
                            Val val = new Val();
                            DocumentEntryToDocumentReference trans = new DocumentEntryToDocumentReference();
                            trans
                                    .setContainedIdAllocator(new ContainedIdAllocator())
                                    .setVal(val);
                            DocumentReference dr = trans.getDocumentReference((ExtrinsicObjectType) it);
                            outContents.add(dr);
                        } else {
                            OperationOutcome oo = wrapErrorInOperationOutcome("Cannot transform " + it.getClass().getSimpleName() + " to FHIR");
                            outContents.add(oo);
                        }
                    }
                    Bundle bundle = bundleWith(outContents);
                }
            }
        } else {
            Objects.requireNonNull(requestBundle);
            OperationOutcome oo = new OperationOutcome();
            String responsePart = responseIn.getResponseText();
            String registryResponse = "";
            try {
                if (responsePart == null || responsePart.equals("")) {
                    transformError("Empty response from XDS.ProvideAndRegister", responseOut);
                    return;
                }
                String envelope = FaultParser.unwrapPart(responsePart);
                if (envelope == null || envelope.equals("")) {
                    transformError("Empty SOAP Envelope from XDS.ProvideAndRegister", responseOut);
                    return;
                }
                String faultMsg = FaultParser.parse(envelope);
                if (faultMsg != null && !faultMsg.equals("")) {
                    transformError(faultMsg, responseOut);
                    return;
                }
                registryResponse = FaultParser.extractRegistryResponse(envelope);
                if (registryResponse == null || registryResponse.equals("")) {
                    transformError("No RegistryResponse returned from XDS.ProvideAndRegister", responseOut);
                    return;
                }
            } catch (Throwable e) {
                transformError("Error processing RegistryResponse:\n" + responsePart + "\n", e, responseOut);
                return;
            }

            RegistryResponseType rrt;
            try {
                rrt = new RegistryResponseBuilder().fromInputStream(new ByteArrayInputStream(registryResponse.getBytes()));
            } catch (Exception e) {
                transformError(ExceptionUtils.getStackTrace(e), responseOut);
                return;
            }
            RegErrorList regErrorList = RegistryResponseBuilder.asErrorList(rrt);

            for (RegError re : regErrorList.getList()) {
                System.out.println(re.getSeverity() + " - " + re.getMsg());
            }
            List<RegError> lst = regErrorList.getList();
            List<String> raw = new ArrayList<>();

            for (RegError re : lst) {
                ErrorType errorType = re.getSeverity();
                String msg = trim(re.getMsg());
                if (raw.contains(msg))
                    continue;
                raw.add(msg);
                OperationOutcome.OperationOutcomeIssueComponent issue = oo.addIssue();
                issue.setCode(OperationOutcome.IssueType.UNKNOWN);
                issue.setDiagnostics(msg);
                issue.setSeverity(errorType == ErrorType.Error ? OperationOutcome.IssueSeverity.ERROR : OperationOutcome.IssueSeverity.WARNING);
            }
            packageResponse(
                    responseOut,
                    lst.isEmpty() ? null : oo
            );
        }
    }

    private boolean isWhite(char c) {
        if (c == ' ') return true;
        if (c == '\n') return true;
        if (c == '\t') return true;
        return false;
    }

    private String trim(String s) {
        if (s == null) return s;
        while (s.length() > 0 && isWhite(s.charAt(0))) s = s.substring(1);
        while (s.length() > 0 && isWhite(s.charAt(s.length()-1))) s = s.substring(0, s.length()-1);
        return s;
    }

    private void packageResponse(HttpBase responseOut, OperationOutcome oo) {
        Bundle response = new Bundle();
        response.setType(Bundle.BundleType.TRANSACTIONRESPONSE);
        boolean first = true;
        for (Bundle.BundleEntryComponent componentIn : requestBundle.getEntry()) {
            BaseResource resource = componentIn.getResource();
            SubmittedObject submittedObject = bundleToRegistryObjectList.findSubmittedObject(resource);
            Bundle.BundleEntryComponent componentOut = response.addEntry();
            Bundle.BundleEntryResponseComponent responseComponent = componentOut.getResponse();
            if (first && oo != null) {
                responseComponent.setStatus("400");
                responseComponent.setOutcome(oo);
            } else {
                responseComponent.setStatus("200");
                if (submittedObject != null) {
                    String url = proxyBase + "/" + resource.getClass().getSimpleName() + "/" + submittedObject.getUid();
                    responseComponent.setLocation(url);
                }
            }
            first = false;
        }
        if (returnFormatType == null)
            returnFormatType = Format.XML;
        responseOut.setResponseText(ProxyBase.encode(response, returnFormatType));
        responseOut.setResponseContentType(returnFormatType.getContentType());
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

    public void setReturnFormatType(Format returnFormatType) {
        this.returnFormatType = returnFormatType;
    }
}
