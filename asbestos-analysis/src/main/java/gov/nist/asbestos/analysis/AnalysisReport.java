package gov.nist.asbestos.analysis;

import com.google.common.base.Strings;
import gov.nist.asbestos.client.Base.DocumentCache;
import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.reporting.IErrorReporter;
import gov.nist.asbestos.client.resolver.ChannelUrl;
import gov.nist.asbestos.client.resolver.FhirPath;
import gov.nist.asbestos.client.resolver.IdBuilder;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.mhd.transforms.MhdTransforms;
import gov.nist.asbestos.mhd.transforms.MhdV4;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.testEngine.engine.AssertionRunner;
import gov.nist.asbestos.testEngine.engine.FhirPathEngineBuilder;
import gov.nist.asbestos.testEngine.engine.TestEngine;
import gov.nist.asbestos.testEngine.engine.assertion.MinimumId;
import gov.nist.asbestos.utilities.ResourceHasMethodsFilter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hl7.fhir.r4.model.*;

import java.io.File;
import java.net.URISyntaxException;
import java.util.*;

public class AnalysisReport {
    private static final Logger log = Logger.getLogger(AnalysisReport.class.getName());
    private Ref fhirBase = null;
    private Ref baseRef = null;
    private List<Ref> relatedRefs = new ArrayList<>();
    private ResourceWrapper baseObj = null;
    private List<ResourceWrapper> relatedObjs = new ArrayList<>();
    private List<Related> related = new ArrayList<>();
    private List<String> minimalErrors = new ArrayList<>();
    private Checked minimalChecked;
    private List<String> comprehensiveErrors;
    private Checked comprehensiveChecked;
    private List<String> codingErrors = new ArrayList<>();
    private List<String> generalErrors = new ArrayList<>();
    private List<String> generalWarnings = new ArrayList<>();
    private FhirClient fhirClient = new FhirClient().setSupportRequest(true);
    private String source;
    private EC ec;
    private CodesValidation codesValidation;
    private Map atts;
    private String binaryUrl;
    private boolean useGzip = false;
    private boolean useProxy = false;
    private ResourceWrapper contextResourceBundle = null;
    private Map<BaseResource, List<Reference2>> refs = new HashMap<>();
    private boolean isRequest = false;  // as opposed to response/contents from server
    private String baseObjectEventId = null;
    private String baseObjectResourceType = null;
    private boolean runValidation = false;

    private class ErrorReporter implements IErrorReporter {

        @Override
        public void requireNonNull(Object o, String msg) {
            if (o == null) {
                generalErrors.add(msg);
                throw new Error(msg);
            }
        }

        @Override
        public void requireNull(Object o, String msg) {
            if (o != null) {
                generalErrors.add(msg);
                throw new Error(msg);
            }
        }
    }
    private ErrorReporter errorReporter = new ErrorReporter();

    private Bundle getContextBundle() {
        if (contextResourceBundle == null)
            return null;
        if (contextResourceBundle.getResource() instanceof Bundle)
            return (Bundle) contextResourceBundle.getResource();
        return null;
    }

    private boolean hasContextBundle() {
        return contextResourceBundle != null;
    }

    private Report buildReport() {
        Report report = new Report();
        report.baseObjectEventId = baseObjectEventId;
        report.baseObjectResourceType = baseObjectResourceType;

        report.source = source;
        report.errors = new ArrayList<>(generalErrors);
        report.warnings = new ArrayList<>(generalWarnings);

        if (comprehensiveErrors == null)
            comprehensiveErrors = new ArrayList<>();

        if (baseObj != null && baseObj.getResource() != null && (!isSearchSet() || isSearchSetSingleResult())) {
            report.base = new RelatedReport(baseObj, "");
            report.base.comprehensiveErrors = comprehensiveChecked == null ? new ArrayList<>() : comprehensiveChecked.report.missing; //comprehensiveErrors;
            report.base.comprehensiveErrors.addAll(comprehensiveErrors);
            report.base.isComprehensive = report.base.comprehensiveErrors.isEmpty(); // comprehensiveErrors != null && comprehensiveErrors.isEmpty();

            report.base.minimalErrors = minimalChecked == null ? new ArrayList<>() : minimalChecked.report.missing; //minimalErrors;
            report.base.minimalErrors.addAll(minimalErrors);
            report.base.isMinimal = report.base.minimalErrors.isEmpty(); //minimalErrors!= null && minimalErrors.isEmpty();

            report.base.minimalChecked = minimalChecked == null ? new ArrayList<>() : minimalChecked.report.expected;
            report.base.comprehensiveChecked = comprehensiveChecked == null ? new ArrayList<>() : comprehensiveChecked.report.expected;
            report.base.codingErrors = codingErrors;
            report.base.atts = atts;
            report.base.binaryUrl = binaryUrl;
            if (runValidation)
                report.base.validationResult = runValidation(baseObj.getResource());
            else
                report.base.validationResult = new OperationOutcome();
        }

        for (Related rel : related) {
            ResourceWrapper wrapper = rel.wrapper;
            BaseResource resource = wrapper.getResource();
            RelatedReport relatedReport = new RelatedReport(wrapper, rel.howRelated);
            if (resource != null) {
                relatedReport.comprehensiveErrors = rel.comprehensiveChecked == null ? new ArrayList<>() : rel.comprehensiveChecked.report.missing;
                relatedReport.comprehensiveErrors.addAll(rel.comprehensiveErrors);
                relatedReport.isComprehensive = relatedReport.comprehensiveErrors.isEmpty(); //rel.comprehensiveErrors != null && rel.comprehensiveErrors.isEmpty();
                relatedReport.minimalErrors = rel.minimalChecked == null ? new ArrayList<>() : rel.minimalChecked.report.missing;
                relatedReport.minimalErrors.addAll(rel.minimalErrors);
                relatedReport.isMinimal = relatedReport.minimalErrors.isEmpty();  //rel.minimalErrors!= null && rel.minimalErrors.isEmpty();
                relatedReport.comprehensiveChecked = rel.comprehensiveChecked == null ? new ArrayList<>() : rel.comprehensiveChecked.report.expected;
                relatedReport.minimalChecked = rel.minimalChecked == null ? new ArrayList<>() : rel.minimalChecked.report.expected;
                relatedReport.codingErrors = rel.codingErrors;
                relatedReport.atts = rel.atts;
                relatedReport.binaryUrl = rel.binaryUrl;

                if (runValidation)
                    relatedReport.validationResult = runValidation(resource);
                else
                    relatedReport.validationResult = new OperationOutcome();
            }
            report.objects.add(relatedReport);
        }

        return report;
    }

