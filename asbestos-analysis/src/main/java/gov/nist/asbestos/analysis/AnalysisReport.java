package gov.nist.asbestos.analysis;

import gov.nist.asbestos.client.Base.DocumentCache;
import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.reporting.IErrorReporter;
import gov.nist.asbestos.client.resolver.ChannelUrl;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.testEngine.engine.AssertionRunner;
import gov.nist.asbestos.testEngine.engine.TestEngine;
import gov.nist.asbestos.testEngine.engine.assertion.MinimumId;
import gov.nist.asbestos.utilities.ResourceHasMethodsFilter;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.hl7.fhir.r4.model.*;

import java.io.File;
import java.net.URISyntaxException;
import java.util.*;

public class AnalysisReport {
    private static Logger log = Logger.getLogger(AnalysisReport.class);
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
    private FhirClient fhirClient = new FhirClient();
    private String source;
    private EC ec;
    private CodesValidation codesValidation;
    private Map atts;
    private String binaryUrl;
    private boolean useGzip = false;
    private boolean useProxy = false;
    private BaseResource contextResource = null;
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

    private Report buildReport() {
        Report report = new Report();
        report.baseObjectEventId = baseObjectEventId;
        report.baseObjectResourceType = baseObjectResourceType;

        report.source = source;
        report.errors = new ArrayList<>(generalErrors);
        report.warnings = new ArrayList<>(generalWarnings);

        if (baseObj != null && baseObj.getResource() != null && !isSearchSet()) {
            report.base = new RelatedReport(baseObj, "");
            report.base.comprehensiveErrors = comprehensiveChecked == null ? new ArrayList<>() : comprehensiveChecked.report.missing; //comprehensiveErrors;
            report.base.isComprehensive = report.base.comprehensiveErrors.isEmpty(); // comprehensiveErrors != null && comprehensiveErrors.isEmpty();
            report.base.minimalErrors = minimalChecked == null ? new ArrayList<>() : minimalChecked.report.missing; //minimalErrors;
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
                relatedReport.isComprehensive = relatedReport.comprehensiveErrors.isEmpty(); //rel.comprehensiveErrors != null && rel.comprehensiveErrors.isEmpty();
                relatedReport.minimalErrors = rel.minimalChecked == null ? new ArrayList<>() : rel.minimalChecked.report.missing;
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
        FhirClient fhirClient = new FhirClient()
                .sendGzip(gzip)
                .requestGzip(gzip);
        ResourceWrapper wrapper = fhirClient.writeResource(resource,
                new Ref(validationServer + "/" + resourceType + "/$validate?profile=http://hl7.org/fhir/StructureDefinition/" + resourceType),
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

    public AnalysisReport withGzip(boolean value) {
        useGzip = value;
        return this;
    }

    public AnalysisReport withProxy(boolean value) {
        useProxy = value;
        return this;
    }

    public AnalysisReport withContextResource(BaseResource contextResource) {
        this.contextResource = contextResource;
        return this;
    }

    public AnalysisReport analyseRequest(boolean isRequest) {
        this.isRequest = isRequest;
        return this;
    }

    private ResourceWrapper findResourceInBundle(Bundle bundle, String ref) {
        for (Bundle.BundleEntryComponent comp : bundle.getEntry()) {
            if (ref.equals(comp.getFullUrl())) {
                ResourceWrapper wrapper = new ResourceWrapper(comp.getResource());
                wrapper.setRef(new Ref(ref));
                return wrapper;
            }
        }
        return null;
    }

    public Report run() {
        try {
            if (baseRef != null && baseObj == null && contextResource != null) {
                loadBaseFromContext();
                if (baseObj == null)
                    loadBase();
                Objects.requireNonNull(baseObj);
            }
            else if (baseRef != null && baseObj == null) {
                loadBase();
                if (!generalErrors.isEmpty())
                    return buildReport();
            } else if (contextResource != null &&
                    contextResource instanceof Bundle &&
                    ((Bundle) contextResource).hasType() &&
                    ((Bundle) contextResource).getType().equals(Bundle.BundleType.SEARCHSET)) {
                Bundle bundle = (Bundle) contextResource;
                loadSearchSetFromContext(bundle);
            } else if (baseRef == null &&
                    contextResource != null &&
                    !(contextResource instanceof Bundle)) {
                baseObj = new ResourceWrapper(contextResource);
            }
            buildRelated();
            comprehensiveEval();
            minimalEval();
            codingEval();
            buildAtts();
            binaryEval();
            return buildReport();
        } catch (Throwable t) {
            generalErrors.add(ExceptionUtils.getStackTrace(t));
            return buildReport();
        }
    }


    private void buildAtts() {
        if (baseObj == null)
            return;
        atts = ResourceHasMethodsFilter.toMap(baseObj.getResource());
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
        TestEngine testEngine = comprehensiveEval(baseObj);
        Checked checked = getMinimumIdReport(testEngine.getTestReport());
        comprehensiveErrors = testEngine.getTestReportErrors();
        comprehensiveChecked = checked;
        for (Related rel : related) {
            testEngine = comprehensiveEval(rel.wrapper);
            rel.comprehensiveChecked = getMinimumIdReport(testEngine.getTestReport());
            rel.comprehensiveErrors = testEngine.getTestReportErrors();
        }
    }

    private void minimalEval() {
        TestEngine testEngine  = minimalEval(baseObj);
        minimalErrors = testEngine.getTestReportErrors();
        minimalChecked = getMinimumIdReport(testEngine.getTestReport());
        for (Related rel : related) {
            testEngine = minimalEval(rel.wrapper);
            rel.minimalChecked = getMinimumIdReport(testEngine.getTestReport());
            rel.minimalErrors = testEngine.getTestReportErrors();
        }
    }

    private TestEngine comprehensiveEval(ResourceWrapper wrapper) {
        File testDef = new File(new File(new File(ec.externalCache, "FhirTestCollections"), "Internal"), "Comprehensive");
        TestEngine testEngine = new TestEngine(testDef)
                .setVal(new Val())
                .setTestSession("default")
                .setExternalCache(ec.externalCache)
                .runEval(wrapper, null);
        return testEngine;
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
        File testDef = new File(new File(new File(ec.externalCache, "FhirTestCollections"), "Internal"), "Minimal");
        TestEngine testEngine = new TestEngine(testDef)
                .setVal(new Val())
                .setTestSession("default")
                .setExternalCache(ec.externalCache)
                .runEval(wrapper, null);
        return testEngine;
    }

    private Ref translateToProxyServerSide(Ref theRef) {
        Ref resourceRef;
        Ref baseRefRelative = theRef.getRelative();
        try {
            fhirBase = new Ref(new ChannelUrl(ec.externalCache).getFhirBase(theRef.getUri()));
            if (fhirBase.toString().equals("")) {
                generalWarnings.add("No FHIRBASE registered for this channel. This may be an MHD channel. Directing queries to channel.");
                resourceRef = theRef;
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
        errorReporter.requireNonNull(contextResource, "contextResource must be non-null in AnalysisReport.loadBaseFromContext");
        errorReporter.requireNull(baseObj, "baseObj must be null in AnalysisReport.loadBaseFromContext");
        Resource resource = resourceFromBundle((Bundle)contextResource, baseRef);
        if (resource != null)
            baseObj = new ResourceWrapper(resource).setRef(baseRef);
        // load all bundle parts into related so they are found without pinging a server
//        if (contextResource instanceof Bundle) {
//            Bundle context = (Bundle) contextResource;
//            loadRelatedFromPDB(context);
//        }
 //       errorReporter.requireNonNull(baseObj, "baseObj must be loaded by AnalysisReport.loadBaseFromContext");
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
                            ResourceWrapper wrapper = new ResourceWrapper(theResource).setRef(theRef);
                            related.add(new Related(wrapper, att));
                            continue;
                        }
                        if (theResource == null && comp.getResource() instanceof DomainResource)
                            theResource = resourceFromContained((DomainResource) comp.getResource(), theRef);
                        if (theResource != null) {
                            ResourceWrapper wrapper = new ResourceWrapper(theResource).setRef(theRef);
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
        if (contextResource == null) return false;
        if (contextResource instanceof Bundle) {
            Bundle bundle = (Bundle) contextResource;
            return bundle.getType().equals(Bundle.BundleType.SEARCHSET);
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
            ResourceWrapper wrapper = new ResourceWrapper(resource).setRef(new Ref(resource.getId()));
            if (baseObj == null)
                baseObj = wrapper;
            related.add(new Related(wrapper, howRelated));
        }
    }

    private Resource resourceFromBundle(Bundle bundle, Ref fullUrl) {
        Objects.requireNonNull(fullUrl);
        for( Bundle.BundleEntryComponent comp : bundle.getEntry()) {
            // asString is used in case there is an anchor in the fullUrl
            if (fullUrl.asString().equals(comp.getFullUrl())) {
                if (fullUrl.hasAnchor()) {
                    if (comp.getResource() instanceof DomainResource)
                        return findResourceInContained((DomainResource) comp.getResource(), fullUrl);
                }
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
        baseObj = new FhirClient().requestGzip(useGzip).readResource(resourceRef);
        if (resourceRef.hasAnchor()) {
            BaseResource baseResource = baseObj.getResource();
            if (baseResource instanceof DomainResource) {
                Resource resource = findResourceInContained((DomainResource) baseResource, resourceRef);
                if (resource != null) {
                    baseObj = new ResourceWrapper(resource, resourceRef);
                }
            } else {
                generalErrors.add("AnalysisReport#loadBase(): baseObj is not a DomainResource");
            }
        }
        baseObjectEventId = baseObj.getEventId();
        baseObjectResourceType = baseObj.getResponseResourceType();
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
        if (baseObj == null && contextResource != null && contextResource instanceof Bundle) {
            generalErrors.add("baseObject is null - shown is listing of contents only");
            plainListing((Bundle) contextResource);
            return;
        }
        errorReporter.requireNonNull(baseObj, "baseObject is null");
        BaseResource baseResource = baseObj.getResource();
        if (baseResource instanceof DomainResource) {
            DomainResource domainResource = (DomainResource) baseResource;
            try {
                buildRelated(domainResource);
            } catch (Throwable t) {
                generalErrors.add("Do not know how to load DomainResource " + domainResource.getClass().getName() + " - " + t.getMessage());
            }
        }  else if (baseResource instanceof Binary) {
            try {
                buildRelated(baseResource);
            } catch (Throwable t) {
                generalErrors.add("Do not know how to load BaseResource " + baseResource.getClass().getName());
            }
        } else if (baseResource instanceof Bundle) {
            buildRelated(baseResource);
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

    private void buildRelated(Bundle bundle) {
        if (bundle.hasLink()) {
            Bundle.BundleLinkComponent bundleLinkComponent = bundle.getLink("self");
            if (bundleLinkComponent.hasUrl()) {
                fhirBase = new Ref(bundleLinkComponent.getUrl());
            }
        }

        fhirBase = translateToProxyServerSide(fhirBase);

        for (Bundle.BundleEntryComponent entryComponent : bundle.getEntry()) {
            if (entryComponent.hasResponse()) {
                Bundle.BundleEntryResponseComponent responseComponent = entryComponent.getResponse();
                if (responseComponent.hasLocation()) {
                    String rawLocation = responseComponent.getLocation();
                    load(new Ref(rawLocation), "component", bundle);
                }
            }
        }
    }

    private void buildRelated(DocumentManifest documentManifest) {
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
    }

    private void buildRelated(DocumentReference documentReference) {
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
                Ref spi = new Ref(documentReference.getId()).withAnchor(documentReference.getContext().getSourcePatientInfo().getReference());
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
                Related related = load(new Ref(component.getAttachment().getUrl()), "content/attachment", documentReference);
                if (related != null && related.wrapper.hasResource()) {
                    if (!related.wrapper.getResource().getClass().getSimpleName().equals("Binary"))
                        generalErrors.add("DocumentReference: " + related.wrapper.getResource().getClass().getSimpleName() + " is not a valid content/attachment resource");
                    buildRelated((Binary) related.wrapper.getResource());
                }
            }
        }

    }

    private void buildRelated(Patient patient) {
        log.info("buildRelated Patient");
    }

    private void buildRelated(ListResource list) {
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

    private void buildRelated(Binary binary) {
        log.info("buildRelated Binary");
    }

    private String extractDocument(Binary binary) {
        String contentType = binary.getContentType();
        byte[] data = binary.getData();
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

    private void buildRelated(BaseResource baseResource) {
        if (baseResource instanceof DocumentManifest) buildRelated((DocumentManifest) baseResource);
        else if (baseResource instanceof DocumentReference) buildRelated((DocumentReference) baseResource);
        else if (baseResource instanceof ListResource) buildRelated((ListResource) baseResource);
        else if (baseResource instanceof Binary) buildRelated((Binary) baseResource);
        else if (baseResource instanceof Patient) buildRelated((Patient) baseResource);
        else if (baseResource instanceof Bundle) buildRelated((Bundle) baseResource);
        else
            buildRelatedOther(baseResource);
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
        for (Class c : types)
            names.add(c.getSimpleName());
        Related rel = load(ref, "", null);
        if (rel.wrapper != null && rel.wrapper.hasResource()) {
            Class theClass = rel.wrapper.getResource().getClass();
            if (!types.contains(theClass))
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

    private Related load(Ref ref, String howRelated, BaseResource parent) {
        if (contextResource != null && contextResource instanceof Bundle) {
            ResourceWrapper wrapper = findResourceInBundle((Bundle)contextResource, ref.toString());
            if (wrapper != null) {
                Related rel = new Related(wrapper, howRelated);
                related.add(rel);
                return rel;
            }
        }
        if (ref.hasAnchor()) {
            String anchor = ref.getAnchor();
            if (parent instanceof DomainResource) {
                DomainResource domainResource = (DomainResource) parent;
                if (domainResource.hasContained()) {
                    DomainResource contained = (DomainResource) getResourceById(domainResource.getContained(), anchor);
                    ResourceWrapper wrapper = new ResourceWrapper(contained);
                    if (ref.isRelative())
                        wrapper.setRef(ref.rebase(baseRef));
                    else
                        wrapper.setRef(ref);
                    Related rel = new Related(wrapper, howRelated + "/contained");
                    rel.contained();
                    related.add(rel);
                    return rel;
                }
            }
            if (!ref.asString().endsWith("html"))
                generalErrors.add("Do not understand address " + anchor + " relative to parent id " + parent.getId());
            return null;
        }
        Related rel = getFromRelated(ref);
        if (rel == null) {
            if (ref.isContained() && (parent instanceof DomainResource)) {
                Related rel2 = new Related(new ResourceWrapper(ref.getContained((DomainResource) parent)), howRelated + "/contained").contained();
                //rel2.wrapper.setRef(new Ref());
                related.add(rel2);
                return rel2;
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
                wrapper = fhirClient.readResource(ref);
            } catch (Throwable e) {
                generalErrors.add(e.getMessage());
                wrapper = new ResourceWrapper(ref);
                Related rel2 = new Related(wrapper, howRelated);
                related.add(rel2);
                return rel2;
            }
            if (fhirClient.getStatus() == 200) {
                Related rel2 = new Related(wrapper, howRelated);
                related.add(rel2);
                return rel2;
            } else {
                generalErrors.add("Cannot load " + ref + " status was " + fhirClient.getStatus());
                return null;
            }
        }
        return rel;
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
                related.add(new Related(new ResourceWrapper(resource), "contained"));
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
