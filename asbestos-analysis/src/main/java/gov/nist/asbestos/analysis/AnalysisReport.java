package gov.nist.asbestos.analysis;

import com.google.gson.Gson;
import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.testEngine.engine.TestEngine;
import gov.nist.asbestos.utilities.ResourceHasMethodsFilter;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.hl7.fhir.r4.model.*;

import java.io.File;
import java.util.*;

public class AnalysisReport {
    private static Logger log = Logger.getLogger(AnalysisReport.class);
    private Ref fhirBase = null;
    private Ref baseRef = null;
    private ResourceWrapper baseObj = null;
    private List<Related> related = new ArrayList<>();
    private List<String> minimalErrors = new ArrayList<>();
    private Checked minimalChecked;
    private List<String> comprehensiveErrors;
    private Checked comprehensiveChecked;
    private List<String> codingErrors = new ArrayList<>();
    private List<String> generalErrors = new ArrayList<>();
    private FhirClient fhirClient = new FhirClient();
    private String source;
    private EC ec;
    private CodesValidation codesValidation;
    private Map atts;

    class Related {
        ResourceWrapper wrapper;
        String howRelated;
        private List<String> minimalErrors;
        private List<String> comprehensiveErrors;
        private List<String> codingErrors = new ArrayList<>();
        Checked comprehensiveChecked;
        Checked minimalChecked;
        Map atts;
        boolean contained = false;

        Related(ResourceWrapper wrapper, String howRelated) {
            this.wrapper = wrapper;
            this.howRelated = howRelated;
        }

        Related contained() {
            contained = true;
            return this;
        }
    }

    public static class RelatedReport {
        String name;
        String relation;
        String url;
        boolean isMinimal;
        boolean isComprehensive;
        List<String> minimalErrors;
        List<String> comprehensiveErrors;
        List<String> codingErrors;
        String minimalChecked;
        String comprehensiveChecked;
        Map atts;

        RelatedReport(ResourceWrapper wrapper, String relation) {
            this.name = wrapper.getResource().getClass().getSimpleName();
            this.relation = relation;
            if (wrapper.getRef() == null)
                this.url = "Contained";
            else
                this.url = wrapper.getRef().toString();
        }
    }

    public static class Report {
        String source = null;
        RelatedReport base = null;
        List<RelatedReport> objects = new ArrayList<>();
        List<String> errors;

        public Report() {}

        public Report(String error) {
            errors = Collections.singletonList(error);
        }
    }


    private Report buildReport() {
        Report report = new Report();

        report.source = source;
        report.errors = new ArrayList<>(generalErrors);

        if (baseObj != null && baseObj.getResource() != null) {
            report.base = new RelatedReport(baseObj, "");
            report.base.comprehensiveErrors = comprehensiveErrors;
            report.base.isComprehensive = comprehensiveErrors.isEmpty();
            report.base.minimalErrors = minimalErrors;
            report.base.isMinimal = minimalErrors.isEmpty();
            report.base.minimalChecked = minimalChecked.attsChecked;
            report.base.comprehensiveChecked = comprehensiveChecked.attsChecked;
            report.base.codingErrors = codingErrors;
            report.base.atts = atts;
        }

        for (Related rel : related) {
            ResourceWrapper wrapper = rel.wrapper;
            BaseResource resource = wrapper.getResource();
            if (resource != null) {
                RelatedReport relatedReport = new RelatedReport(wrapper, rel.howRelated);
                relatedReport.comprehensiveErrors = rel.comprehensiveErrors;
                relatedReport.isComprehensive = rel.comprehensiveErrors.isEmpty();
                relatedReport.minimalErrors = rel.minimalErrors;
                relatedReport.isMinimal = rel.minimalErrors.isEmpty();
                relatedReport.comprehensiveChecked = rel.comprehensiveChecked.attsChecked;
                relatedReport.minimalChecked = rel.minimalChecked.attsChecked;
                relatedReport.codingErrors = rel.codingErrors;
                relatedReport.atts = rel.atts;
                report.objects.add(relatedReport);
            }
        }

        return report;
    }

    public AnalysisReport(Ref baseRef, String source, EC ec) {
        this.baseRef = baseRef;
        this.source = source;
        this.ec = ec;
        this.codesValidation = new CodesValidation(ec);
    }