    private OperationOutcome runValidation(BaseResource resource) {
        List<String> errors = new ArrayList<>();
        boolean gzip = true;
        String resourceType = resource.getClass().getSimpleName();
        String validationServer = ServiceProperties.getInstance().getPropertyOrStop(ServicePropertiesEnum.FHIR_VALIDATION_SERVER);
        FhirClient fhirClient = new FhirClient().setSupportRequest(true)
                .sendGzip(gzip)
                .requestGzip(gzip);
        Ref valRef = new Ref(validationServer + "/" + resourceType + "/$validate?profile=http://hl7.org/fhir/StructureDefinition/" + resourceType);
        ResourceWrapper wrapper = fhirClient.writeResource(resource, valRef,
                Format.JSON,
                new Headers().withContentType(Format.JSON.getContentType()));
        if (wrapper.getResponseResource() == null) {
            OperationOutcome oo = new OperationOutcome();
            OperationOutcome.OperationOutcomeIssueComponent comp = new OperationOutcome.OperationOutcomeIssueComponent();
            comp.setCode(OperationOutcome.IssueType.EXCEPTION);
            comp.setSeverity(OperationOutcome.IssueSeverity.FATAL);
            comp.setDiagnostics("request to validation server (\" +  validationServer  + \" ) failed");
            return oo;
        } else if ("OperationOutcome".equals(wrapper.getResponseResource().getClass().getSimpleName())) {
            return (OperationOutcome) wrapper.getResponseResource();
        } else {
            OperationOutcome oo = new OperationOutcome();
            OperationOutcome.OperationOutcomeIssueComponent comp = new OperationOutcome.OperationOutcomeIssueComponent();
            comp.setCode(OperationOutcome.IssueType.EXCEPTION);
            comp.setSeverity(OperationOutcome.IssueSeverity.FATAL);
            comp.setDiagnostics("OperationOutcome not returned from  validation server (" +  validationServer  + " ) "
                    + wrapper.getResponseResource().getClass() + " returned instead");
            return oo;
        }
    }

    public AnalysisReport(Ref baseRef, String source, EC ec) {
        this.baseRef = baseRef;
        this.source = source;
        this.ec = ec;
        this.codesValidation = new CodesValidation(ec);
    }

    public AnalysisReport(Bundle bundle, String source, EC ec) {
        this.baseObj = new ResourceWrapper(bundle);
        this.source = source;
        this.ec = ec;
        this.codesValidation = new CodesValidation(ec);
    }

    // for tests only
    AnalysisReport() {}

    public AnalysisReport(EC ec, ResourceWrapper wrapper) {
        this.ec = ec;
        this.codesValidation = new CodesValidation(ec);
        baseObj = wrapper;
        baseRef = baseObj.getRef();
    }

    public AnalysisReport withGzip(boolean value) {
        useGzip = value;
        return this;
    }

    public AnalysisReport withProxy(boolean value) {
        useProxy = value;
        return this;
    }

    public AnalysisReport withContextResource(ResourceWrapper contextResource) {
        this.contextResourceBundle = contextResource;
        return this;
    }

    public AnalysisReport analyseRequest(boolean isRequest) {
        this.isRequest = isRequest;
        return this;
    }

    private ResourceWrapper findResourceInBundle(Bundle bundle, String ref) {
        if (bundle == null)
            return null;
        for (Bundle.BundleEntryComponent comp : bundle.getEntry()) {
            if (ref.equals(comp.getFullUrl())) {
                ResourceWrapper wrapper = new ResourceWrapper(comp.getResource());
                wrapper.setRef(new Ref(ref));
                wrapper.setContext(bundle);
                return wrapper;
            }
        }
        return null;
    }

    public Report run() {
        Report report = establishBase();
        if (report != null)
            return report;
        try {
            buildRelated();
            comprehensiveEval();
            minimalEval();
            codingEval();
            buildAtts();
            binaryEval();
            return buildReport();
        } catch (Throwable t) {
            String error = "AnalysisReport#run Error: " + t;
            log.log(Level.SEVERE, error, t);
            generalErrors.add(String.format("%s. Check server log for details.", error));
            return buildReport();
        }
    }

    private Report establishBase() {
        try {
            if (baseRef != null && baseObj == null && contextResourceBundle != null) {
                loadBaseFromContext();
                if (baseObj == null)
                    loadBase();
                Objects.requireNonNull(baseObj);
            }
            else if (baseRef != null && baseObj == null) {
                loadBase();
                if (!generalErrors.isEmpty())
                    return buildReport();
            } else if (getContextBundle() != null &&
                    getContextBundle().hasType() &&
                    getContextBundle().getType().equals(Bundle.BundleType.SEARCHSET)) {
                Bundle bundle = (Bundle) getContextBundle();
                loadSearchSetFromContext(bundle);
            } else if (baseRef == null && getContextBundle() != null) {
                baseObj = contextResourceBundle;
            } else if (baseObj != null) {
                try {
                    BaseResource res = baseObj.getResource(); // maybe force loading from UIEvent
                    if (res instanceof Bundle) {
                        contextResourceBundle = new ResourceWrapper(res);
                        contextResourceBundle.setRef(baseObj.getRef());
                        contextResourceBundle.setEvent(baseObj.getEvent(), baseObj.isRequest());
                        baseObj = null;
                        loadBaseFromContext();
                    }
                } catch (Exception e) {
                    if (baseObj != null)
                        generalErrors.add((baseObj.isRequest() ? "Request " : "Response ") + "contains no Resource");
                    return buildReport();
                }
                buildAtts();
                //   return buildReport();
            }
            if (baseObj != null && !baseObj.hasEvent() && contextResourceBundle != null) {
                baseObj.setEvent(contextResourceBundle.getEvent(), contextResourceBundle.isRequest());
            }
        } catch (Throwable t) {
            String error = "AnalysisReport#establishBase Error: " + t;
            log.log(Level.SEVERE, error, t);
            generalErrors.add(String.format("%s. Check server log for details.", error));
            return buildReport();
        }
        if (!generalErrors.isEmpty())
            return buildReport();
        return null;
    }


