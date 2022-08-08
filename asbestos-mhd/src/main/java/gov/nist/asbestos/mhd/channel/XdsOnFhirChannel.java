package gov.nist.asbestos.mhd.channel;

import gov.nist.asbestos.client.Base.ParserBase;
import gov.nist.asbestos.client.channel.BaseChannel;
import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.events.Event;
import gov.nist.asbestos.client.events.ITask;
import gov.nist.asbestos.client.general.ChannelSupport;
import gov.nist.asbestos.client.resolver.IdBuilder;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceCacheMgr;
import gov.nist.asbestos.client.resolver.ResourceMgr;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.headers.Header;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.http.operations.HttpDelete;
import gov.nist.asbestos.http.operations.HttpGetter;
import gov.nist.asbestos.http.operations.HttpPost;
import gov.nist.asbestos.http.operations.Verb;
import gov.nist.asbestos.mhd.SubmittedObject;
import gov.nist.asbestos.mhd.exceptions.TransformException;
import gov.nist.asbestos.mhd.transactionSupport.AhqrSender;
import gov.nist.asbestos.mhd.transactionSupport.AssigningAuthorities;
import gov.nist.asbestos.mhd.transactionSupport.CodeTranslator;
import gov.nist.asbestos.mhd.transactionSupport.FaultParser;
import gov.nist.asbestos.mhd.transactionSupport.ProvideAndRegisterBuilder;
import gov.nist.asbestos.mhd.transactionSupport.RetrieveContent;
import gov.nist.asbestos.mhd.transactionSupport.XmlTools;
import gov.nist.asbestos.mhd.transforms.BundleToRegistryObjectList;
import gov.nist.asbestos.mhd.transforms.DocumentEntryToDocumentReference;
import gov.nist.asbestos.mhd.transforms.MhdTransforms;
import gov.nist.asbestos.mhd.translation.ContainedIdAllocator;
import gov.nist.asbestos.mhd.translation.search.DocManSQParamTranslator;
import gov.nist.asbestos.mhd.translation.search.FhirSq;
import gov.nist.asbestos.mhd.util.XdsActorMapper;
import gov.nist.asbestos.simapi.tk.installation.Installation;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.utilities.ErrorType;
import gov.nist.asbestos.utilities.MultipartSender;
import gov.nist.asbestos.utilities.PnrWrapper;
import gov.nist.asbestos.utilities.RegError;
import gov.nist.asbestos.utilities.RegErrorList;
import gov.nist.asbestos.utilities.RegistryResponseBuilder;
import ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;
import oasis.names.tc.ebxml_regrep.xsd.lcm._3.SubmitObjectsRequest;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.IdentifiableType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectListType;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType;
import org.apache.commons.codec.binary.Base64;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

// TODO - honor the Prefer header - http://hl7.org/fhir/http.html#ops
public class XdsOnFhirChannel extends BaseChannel /*implements IBaseChannel*/ {
    private static final Logger log = Logger.getLogger(XdsOnFhirChannel.class.getName());
    private Bundle requestBundle = null;
    private String serverBase;
    private String proxyBase;
    private BundleToRegistryObjectList bundleToRegistryObjectList;
    private AhqrSender sender = null;
    private Binary binary = null;
    private MhdTransforms mhdTransforms;
    private MhdVersionEnum defaultVersion = MhdVersionEnum.MHDv3x;
    private MhdProfileVersionInterface mhdImpl;

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
        bundleToRegistryObjectList.setTask(task);