    public Report run() {
        try {
            loadBase();
            if (!generalErrors.isEmpty())
                return buildReport();
            buildRelated();
            comprehensiveEval();
            minimalEval();
            codingEval();
            buildAtts();
            return buildReport();
        } catch (Throwable t) {
            generalErrors.add(ExceptionUtils.getStackTrace(t));
            return buildReport();
        }
    }

    public class Checked {
        String className;
        String attsChecked;
        String script;

        Checked(String className, String attsChecked, String script) {
            this.className = className;
            this.attsChecked = attsChecked;
            this.script = script;
        }

        public String toString() {
            return "Checked: " + className + " Script: " + script + " Atts: " + attsChecked;
        }
    }

    private void buildAtts() {
        atts = ResourceHasMethodsFilter.toMap(baseObj.getResource());
        for (Related rel : related) {
            rel.atts = ResourceHasMethodsFilter.toMap(rel.wrapper.getResource());
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
        comprehensiveErrors = testEngine.getTestReportErrors();
        comprehensiveChecked = getFirstAssertDetails(testEngine.getTestReport());
        for (Related rel : related) {
            testEngine = comprehensiveEval(rel.wrapper);
            rel.comprehensiveChecked = getFirstAssertDetails(testEngine.getTestReport());
            rel.comprehensiveErrors = testEngine.getTestReportErrors();
        }
    }

    private void minimalEval() {
        TestEngine testEngine  = minimalEval(baseObj);
        minimalErrors = testEngine.getTestReportErrors();
        minimalChecked = getFirstAssertDetails(testEngine.getTestReport());
        for (Related rel : related) {
            testEngine = minimalEval(rel.wrapper);
            rel.minimalChecked = getFirstAssertDetails(testEngine.getTestReport());
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

    private Checked getFirstAssertDetails(TestReport testReport) {
        if (testReport == null) return new Checked("", "", "");
        for (TestReport.TestReportTestComponent testComponent : testReport.getTest()) {
            for (TestReport.TestActionComponent actionComponent : testComponent.getAction()) {
                TestReport.SetupActionAssertComponent assertComponent = actionComponent.getAssert();
                if (assertComponent != null) {
                    String detail = assertComponent.getDetail();
                    if (detail != null && !detail.equals(""))
                        return new Checked((String)assertComponent.getUserData("Evaluating type"), detail, (String)assertComponent.getUserData("Script"));
                }
            }
        }
        return new Checked("", "", "");
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

    private void loadBase() {
        Objects.requireNonNull(baseRef);

        baseObj = fhirClient.readResource(baseRef);
        if (baseObj.getStatus() != 200) {
            generalErrors.add("Status " + baseObj.getStatus());
        } else if (baseObj.getResource() instanceof OperationOutcome) {
            OperationOutcome oo = (OperationOutcome) baseObj.getResource();
            generalErrors.add(oo.getIssueFirstRep().getDiagnostics());
        } else {
            //related.add(baseObj);
            fhirBase = baseRef.getBase();
        }
    }

    private void buildRelated() {
        BaseResource baseResource = baseObj.getResource();
        if (baseResource instanceof DomainResource) {
            DomainResource domainResource = (DomainResource) baseResource;
            try {
                buildRelated(domainResource);
            } catch (Throwable t) {
                generalErrors.add("Do not know how to load DomainResource " + domainResource.getClass().getName());
            }
        }  else if (baseResource instanceof Binary) {
            try {
                buildRelated(baseResource);
            } catch (Throwable t) {
                generalErrors.add("Do not know how to load DomainResource " + baseResource.getClass().getName());
            }
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

    private void buildRelated(BaseResource baseResource) {
        if (baseResource instanceof DocumentManifest) buildRelated((DocumentManifest) baseResource);
        else if (baseResource instanceof DocumentReference) buildRelated((DocumentReference) baseResource);
        else if (baseResource instanceof ListResource) buildRelated((ListResource) baseResource);
        else if (baseResource instanceof Binary) buildRelated((Binary) baseResource);
        else if (baseResource instanceof Patient) buildRelated((Patient) baseResource);
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

    private Related load(Ref ref, String howRelated, DomainResource parent) {
        Related rel = getFromRelated(ref);
        if (rel == null) {
            if (ref.isContained() && parent != null) {
                Related rel2 = new Related(new ResourceWrapper(ref.getContained(parent)), howRelated + "/contained").contained();
                related.add(rel2);
                return rel2;
            }
            if (ref.isRelative())
                ref = ref.rebase(fhirBase);
            ResourceWrapper wrapper = fhirClient.readResource(ref);
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