    private void buildAtts() {
        if (baseObj == null)
            return;
        BaseResource baseResource = baseObj.getResource();
        if (baseResource == null) {
            generalErrors.add("No Resource to analyse");
            return;
        }
        atts = ResourceHasMethodsFilter.toMap(baseResource);
        for (Related rel : related) {
            if (rel.wrapper.hasResource()) {
                rel.atts = ResourceHasMethodsFilter.toMap(rel.wrapper.getResource());
            }
        }
    }

    private void binaryEval() {
        if (baseObj == null)
            return;
        if (baseObj.getResource().getClass().getSimpleName().equals("Binary")) {
            binaryUrl = extractDocument((Binary) baseObj.getResource());
        }
        for (Related rel : related) {
            if (rel.wrapper.getResource() instanceof Binary)
                rel.binaryUrl = extractDocument((Binary) rel.wrapper.getResource());
        }
    }


    private void codingEval() {
        if (baseObj == null)
            return;
        codingErrors.addAll(codesValidation.validate(baseObj.getResource()));
        for (Related rel : related) {
            rel.codingErrors.addAll(codesValidation.validate(rel.wrapper.getResource()));
        }
    }

    private void comprehensiveEval() {
        if (baseObj == null)
            return;
        TestEngine testEngine = comprehensiveEval(baseObj);
        // Guard seems to have a side effect
//        if (testEngine == null)
//            return;
        TestReport report = testEngine.getTestReport();
        Checked checked = getMinimumIdReport(report);
        comprehensiveErrors = testEngine.getTestReportErrors();
        comprehensiveChecked = checked;
        for (Related rel : related) {
            if (rel.wrapper != null) {
                testEngine = comprehensiveEval(rel.wrapper);
//                if (testEngine != null) {
                    rel.comprehensiveChecked = getMinimumIdReport(testEngine.getTestReport());
                    rel.comprehensiveErrors = testEngine.getTestReportErrors();
//                } else {
//                    rel.comprehensiveChecked = getMinimumIdReport(null);
//                    rel.comprehensiveErrors = new ArrayList<String>();
//                }
            }
        }
    }

    private void minimalEval() {
        if (baseObj == null)
            return;
        TestEngine testEngine  = minimalEval(baseObj);
        // Guard seems to have a side effect
//        if (testEngine == null)
//            return;
        minimalErrors = testEngine.getTestReportErrors();
        minimalChecked = getMinimumIdReport(testEngine.getTestReport());
        for (Related rel : related) {
            if (rel.wrapper != null) {
                testEngine = minimalEval(rel.wrapper);
//                if (testEngine != null) {
                    rel.minimalChecked = getMinimumIdReport(testEngine.getTestReport());
                    rel.minimalErrors = testEngine.getTestReportErrors();
//                } else {
//                    rel.minimalChecked =  getMinimumIdReport(null);
//                    rel.minimalErrors = new ArrayList<>();
//                }
            }
        }
    }

    private TestEngine comprehensiveEval(ResourceWrapper wrapper) {
        Objects.requireNonNull(ec);
        String type = wrapper.getResourceType();
        // TODO - huh?
        File testDef = new File(new File(new File(ec.externalCache, "FhirTestCollections"), "Internal"), "Comprehensive_" + type);
        // Guard seems to have a side effect
//        if (testDef.exists()) {
            TestEngine testEngine = new TestEngine(testDef, null)
                    .setVal(new Val())
                    .setTestSession("default")
                    .setExternalCache(ec.externalCache)
                    .setTestCollection("Analysis")
                    .setTestId("Analysis")
                    .runEval(wrapper, null);
            return testEngine;
//        } else {
//            return null; // Resource does not have an eval TestScript
//        }
    }

    private Checked getMinimumIdReport(TestReport testReport) {
        if (testReport == null) return new Checked("", new MinimumId.Report(), "");
        for (TestReport.TestReportTestComponent testComponent : testReport.getTest()) {
            for (TestReport.TestActionComponent actionComponent : testComponent.getAction()) {
                TestReport.SetupActionOperationComponent operationComponent = actionComponent.getOperation();
                if (operationComponent != null) {
                    if (operationComponent.hasResult()) {
                        if (operationComponent.getResult().equals(TestReport.TestReportActionResult.ERROR)) {
//                            return new Checked(MinimumId.getReport(operationComponent.getMessage()));
                            throw new Error(operationComponent.getMessage());
                        }
                    }
                }
                TestReport.SetupActionAssertComponent assertComponent = actionComponent.getAssert();
                if (assertComponent != null) {
                    if (assertComponent.getUserData(AssertionRunner.RAW_REPORT) != null && assertComponent.getUserData(AssertionRunner.RAW_REPORT) instanceof MinimumId.Report)
                        return new Checked((MinimumId.Report) assertComponent.getUserData(AssertionRunner.RAW_REPORT));
                }
            }
        }
        return new Checked(new MinimumId.Report());
    }

    private TestEngine minimalEval(ResourceWrapper wrapper) {
        String type = wrapper.getResourceType();
        File testDef = new File(new File(new File(ec.externalCache, "FhirTestCollections"), "Internal"), "Minimal_" + type);
        // Guard seems to have a side effect
//        if (!testDef.isDirectory())
//            return null;
//        if (testDef.exists()) {
            TestEngine testEngine = new TestEngine(testDef, null)
                    .setVal(new Val())
                    .setTestCollection("Analysis")
                    .setTestId("Analysis")
                    .setTestSession("default")
                    .setExternalCache(ec.externalCache)
                    .runEval(wrapper, null);
            return testEngine;
//        } else {
//            return null; // Resource type does not have an eval TestScript configured in FTK test collections
//        }
    }

    private Ref translateToProxyServerSide(Ref theRef) {
        Objects.requireNonNull(theRef);
        ServiceProperties serviceProperties = ServiceProperties.getInstance();
        String proxyAddrPrefix = serviceProperties.getPropertyOrStop(ServicePropertiesEnum.FHIR_TOOLKIT_UI_HOME_PAGE);

        if (!theRef.asString().startsWith(proxyAddrPrefix))
            return theRef;
        Ref resourceRef;
        Ref baseRefRelative = theRef.getRelative();
        try {
            fhirBase = new Ref(new ChannelUrl(ec.externalCache).getFhirBase(theRef.getUri()));
            if (Strings.isNullOrEmpty(fhirBase.toString())) {
                generalWarnings.add("No FHIRBASE registered for this channel. This may be an MHD channel. Directing queries to channel.");
                resourceRef = theRef;
            } else if (Strings.isNullOrEmpty(theRef.getResourceType())) {
                return fhirBase;
            } else {
                resourceRef = baseRefRelative.rebase(fhirBase);
            }

        } catch (URISyntaxException e) {
            generalErrors.add("Error extracting FHIRBASE - " + e.getMessage());
            return null;
        }
        return resourceRef;
    }

