package gov.nist.asbestos.analysis;

import gov.nist.asbestos.client.Base.DocumentCache;
import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.resolver.ChannelUrl;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
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


    private Report buildReport() {
        Report report = new Report();

        report.source = source;
        report.errors = new ArrayList<>(generalErrors);
        report.warnings = new ArrayList<>(generalWarnings);

        if (baseObj != null && baseObj.getResource() != null) {
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
            }
            report.objects.add(relatedReport);
        }

        return report;
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


//    public AnalysisReport(List<Ref> baseRefs, String source, EC ec) {
//        this.relatedRefs.addAll(baseRefs);
//        this.source = source;
//        this.ec = ec;
//        this.codesValidation = new CodesValidation(ec);
//    }

//    private Report runRelateOnly() {
//        try {
//            loadRelated();
//            if (!generalErrors.isEmpty())
//                return buildReport();
//        } catch (Throwable t) {
//            generalErrors.add(ExceptionUtils.getStackTrace(t));
//            return buildReport();
//        }
//    }

    public Report run() {
        try {
            if (baseRef != null && baseObj == null) {
                loadBase();
                if (!generalErrors.isEmpty())
                    return buildReport();
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

    public class Checked {
        String className;
        MinimumId.Report report;
        String script;

        Checked(String className, MinimumId.Report report, String script) {
            this.className = className;
            this.report = report;
            this.script = script;
        }

        Checked(MinimumId.Report report) {
            this.className = "";
            this.script = "";
            this.report = report;
        }

        public String toString() {
            return "Checked: " + className + " Script: " + script + " Atts: " + report.expected;
        }
    }

    private void buildAtts() {
        atts = ResourceHasMethodsFilter.toMap(baseObj.getResource());
        for (Related rel : related) {
            if (rel.wrapper.hasResource())
                rel.atts = ResourceHasMethodsFilter.toMap(rel.wrapper.getResource());
        }
    }

    private void binaryEval() {
        if (baseObj.getResource().getClass().getSimpleName().equals("Binary")) {
            binaryUrl = extractDocument((Binary) baseObj.getResource());
        }
        for (Related rel : related) {
            if (rel.wrapper.getResource() instanceof Binary)
                rel.binaryUrl = extractDocument((Binary) rel.wrapper.getResource());
        }
    }


    private void codingEval() {
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
//        List<String> errors = testEngine.getTestReportErrors();
//        return errors;
    }

    private Checked getMinimumIdReport(TestReport testReport) {
        if (testReport == null) return new Checked("", new MinimumId.Report(), "");
        for (TestReport.TestReportTestComponent testComponent : testReport.getTest()) {
            for (TestReport.TestActionComponent actionComponent : testComponent.getAction()) {
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

    private void loadBase() {
        Objects.requireNonNull(baseRef);
        Ref resourceRef = translateToProxyServerSide(baseRef);
        if (resourceRef == null)
            return;
//        Ref baseRefRelative = baseRef.getRelative();
//        try {
//            fhirBase = new Ref(new ChannelUrl(ec.externalCache).getFhirBase(baseRef.getUri()));
//            if (fhirBase.toString().equals("")) {
//                generalWarnings.add("No FHIRBASE registered for this channel. This may be an MHD channel. Directing queries to channel.");
//                resourceRef = baseRef;
//            } else {
//                resourceRef = baseRefRelative.rebase(fhirBase);
//            }
//
//        } catch (URISyntaxException e) {
//            generalErrors.add("Error extracting FHIRBASE - " + e.getMessage());
//            return;
//        }

        baseObj = fhirClient.readResource(resourceRef);
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
            generalErrors.add("Do not know how to load BaseResource " + baseResource.getClass().getName());
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

    private void buildRelated(Bundle bundle) {
//        Ref base = null;
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
//                    Ref entryRef;
//                    Ref rawRef = new Ref(rawLocation);
//                    if (rawRef.isRelative()) {
//                        if (base == null) {
//                            generalErrors.add("response.location is " + rawLocation + " and no self link is present");
//                            continue;
//                        }
//                        entryRef = rawRef.rebase(base);
//                    } else {
//                        entryRef = rawRef;
//                    }
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
        } else {
            generalErrors.add("DocumentManifest has no subject");
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
        } else {
            generalErrors.add("DocumentReference has no subject");
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
                Related related = load(new Ref(documentReference.getContext().getSourcePatientInfo()), "SourcePatientInfo", documentReference);
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
            generalErrors.add("Do not understand resource type " + baseResource.getClass().getSimpleName());
    }

    private Related getFromRelated(Ref ref) {
        Objects.requireNonNull(ref);
        if (ref.isRelative())
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

    private Related load(Ref ref, String howRelated, BaseResource parent) {
        Related rel = getFromRelated(ref);
        if (rel == null) {
            if (ref.isContained() && (parent instanceof DomainResource)) {
                Related rel2 = new Related(new ResourceWrapper(ref.getContained((DomainResource) parent)), howRelated + "/contained").contained();
                related.add(rel2);
                return rel2;
            }
            if (ref.isRelative())
                ref = ref.rebase(fhirBase);
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
}
