package gov.nist.asbestos.asbestosProxy.channels.mhd;

import gov.nist.asbestos.asbestosProxy.channel.BaseChannel;
import gov.nist.asbestos.asbestosProxy.channels.passthrough.PassthroughChannel;
import gov.nist.asbestos.asbestosProxy.util.XdsActorMapper;
import gov.nist.asbestos.mhd.SubmittedObject;
import gov.nist.asbestos.mhd.exceptions.TransformException;
import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.events.Event;
import gov.nist.asbestos.client.events.ITask;
import gov.nist.asbestos.client.resolver.IdBuilder;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceCacheMgr;
import gov.nist.asbestos.client.resolver.ResourceMgr;
import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.*;
import gov.nist.asbestos.mhd.transactionSupport.*;
import gov.nist.asbestos.mhd.transforms.BundleToRegistryObjectList;
import gov.nist.asbestos.mhd.transforms.DocumentEntryToDocumentReference;
import gov.nist.asbestos.mhd.transforms.SubmissionSetToDocumentManifest;
import gov.nist.asbestos.mhd.translation.ContainedIdAllocator;
import gov.nist.asbestos.mhd.translation.search.FhirSq;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.simapi.tk.installation.Installation;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
import gov.nist.asbestos.utilities.*;
import ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;
import ihe.iti.xds_b._2007.RetrieveDocumentSetRequestType;
import ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType;
import oasis.names.tc.ebxml_regrep.xsd.lcm._3.SubmitObjectsRequest;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.*;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Attachment;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;

// TODO - honor the Prefer header - http://hl7.org/fhir/http.html#ops
public class XdsOnFhirChannel extends BaseChannel /*implements IBaseChannel*/ {
    private Bundle requestBundle = null;
    private String serverBase;
    private String proxyBase;
    private BundleToRegistryObjectList bundleToRegistryObjectList = new BundleToRegistryObjectList();
    private AhqrSender sender = null;
    private Binary binary = null;

    public XdsOnFhirChannel() {}

    private String transformPDBToPNR(Bundle bundle, URI toAddr, ITask task) {
        Objects.requireNonNull(task);
        Val val = new Val();

        FhirClient fhirClient = new FhirClient();
        ResourceCacheMgr resourceCacheMgr = new ResourceCacheMgr(getExternalCache());
        fhirClient.setResourceCacheMgr(resourceCacheMgr);

        ResourceMgr rMgr = new ResourceMgr();
        rMgr.setVal(val);
        rMgr.setFhirClient(fhirClient);
        rMgr.setTask(task);

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
            throw new TransformException(bundleToRegistryObjectList.getResponseBundle());
        }

        if (val.hasErrors()) {
            throw new TransformException(val.errorsAsOperationOutcome());
        }

        ProvideAndRegisterDocumentSetRequestType pnr = new ProvideAndRegisterDocumentSetRequestType();
        SubmitObjectsRequest sor = new SubmitObjectsRequest();
        sor.setRegistryObjectList(registryObjectListType);
        pnr.setSubmitObjectsRequest(sor);

        for (String id : bundleToRegistryObjectList.getDocumentContents().keySet()) {
            byte[] contents = bundleToRegistryObjectList.getDocumentContents(id);
            lastDocument = contents;
            String contentsAsString = new String(contents);
            lastDocumentStr = contentsAsString;
            String strContents = Base64.encodeBase64String(contents);
            ProvideAndRegisterDocumentSetRequestType.Document document1 = new ProvideAndRegisterDocumentSetRequestType.Document();
            document1.setValue(contents);
            document1.setId(id);
            pnr.getDocument().add(document1);
        }

        ByteArrayOutputStream pnrStream = new ByteArrayOutputStream();
        new ProvideAndRegisterBuilder().toOutputStream(pnr, pnrStream);

        String pnrString = XmlTools.deleteXMLInstruction(new String(pnrStream.toByteArray()));
        String soapString = PnrWrapper.wrap(toAddr.toString(), pnrString);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            MultipartSender.getMultipartEntity(soapString).writeTo(os);
        } catch (IOException e) {
            //
        }
        return os.toString();
    }

    public static byte[] lastDocument;
    public static String lastDocumentStr;