    // initialize baseObj
    // if contextResource is loaded then load its contents into related
    private void loadBaseFromContext() {
        errorReporter.requireNonNull(baseRef, "baseRef must be non-null in AnalysisReport.loadBaseFromContext");
        errorReporter.requireNonNull(contextResourceBundle, "contextResourceBundle must be non-null in AnalysisReport.loadBaseFromContext");
        errorReporter.requireNull(baseObj, "baseObj must be null in AnalysisReport.loadBaseFromContext");

        Map<String, String> params = baseRef.getParametersAsMap();
        BaseResource resource = null;
        if (hasValue(params, "focusUrl")) {
            String focusUrl = params.get("focusUrl");
            ResourceWrapper wrapper = findResourceInBundle(getContextBundle(), focusUrl);
            resource = wrapper.getResource();
        } else if (hasValue(params, "fhirPath")) {
            FhirPath fhirPath = new FhirPath(params.get("fhirPath"));
            resource = resourceFromBundle((Bundle) contextResourceBundle.getResource(), fhirPath);
        } else if ("Bundle".equals(baseRef.getResourceType()) &&"Bundle".equals(contextResourceBundle.getResourceType())){
            Bundle bundle = (Bundle) contextResourceBundle.getResource();
            if (isPDBRequest(bundle)) {
                baseObj = findSubmissionSetFhirCounterpartInRequestBundle(bundle);
                if (baseObj != null) {
                    Ref baseObjRef = baseObj.getRef();
                    baseObj.setContext(bundle);
                    baseRef.addParameter("focusUrl", baseObjRef.toString());
                    baseObj.setRef(baseRef);
                }
            } else if (isPDBResponse(bundle)) {
                Ref dmLocation = findSubmissionSetFhirCounterpartInResponseBundle(bundle);
                if (dmLocation != null) {
                    try {
                        baseObj = contextResourceBundle;
                        // Since DocResponder can be independent from the docRecip, use the direct location from the PDB response if the location is a URL
                        // try direct location reference if it exists in the response
                        Ref absRef = new Ref(dmLocation.getUri().toURL()); // Will automatically use this URL in future
                        FhirClient fhirClient = new FhirClient();
                        ResourceWrapper dm = fhirClient.readResource(absRef);
                        baseObj = dm;
                        baseRef = absRef;
                    } catch (Exception ex) {
                        // try rebase using the self link if it exists
                        String bundleLink = bundle.hasLink() ? bundle.getLink("self").getUrl() : null;
                        if (bundleLink != null) {
                            Ref dmRef = dmLocation.rebase(bundleLink);
                            FhirClient fhirClient = new FhirClient();
                            ResourceWrapper dm = fhirClient.readResource(dmRef);
                            baseObj = dm;
                            baseRef = dmRef;
                        }
                        // else
                            // Default behavior
                            // Possible option: rebase using the proxy channel fhir base address
                    }
                }
            } else {
                baseObj = new ResourceWrapper(bundle)
                        .setRef(baseRef)
                        .setContext((Bundle)contextResourceBundle.getResource());
            }
        } else {
            load(baseRef, null);
        }
        if (resource != null && baseObj == null)
            baseObj = new ResourceWrapper(resource)
                    .setRef(baseRef)
                    .setContext((Bundle)contextResourceBundle.getResource());
    }

    private static boolean hasValue(Map<String, String> params, String key) {
        if (!params.containsKey(key)) return false;
        return !Strings.isNullOrEmpty(params.get(key));
    }

    private static List<String> pdbProfles = Arrays.asList(
            "IHE_MHD_Provide_Minimal_DocumentBundle",
            "IHE_MHD_Provide_Comprehensive_DocumentBundle"
    );

    public static boolean isPDBRequest(ResourceWrapper wrapper) {
        if (!wrapper.hasResource()) return false;
        if (!wrapper.getResourceType().equals("Bundle"))
            return false;
        return isPDBRequest((Bundle)wrapper.getResource());
    }

    public static boolean isPDBRequest(Bundle bundle) {
        List<String> types = new ArrayList<>();
        if (bundle.getType() != Bundle.BundleType.TRANSACTION)
            return false;
        if (bundle.hasMeta() && bundle.getMeta().hasProfile()) {
            for (CanonicalType type : bundle.getMeta().getProfile()) {
                String theType = trueType(type.asStringValue());
                types.add(theType);
            }
        }
        return hasIntersection(pdbProfles, types);
    }

    public static boolean isPDBResponse(Bundle bundle) {
        return bundle.getType() == Bundle.BundleType.TRANSACTIONRESPONSE;
    }

    private static String trueType(String theType) {
        String[] parts =  theType.split(("/"));
        if (parts.length > 0) {
            return parts[parts.length - 1];
        }
        return theType;
    }

    private static boolean hasIntersection(List<String> l1, List<String> l2) {
        return l1.stream()
                .filter(l2::contains).count() > 0;
    }

    private Ref findSubmissionSetFhirCounterpartInResponseBundle(Bundle bundle) {
        for (Bundle.BundleEntryComponent bundleEntryComponent : bundle.getEntry()) {
            Bundle.BundleEntryResponseComponent bundleEntryResponseComponent = bundleEntryComponent.getResponse();
            String location = bundleEntryResponseComponent.getLocation();
            if (!Strings.isNullOrEmpty(location)) {
                if (location.contains("DocumentManifest")) {
                    return new Ref(location);
                } else if (location.contains(String.format("/%s/",MhdTransforms.MhdListResourceName))) {
                    Ref ref = new Ref(location);
                    String localBase = ServiceProperties.getInstance().getPropertyOrStop(ServicePropertiesEnum.FHIR_TOOLKIT_BASE);
                    if (ref.getBase().toString().startsWith(localBase)) {
                        // This opaque Id convention only applies to local channels
                        if (IdBuilder.isOpaqueLogicalId(IdBuilder.SS_OPAQUE_ID, ref.getId())) {
                            return ref;
                        }
                    } else {
                        // May be an external system response, need to GET to differentiate if list is of submissionset type
                        ResourceWrapper resourceWrapper = new ResourceWrapper(ref);
                        BaseResource baseResource = resourceWrapper.getResource();
                        if (baseResource != null) {
                            if (baseResource instanceof ListResource && MhdV4.isCodedListType(baseResource, "submissionset") ) {
                               return ref;
                            }
                        }
                    }
                }

            }
        }
        log.severe("No submissionset counterpart found in response.");
        return null;
    }