        try {
            HttpGetter requestIn = new HttpGetter();
            // only resource type is important
            requestIn.setRequestHeaders(new Headers().setPathInfo(new URI("DocumentReference/1")));
            bundleToRegistryObjectList.setSqEndpoint(transformRequestUrl(null, requestIn));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // parse bundle
        rMgr.setBundle(bundle);

        // Setup MHD specific implementation
        if (! isMhdVersionSpecificImplInitialized()) {
            mhdTransforms = new MhdTransforms(rMgr, val, task);
            mhdImpl = getMhdVersionSpecificImpl(bundle, val);
        } else {
            throw new RuntimeException("MhdVersionSpecificImpl already initialized");
        }

        // perform translation
        RegistryObjectListType registryObjectListType = bundleToRegistryObjectList.build(mhdImpl,  bundle);
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
            lastDocumentStr = new String(contents);
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

    /**
     * Returns a defaultVersion of implementation or an implementation based on bundle profile if it is recognized
     * @param bundle
     * @param val
     * @return
     */
    private MhdProfileVersionInterface getMhdVersionSpecificImpl(Bundle bundle, Val val) {
        Objects.requireNonNull(channelConfig);
        String[] allowedMhdVersions = channelConfig.getMhdVersions();

        if (allowedMhdVersions != null) {
            // Allow only from the Accept list
            return findMhdImpl(bundle, allowedMhdVersions, defaultVersion, val);
        } else {
            // All MHD versions are implicitly acceptable by channelConfig.
            // Auto-detect based on Bundle profile
            List<String> list = Arrays.stream(MhdVersionEnum.values())
                    .map(MhdVersionEnum::toString)
                    .collect(Collectors.toList());
            return findMhdImpl(bundle, list.toArray(new String[list.size()]), defaultVersion, val);
        }
    }

    private MhdProfileVersionInterface findMhdImpl(Bundle bundle, String[] acceptableMhdVersions, MhdVersionEnum defaultVersion, Val val) {
        Objects.requireNonNull(bundle);
        Objects.requireNonNull(acceptableMhdVersions);
        Objects.requireNonNull(val);
        Objects.requireNonNull(mhdTransforms);

        MhdVersionEnum bundleVersion = defaultVersion;
        try {
            Optional<MhdVersionEnum> optionalMhdVersionEnum = Arrays.stream(acceptableMhdVersions)
                    .map(MhdVersionEnum::find)
                    .filter(e -> {
                        if (e == null) {
                            return false;
                        } else {
                            MhdProfileVersionInterface intf = MhdImplFactory.getImplementation(bundle, e, val, mhdTransforms);
                            if (intf == null) {
                                return false;
                            } else {
                                CanonicalUriCodeEnum ce = intf.getDetectedBundleProfile();
                                return ce != null;
                            }
                        }
                    })
                    .findAny();
            if (optionalMhdVersionEnum.isPresent()) {
                bundleVersion = optionalMhdVersionEnum.get();
            }
        } catch (Exception ex) {
            log.warning("findMhdImpl Exception: " + ex.toString());
        }
        return MhdImplFactory.getImplementation(bundle, bundleVersion, val, mhdTransforms);
    }

    public static byte[] lastDocument;
    public static String lastDocumentStr;


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
        bundleToRegistryObjectList = new BundleToRegistryObjectList(channelConfig);
        byte[] request = requestIn.getRequest();
        String contentType = requestIn.getRequestContentType();
        IBaseResource resource;
        if (contentType == null)
            throw new RuntimeException("No Content Type");
        if (contentType.startsWith("application/fhir+json")) {
            try {
                String requestString = new String(request);
                resource = ParserBase.getFhirContext().newJsonParser().parseResource(requestString);
            } catch (Throwable t) {
                returnErrorInOperationOutcome("Failed to parse request", t, requestIn);
                return;
            }
        } else if (contentType.startsWith("application/fhir+xml")) {
            resource = ParserBase.getFhirContext().newXmlParser().parseResource(new String(request));
        } else
            throw new RuntimeException("Do not understand Content-Type " + contentType);

        URI toAddr = transformRequestUrl(null, requestIn);

        if (!(resource instanceof Bundle) ) {
            // forward to fhir server
            ChannelSupport.passHeaders(requestIn, requestOut);
            requestOut.setRequest(requestIn.getRequest());
            return;
        }
        Bundle bundle = (Bundle) resource;
        requestBundle = bundle;


        String soapString = transformPDBToPNR(bundle, toAddr, getTask());
        if (soapString == null) {
            OperationOutcome oo = new OperationOutcome();
            //  huh?
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
    public void transformRequest(HttpGetter requestIn, HttpGetter requestOut) {
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
            } else if (resourceType.equals("DocumentManifest")) {
                sender = FhirSq.docManQuery(params, toAddr, task);
                returnAhqrResults(requestOut);
            } else if (resourceType.equals(MhdTransforms.MhdListResourceName)) {
                // Code value must be supplied to distinguish SS from a Folder
                if (DocManSQParamTranslator.parseParms(params).contains("code=submissionset")) {
                    sender = FhirSq.docManQuery(params, toAddr, task);
                    returnAhqrResults(requestOut);
                } else {
                    throw new RuntimeException("SEARCH resource " + resourceType + " not supported on this channel");
                }
            } else {
                throw new RuntimeException("SEARCH " + resourceType + " not supported on this channel");
            }
        } else {
            // GET
            if (resourceType.equals("DocumentReference") && uid.contains(".")) {
                // by UID
                sender = FhirSq.documentEntryByUidQuery(uid, null, toAddr, task);
                returnAhqrResults(requestOut);
            } else if (resourceType.equals("DocumentReference") && uid.contains("-")) {
                // by UUID
                sender = FhirSq.documentEntryByUUIDQuery("urn:uuid:" + uid, toAddr, task);
                returnAhqrResults(requestOut);
            } else if (resourceType.equals("DocumentManifest")  && uid.contains(".")) {
                sender = FhirSq.submissionSetByUidQuery(uid, toAddr, task);
                returnAhqrResults(requestOut);
            } else if (resourceType.equals(MhdTransforms.MhdListResourceName)  && uid.contains(".")) {
                if (IdBuilder.isOpaqueLogicalId(IdBuilder.SS_OPAQUE_ID, uid)) {
                    String id = IdBuilder.stripPrefix(IdBuilder.SS_OPAQUE_ID, uid);
                    sender = FhirSq.submissionSetByUidQuery(id, toAddr, task);
                    returnAhqrResults(requestOut);
                } else {
                    throw new RuntimeException("GET resource " + resourceType + " not supported");
                }
            } else if (resourceType.equalsIgnoreCase("Binary") && uid.contains(".")) {
                // by UUID
                String repUid = "1.1.1";
                RetrieveContent retrieveContent = FhirSq.binaryByUidRetrieve(uid, repUid, toAddr, task);
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
                //j = "";
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

    private void returnAhqrResults(HttpGetter requestOut) {
        requestOut.setRequestHeaders(sender.getRequestHeaders());
        requestOut.setRequestText(sender.getRequestBody());
    }

    private void returnOperationOutcome(HttpBase resp, OperationOutcome oo) {
        Headers responseHeaders = new Headers();
        Format format = getReturnFormatType();
        String encoded = ParserBase.encode(oo, format);
        resp.setResponseText(encoded);
        resp.getResponseHeaders().add(new Header("Content-Type", format.getContentType()));
        resp.setStatus(500);
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
        } else if (requestIn instanceof HttpGetter) {
            String actorType;
            String transType;

            if (resourceType == null)
                throw new Error("Cannot retrieve XDS contents for resource type " + resourceType);

            if (resourceType.equals("DocumentReference")
                    || resourceType.equals("DocumentManifest")
                    || resourceType.equals(MhdTransforms.MhdListResourceName)) {
                actorType = "reg";
                transType = "sq";
            } else if (resourceType.equals("Binary")) {
                actorType = "rep";
                transType = "ret";
            } else {
                String fhirBase = channelConfig.getFhirBase();
                if (fhirBase == null)
                    return null;
                return new Ref(fhirBase).withResource(resourceType).getUri();
            }

            return  new XdsActorMapper().getEndpoint(channelConfig.getXdsSiteName(), actorType, transType, false);
        }
        return null;
    }

    private void returnErrorInOperationOutcome(String msg, Throwable t, HttpBase responseOut) {
        String logReference = logReference(log, "returnErrorInOperationOutcome", t);
        String errorMessage = String.format("%s, %s. Check server log for details.",
                logReference,
                msg);
        returnErrorInOperationOutcome(errorMessage, responseOut);
    }

    private OperationOutcome wrapErrorInOperationOutcome(String msg) {
        OperationOutcome oo = new OperationOutcome();
        MhdTransforms.addErrorToOperationOutcome(oo, msg);
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
    public void transformResponse(HttpBase responseIn, HttpBase responseOut, String proxyHostPort, String requestedType, String search) {
        if (sender != null) {
            // there is a query response to transform
            transformDSResponse(sender, responseOut, requestedType, search);
            return;
        }
        if (requestedType != null && requestedType.equals("Binary")) {
            if (binary != null) {
                responseOut.setStatus(200);
                responseOut.setResponseContentType(returnFormatType.getContentType());
                responseOut.setResponse(ParserBase.encode(binary, returnFormatType).getBytes());
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
            String registryResponse;
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
                String logReference = logReference(log, "XdsOnFhirChannel#transform", e);
                returnErrorInOperationOutcome(logReference.concat("Check server log for more details."), responseOut);
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


    /**
     * Transform document sharing actor response
     * @param sender
     * @param responseOut
     * @param requestedType
     * @param search
     */
    private void transformDSResponse(AhqrSender sender, HttpBase responseOut, String requestedType, String search) {
        responseOut.setResponseContentType(returnFormatType.getContentType());
        Ref searchRef = new Ref(search);
        boolean isSearch = !searchRef.hasId();
        if (responseOut.getVerb().equals(Verb.GET.toString())) {  // FHIR READ
            if (sender.hasErrors()) {
                OperationOutcome oo = regErrorListAsOperationOutcome(sender.getErrorList());
                returnOperationOutcome(responseOut, oo);
            } else if (isSearch) /* This is not a GET by logical ID provided, but has search params */ {
                if (requestedType.equals("DocumentReference")) {
                    List<ResourceWrapper> results = new ArrayList<>();
                    for (IdentifiableType identifiableType  : sender.getContents()) {
                        if (identifiableType instanceof ExtrinsicObjectType) {
                            BaseResource resource = toFhir((ExtrinsicObjectType) identifiableType);
                            if (resource instanceof OperationOutcome) {
                                responseOut.setResponseText(ParserBase.encode(resource, returnFormatType));
                                responseOut.setResponseContentType(returnFormatType.getContentType());
                                responseOut.setStatus(500);
                                return;
                            }
                            ResourceWrapper wrapper = new ResourceWrapper(resource);
                            if (resource.getId() != null) {
                                Ref ref = searchRef.withNewId(resource.getId());
                                wrapper.setRef(ref);
                            }
                            results.add(wrapper);
                        }
                    }
                    responseOut.setResponseText(ParserBase.encode(buildSearchResult(results, search), returnFormatType));
                    responseOut.setResponseContentType(returnFormatType.getContentType());
                } else if (requestedType.equals("DocumentManifest")) {
                    // this assumes a single manifest - must be extended to get more
                    BaseResource resource = MhdTransforms.ssToDocumentManifest(getCodeTranslator(), getExternalCache(), sender, channelConfig);
                    resourceResponse(responseOut, search, searchRef, resource);
                } else if (requestedType.equals(MhdTransforms.MhdListResourceName)) {
                    List<String> paramList = DocManSQParamTranslator.parseParms(search);
                    String error = "";
                    try {
                        if (isMhdVersionSpecificImplInitialized()) {
                            Optional<String> matchParam = mhdImpl.hasSsQueryParam(paramList);
                            if (matchParam.isPresent()) {
                                BaseResource resource = MhdTransforms.ssToListResource(mhdImpl.getMhdVersion().getMhdImplClass(), getCodeTranslator(), getExternalCache(), sender, channelConfig);
                                resourceResponse(responseOut, search, searchRef, resource);
                                return;
                            } else {
                                throw new Exception("mhdImpl not initialized.");
                            }
                        }
                    } catch (Exception ex) {
                        error = ex.toString();
                    }

                    if (returnFormatType.equals(Format.JSON)) {
                        responseOut.setResponseText(String.format("{\"errorString\":\"%s\"}", error));
                    } else {
                        responseOut.setResponseText(error);
                    }
                    responseOut.setResponseContentType("text/plain");
                    responseOut.setStatus(500);

                }
            } else /* HTTP Verb GET resource by ID */ {
                if (sender.getContents().size() == 1 && requestedType != null) {
                    if (requestedType.equals("DocumentReference")) {
                        BaseResource resource = toFhir((ExtrinsicObjectType) sender.getContents().get(0));
                        if (resource == null) {
                            responseOut.setStatus(404);
                            return;
                        }
                        responseOut.setResponseText(ParserBase.encode(resource, returnFormatType));
                        responseOut.setResponseContentType(returnFormatType.getContentType());
                    } else {
                        String errorRequestType = (requestedType == null)?"requestedType is null":requestedType;
                        returnOperationOutcome(responseOut,
                                new OperationOutcome()
                                        .addIssue(new OperationOutcome.OperationOutcomeIssueComponent().setSeverity(OperationOutcome.IssueSeverity.ERROR).setDiagnostics(errorRequestType)));
                    }
                } else if (requestedType != null && requestedType.equals("DocumentManifest")) {
                    BaseResource fhirResource = MhdTransforms.ssToDocumentManifest(getCodeTranslator(), getExternalCache(), sender, channelConfig);
                    responseResourceGet(responseOut, fhirResource);
                } else if (requestedType.equals(MhdTransforms.MhdListResourceName)) {
                    if (IdBuilder.isOpaqueLogicalId(IdBuilder.SS_OPAQUE_ID, searchRef.getId())) {
                        BaseResource fhirResource = MhdTransforms.ssToListResource(mhdImpl.getMhdVersion().getMhdImplClass(), getCodeTranslator(), getExternalCache(), sender, channelConfig);
                        responseResourceGet(responseOut, fhirResource);
                    } else {
                        responseOut.setResponseContentType(returnFormatType.getContentType());
                        responseOut.setResponseText("MhdListResource URL is malformed.");
                        responseOut.setStatus(500);
                    }
                } else { // no contents
                     responseOut.setResponseContentType(returnFormatType.getContentType());
                     responseOut.setStatus(404);
                 }
            }
        } /* HTTP Verb is something other than a GET */ else {
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

    private void responseResourceGet(HttpBase responseOut, BaseResource fhirResource) {
        responseOut.setResponseContentType(returnFormatType.getContentType());
        if (fhirResource == null) {
            responseOut.setStatus(404);
        } else if (fhirResource instanceof DocumentManifest || fhirResource instanceof ListResource) {
            responseOut.setStatus(200);
            responseOut.setResponseText(ParserBase.encode(fhirResource, returnFormatType));
        } else {
            // OperationOutcome
            responseOut.setStatus(500);
            responseOut.setResponseText(ParserBase.encode(fhirResource, returnFormatType));
        }
    }

    private void resourceResponse(HttpBase responseOut, String search, Ref searchRef, BaseResource resource) {
        if (resource == null) {
            // No error just no response (empty SearchSet)
            responseOut.setResponseText(ParserBase.encode(buildSearchResult(Collections.EMPTY_LIST, search), returnFormatType));
            return;
        }
        if (resource instanceof OperationOutcome) {
            responseOut.setResponseText(ParserBase.encode(resource, returnFormatType));
            responseOut.setResponseContentType(returnFormatType.getContentType());
            responseOut.setStatus(500);
        } else {
            ResourceWrapper wrapper = new ResourceWrapper(resource);
            if (resource.getId() != null) {
                Ref ref = searchRef.withNewId(resource.getId());
                wrapper.setRef(ref);
            }
            responseOut.setResponseText(ParserBase.encode(buildSearchResult(Collections.singletonList(wrapper), search), returnFormatType));
            responseOut.setResponseContentType(returnFormatType.getContentType());
        }
    }


    private CodeTranslator getCodeTranslator() {
        CodeTranslator codeTranslator;
        try {
            codeTranslator = new CodeTranslator(Installation.instance().getCodesFile(channelConfig.getEnvironment()));
            return codeTranslator;
        } catch (Exception e) {
            throw new RuntimeException("Cannot load codes file for environment " + channelConfig.getEnvironment(), e);
        }
    }

    private BaseResource toFhir(ExtrinsicObjectType eo) {
        Objects.requireNonNull(eo);
        Val val = new Val();
        CodeTranslator codeTranslator = getCodeTranslator();

        ResourceCacheMgr resourceCacheMgr = new ResourceCacheMgr(getExternalCache());
        FhirClient fhirClient = new FhirClient()
                .setResourceCacheMgr(resourceCacheMgr);

        DocumentEntryToDocumentReference trans = new DocumentEntryToDocumentReference();
        trans
                .setContainedIdAllocator(new ContainedIdAllocator())
                .setResourceCacheMgr(resourceCacheMgr)
                .setCodeTranslator(codeTranslator)
                .setFhirClient(fhirClient)
                .setVal(val);

        DocumentReference dr = trans.getDocumentReference(eo, channelConfig);

        if (dr.hasSubject())
            MhdTransforms.withNewBase(channelConfig.getProxyURI(), dr.getSubject());

        if (val.hasErrors())
            return MhdTransforms.operationOutcomefromVal(val);

        return dr;
    }



    private boolean isWhite(char c) {
        if (c == ' ') return true;
        if (c == '\n') return true;
        return c == '\t';
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
                    String resourceName = resource.fhirType();
                    String logicalId = submittedObject.getUid();
                    if (MhdTransforms.MhdListResourceName.equals(resourceName)) {
                        if (MhdProfileVersionInterface.isCodedListType(MhdProfileVersionInterface.ANY_VERSION, resource, "submissionset")) {
                            logicalId = IdBuilder.makeOpaqueLogicalId(IdBuilder.SS_OPAQUE_ID, logicalId);
                        }
                    }
                    String url = proxyBase + "/" + resourceName + "/" + logicalId;
                    responseComponent.setLocation(url);
                }
            }
        }
        if (returnFormatType == null)
            returnFormatType = Format.XML;
        responseOut.setResponseText(ParserBase.encode(returnResource, returnFormatType));
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

    private Bundle buildSearchResult(List<ResourceWrapper> wrappers, String search) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal(wrappers.size());
        bundle.setLink(Collections.singletonList(
                new Bundle.BundleLinkComponent()
                .setRelationElement(new StringType("self"))
                .setUrl(search)));
        for (ResourceWrapper wrapper : wrappers) {
            Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent().setResource((Resource) wrapper.getResource());
            entry.setFullUrl(wrapper.getRef().toString());
            entry.setSearch(new Bundle.BundleEntrySearchComponent().setMode(Bundle.SearchEntryMode.MATCH));
            bundle.addEntry(entry);
        }
        return bundle;
    }


    private boolean isMhdVersionSpecificImplInitialized() {
        return mhdTransforms != null && mhdImpl != null;
    }
}