//    private String transformBinaryQueryToRetrieve(String docUid, String repUid, URI toAddr, ITask task) {
//        Objects.requireNonNull(task);
//
//        RetrieveDocumentSetRequestType ret = new RetrieveDocumentSetRequestType();
//        RetrieveDocumentSetRequestType.DocumentRequest docReq = new RetrieveDocumentSetRequestType.DocumentRequest();
//        docReq.setDocumentUniqueId(docUid);
//        docReq.setRepositoryUniqueId(repUid);
//        ret.getDocumentRequest().add(docReq);
//
//        ByteArrayOutputStream retStream = new ByteArrayOutputStream();
//        new RetrieveBuilder().toOutputStream(ret, retStream);
//
//        String retString = XmlTools.deleteXMLInstruction(new String(retStream.toByteArray()));
//        String soapString = RetWrapper.wrap(toAddr.toString(), retString);
//        ByteArrayOutputStream os = new ByteArrayOutputStream();
//        try {
//            MultipartSender.getMultipartEntity(soapString).writeTo(os);
//        } catch (IOException e) {
//            //
//        }
//        return os.toString();
//    }


    private AhqrSender documentEntryByUidQuery(String uid, URI toAddr, ITask task)  {
        Map<String, List<String>> model = new HashMap<>();
        model.put("$XDSDocumentEntryUniqueId", Collections.singletonList(uid));
        return FhirSq.run(model, "urn:uuid:5c4f972b-d56b-40ac-a5fc-c8ca9b40b9d4", toAddr, true, task);
    }

    private AhqrSender documentEntryByUUIDQuery(String uuid, URI toAddr, ITask task)  {
        Map<String, List<String>> model = new HashMap<>();
        model.put("$XDSDocumentEntryEntryUUID", Collections.singletonList(uuid));
        return FhirSq.run(model, "urn:uuid:5c4f972b-d56b-40ac-a5fc-c8ca9b40b9d4", toAddr, true, task);
    }

    private RetrieveContent binaryByUidRetrieve(String docUid, String repUid, URI toAddr, ITask task)  {
        RetrieveDocumentSetRequestType request = new RetrieveDocumentSetRequestType();
        RetrieveDocumentSetRequestType.DocumentRequest docRequest = new RetrieveDocumentSetRequestType.DocumentRequest();
        docRequest.setRepositoryUniqueId(repUid);
        docRequest.setDocumentUniqueId(docUid);
        request.getDocumentRequest().add(docRequest);

        return new RetrieveSender().send(request, toAddr, task);
    }

    private AhqrSender submissionSetByUidQuery(String uid, URI toAddr, ITask task)  {
        Map<String, List<String>> model = new HashMap<>();
        model.put("$XDSSubmissionSetUniqueId", Collections.singletonList(uid));
        AhqrSender sender = FhirSq.run(model, "urn:uuid:e8e3cb2c-e39c-46b9-99e4-c12f57260b83", toAddr, true, task);

        // query returns more than what we want - prune results

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

        URI toAddr = transformRequestUrl(null, requestIn);

        if (!(resource instanceof Bundle) ) {
            // forward to fhir server
            PassthroughChannel.passHeaders(requestIn, requestOut);
            requestOut.setRequest(requestIn.getRequest());
            return;
        }
        Bundle bundle = (Bundle) resource;
        requestBundle = bundle;


        String soapString = transformPDBToPNR(bundle, toAddr, getTask());
        if (soapString == null) {
            // OperationOutcome is loaded with errors to return
            throw new TransformException(oo);
        }
        requestOut.setRequestText(soapString);
        requestOut.setRequestHeaders(new Headers().withContentType(MultipartSender.getContentType()));
    }


    private static File getCodesFile() {
        return Installation.instance().getCodesFile("default");
    }

    private static File getExternalCache() {
        return Installation.instance().externalCache();
    }

    // TODO - expand support for search
    // TODO - add RetrieveDocument
    @Override
    public void transformRequest(HttpGet requestIn, HttpGet requestOut) {
        Ref ref = new Ref(requestIn.getRequestHeaders().getPathInfo());
        String resourceType = ref.getResourceType();
        if (resourceType == null)
            throw new Error("Cannot retrieve XDS contents for resource type " + resourceType);

        URI toAddr = transformRequestUrl(null, requestIn);

        String uid = ref.getId();
        if (uid.equals("")) {
            // SEARCH
            String params = ref.getParameters();
            if (params == null || params.equals(""))
                throw new RuntimeException("No search parameters - " + ref);
            if (resourceType.equals("DocumentReference")) {
                sender = FhirSq.docRefQuery(params, toAddr, task);
                returnAhqrResults(requestOut);
            } else {
                throw new RuntimeException("SEARCH " + resourceType + " not supported");
            }
        } else {
            // GET
            if (resourceType.equals("DocumentReference") && uid.contains(".")) {
                // by UID
                sender = documentEntryByUidQuery(uid, toAddr, task);
                returnAhqrResults(requestOut);
            } else if (resourceType.equals("DocumentReference") && uid.contains("-")) {
                // by UUID
                sender = documentEntryByUUIDQuery("urn:uuid:" + uid, toAddr, task);
                returnAhqrResults(requestOut);
            } else if (resourceType.equals("DocumentManifest")  && uid.contains(".")) {
                sender = submissionSetByUidQuery(uid, toAddr, task);
                returnAhqrResults(requestOut);
            } else if (resourceType.equalsIgnoreCase("Binary") && uid.contains(".")) {
                // by UUID
                String repUid = "1.1.1";
                RetrieveContent retrieveContent = binaryByUidRetrieve(uid, repUid, toAddr, task);
                String retrieveContentStr = new String(retrieveContent.getContent());
                binary = new Binary();
                binary.setId(uid);
//                RetrieveDocumentSetResponseType.DocumentResponse doc = response.getDocumentResponse().get(0);
                binary.setContentType(retrieveContent.getContentType());
                String lastDocumentAsStringLocal = lastDocumentStr;
                String asString = new String(retrieveContent.getContent());
                binary.setData(retrieveContent.getContent());
                String j;
                if (lastDocument != null)
                    compare(lastDocument,retrieveContent.getContent());
                j = "";
                //binary.setData(lastDocument);
            } else {
                throw new RuntimeException("GET " + resourceType + " not supported");
            }
        }
    }

    private int compare(byte[] send, byte[] retrieve) {
        int sendSize = send.length;
        int retSize = retrieve.length;
        int size = Math.min(sendSize, retSize);
        int i = 0;
        while (i < size) {
            if (send[i] !=  retrieve[i])
                break;
            i++;
        }
        return i;
    }

    private void returnAhqrResults(HttpGet requestOut) {
        requestOut.setRequestHeaders(sender.getRequestHeaders());
        requestOut.setRequestText(sender.getRequestBody());
    }

    private void returnOperationOutcome(HttpBase resp, OperationOutcome oo) {
        Headers responseHeaders = new Headers();
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
        Ref ref = new Ref(requestIn.getRequestHeaders().getPathInfo());
        String resourceType = ref.getResourceType();
        if (channelConfig.getXdsSiteName() == null || channelConfig.getXdsSiteName().equals(""))
            throw new RuntimeException("ChannelConfig does not have XdsSiteName");
        if (requestIn instanceof HttpPost) {
            if (resourceType.equals("")) {
                // transaction - send to mhd back end
                String actorType = "rep";
                String transType = "prb";
                return new XdsActorMapper().getEndpoint(channelConfig.getXdsSiteName(), actorType, transType, false);
            } else {
                // posting of resource - send to FHIR server
                Ref fhir = new Ref(channelConfig.getFhirBase());
                fhir = fhir.withResource(resourceType);
                return fhir.getUri();
            }
        } else if (requestIn instanceof  HttpGet) {
            String actorType;
            String transType;

            if (resourceType == null)
                throw new Error("Cannot retrieve XDS contents for resource type " + resourceType);

            if (resourceType.equals("DocumentReference") || resourceType.equals("DocumentManifest")) {
                actorType = "reg";
                transType = "sq";
            } else if (resourceType.equals("Binary")) {
                actorType = "rep";
                transType = "ret";
            } else {
                return new Ref(channelConfig.getFhirBase()).withResource(resourceType).getUri();
            }

            return  new XdsActorMapper().getEndpoint(channelConfig.getXdsSiteName(), actorType, transType, false);
        }
        return null;
    }

    private void returnErrorInOperationOutcome(String msg, Throwable t, HttpBase responseOut) {
        returnErrorInOperationOutcome(msg + "\n" + ExceptionUtils.getStackTrace(t), responseOut);
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

    private void returnErrorInOperationOutcome(String message, HttpBase responseOut) {
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

    public static void transferHeadersFromResponse(HttpBase responseIn, HttpBase responseOut) {
        List<String> exclude = Arrays.asList("transfer-encoding", "x-powered-by", "content-length");
        Headers inHeaders = responseIn.getResponseHeaders();
        Headers outHeaders = responseOut.getResponseHeaders();

        for (Header header : inHeaders.getHeaders()) {
            if (exclude.contains(header.getName().toLowerCase()))
                continue;
            if (!outHeaders.hasHeader(header.getName()))
                outHeaders.add(new Header(header.getName(), header.getValue()));
        }
    }

    @Override
    public void transformResponse(HttpBase responseIn, HttpBase responseOut, String proxyHostPort, String requestedType) {
        if (sender != null) {
            // there is a query response to transform
            transformDSResponse(sender, responseOut, requestedType);
            return;
        }
        if (requestedType != null && requestedType.equals("Binary")) {
            if (binary != null) {
                responseOut.setStatus(200);;
                responseOut.setResponseContentType(returnFormatType.getContentType());
                responseOut.setResponse(ProxyBase.encode(binary, returnFormatType).getBytes());
                return;
            }
        }
        transferHeadersFromResponse(responseIn, responseOut);
        if (requestBundle == null) {
            String resourceText = responseIn.getResponseText();
            responseOut.setResponseText(resourceText);
            responseOut.setResponseContentType(responseIn.getResponseContentType());
        } else {
            OperationOutcome oo = new OperationOutcome();
            if (responseIn.getStatus() >= 400) {
                returnErrorInOperationOutcome("XDS.ProvideAndRegister returned status " + responseIn.getStatus(), responseOut);
                return;
            }
            String responsePart = responseIn.getResponseText();
            String registryResponse = "";
            try {
                if (responsePart == null || responsePart.equals("")) {
                    returnErrorInOperationOutcome("Empty response from XDS.ProvideAndRegister", responseOut);
                    return;
                }
                String envelope = FaultParser.unwrapPart(responsePart);
                if (envelope == null || envelope.equals("")) {
                    returnErrorInOperationOutcome("Empty SOAP Envelope from XDS.ProvideAndRegister", responseOut);
                    return;
                }
                String faultMsg = FaultParser.parse(envelope);
                if (faultMsg != null && !faultMsg.equals("")) {
                    returnErrorInOperationOutcome(faultMsg, responseOut);
                    return;
                }
                registryResponse = FaultParser.extractRegistryResponse(envelope);
                if (registryResponse == null || registryResponse.equals("")) {
                    returnErrorInOperationOutcome("No RegistryResponse returned from XDS.ProvideAndRegister", responseOut);
                    return;
                }
            } catch (Throwable e) {
                returnErrorInOperationOutcome("Error processing RegistryResponse:\n" + responsePart + "\n", e, responseOut);
                return;
            }

            RegistryResponseType rrt;
            try {
                rrt = new RegistryResponseBuilder().fromInputStream(new ByteArrayInputStream(registryResponse.getBytes()));
            } catch (Exception e) {
                returnErrorInOperationOutcome(ExceptionUtils.getStackTrace(e), responseOut);
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

    private void transformDSResponse(AhqrSender sender, HttpBase responseOut, String requestedType) {
        if (responseOut.getVerb().equals(Verb.GET.toString())) {  // FHIR READ
            if (sender.hasErrors()) {
                OperationOutcome oo = regErrorListAsOperationOutcome(sender.getErrorList());
                returnOperationOutcome(responseOut, oo);
            } else if (sender.getContents().size() == 1 || requestedType != null) {
                Val val = new Val();
                CodeTranslator codeTranslator;
                try {
                    codeTranslator = new CodeTranslator(Installation.instance().getCodesFile(channelConfig.getEnvironment()));
                } catch (Exception e) {
                    throw new RuntimeException("Cannot load codes file for environment " + channelConfig.getEnvironment(), e);
                }
                ResourceCacheMgr resourceCacheMgr = new ResourceCacheMgr(getExternalCache());
                FhirClient fhirClient = new FhirClient()
                        .setResourceCacheMgr(resourceCacheMgr);
                if (requestedType.equals("DocumentReference")) {
                    DocumentEntryToDocumentReference trans = new DocumentEntryToDocumentReference();
                    trans
                            .setContainedIdAllocator(new ContainedIdAllocator())
                            .setResourceCacheMgr(resourceCacheMgr)
                            .setCodeTranslator(codeTranslator)
                            .setFhirClient(fhirClient)
                            .setVal(val);

                    DocumentReference dr = trans.getDocumentReference((ExtrinsicObjectType) sender.getContents().get(0), channelConfig);

                    if (val.hasErrors()) {
                        OperationOutcome oo = new OperationOutcome();
                        for (ValE err : val.getErrors())
                            addErrorToOperationOutcome(oo, err.getMsg());
                        returnOperationOutcome(responseOut, oo);
                        return;
                    }


                    responseOut.setResponseText(ProxyBase.encode(dr, returnFormatType));
                    responseOut.setResponseContentType(returnFormatType.getContentType());
                } else if (requestedType.equals("DocumentManifest")) {
                    SubmissionSetToDocumentManifest trans = new SubmissionSetToDocumentManifest();
                    trans
                            .setContainedIdAllocator(new ContainedIdAllocator())
                            .setResourceCacheMgr(resourceCacheMgr)
                            .setCodeTranslator(codeTranslator)
                            .setFhirClient(fhirClient)
                            .setVal(val);

                    RegistryPackageType ss = null;
                    List<AssociationType1> assocs = new ArrayList<>();
                    for (IdentifiableType identifiableType : sender.getContents()) {
                        if (identifiableType instanceof RegistryPackageType) {
                            RegistryPackageType rpt = (RegistryPackageType) identifiableType;
                            for (ClassificationType classificationType : rpt.getClassification()) {
                                if ("urn:uuid:a54d6aa5-d40d-43f9-88c5-b4633d873bdd".equals(classificationType.getClassificationNode()) ) {
                                    ss = rpt;
                                }
                            }
                        } else if (identifiableType instanceof AssociationType1) {
                            assocs.add((AssociationType1) identifiableType);
                        }
                    }

                    DocumentManifest dm = null;

                    if (ss == null) {
                        val.add(new ValE("No SubmissionSet in query response.").asError());
                    } else {
                        dm = trans.getDocumentManifest(ss, assocs, channelConfig);
                    }

                    if (val.hasErrors()) {
                        OperationOutcome oo = new OperationOutcome();
                        for (ValE err : val.getErrors())
                            addErrorToOperationOutcome(oo, err.getMsg());
                        returnOperationOutcome(responseOut, oo);
                        return;
                    }


                    responseOut.setResponseText(ProxyBase.encode(dm, returnFormatType));
                    responseOut.setResponseContentType(returnFormatType.getContentType());
                }

            } else if (sender.getContents().size() > 1){
                OperationOutcome oo = wrapErrorInOperationOutcome("XDS Query returned " + sender.getContents().size() + " objects");
                responseOut.setResponseText(ProxyBase.encode(oo, returnFormatType));
                responseOut.setResponseContentType(returnFormatType.getContentType());
            } else { // no contents
                responseOut.setResponseContentType(returnFormatType.getContentType());
                responseOut.setStatus(404);
            }
        } else {
            // TODO when is AhqrSender used and verb is not GET?
            if (sender.hasErrors()) {
                OperationOutcome oo = regErrorListAsOperationOutcome(sender.getErrorList());
                returnOperationOutcome(responseOut, oo);
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
                        DocumentReference dr = trans.getDocumentReference((ExtrinsicObjectType) it, channelConfig);
                        outContents.add(dr);
                    } else {
                        OperationOutcome oo = wrapErrorInOperationOutcome("Cannot transform " + it.getClass().getSimpleName() + " to FHIR");
                        outContents.add(oo);
                    }
                }
            }
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
        responseOut.setOperationOutcome(oo);
        Resource returnResource = oo;
        if (oo == null) {
            Bundle response = new Bundle();
            returnResource = response;
            response.setType(Bundle.BundleType.TRANSACTIONRESPONSE);
            for (Bundle.BundleEntryComponent componentIn : requestBundle.getEntry()) {
                BaseResource resource = componentIn.getResource();
                SubmittedObject submittedObject = bundleToRegistryObjectList.findSubmittedObject(resource);
                Bundle.BundleEntryComponent componentOut = response.addEntry();
                Bundle.BundleEntryResponseComponent responseComponent = componentOut.getResponse();
                responseComponent.setStatus("201");
                if (submittedObject == null) {
                    if (resource instanceof Binary) {
                        Binary binary = (Binary) resource;
                        DocumentReference docRef = findAttached(requestBundle, binary);
                        if (docRef != null) {
                            SubmittedObject submittedObject2 = bundleToRegistryObjectList.findSubmittedObject(docRef);
                            String url = proxyBase + "/Binary/" + submittedObject2.getUid();
                            responseComponent.setLocation(url);
                        }
                    }
                } else {
                    String url = proxyBase + "/" + resource.getClass().getSimpleName() + "/" + submittedObject.getUid();
                    responseComponent.setLocation(url);
                }
            }
        }
        if (returnFormatType == null)
            returnFormatType = Format.XML;
        responseOut.setResponseText(ProxyBase.encode(returnResource, returnFormatType));
        responseOut.setResponseContentType(returnFormatType.getContentType());
        if (oo == null)
            responseOut.setStatus(200);
        else
            responseOut.setStatus(400);
    }

    // return DocumentReference this binary is attached to
    private DocumentReference findAttached(Bundle bundle, Binary binary) {
        for (Bundle.BundleEntryComponent componentIn : requestBundle.getEntry()) {
            BaseResource resource = componentIn.getResource();
            if (resource instanceof DocumentReference) {
                DocumentReference docRef = (DocumentReference) resource;
                for (DocumentReference.DocumentReferenceContentComponent contentComponent : docRef.getContent()) {
                    if (contentComponent.hasAttachment()) {
                        Attachment attachement = contentComponent.getAttachment();
                        String url = attachement.getUrl();
                        if (url != null) {
                            Resource res = findResourceInBundle(bundle, url);
                            if (res instanceof Binary) {
                                Binary foundBinary = (Binary) res;
                                if (binary.equals(foundBinary))
                                    return docRef;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private Resource findResourceInBundle(Bundle bundle, String fullUrl) {
        for (Bundle.BundleEntryComponent componentIn : requestBundle.getEntry()) {
            if (fullUrl.equals(componentIn.getFullUrl())) {
                return componentIn.getResource();
            }
        }
        return null;
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
    public void teardown() {

    }

    @Override
    public void validateConfig(ChannelConfig simConfig) {

    }

    @Override
    public void handle(Event event) {

    }

    public void setReturnFormatType(Format returnFormatType) {
        this.returnFormatType = returnFormatType;
    }
}