    private ResourceWrapper findSubmissionSetFhirCounterpartInRequestBundle(Bundle bundle) {
        for (Bundle.BundleEntryComponent bundleEntryComponent : bundle.getEntry()) {
            Resource componentResource = bundleEntryComponent.getResource();
            if (componentResource instanceof DocumentManifest
                    || (componentResource instanceof ListResource && MhdV4.isCodedListType(componentResource, "submissionset"))) {
                ResourceWrapper wrapper = new ResourceWrapper(componentResource);
                String fullUrl = bundleEntryComponent.getFullUrl();
                if (!Strings.isNullOrEmpty(fullUrl)) {
                    Ref ref = new Ref(fullUrl);
//                    if (fullUrl.startsWith("urn:uuid:")) {
//                        ref.addParameter("focusUrl", fullUrl);
//                    } else {
                        Bundle.BundleLinkComponent bundleLinkComponent = bundle.getLink("self");
                        if (bundleLinkComponent != null && bundleLinkComponent.hasUrl()) {
                            String url = bundleLinkComponent.getUrl();
                            ref = ref.rebase(url);
                        }
//                    }
                    wrapper.setRef(ref);
                }
                return wrapper;
            }
        }
        log.severe("No submissionset counterpart found in request");
        return null;
    }

    private void loadRelatedFromPDB(Bundle context) {
        for (Bundle.BundleEntryComponent comp : context.getEntry()) {
            if (baseObj.getResource().equals(comp.getResource())) {
                // don't load baseObj into related or it will be listed twice
                List<Reference2> refs = Reference2Builder.buildReferences(baseObj.getResource());
                for (Reference2 ref : refs) {
                    String att = ref.att;
                    Ref theRef = new Ref(ref.reference);
                    if (theRef.toString().startsWith("#"))
                        continue;  // Reference2Builder.buildReferences digs too deep
                    if (theRef.isRelative()) {
                        Resource theResource = resourceFromBundle(context, theRef);
                        if (theResource != null) {
                            ResourceWrapper wrapper = new ResourceWrapper(theResource).setRef(theRef).setContext(getContextBundle());
                            related.add(new Related(wrapper, att));
                            continue;
                        }
                        if (theResource == null && comp.getResource() instanceof DomainResource)
                            theResource = resourceFromContained((DomainResource) comp.getResource(), theRef);
                        if (theResource != null) {
                            ResourceWrapper wrapper = new ResourceWrapper(theResource).setRef(theRef).setContext(getContextBundle());
                            related.add(new Related(wrapper, att));
                        }
                    }
                }
                break;
            }
        }
    }

    private Resource resourceFromContained(DomainResource resource, Ref ref) {
        if (ref == null)
            return null;
        String refId = ref.toString();  // include anchor
        if (refId == null)
            return null;
        for (Resource res : resource.getContained()) {
            String resId = res.getId();
            if (refId.equals(resId))
                return res;
        }
        return null;
    }

    private boolean isSearchSet() {
        if (contextResourceBundle == null) return false;
        if (contextResourceBundle.getResource() instanceof Bundle) {
            Bundle bundle = (Bundle) contextResourceBundle.getResource();
            return bundle.getType().equals(Bundle.BundleType.SEARCHSET);
        }
        return false;
    }

    private boolean isSearchSetSingleResult() {
        if (contextResourceBundle.getResource() == null) return false;
        if (contextResourceBundle.getResource() instanceof Bundle) {
            Bundle bundle = (Bundle) contextResourceBundle.getResource();
            return bundle.getType().equals(Bundle.BundleType.SEARCHSET) && bundle.getEntry().size() == 1;
        }
        return false;
    }

    private void loadSearchSetFromContext(Bundle bundle) {
        buildListingFromContext(bundle, "searchset");
    }

    private void plainListing(Bundle bundle) {
        buildListingFromContext(bundle, "listing");
    }

    private void buildListingFromContext(Bundle bundle, String howRelated) {
        for (Bundle.BundleEntryComponent comp : bundle.getEntry()) {
            Resource resource = comp.getResource();
            if (resource != null) {
                ResourceWrapper wrapper = new ResourceWrapper(resource).setRef(new Ref(resource.getId())).setContext(getContextBundle());
                if (baseObj == null)
                    baseObj = wrapper;
                related.add(new Related(wrapper, howRelated));
            }
        }
    }

    private Resource resourceFromBundle(Bundle bundle, FhirPath fhirPath) {
        return FhirPathEngineBuilder.evalForResource(bundle, fhirPath.getValue());
    }

    private Resource resourceFromBundle(Bundle bundle, Ref fullUrl) {
        Objects.requireNonNull(fullUrl);
        Map<String, String> urlParms = fullUrl.getParametersAsMap();
        if (urlParms.containsKey("url")) {
            String url = urlParms.get("url");
            String fullUrlString = fullUrl.asString();
            return extractResourceFromBundle(bundle, fullUrl);
        } else if (urlParms.containsKey("focusUrl")) {
            String focusUrl = urlParms.get("focusUrl");
            return extractResourceFromBundle(bundle, new Ref(focusUrl));
        }
        return null;
    }

    private Resource extractResourceFromBundle(Bundle bundle, Ref fullUrl) {
        for (Bundle.BundleEntryComponent comp : bundle.getEntry()) {
            // asString is used in case there is an anchor in the fullUrl, it should be included
            if (fullUrl.equals(comp.getFullUrl())) {
                if (fullUrl.hasAnchor()) {
                    if (comp.getResource() instanceof DomainResource)
                        return findResourceInContained((DomainResource) comp.getResource(), fullUrl);
                }
                return comp.getResource();
            }
            String url = fullUrl.asString();
            if (url != null && url.equals(comp.getFullUrl())) {
                return comp.getResource();
            }
        }
        return null;
    }

    private Resource findResourceInContained(DomainResource theResource, Ref fullUrl) {
        String anchor = fullUrl.getAnchor();
        if (anchor == null)
            return null;
        for (Resource resource : theResource.getContained()) {
            if (anchor.equals(resource.getId())) {
                return resource;
            }
        }
        return null;
    }

