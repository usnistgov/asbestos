package gov.nist.asbestos.analysis;

import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import org.apache.log4j.Logger;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AnalysisReport {
    private static Logger log = Logger.getLogger(AnalysisReport.class);
    private Ref fhirBase = null;
    private Ref baseRef = null;
    private ResourceWrapper baseObj = null;
    private List<ResourceWrapper> related = new ArrayList<>();
    private List<String> minimalErrors = new ArrayList<>();
    private List<String> comprehensiveErrors = new ArrayList<>();
    private List<String> codingErrors = new ArrayList<>();
    private List<String> generalErrors = new ArrayList<>();
    private FhirClient fhirClient = new FhirClient();

    public class Report {
        List<String> objects = new ArrayList<>();
        List<String> errors;
    }


    private Report buildReport() {
        Report report = new Report();
        report.errors = new ArrayList<>(generalErrors);

        for (ResourceWrapper wrapper : related) {
            BaseResource resource = wrapper.getResource();
            if (resource != null)
                report.objects.add(resource.getClass().getSimpleName());
        }

        return report;
    }

    public AnalysisReport(Ref baseRef) {
        this.baseRef = baseRef;
    }

    public Report run() {
        try {
            loadBase();
            if (!generalErrors.isEmpty())
                return buildReport();
            buildRelated();
            return buildReport();
        } catch (Throwable t) {
            generalErrors.add(t.getMessage());
            return buildReport();
        }
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
            related.add(baseObj);
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
        } else {
            generalErrors.add("Do not know how to load BaseResource " + baseResource.getClass().getName());
        }
    }

    private void buildRelated(DocumentManifest documentManifest) {
        log.info("buildRelated DocumentManifest");
        // subject
        if (documentManifest.hasSubject()) {
            load(new Ref(documentManifest.getSubject()));
        } else {
            generalErrors.add("DocumentManifest has no subject");
        }
        // author - contained only
        if (documentManifest.hasAuthor()) {
            loadContained(documentManifest, "Author", documentManifest.getAuthor());
        }
        // recipient
        if (documentManifest.hasRecipient()) {
            for (Reference reference : documentManifest.getRecipient()) {
                load(new Ref(reference));
            }
        }
        // related
        if (documentManifest.hasContent()) {
            boolean hasDocRef = false;
            for (Reference reference : documentManifest.getContent()) {
                ResourceWrapper wrapper = load(new Ref(reference));
                if (wrapper != null && wrapper.hasResource() && wrapper.isOk() && wrapper.getResource() instanceof DocumentReference)
                    hasDocRef = true;
            }
            if (!hasDocRef)
                generalErrors.add("DocumentManifest has no related DocumentReferences");
        } else {
            generalErrors.add("DocumentManifest has no related resources - shall have DocumentReference");
        }
    }

    private void buildRelated(DocumentReference documentReference) {
        log.info("buildRelated DocumentReference");
        // author - contained only
        if (documentReference.hasAuthor()) {
            loadContained(documentReference, "Author", documentReference.getAuthor());
        }
        // subject
        if (documentReference.hasSubject()) {
            load(new Ref(documentReference.getSubject()));
        } else {
            generalErrors.add("DocumentReference has no subject");
        }
        // authenticator - contained only
        if (documentReference.hasAuthenticator()) {
            loadContained(documentReference, "Authenticator", Collections.singletonList(documentReference.getAuthenticator()));
        }
        // custodian - not defined
        // relatesTo
        if (documentReference.hasRelatesTo()) {
            for (DocumentReference.DocumentReferenceRelatesToComponent component : documentReference.getRelatesTo()) {
                load(new Ref(component.getTarget()));
            }
        }
        if (documentReference.hasContext()) {
            // sourcePatientInfo - contained only
            if (documentReference.getContext().hasSourcePatientInfo()) {
                loadContained(documentReference, "Patient", Collections.singletonList(documentReference.getContext().getSourcePatientInfo()));
            }
            // context/related
            if (documentReference.getContext().hasRelated()) {
                for (Reference reference : documentReference.getContext().getRelated()) {
                    load(new Ref(reference));
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
            load(new Ref(list.getSubject()));
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

    private ResourceWrapper getFromRelated(Ref ref) {
        Objects.requireNonNull(ref);
        if (ref.isRelative())
            ref.rebase(fhirBase);
        for (ResourceWrapper wrapper : related) {
            if (ref.equals(wrapper.getRef()))
                return wrapper;
        }
        return null;
    }

    private ResourceWrapper load(Ref ref, List<Class> types) {
        List<String> names = new ArrayList<>();
        for (Class c : types)
            names.add(c.getSimpleName());
        ResourceWrapper wrapper = load(ref);
        if (wrapper != null && wrapper.hasResource()) {
            Class theClass = wrapper.getResource().getClass();
            if (!types.contains(theClass))
                generalErrors.add("Trying to load one of " + names + " but got a " + theClass.getSimpleName());
        }
        return wrapper;
    }

    private ResourceWrapper load(Ref ref) {
        ResourceWrapper wrapper = getFromRelated(ref);
        if (wrapper == null) {
            if (ref.isRelative())
                ref = ref.rebase(fhirBase);
            wrapper = fhirClient.readResource(ref);
            if (fhirClient.getStatus() == 200) {
                related.add(wrapper);
                return wrapper;
            } else {
                generalErrors.add("Cannot load " + ref + " status was " + fhirClient.getStatus());
                return null;
            }
        }
        return wrapper;
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
                load(new Ref(reference));
                generalErrors.add(parentResource.getClass().getSimpleName() + ": external " + containedType + " referenced - shall be contained.");
            } else {
                related.add(new ResourceWrapper(resource));
            }
        }
    }

    public List<String> getGeneralErrors() {
        return generalErrors;
    }
}
