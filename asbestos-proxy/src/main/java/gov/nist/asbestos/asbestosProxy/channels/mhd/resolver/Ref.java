package gov.nist.asbestos.asbestosProxy.channels.mhd.resolver;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Reference;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Ref {
    private URI uri;
    private IBaseResource resource = null;

    public Ref(URI uri) {
        Objects.requireNonNull(uri);
        this.uri = uri;
    }

    public Ref(String ref)  {
        Objects.requireNonNull(ref);
        uri = build(ref);
    }

    public Ref(String base, String resourceType, String id)  {
        String theRef;
        if (id == null || id.equals(""))
            theRef = String.join("/", base, resourceType);
        else
            theRef = String.join("/", base, resourceType, id);
        uri = build(theRef);
    }

    public Ref(Ref base, String resourceType, String id)  {
        String theRef;
        if (id == null || id.equals(""))
            theRef = String.join("/", base.toString(), resourceType);
        else
            theRef = String.join("/", base.toString(), resourceType, id);
        uri = build(theRef);
    }

//    Ref(Ref ref) {
//        this.uri = ref.uri;
//    }

    public Ref(Reference reference) throws Exception {
        Objects.requireNonNull(reference);
        uri = build(reference.getReference());
    }

    public String getId() {
        String path = uri.getPath();
        if (!path.contains("/")) return path;
        String[] parts = path.split("/");
        for (int i=0; i<parts.length-1; i++) {
            if (resourceNames.contains(parts[i]))
                return parts[i+1];
        }
        return "";
    }

    public String getResourceType() {
        String path = uri.getPath();
        String[] parts = path.split("/");
        for (int i=0; i<parts.length; i++) {
            if (resourceNames.contains(parts[i]))
                return parts[i];
        }
        return "";
    }

    public Ref getRelative() throws Exception {  // without base
        String path = uri.getPath();
        List<String> parts = Arrays.asList(path.split("/"));
        for (int i=0; i<parts.size(); i++) {
            if (resourceNames.contains(parts.get(i)))
                return new Ref(String.join("/", parts.subList(i, parts.size())));
        }
        return new Ref("");
    }

    public Ref getBase() throws Exception {
        String path = uri.toString();
        List<String> parts = Arrays.asList(path.split("/"));
        for (int i=0; i<parts.size(); i++) {
            if (resourceNames.contains(parts.get(i)))
                return new Ref(String.join("/", parts.subList(0, i)));
        }
        return new Ref(uri.toString());
    }

    public Ref withNewId(String newId) throws Exception {
        Objects.requireNonNull(newId);
        return new Ref(getBase(), getResourceType(), newId);
    }

    public Ref rebase(String newBase) throws Exception {
        Objects.requireNonNull(newBase);
        Ref theBase = new Ref(newBase).getBase();
        return new Ref(theBase, getRelative().toString(), null);
    }

    // TODO needs test
    public Ref rebase(Ref newBase) throws Exception {
        Objects.requireNonNull(newBase);
        return new Ref(newBase.getBase(), getRelative().toString(), null);
    }

    public Ref getFull() throws Exception {  // without version
        String baseStr = getBase().toString();
        String resourceTypeStr = getResourceType();
        String idStr = getId();
        boolean hasScheme = uri.getScheme() != null;
        if (!hasScheme && baseStr != null && resourceTypeStr != null && idStr != null)
            return new Ref(baseStr, resourceTypeStr, idStr);
        if (!hasScheme && resourceTypeStr != null && idStr == null)
            return new Ref(getResourceType());
        if (!hasScheme && resourceTypeStr != null && idStr != null)
            return new Ref(String.format("%s/%s", resourceTypeStr, idStr));
        if (uri.toString().contains("_history"))
            return new Ref(uri.toString().split("/_history", 2)[0]);
        return new Ref(uri);
    }

    public String getVersion() {
        String[] parts = uri.toString().split("_history/");
        if (parts.length < 2)
            return null;
        return parts[1];
    }

    public boolean isAbsolute() throws Exception {
        return StringUtils.isEmpty(uri.getScheme()) &&
                StringUtils.isEmpty(getBase().toString()) &&
                        StringUtils.isEmpty(getId());
    }

    @Override
    public String toString() {
        return uri.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ref ref = (Ref) o;
        return Objects.equals(uri, ref.uri) &&
                Objects.equals(resource, ref.resource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri, resource);
    }

    private URI build(String ref) {
        if (ref.startsWith("#")) {
            ref = ref.substring(1);
        }
        try {
            return new URI(ref);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static private List<String> resourceNames = Arrays.asList(
            "CapabilityStatement",
            "StructureDefinition",
            "ImplementationGuide",
            "SearchParameter",
            "MessageDefinition",
            "OperationDefinition",
            "CompartmentDefinition",
            "StructureMap",
            "GraphDefinition",
            "ExampleScenario",
            "CodeSystem",
            "ValueSet",
            "ConceptMap",
            "NamingSystem",
            "TerminologyCapabilities",
            "Provenance",
            "AuditEvent",
            "Consent",
            "Composition",
            "DocumentManifest",
            "DocumentReference",
            "CatalogEntry",
            "Basic",
            "Binary",
            "Bundle",
            "Linkage",
            "MessageHeader",
            "OperationOutcome",
            "Parameters",
            "Subscription",
            "Patient",
            "Practitioner",
            "PractitionerRole",
            "RelatedPerson",
            "Person",
            "Group",
            "Organization",
            "OrganizationAffiliation",
            "HealthcareService",
            "Endpoint",
            "Location",
            "Substance",
            "BiologicallyDerivedProduct",
            "Device",
            "DeviceMetric",
            "Task",
            "Appointment",
            "AppointmentResponse",
            "Schedule",
            "Slot",
            "VerificationResult",
            "Encounter",
            "EpisodeOfCare",
            "Flag",
            "List",
            "Library",
            "AllergyIntolerance",
            "AdverseEvent",
            "Condition",
            "Procedure",
            "FamilyMemberHistory",
            "ClinicalImpression",
            "DetectedIssue",
            "Observation",
            "Media",
            "DiagnosticReport",
            "Specimen",
            "BodyStructure",
            "ImagingStudy",
            "QuestionnaireResponse",
            "MolecularSequence",
            "MedicationRequest",
            "MedicationAdministration",
            "MedicationDispense",
            "MedicationStatement",
            "Medication",
            "MedicationKnowledge",
            "Immunization",
            "ImmunizationEvaluation",
            "ImmunizationRecommendation",
            "CarePlan",
            "CareTeam",
            "Goal",
            "ServiceRequest",
            "NutritionOrder",
            "VisionPrescription",
            "RiskAssessment",
            "RequestGroup",
            "Communication",
            "CommunicationRequest",
            "DeviceRequest",
            "DeviceUseStatement",
            "GuidanceResponse",
            "SupplyRequest",
            "SupplyDelivery",
            "Coverage",
            "CoverageEigibilityRequest",
            "CoverageEligibilityResponse",
            "EnrollmentRequest",
            "EnrollmentResponse",
            "Claim",
            "ClaimResponse",
            "Invoice",
            "PaymentNotice",
            "PaymentReconciliation",
            "Account",
            "ChargeItem",
            "ChargeItemDefinition",
            "Contract",
            "ExplanationOfBenefit",
            "InsurancePlan",
            "ResearchStudy",
            "ResearchSubject",
            "ActivityDefinition",
            "DeviceDefinition",
            "EventDefinition",
            "ObservationDefinition",
            "PlanDefinition",
            "Questionnaire",
            "SpecimenDefinition",
            "ResearchDefinition",
            "ResearchElementDefinition",
            "Evidence",
            "EvidenceVariable",
            "EffectEvidenceSynthesis",
            "RiskEvidenceSynthesis",
            "Measure",
            "MeasureReport",
            "TestScript",
            "TestReport",
            "MedicinalProduct",
            "MedicinalProductAuthorization",
            "MedicinalProductContraindication",
            "MedicinalProductIndication",
            "MedicinalProductIngredient",
            "MedicinalProductInteraction",
            "MedicinalProductManufactured",
            "MedicinalProductPackaged",
            "MedicinalProductPharmaceutical",
            "MedicinalProductUndesirableEffect 0",
            "SubstancePolymer",
            "SubstanceReferenceInformation",
            "SubstanceSpecification"
    );


}