    private void loadBase() {
        Objects.requireNonNull(baseRef);
        Ref resourceRef;
        resourceRef = (useProxy) ? baseRef : translateToProxyServerSide(baseRef);
        if (resourceRef == null)
            return;
        baseObj = new FhirClient().setSupportRequest(true).requestGzip(useGzip).readResource(resourceRef);
        if (resourceRef.hasAnchor()) {
            BaseResource baseResource = baseObj.getResource();
            if (baseResource instanceof DomainResource) {
                Resource resource = findResourceInContained((DomainResource) baseResource, resourceRef);
                if (resource != null) {
                    baseObj = new ResourceWrapper(resource, resourceRef).setContext(getContextBundle());
                }
            } else {
                generalErrors.add("AnalysisReport#loadBase(): baseObj is not a DomainResource");
            }
        }
        baseObjectEventId = baseObj.getEventId();
        baseObjectResourceType = baseObj.getResponseResourceType();
        if (baseObjectResourceType == null) baseObjectResourceType = baseObj.getResourceType();
        if (baseObj.getStatus() != 200) {
            generalErrors.add("Status " + baseObj.getStatus());
        } else if (baseObj.getResource() instanceof OperationOutcome) {
            OperationOutcome oo = (OperationOutcome) baseObj.getResource();
            generalErrors.add(oo.getIssueFirstRep().getDiagnostics());
        }
    }

    private void loadRelated() {
        for (Ref ref : relatedRefs) {
            Ref resourceRef = translateToProxyServerSide(ref);
            if (resourceRef == null)
                return;

            ResourceWrapper thisObj = fhirClient.readResource(resourceRef);
            relatedObjs.add(thisObj);
            if (thisObj.getStatus() != 200) {
                generalErrors.add("Status " + thisObj.getStatus());
            } else if (thisObj.getResource() instanceof OperationOutcome) {
                OperationOutcome oo = (OperationOutcome) thisObj.getResource();
                generalErrors.add(oo.getIssueFirstRep().getDiagnostics());
            }
        }
    }

    private void buildRelated() {
        if (baseObj == null && contextResourceBundle != null) {
            generalErrors.add("No content is available for display. Check if FHIR submissionset counterpart resource exists in bundle.");
            plainListing((Bundle) contextResourceBundle.getResource());
            return;
        }
        errorReporter.requireNonNull(baseObj, "baseObject is null");
        errorReporter.requireNonNull(baseObj.getResource(), "baseObject references null resource");
        BaseResource baseResource = baseObj.getResource();
        if (baseResource instanceof DomainResource) {
            DomainResource domainResource = (DomainResource) baseResource;
            try {
                buildRelated(new ResourceWrapper(domainResource));
            } catch (Throwable t) {
                generalErrors.add("Do not know how to load DomainResource " + domainResource.getClass().getName() + " - " + t.getMessage());
            }
        }  else if (baseResource instanceof Binary) {
            try {
                buildRelated(baseObj);
            } catch (Throwable t) {
                generalErrors.add("Do not know how to load BaseResource " + baseResource.getClass().getName());
            }
        } else if (baseResource instanceof Bundle) {
            buildRelated(baseObj);
        } else {
            buildRelatedOther(baseResource);
            //generalErrors.add("Do not know how to load BaseResource " + baseResource.getClass().getName());
        }
    }

    static private List<String> authorTypes = Arrays.asList("Practitioner", "PractitionerRole", "Organization", "Device", "Patient", "Related", "Person");

    private boolean validAuthorType(BaseResource resource) {
        return authorTypes.contains(resource.getClass().getSimpleName());
    }

    static private List<String> authenticatorTypes = Arrays.asList("Practitioner", "PractitionerRole", "Organization");


    private boolean validAuthenticatorType(BaseResource resource) {
        return authenticatorTypes.contains(resource.getClass().getSimpleName());
    }

    // used for resoources outside of MHD - a more general approach
    private void buildRelatedOther(BaseResource resource) {
        List<Reference2> refs = this.refs.get(resource);
        if (refs == null) {
            refs = Reference2Builder.buildReferences(resource);
            this.refs.put(resource, refs);
        }
        for (Reference2 ref : refs) {
            load(new Ref(ref.reference), ref.att, resource);
        }
    }

    private void buildRelatedBundle(ResourceWrapper wrapper) {
        Bundle bundle = (Bundle) wrapper.getResource();
        if (bundle.hasLink()) {
            Bundle.BundleLinkComponent bundleLinkComponent = bundle.getLink("self");
            if (bundleLinkComponent.hasUrl()) {
                fhirBase = new Ref(bundleLinkComponent.getUrl());
            }
        }

        if (fhirBase != null)
            fhirBase = translateToProxyServerSide(fhirBase);

        for (Bundle.BundleEntryComponent entryComponent : bundle.getEntry()) {
            if (entryComponent.hasResponse()) {
                Bundle.BundleEntryResponseComponent responseComponent = entryComponent.getResponse();
                if (responseComponent.hasLocation()) {
                    String rawLocation = responseComponent.getLocation();
                    load(new Ref(rawLocation), "component", bundle);
                }
            } else {
                BaseResource request  = entryComponent.getResource();
                ResourceWrapper wrapper1 = wrapper.newWithContext().setResource(request);
                wrapper1.getRef().withFocusUrl(entryComponent.getFullUrl());
                addRelated(wrapper1, "In Bundle", false);
            }
        }
    }

    private void buildRelatedDocumentManifest(ResourceWrapper wrapper) {
        DocumentManifest documentManifest = (DocumentManifest) wrapper.getResource();
        log.info("buildRelated DocumentManifest");
        // subject
        if (documentManifest.hasSubject()) {
            Related related = load(new Ref(documentManifest.getSubject()), "subject", documentManifest);
            if (related != null && related.contained)
                generalErrors.add("DocumentManifest.subject is contained");
        }
        // author - contained only
        if (documentManifest.hasAuthor()) {
            for (Reference reference : documentManifest.getAuthor()) {
                Related related = load(new Ref(reference), "author", documentManifest);
                if (related != null && !related.contained)
                    generalErrors.add("DocumentManifest.author must be contained");
                if (related != null && related.wrapper.hasResource()) {
                    if (!validAuthorType(related.wrapper.getResource()))
                        generalErrors.add("DocumentManifest: " + related.wrapper.getResource().getClass().getSimpleName() + " is not a valid Author resource");
                }
            }
        }
        // recipient
        if (documentManifest.hasRecipient()) {
            for (Reference reference : documentManifest.getRecipient()) {
                load(new Ref(reference), "recipient", documentManifest);
            }
        }
        // content
        if (documentManifest.hasContent()) {
            boolean hasDocRef = false;
            for (Reference reference : documentManifest.getContent()) {
                Related rel = load(new Ref(reference), "content", documentManifest);
                if (rel != null && rel.wrapper != null && rel.wrapper.hasResource() && rel.wrapper.isOk() && rel.wrapper.getResource() instanceof DocumentReference)
                    hasDocRef = true;
            }
            if (!hasDocRef)
                generalErrors.add("DocumentManifest has no related DocumentReferences");
        } else {
            generalErrors.add("DocumentManifest has no content resources - shall have DocumentReference");
        }
        if (related.isEmpty() && hasContextBundle())
            buildRelatedBundle(contextResourceBundle);
    }

