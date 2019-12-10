package gov.nist.asbestos.analysis;

import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import org.apache.log4j.Logger;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AnalysisReport {
    private static Logger log = Logger.getLogger(AnalysisReport.class);
    private Ref baseRef = null;
    private ResourceWrapper baseObj = null;
    private List<ResourceWrapper> related = new ArrayList<>();
    private List<String> minimalErrors = new ArrayList<>();
    private List<String> comprehensiveErrors = new ArrayList<>();
    private List<String> codingErrors = new ArrayList<>();
    private List<String> generalErrors = new ArrayList<>();
    private FhirClient fhirClient = new FhirClient();

    public AnalysisReport(Ref baseRef) {
        this.baseRef = baseRef;
    }

    public void run() {
        loadBase();
        buildRelated();
    }

    private void loadBase() {
        Objects.requireNonNull(baseRef);

        baseObj = fhirClient.readResource(baseRef);
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
        // recipient
        if (documentManifest.hasRecipient()) {
            for (Reference reference : documentManifest.getRecipient()) {
                load(new Ref(reference));
            }
        }
        // related
        if (documentManifest.hasRelated()) {
            for (DocumentManifest.DocumentManifestRelatedComponent component : documentManifest.getRelated()) {
                load(new Ref(component.getRef()));
            }
        } else {
            generalErrors.add("DocumentManifest has no related resources");
        }
    }

    private void buildRelated(DocumentReference documentReference) {
        log.info("buildRelated DocumentReference");
        // author - contained only
        // subject
        if (documentReference.hasSubject()) {
            load(new Ref(documentReference.getSubject()));
        } else {
            generalErrors.add("DocumentReference has no subject");
        }
        // authenticator - contained only
        // custodian - not defined
        // relatesTo
        if (documentReference.hasRelatesTo()) {
            for (DocumentReference.DocumentReferenceRelatesToComponent component : documentReference.getRelatesTo()) {
                load(new Ref(component.getTarget()));
            }
        }
        // sourcePatientInfo
        // context/related

    }

    private void buildRelated(Patient patient) {
        log.info("buildRelated Patient");

    }

    private void buildRelated(ListResource list) {
        log.info("buildRelated ListResource");

    }

    private void buildRelated(Binary binary) {
        log.info("buildRelated Binary");

    }

    private void buildRelated(BaseResource baseResource) {
        log.info("buildRelated BaseResource");
    }

    private ResourceWrapper getFromRelated(Ref ref) {
        Objects.requireNonNull(ref);
        for (ResourceWrapper wrapper : related) {
            if (ref.equals(wrapper.getRef()))
                return wrapper;
        }
        return null;
    }

    private ResourceWrapper load(Ref ref) {
        ResourceWrapper wrapper = getFromRelated(ref);
        if (wrapper == null) {
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

}