    private void buildRelatedDocumentReference(ResourceWrapper wrapper) {
        DocumentReference documentReference = (DocumentReference) wrapper.getResource();
        log.info("buildRelated DocumentReference");
        // author - contained only
        if (documentReference.hasAuthor()) {
            for (Reference reference : documentReference.getAuthor()) {
                Related related = load(new Ref(reference), "author", documentReference);
                if (related != null && !related.contained)
                    generalErrors.add("DocumentReference.author must be contained");
                if (related != null && related.wrapper.hasResource()) {
                    if (!validAuthorType(related.wrapper.getResource()))
                        generalErrors.add("DocumentReference: " + related.wrapper.getResource().getClass().getSimpleName() + " is not a valid Author resource");
                }
            }
        }
        // subject
        if (documentReference.hasSubject()) {
            Related related = load(new Ref(documentReference.getSubject()), "subject", documentReference);
            if (related.contained)
                generalErrors.add("DocumentReference.subject is contained");
        }
        // authenticator - contained only
        if (documentReference.hasAuthenticator()) {
            Related related = load(new Ref(documentReference.getAuthenticator()), "Authenticator", documentReference);
            if (related != null && !related.contained)
                generalErrors.add("DocumentReference.authenticator must be contained");
            if (related != null && related.wrapper.hasResource()) {
                if (!validAuthenticatorType(related.wrapper.getResource()))
                    generalErrors.add("DocumentReference: " + related.wrapper.getResource().getClass().getSimpleName() + " is not a valid Authenticator resource");
            }
        }
        // custodian - not defined
        // relatesTo
        if (documentReference.hasRelatesTo()) {
            for (DocumentReference.DocumentReferenceRelatesToComponent component : documentReference.getRelatesTo()) {
                load(new Ref(component.getTarget()), "relatesTo", documentReference);
            }
        }
        if (documentReference.hasContext()) {
            // sourcePatientInfo - contained only
            if (documentReference.getContext().hasSourcePatientInfo()) {
//                Related related = load(new Ref(documentReference.getContext().getSourcePatientInfo()), "SourcePatientInfo", documentReference);
                if (documentReference.hasId() && !documentReference.getId().equals("null")) {
                    Ref spi = new Ref(documentReference.getId())
                            .withAnchor(documentReference.getContext()
                                    .getSourcePatientInfo()
                                    .getReference());
                    Related related = load(spi,
                            "SourcePatientInfo",
                            documentReference);
                    if (related != null && !related.contained)
                        generalErrors.add("DocumentReference.context.sourcePatientInfo must be contained");
                    if (related != null && related.wrapper.hasResource()) {
                        if (!related.wrapper.getResource().getClass().getSimpleName().equals("Patient"))
                            generalErrors.add("DocumentReference: " + related.wrapper.getResource().getClass().getSimpleName() + " is not a valid context/sourcePatientInfo resource");
                    }
                }
            }
            // context/related
            if (documentReference.getContext().hasRelated()) {
                for (Reference reference : documentReference.getContext().getRelated()) {
                    Related related = load(new Ref(reference), "context/related", documentReference);
                    if (related != null && related.wrapper.hasResource()) {
                        if (!related.wrapper.getResource().getClass().getSimpleName().equals("DocumentReference"))
                            generalErrors.add("DocumentReference: " + related.wrapper.getResource().getClass().getSimpleName() + " is not a valid context/related resource");
                    }
                }
            }
        }
        if (documentReference.hasContent()) {
            for (DocumentReference.DocumentReferenceContentComponent component : documentReference.getContent()) {
                String url = component.getAttachment().getUrl();
                if (Strings.isNullOrEmpty(url)) {
                    generalErrors.add("DocumentReference: content.attachment.url is missing." );
                } else {
                    Ref ref = new Ref(url);
                    Related related = load(ref, "content/attachment", documentReference);
                    if (related != null && related.wrapper.hasResource()) {
                        if (!related.wrapper.getResource().getClass().getSimpleName().equals("Binary"))
                            generalErrors.add("DocumentReference: " + related.wrapper.getResource().getClass().getSimpleName() + " is not a valid content/attachment resource");
                        buildRelatedBinary(related.wrapper);
                    }
                }
            }
        }

    }

    private void buildRelatedPatient(ResourceWrapper wrapper) {
        Patient patient = (Patient) wrapper.getResource();
        log.info("buildRelated Patient");
    }

    private void buildRelatedListResource(ResourceWrapper wrapper) {
        ListResource list = (ListResource) wrapper.getResource();
        log.info("buildRelated List");
        // subject
        if (list.hasSubject()) {
            Related related = load(new Ref(list.getSubject()), "subject", list);
            if (related.contained)
                generalErrors.add("List.subject is contained");
        } else {
            generalErrors.add("List has no subject");
        }
        // source - not defined
        // entry/item
        if (list.hasEntry()) {
            for (int i=0; i<list.getEntry().size(); i++) {
                ListResource.ListEntryComponent component = list.getEntry().get(i);
                load(new Ref(component.getItem()), Collections.singletonList(DocumentReference.class));
            }
        }
    }

    private void buildRelatedBinary(ResourceWrapper wrapper) {
        Binary binary = (Binary) wrapper.getResource();
        log.info("buildRelated Binary");
    }

    private String extractDocument(Binary binary) {
        String contentType = binary.getContentType();
        byte[] data = binary.getData();
        if (data == null)
            return null;
        String strData = new String(data);
        byte[] byteData = strData.getBytes();
        String id = new DocumentCache(ec).putDocumentCache(data, contentType);
        // this must match what is in GetDocumentRequest
        ServiceProperties serviceProperties = ServiceProperties.getInstance();
        String url = serviceProperties.getPropertyOrStop(ServicePropertiesEnum.FHIR_TOOLKIT_BASE) +
                "/log/document/" +
                id;
        return url;
    }

    private void buildRelated(ResourceWrapper wrapper) {
        BaseResource resource = wrapper.getResource();
        if (resource instanceof DocumentManifest)
            buildRelatedDocumentManifest(wrapper);
        else if (resource instanceof DocumentReference)
            buildRelatedDocumentReference(wrapper);
        else if (resource instanceof ListResource)
            buildRelatedListResource(wrapper);
        else if (resource instanceof Binary)
            buildRelatedBinary(wrapper);
        else if (resource instanceof Patient)
            buildRelatedPatient(wrapper);
        else if (resource instanceof Bundle)
            buildRelatedBundle(wrapper);
        else
            buildRelatedOther(resource);
    }

    private Related getFromRelated(Ref ref) {
        Objects.requireNonNull(ref);
        if (ref.isRelative() && fhirBase != null)
            ref.rebase(fhirBase);
        for (Related rel : related) {
            if (ref.equals(rel.wrapper.getRef()))
                return rel;
        }
        return null;
    }

    private Related load(Ref ref, List<Class> types) {
        List<String> names = new ArrayList<>();
        if (types != null) {
            for (Class c : types)
                names.add(c.getSimpleName());
        }
        Related rel = load(ref, "", null);
        if (rel != null && rel.wrapper != null && rel.wrapper.hasResource()) {
            Class theClass = rel.wrapper.getResource().getClass();
            if (types != null && !types.contains(theClass))
                generalErrors.add("Trying to load one of " + names + " but got a " + theClass.getSimpleName());
        }
        return rel;
    }

    private Resource getResourceById(List<Resource> list, String id) {
        for (Resource resource : list) {
            if (id.equals(resource.getId()))
                return resource;
        }
        return null;
    }

    private Related addRelated(ResourceWrapper wrapper, String howRelated, boolean contained) {
        if (baseRef.equals(wrapper.getRef()))
            return null;
        Related rel = getRelated(wrapper.getRef());
        if (rel != null)
            return rel;
        rel = new Related(wrapper, howRelated);
        if (contained)
            rel.contained();
        related.add(rel);
        return rel;
    }

    private Related load(Ref ref, String howRelated, BaseResource parent) {
        if (hasContextBundle()) {
            ResourceWrapper wrapper = findResourceInBundle(getContextBundle(), ref.toString());
            if (wrapper != null) {
                if (ref.toString().startsWith("urn:uuid:")) {
                    Ref theRef = contextResourceBundle.getRef().copy();
                    theRef.addParameter("focusUrl", ref.toString());
                    wrapper.setRef(theRef);
                }
                return addRelated(wrapper, howRelated, false);
            }
        }
        if (ref.hasAnchor()) {
            String anchor = ref.getAnchor();
            if (parent instanceof DomainResource) {
                DomainResource domainResource = (DomainResource) parent;
                if (domainResource.hasContained()) {
                    DomainResource contained = (DomainResource) getResourceById(domainResource.getContained(), anchor);
                    ResourceWrapper wrapper = new ResourceWrapper(contained).setContext(getContextBundle());
                    if (ref.isRelative() && baseRef != null)
                        wrapper.setRef(ref.rebase(baseRef));
                    else
                        wrapper.setRef(ref);
                    return addRelated(wrapper, howRelated + "/contained", true);
                }
            }
            if (!ref.asString().endsWith("html"))
                generalErrors.add("Do not understand address " + anchor +
                                (parent == null ? "" : " relative to parent id " + parent.getId())
                );
            return null;
        }
        Related rel = getFromRelated(ref);
        if (rel == null) {
            if (ref.isContained() && (parent instanceof DomainResource)) {
                ResourceWrapper wrapper = new ResourceWrapper(ref.getContained((DomainResource) parent)).setContext((Bundle)contextResourceBundle.getResource());
                return addRelated(wrapper, howRelated + "/contained", true);
            }
            if (ref.isRelative()) {
                if (fhirBase != null)
                    ref = ref.rebase(fhirBase);
                else if (baseRef != null)
                    ref = ref.rebase(baseRef);
                else
                    generalErrors.add("Ref (" + ref + ") is relative and both fhirBase and baseRef are null.");
            }
            ResourceWrapper wrapper;
            try {
                log.info("Read " + ref.asString());
                wrapper = fhirClient.readResource(ref);
            } catch (Throwable e) {
                generalErrors.add(e.getMessage());
                if (contextResourceBundle == null)
                    return null;
                wrapper = new ResourceWrapper(ref).setContext((Bundle)contextResourceBundle.getResource());
                return addRelated(wrapper, howRelated, false);
            }
            if (fhirClient.getStatus() == 200) {
                return addRelated(wrapper, howRelated, false);
            } else {
                generalErrors.add("Cannot load " + ref + " status was " + fhirClient.getStatus());
                return null;
            }
        }
        return rel;
    }

    private Related getRelated(Ref ref) {
        for (Related rel1 : related) {
            if (rel1.wrapper.getRef().equals(ref))
                return rel1;
        }
        return null;
    }

    private Resource getContained(DomainResource resource, Reference reference) {
        String id = reference.getId();
        if (id == null || id.equals(""))
            return null;
        for (Resource resource1 : resource.getContained()) {
            if (id.equals(resource1.getId()))
                return resource1;
        }
        return null;
    }

    private void loadContained(DomainResource parentResource, String containedType, List<Reference> references) {
        for (Reference reference : references) {
            Resource resource = getContained(parentResource, reference);
            if (resource == null) {
                load(new Ref(reference), "contained", parentResource);
                generalErrors.add(parentResource.getClass().getSimpleName() + ": external " + containedType + " referenced - shall be contained.");
            } else {
                related.add(new Related(new ResourceWrapper(resource).setContext((Bundle)contextResourceBundle.getResource()), "contained"));
            }
        }
    }

    public List<String> getGeneralErrors() {
        return generalErrors;
    }

    public String getBaseObjectEventId() {
        return baseObjectEventId;
    }

    public AnalysisReport withValidation(boolean runValidation) {
        this.runValidation = runValidation;
        return this;
    }
}
