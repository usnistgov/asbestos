package gov.nist.asbestos.mhd.transforms;


import com.google.common.base.Strings;
import gov.nist.asbestos.asbestorCodesJaxb.Code;
import gov.nist.asbestos.client.Base.IVal;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.FhirClientBuilder;
import gov.nist.asbestos.client.events.ITask;
import gov.nist.asbestos.client.resolver.*;
import gov.nist.asbestos.mhd.SubmittedObject;
import gov.nist.asbestos.mhd.exceptions.TransformException;
import gov.nist.asbestos.mhd.transactionSupport.AhqrSender;
import gov.nist.asbestos.mhd.transactionSupport.AssigningAuthorities;
import gov.nist.asbestos.mhd.transactionSupport.CodeTranslator;
import gov.nist.asbestos.mhd.translation.attribute.Author;
import gov.nist.asbestos.mhd.translation.attribute.AuthorRole;
import gov.nist.asbestos.mhd.translation.attribute.DateTransform;
import gov.nist.asbestos.mhd.translation.attribute.EntryUuid;
import gov.nist.asbestos.mhd.translation.search.FhirSq;
import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.mhd.util.Utils;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.*;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 */

// TODO enable runtime assertions with ClassLoader.getSystemClassLoader().setClassAssertionStatus("gov.nist");
// TODO - add legalAuthenticator
// TODO - add sourcePatientInfo
// TODO - add referenceIdList
// TODO - add author
// TODO - add case where Patient not in bundle?????

/**
 * Association id = ID06
 *    source = SubmissionSet_ID02
 *    target = Document_ID01
 *
 * RegistryPackage id = 234...
 *
 * ExtrinsicObject id = ID07
 */


public class BundleToRegistryObjectList implements IVal {
//    static private final Logger logger = Logger.getLogger(BundleToRegistryObjectList.class)
    private static List<Class<?>> acceptableResourceTypes = Arrays.asList(DocumentManifest.class, DocumentReference.class, Binary.class, ListResource.class);
    private static String comprehensiveMetadataProfile = "http://ihe.net/fhir/StructureDefinition/IHE_MHD_Provide_Comprehensive_DocumentBundle";
    private static String minimalMetadataProfile = "http://ihe.net/fhir/StructureDefinition/IHE_MHD_Provide_Minimal_DocumentBundle";
    private static List<String> profiles = Arrays.asList(comprehensiveMetadataProfile, minimalMetadataProfile);
    private static String baseContentId = ".de1e4efca5ccc4886c8528535d2afb251e0d5fa31d58a815@ihexds.nist.gov";
    private static String mhdProfileRef = "MHD Profile - Rev 3.1";
    private static Map<String, String> buildTypeMap() {
        return Collections.unmodifiableMap(Stream.of(
                new AbstractMap.SimpleEntry<>("replaces", "urn:ihe:iti:2007:AssociationType:RPLC"),
                new AbstractMap.SimpleEntry<>("transforms", "urn:ihe:iti:2007:AssociationType:XFRM"),
                new AbstractMap.SimpleEntry<>("signs", "urn:ihe:iti:2007:AssociationType:signs"),
                new AbstractMap.SimpleEntry<>("appends", "urn:ihe:iti:2007:AssociationType:APND"))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue)));
    }
    private static final String DRTable = "MHD: Table 4.5.1.1-1";

    private CodeTranslator codeTranslator;
    private Configuration config;
    private AssigningAuthorities assigningAuthorities;
    private ResourceMgr rMgr;
    private Val val;
    private IdBuilder idBuilder;
    private Map<String, byte[]> documentContents = new HashMap<>();
    private boolean errorsOnly = true;
    private Bundle responseBundle = null;
    private boolean responseHasError = false;
    private List<SubmittedObject> submittedObjects = new ArrayList<>();
    private ITask task = null;
    private URI sqEndpoint = null;
//    private File externalCache = Installation.instance().externalCache();
    private ChannelConfig channelConfig;
    private boolean isMinimalMetadata = false;


    public BundleToRegistryObjectList(ChannelConfig channelConfig) {
        this.channelConfig = channelConfig;
    }

    public SubmittedObject findSubmittedObject(BaseResource resource) {
        for (SubmittedObject submittedObject : submittedObjects) {
            if (submittedObject.getResource().equals(resource))
                return submittedObject;
        }
        return null;
    }

    public RegistryObjectListType build(Bundle bundle) {
        Objects.requireNonNull(val);
        Objects.requireNonNull(bundle);
        Objects.requireNonNull(rMgr);
        Objects.requireNonNull(idBuilder);

        scanBundleForAcceptability(bundle, rMgr);

        return buildRegistryObjectList();
    }

    // TODO handle List/Folder or signal error
    public RegistryObjectListType buildRegistryObjectList() {
        Objects.requireNonNull(val);
        Objects.requireNonNull(rMgr);

        //rMgr.setBundle(bundle);
        //scanBundleForAcceptability(bundle, rMgr);
        RegistryObjectListType rol = new RegistryObjectListType();

        responseBundle = new Bundle();

        DocumentManifest documentManifest = null;
        RegistryPackageType ss = null;
        ValE ssVale = null;
        List<ExtrinsicObjectType> eos = new ArrayList<>();

        for (ResourceWrapper wrapper : rMgr.getBundleResourceList()) {
            Bundle.BundleEntryComponent responseComponent1 = responseBundle.addEntry();
            Bundle.BundleEntryResponseComponent responseComponent = responseComponent1.getResponse();
            responseComponent.setStatus("200");
            ValE vale = new ValE(val);
            BaseResource resource = wrapper.getResource();
            if (resource instanceof DocumentManifest) {
                DocumentManifest dm = (DocumentManifest) resource;
                ssVale = vale;
                if (documentManifest == null) {
                    documentManifest = dm;
                    ss = createSubmissionSet(wrapper, vale);
                    submittedObjects.add(new SubmittedObject(wrapper.getAssignedUid(), resource));
                    rol.getIdentifiable().add(new JAXBElement<>(new QName("urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0", "RegistryPackage"), RegistryPackageType.class, ss));
                } else {
                    vale.add(new ValE("Found multiple DocumentManifests - one required").asError());
                }
            } else if (resource instanceof DocumentReference) {
                DocumentReference dr = (DocumentReference) resource;
                ExtrinsicObjectType eo = createExtrinsicObject(wrapper, vale);
                eos.add(eo);
                submittedObjects.add(new SubmittedObject(wrapper.getAssignedUid(), resource));
                rol.getIdentifiable().add(new JAXBElement<>(new QName("urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0", "ExtrinsicObject"), ExtrinsicObjectType.class, eo));
                if (dr.hasRelatesTo()) {
                    List<DocumentReference.DocumentReferenceRelatesToComponent> relates = dr.getRelatesTo();
                    for (DocumentReference.DocumentReferenceRelatesToComponent relate : relates) {
                        Reference reference = null;
                        DocumentReference.DocumentRelationshipType code = null;
                        if (relate.hasTarget())
                            reference = relate.getTarget();
                        if (relate.hasCode())
                            code = relate.getCode();
                        if (reference == null)
                            vale.add(new ValE("relatesTo is missing the target.reference").asError());
                        if (code == null)
                            vale.add(new ValE("relatesTo is missing the target.code").asError());
                        if (reference != null && code != null) {
                            AssociationType1 hasMember = addRelationship(eo, code.toCode(), new Ref(reference.getReference()), vale);
                            rol.getIdentifiable().add(new JAXBElement<>(new QName("urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0", "Association"), AssociationType1.class, hasMember));
                        }
                    }
                }
            } else if (resource instanceof Binary) {
            } else {
                vale.add(new ValE("Ignoring resource of type " + resource.getClass().getSimpleName()));
            }
            throwTransformExceptionIfError(vale);
//            responseHasError |= valeToResponseComponent(vale, responseComponent, errorsOnly);
        }

        if (eos.isEmpty()) {
            ValE vale = new ValE(val);
            vale.add(new ValE("Found no DocumentReferences - one ore more required").asError());
            throwTransformExceptionIfError(vale);
        }

        if (ss != null) {
            for (ExtrinsicObjectType eo : eos) {
                AssociationType1 a = createSSDEAssociation(ss, eo, ssVale);
                rol.getIdentifiable().add((new JAXBElement<>(new QName("urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0", "Association"), AssociationType1.class, a)));
            }
        }

        return rol;
    }

    // if error is found in vale, throw TransformException with OperationOutcome
    private void throwTransformExceptionIfError(ValE vale) {
        OperationOutcome oo = null;
        boolean returnError = false;
        if (vale.hasErrors()) {
            oo = new OperationOutcome();
            for (ValE e : vale.getErrors()) {
                OperationOutcome.OperationOutcomeIssueComponent issue = oo.addIssue();
                issue.setSeverity(OperationOutcome.IssueSeverity.ERROR);
                issue.setCode(OperationOutcome.IssueType.UNKNOWN);
                issue.setDiagnostics(e.getMsg());
            }
            throw new TransformException(oo);
        }
    }

//    private boolean valeToResponseComponent(ValE vale, Bundle.BundleEntryResponseComponent responseComponent, boolean errorsOnly) {
//        OperationOutcome oo = null;
//        boolean returnError = false;
//        if (vale.hasErrors()) {
//            returnError = true;
//            responseComponent.setStatus("400");
//            oo = new OperationOutcome();
//            responseComponent.setOutcome(oo);
//            for (ValE e : vale.getErrors()) {
//                OperationOutcome.OperationOutcomeIssueComponent issue = oo.addIssue();
//                issue.setSeverity(OperationOutcome.IssueSeverity.ERROR);
//                issue.setCode(OperationOutcome.IssueType.UNKNOWN);
//                issue.setDiagnostics(e.getMsg());
//            }
//        }
//        if (vale.hasWarnings()) {
//            if (oo == null) {
//                oo = new OperationOutcome();
//                responseComponent.setOutcome(oo);
//            }
//            for (ValE e : vale.getWarnings()) {
//                OperationOutcome.OperationOutcomeIssueComponent issue = oo.addIssue();
//                issue.setSeverity(OperationOutcome.IssueSeverity.WARNING);
//                issue.setCode(OperationOutcome.IssueType.UNKNOWN);
//                issue.setDiagnostics(e.getMsg());
//            }
//        }
//        if (!errorsOnly && vale.hasInfo()) {
//            if (oo == null) {
//                oo = new OperationOutcome();
//                responseComponent.setOutcome(oo);
//            }
//            for (ValE e : vale.infos()) {
//                OperationOutcome.OperationOutcomeIssueComponent issue = oo.addIssue();
//                issue.setSeverity(OperationOutcome.IssueSeverity.INFORMATION);
//                issue.setCode(OperationOutcome.IssueType.UNKNOWN);
//                issue.setDiagnostics(e.getMsg());
//            }
//        }
//
//        if (oo != null)
//            responseComponent.setOutcome(oo);
//        return returnError;
//    }


//    private void buildSubmission() {
//        PnrWrapper submission = new PnrWrapper()
//        submission.contentId = 'm' + baseContentId
//
//        int index = 1
//
//        StringWriter writer = new StringWriter()
//        MarkupBuilder xml = new MarkupBuilder(writer)
//        xml.RegistryObjectList(xmlns: 'urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0') {
//            rMgr.resources.each { Ref url, ResourceWrapper resource ->
//                if (resource.resource instanceof DocumentManifest) {
//                    DocumentManifest dm = (DocumentManifest) resource.resource
//                    createSubmissionSet(xml, resource)
//                    addSubmissionSetAssociations(xml, resource)
//                }
//                else if (resource.resource instanceof DocumentReference) {
//                    DocumentReference dr = (DocumentReference) resource.resource
//                    ResourceWrapper loadedResource = rMgr.resolveReference(resource, new Ref(dr.content[0].attachment.url), new ResolverConfig().internalRequired())
//                    if (!(loadedResource.resource instanceof Binary))
//                        val.err(new Val()
//                        .msg("Binary ${dr.content[0].attachment.url} is not available in Bundle."))
//                    Binary b = (Binary) loadedResource.resource
//                    b.id = dr.masterIdentifier.value
//                    Attachment a = new Attachment()
//                    a.contentId = Integer.toString(index) + baseContentId
//                    a.contentType = b.contentType
//                    a.content = b.content
//                    submission.attachments << a
//                    index++
//
//                    createExtrinsicObject(xml, resource)
//                    documents[resource.assignedId] = a.contentId
//                    addRelationshipAssociations(xml, resource)
//                }
//            }
//        }
//        submission.registryObjectList = writer.toString()
//    }

    private AssociationType1 createSSDEAssociation(RegistryPackageType ss, ExtrinsicObjectType de, ValE vale) {
        return createAssociation("urn:oasis:names:tc:ebxml-regrep:AssociationType:HasMember", ss.getId(), de.getId(), "SubmissionSetStatus", Collections.singletonList("Original"), vale);


//        DocumentManifest dm = (DocumentManifest) resource.resource
//        if (!dm.content) return
//        dm.content.each { Reference ref ->
//            ResourceWrapper loadedResource = rMgr.resolveReference(resource, new Ref(ref.reference), new ResolverConfig().internalRequired())
//
//            if (!loadedResource.resource)
//                val.err(new Val()
//                .msg("DocumentManifest references ${ref.resource} - ${loadedResource.ref} is not included in the bundle"))
//            createAssociation(xml, 'urn:oasis:names:tc:ebxml-regrep:AssociationType:HasMember', resource, loadedResource.url, 'SubmissionSetStatus', ['Original'])
//        }
    }

    private void addRelationshipAssociations(ResourceWrapper resource) {
//        assert resource.resource instanceof DocumentReference
//        DocumentReference dr = (DocumentReference) resource.resource
//        if (!dr.relatesTo || dr.relatesTo.size() == 0) return
//
//        // GET relatesTo reference, extract entryUUID, assemble Association
//        dr.relatesTo.each { DocumentReference.DocumentReferenceRelatesToComponent comp ->
//            String type = comp.getCode().toCode()
//            String xdsType = typeMap[type]
//            if (!xdsType)
//                val.err(new Val()
//                .msg("RelatesTo type (${type}) cannot be translated to XDS."))
//
//            Reference ref = comp.target
//
//            ResourceWrapper referencedDocRef = rMgr.resolveReference(resource, new Ref(ref.reference), new ResolverConfig().externalRequired())
//
//            if (!referencedDocRef.resource) {
//                val.err(new Val()
//                        .msg("Trying to load ${xdsType} Association - ${ref.reference} cannot be resolved"))
//                return
//            }
//            createAssociation(xml, xdsType, resource, referencedDocRef.url, null, null)
//        }
    }

    private AssociationType1 createAssociation(String type, ResourceWrapper source, Ref target, String slotName, List<String> slotValues) {
        AssociationType1 at = new AssociationType1();
        val.add(new ValE("Association(" + type + ") source=" + source + " target=" + target));
        at.setSourceObject(source.getAssignedId());
        at.setTargetObject(target.getId());
        at.setAssociationType(type);
        at.setId(rMgr.allocateSymbolicId());
        addSlot(at, slotName, slotValues);
        return at;
    }

    public AssociationType1 createAssociation(String type, String sourceId, String targetId, String slotName, List<String> slotValues, ValE vale) {
        AssociationType1 at = new AssociationType1();
        at.setSourceObject(sourceId);
        at.setTargetObject(targetId);
        at.setAssociationType(type);
        at.setId(rMgr.allocateSymbolicId());
        if (slotName != null)
            addSlot(at, slotName, slotValues);
        return at;
    }

    private RegistryPackageType createSubmissionSet(ResourceWrapper resource, ValE vale) {
        DocumentManifest dm = (DocumentManifest) resource.getResource();

        if (dm.hasIdentifier()) {
            if (dm.getIdentifier().stream().anyMatch(i -> i.hasValue() && i.getValue().startsWith("urn:uuid:")))
                vale.add(new ValE("DocumentManifest has Identifier (entryUUID)").asError());
        }

        RegistryPackageType ss = new RegistryPackageType();

        ValE tr;
        vale.setMsg("DocumentManifest to SubmissionSet");

        val.add(new ValE("SubmissionSet(" + resource.getAssignedId() + ")"));
        ss.setId(resource.getAssignedId());
        ss.setObjectType("urn:oasis:names:tc:ebxml-regrep:ObjectType:RegistryObject:RegistryPackage");

        if (dm.hasCreated())
            addSlot(ss, "submissionTime", translateDateTime(dm.getCreated()));
        if (dm.hasDescription())
            addName(ss, dm.getDescription());
        addClassification(ss, "urn:uuid:a54d6aa5-d40d-43f9-88c5-b4633d873bdd", rMgr.allocateSymbolicId(), resource.getAssignedId());
        if (dm.hasType())
            addClassificationFromCodeableConcept(ss, dm.getType(), CodeTranslator.CONTENTTYPECODE, resource.getAssignedId(), vale);
        if (!dm.hasMasterIdentifier())
            val.add(new ValE("DocumentManifest.masterIdentifier not present - declared by IHE to be [1..1]").asError());
        else {
            addExternalIdentifier(ss, CodeTranslator.SS_UNIQUEID, Utils.stripUrnPrefix(dm.getMasterIdentifier().getValue()), rMgr.allocateSymbolicId(), resource.getAssignedId(), "XDSSubmissionSet.uniqueId", idBuilder);
            resource.setAssignedUid(Utils.stripUrnPrefix(dm.getMasterIdentifier().getValue()));
        }
        if (dm.hasSource())
            addExternalIdentifier(ss, CodeTranslator.SS_SOURCEID, Utils.stripUrnPrefix(dm.getMasterIdentifier().getValue()), rMgr.allocateSymbolicId(), resource.getAssignedId(), "XDSSubmissionSet.sourceId", null);
        if (dm.hasSubject() && dm.getSubject().hasReference())
            addSubject(ss, resource,  new Ref(dm.getSubject()), CodeTranslator.SS_PID, "XDSSubmissionSet.patientId", vale);
        else if (isMinimalMetadata) {
            // Patient is optional in minimal metadata - add reference to No_Patient to make XDS Toolkit happy
            // Adds resource cache to configuration
            FhirClient fhirClient =
                    channelConfig == null
                            ? FhirClientBuilder.get(null)
                            : FhirClientBuilder.get(channelConfig.asChannelId());

            Optional<ResourceWrapper>  patient = fhirClient.readCachedResource(new Ref("Patient/No_Patient"));
            if (patient.isPresent()) {
                ResourceWrapper thePatient = patient.get();
                Bundle patientBundle;
                if (thePatient.getResource() instanceof Bundle) {
                    patientBundle = (Bundle) thePatient.getResource();
                    Ref patRef = new Ref(patientBundle.getEntry().get(0).getFullUrl());  // this must be turned into fullURL (not relative)
                    addSubject(ss, resource, patRef , CodeTranslator.SS_PID, "XDSSubmissionSet.patientId", vale);
                } else {
                    val.add(new ValE("Internal error - Lookup of Patient/No_Patient returned " + thePatient.getResource().getClass().getSimpleName() + " instead of Bundle").asError());
                }
            } else {
                val.add(new ValE("Internal error - cannot locate Patient/No_Patient").asError());
            }
        }
        return ss;
    }

    public ExtrinsicObjectType createExtrinsicObject(ResourceWrapper resource, ValE vale) {
        Objects.requireNonNull(val);
        Objects.requireNonNull(rMgr);

        ValE tr;
        vale.setMsg("DocumentReference to DocumentEntry");

        ExtrinsicObjectType eo = new ExtrinsicObjectType();
        eo.setObjectType("urn:uuid:7edca82f-054d-47f2-a032-9b2a5b5186c1");

        DocumentReference dr = (DocumentReference) resource.getResource();

        vale.add(new ValE("Content section is [1..1]").addIheRequirement(DRTable));
        if (dr.getContent() == null || dr.getContent().isEmpty()) {
            vale.add(new ValE("DocumentReference has no content section").asError());
            return eo;
        }
        if (dr.getContent().size() > 1) {
            vale.add(new ValE("DocumentReference has multiple content sections").asError());
            return eo;
        }
        vale.add(new ValE("Content.Attachment section is [1..1]").addIheRequirement(DRTable));
        if (dr.getContent().get(0).getAttachment() == null) {
            vale.add(new ValE("DocumentReference has no content/attachment").asError());
            return eo;
        }

        DocumentReference.DocumentReferenceContextComponent context = dr.getContext();
        DocumentReference.DocumentReferenceContentComponent content = dr.getContent().get(0);
        Attachment attachment = content.getAttachment();

        new EntryUuid()
                .setVal(vale)
                .setrMgr(rMgr)
                .setResource(resource)
                .assignId(dr.getIdentifier());

        eo.setId(resource.getAssignedId());
        eo.setObjectType("urn:uuid:7edca82f-054d-47f2-a032-9b2a5b5186c1");

        tr = vale.add(new ValE("content.attachment.contentType is [1..1]").addIheRequirement(DRTable));
        tr.add(new ValE("content.attachment.contentType to mimeType").asTranslation());
        if (content.getAttachment().getContentType() == null)
            tr.add(new ValE("content.attachment.contentType not present").asError());
        else
            eo.setMimeType(content.getAttachment().getContentType());

        if (dr.getDate() != null) {
            vale.addTr(new ValE("creationTime"));
            addSlot(eo, "creationTime", translateDateTime(dr.getDate()));
        }
        if (dr.hasStatus()) {
            vale.addTr(new ValE("availabilityStatus"));
            Enumerations.DocumentReferenceStatus fStatus = dr.getStatus();
            String status = null;
            if (fStatus == Enumerations.DocumentReferenceStatus.CURRENT)
                status = "urn:oasis:names:tc:ebxml-regrep:StatusType:Approved";
            else if (fStatus == Enumerations.DocumentReferenceStatus.SUPERSEDED)
                status = "urn:oasis:names:tc:ebxml-regrep:StatusType:Deprecated";
            if (status != null)
                eo.setStatus(status);
        }

        if (context != null) {
            Period period = context.getPeriod();
            if (period != null) {
                if (period.hasStart()) {
                    vale.addTr(new ValE("serviceStartTime"));
                    addSlot(eo, "serviceStartTime", translateDateTime(period.getStart()));
                }
                if (period.hasEnd()) {
                    vale.addTr(new ValE("serviceStopTime"));
                    addSlot(eo, "serviceStopTime", translateDateTime(period.getEnd()));
                }
            }
            if (context.hasSourcePatientInfo()) {
                vale.addTr(new ValE("sourcePatientInfo"));
                addSourcePatient(eo, resource, context.getSourcePatientInfo(), vale);
            }
            if (context.hasFacilityType()) {
                vale.addTr(new ValE("facilityType"));
                addClassificationFromCodeableConcept(eo, context.getFacilityType(), CodeTranslator.HCFTCODE, resource.getAssignedId(), vale);
            }
            if (context.hasPracticeSetting()) {
                vale.addTr(new ValE("practiceSetting"));
                addClassificationFromCodeableConcept(eo, context.getPracticeSetting(), CodeTranslator.PRACCODE, resource.getAssignedId(), vale);
            }
            if (context.hasEvent()) {
                vale.addTr(new ValE("eventCode"));
                addClassificationFromCodeableConcept(eo, context.getEventFirstRep(), CodeTranslator.EVENTCODE, resource.getAssignedId(), vale);
            }
        }
        if (attachment.hasLanguage()) {
            vale.addTr(new ValE("languageCode"));
            addSlot(eo, "languageCode", attachment.getLanguage());
        }
//        if (attachment.hasUrl()) {
//            vale.addTr(new ValE("repositoryUniqueId"));
//            addSlot(eo, "repositoryUniqueId", attachment.getRef());
//        }
        if (attachment.hasHash()) {
            vale.addTr(new ValE("hash"));
            Base64BinaryType hash64 = attachment.getHashElement();
            byte[] hash = hash64.getValue();
            String hashString = DatatypeConverter.printHexBinary(hash).toLowerCase();
            addSlot(eo, "hash", hashString);
        }
        if (dr.hasDescription()) {
            vale.addTr(new ValE("description"));
            addName(eo, dr.getDescription());
        }
        if (attachment.hasTitle()) {
            vale.addTr(new ValE("title"));
            addDescription(eo, attachment.getTitle());
        }
        if (attachment.hasCreation()) {
            vale.addTr(new ValE("creationTime"));
            addCreationTime(eo, attachment.getCreation());
        }
        if (dr.hasType()) {
            tr = vale.addTr(new ValE("typeCode"));
            addClassificationFromCodeableConcept(eo, dr.getType(), CodeTranslator.TYPECODE, resource.getAssignedId(), tr);
        }
        if (dr.hasCategory()) {
            tr = vale.addTr(new ValE("classCode"));
            addClassificationFromCodeableConcept(eo, dr.getCategoryFirstRep(), CodeTranslator.CLASSCODE, resource.getAssignedId(), tr);
        }
        if (dr.hasSecurityLabel()) {
            tr = vale.addTr(new ValE("confCode"));
            addClassificationFromCoding(eo, dr.getSecurityLabel().get(0).getCoding().get(0), CodeTranslator.CONFCODE, resource.getAssignedId(), tr);
        }
        if(content.hasFormat()) {
            tr = vale.addTr(new ValE("formatCode"));
            addClassificationFromCoding(eo, dr.getContent().get(0).getFormat(), CodeTranslator.FORMATCODE, resource.getAssignedId(), tr);
        }

        tr = vale.add(new ValE("DocumentReference.masterIdentifier is [1..1]").addIheRequirement(DRTable));
        if (!dr.hasMasterIdentifier())
            tr.add(new ValE("masterIdentifier not present").asError());
        else {
            tr.add(new ValE("masterIdentifier").asTranslation());
            addExternalIdentifier(eo, CodeTranslator.DE_UNIQUEID, Utils.stripUrnPrefix(dr.getMasterIdentifier().getValue()), rMgr.allocateSymbolicId(), resource.getAssignedId(), "XDSDocumentEntry.uniqueId", idBuilder);
            resource.setAssignedUid(Utils.stripUrnPrefix(dr.getMasterIdentifier().getValue()));
        }

        tr = vale.add(new ValE("DocumentReference.subject is [1..1]").addIheRequirement(DRTable));
//        if (!dr.hasSubject() || !dr.getSubject().hasReference() && isMinimalMetadata) {
//            // Patient is optional in minimal metadata - add reference to No_Patient to make XDS Toolkit happy
//            // Adds resource cache to configuration
//            FhirClient fhirClient =
//                    channelConfig == null
//                            ? FhirClientBuilder.get(null)
//                            : FhirClientBuilder.get(channelConfig.asChannelId());
//
//            Optional<ResourceWrapper>  patient = fhirClient.readCachedResource(new Ref("Patient/No_Patient"));
//            if (patient.isPresent()) {
//                ResourceWrapper thePatient = patient.get();
//                Bundle patientBundle;
//                if (thePatient.getResource() instanceof Bundle) {
//                    patientBundle = (Bundle) thePatient.getResource();
//                    Ref patRef = new Ref(patientBundle.getEntry().get(0).getFullUrl());  // this must be turned into fullURL (not relative)
//                    addSubject(eo, resource, patRef , CodeTranslator.DE_PID, "XDSDocumentEntry.patientId", vale);
//                } else {
//                    val.add(new ValE("Internal error - Lookup of Patient/No_Patient returned " + thePatient.getResource().getClass().getSimpleName() + " instead of Bundle").asError());
//                }
//            } else {
//                val.add(new ValE("Internal error - cannot locate Patient/No_Patient").asError());
//            }
//
//        } else {
            vale.add(new ValE("subject to Patient Id").asTranslation());
            Reference sub = dr.getSubject();
            if (sub == null || Strings.isNullOrEmpty(sub.getReference())) {
                val.add(new ValE("DocumentReference.subject is missing").asError());
            } else {
                addSubject(eo, resource, new Ref(sub), CodeTranslator.DE_PID, "XDSDocumentEntry.patientId", tr);
            }
//        }
        if (dr.hasAuthor()) {
            tr = vale.addTr(new ValE("author"));
            ResourceWrapper containing = new ResourceWrapper();
            containing.setResource(dr);
            for (Reference reference : dr.getAuthor()) {
                Optional<ResourceWrapper> contained = rMgr.resolveReference(containing, new Ref(reference.getReference()), new ResolverConfig().containedRequired(), tr);
                if (contained.isPresent()) {
                    IBaseResource resource1 = contained.get().getResource();
                    ClassificationType classificationType = classificationFromAuthor(resource1, containing);
                    if (classificationType != null) {
                        classificationType.setClassifiedObject(eo.getId());
                        classificationType.setId(rMgr.allocateSymbolicId());
                        eo.getClassification().add(classificationType);
                    }
                }
            }
        }
        if (dr.hasAuthenticator()) {
            tr = vale.addTr(new ValE("authenticator"));
            ResourceWrapper containing = new ResourceWrapper();
            containing.setResource(dr);
            Reference reference = dr.getAuthenticator();
            Optional<ResourceWrapper> contained = rMgr.resolveReference(containing, new Ref(reference.getReference()), new ResolverConfig().containedRequired(), tr);
            if (contained.isPresent()) {
                IBaseResource resource1 = contained.get().getResource();
                ClassificationType classificationType = classificationFromAuthor(resource1, containing);
                if (classificationType != null) {
                    classificationType.setId(rMgr.allocateSymbolicId());
                    for (SlotType1 slot1 : classificationType.getSlot()) {
                        if ("authorPerson".equals(slot1.getName())) {
                            if (!slot1.getValueList().getValue().isEmpty()) {
                                String auth = slot1.getValueList().getValue().get(0);
                                SlotType1 legalAuthenticator = new SlotType1();
                                legalAuthenticator.setName("legalAuthenticator");
                                ValueListType valueListType = new ValueListType();
                                legalAuthenticator.setValueList(valueListType);
                                valueListType.getValue().add(auth);
                                eo.getSlot().add(legalAuthenticator);
                            }
                        }
                    }
                }
            }
        }

        if (attachment.hasData()) {
            byte[] data = attachment.getData();
            documentContents.put(eo.getId(), data);
        } else if (attachment.hasUrl()) {
            String url = attachment.getUrl();
            ResourceWrapper resourceWrapper = new ResourceWrapper();
            resourceWrapper.setRef(new Ref(url));
            Optional<ResourceWrapper> binary = rMgr.resolveReference(resourceWrapper, new Ref(url), new ResolverConfig(), vale);
            if (!binary.isPresent()) {
                vale.add(new ValE("Document(Binary) is not retrievable").asError());
                return eo;
            }
            if (!(binary.get().getResource() instanceof Binary)) {
                vale.add(new ValE("Document(Binary) is not a Binary (" + binary.get().getClass().getSimpleName() + " instead").asError());
                return eo;
            }
            Binary theBinary = (Binary) binary.get().getResource();
            eo.setMimeType(theBinary.getContentType());
            byte[] data = theBinary.getData();
            documentContents.put(eo.getId(), data);
        }
        if (attachment.hasContentType())
            eo.setMimeType(attachment.getContentType());
        return eo;
    }

    private void addCreationTime(ExtrinsicObjectType eo, Date creation) {
        String creationTime = translateDateTime(creation);
        addSlot(eo, "creationTime", creationTime);
    }

    private void addDescription(RegistryObjectType eo, String description) {
        LocalizedStringType lst = new LocalizedStringType();
        lst.setValue(description);
        InternationalStringType ist = new InternationalStringType();
        ist.getLocalizedString().add(lst);
        eo.setDescription(ist);
    }

    private ClassificationType classificationFromAuthor(IBaseResource resource1, ResourceWrapper containing) {
        if (resource1 instanceof Practitioner) {
            Practitioner practitioner = (Practitioner) resource1;
            ClassificationType classificationType = new Author().practitionerToClassification(practitioner);
            classificationType.setClassificationScheme("urn:uuid:93606bcf-9494-43ec-9b4e-a7748d1a838d");
            return classificationType;
        } else if (resource1 instanceof PractitionerRole) {
            PractitionerRole practitionerRole = (PractitionerRole) resource1;
            if (practitionerRole.hasPractitioner()) {
                Optional<ResourceWrapper> contained2 = rMgr.resolveReference(containing, new Ref(practitionerRole.getPractitioner()), new ResolverConfig().containedRequired());
                if (contained2.isPresent() && contained2.get().getResource() instanceof Practitioner) {
                    Practitioner practitioner = (Practitioner) contained2.get().getResource();
                    Author author = new Author();
                    for (CodeableConcept cc : practitionerRole.getCode()) {
                        AuthorRole role = new AuthorRole(cc);
                        author.getAuthorRoles().add(role);
                    }
                    ClassificationType classificationType = author.practitionerToClassification(practitioner);
                    classificationType.setClassificationScheme("urn:uuid:93606bcf-9494-43ec-9b4e-a7748d1a838d");
                    return classificationType;
                }
            }
        } else if (resource1 instanceof Organization) {
            Organization organization = (Organization) resource1;
            Author author = new Author();
            author.setVal(val);
            ClassificationType classificationType = author.organizationToClassification(organization);
            if (classificationType != null) {
                classificationType.setClassificationScheme("urn:uuid:93606bcf-9494-43ec-9b4e-a7748d1a838d");
                return classificationType;
            }
        } else {
            val.add(new ValE("Cannot process author of type " + resource1.getClass().getSimpleName()).asWarning());
        }
        return null;
    }

    private String translateDateTime(Date date) {
        return DateTransform.fhirToDtm(date);
    }

    public void addName(RegistryObjectType eo, String name) {
        LocalizedStringType lst = new LocalizedStringType();
        lst.setValue(name);
        InternationalStringType ist = new InternationalStringType();
        ist.getLocalizedString().add(lst);
        eo.setName(ist);
    }

    public void addSlot(RegistryObjectType registryObject, String name, String value) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);
        SlotType1 slot = new SlotType1();
        slot.setName(name);
        ValueListType valueList = new ValueListType();
        valueList.getValue().add(value);
        slot.setValueList(valueList);
        registryObject.getSlot().add(slot);
    }

    public void addSlot(RegistryObjectType registryObject, String name, List<String> values) {
        Objects.requireNonNull(name);
        SlotType1 slot = new SlotType1();
        slot.setName(name);
        ValueListType valueList = new ValueListType();
        for (String value : values)
            valueList.getValue().add(value);
        slot.setValueList(valueList);
        registryObject.getSlot().add(slot);
    }

    /**
     * Patient resources shall not be in the bundle so don't look there.  Must have fullUrl reference
     */

//    private void addSourcePatientInfo(ExtrinsicObjectType eo, ResourceWrapper resource, Reference sourcePatient, ValE val) {
//    }
        // TODO sourcePatientInfo is not populated
    private void addSourcePatient(ExtrinsicObjectType eo, ResourceWrapper resource, Reference sourcePatient, ValE val) {
        if (sourcePatient.getReference() == null)
            return;
        val.add(new ValE("Resolve ${sourcePatient.reference} as SourcePatient"));
        String extra = "DocumentReference.context.sourcePatientInfo must reference Contained Patient resource with Patient.identifier.use element set to 'usual'";
        Optional <ResourceWrapper> loadedPatient = rMgr.resolveReference(resource, new Ref(sourcePatient.getReference()), new ResolverConfig().containedRequired());
        if (!loadedPatient.isPresent() || loadedPatient.get().getResource() == null) {
            val.add(new ValE("Cannot resolve sourcePatient.reference: " + sourcePatient.getReference()).asError());
            return;
        }

        if (!(loadedPatient.get().getResource() instanceof Patient)) {
            val.add(new ValE("Patient loaded from " + loadedPatient.get().getRef().asString() +  " returned a " + loadedPatient.get().getResource().getClass().getSimpleName() + " instead").asError());
            return;
        }

        Patient patient = (Patient) loadedPatient.get().getResource();

        // find identifier that aligns with required Assigning Authority
        List<Identifier> identifiers = patient.getIdentifier();

        String pid = findAcceptablePID(identifiers);
        if (pid != null)
            addSlot(eo, "sourcePatientId", Collections.singletonList(pid));
    }

    private String findAcceptablePID(List<Identifier> identifiers) {
        Objects.requireNonNull(assigningAuthorities);
        List<String> pids = identifiers.stream()
                .filter(identifier -> assigningAuthorities.check(Utils.stripUrnPrefix(identifier.getSystem())))
                .map(identifier -> identifier.getValue() + "^^^&" + Utils.stripUrnPrefix(identifier.getSystem()) + "&ISO")
                .collect(Collectors.toList());

        return (pids.isEmpty()) ? null : pids.get(0);
    }

    // TODO must be absolute reference
    // TODO official identifiers must be changed
    private void addSubject(RegistryObjectType ro, ResourceWrapper resource, Ref referenced, String scheme, String attName, ValE val) {

        Optional<ResourceWrapper> loadedResource = rMgr.resolveReference(resource, referenced, new ResolverConfig().externalRequired());
        boolean isLoaded = loadedResource.isPresent() && loadedResource.get().isLoaded();
        if (!isLoaded) {
            val.add(new ValE(resource + " makes reference to " + referenced + " which cannot be loaded").asError()
                    .add(new ValE("   All DocumentReference.subject and DocumentManifest.subject values shall be References to FHIR Patient Resources identified by an absolute external reference (URL).").asDoc())
                    .add(new ValE("3.65.4.1.2.2 Patient Identity").asDoc()));
            return;
        }
        if (!(loadedResource.get().getResource() instanceof Patient)) {
            val.add(new ValE(resource + " points to a " + loadedResource.get().getResource().getClass().getSimpleName() + " - it must be a Patient").asError()
                    .add(new ValE("3.65.4.1.2.2 Patient Identity").asDoc()));
            return;
        }

        Patient patient = (Patient) loadedResource.get().getResource();

        List<Identifier> identifiers = patient.getIdentifier();
        String pid = findAcceptablePID(identifiers);

        if (pid != null)
            addExternalIdentifier(ro, scheme, pid, rMgr.allocateSymbolicId(), resource.getAssignedId(), attName, null);
    }

    private AssociationType1 addRelationship(RegistryObjectType source, String relationshipType, Ref target, ValE val) {
        Objects.requireNonNull(task);
        Objects.requireNonNull(sqEndpoint);
        if (relationshipType == null || !relationshipType.equals("replaces")) {
            val.add(new ValE("Relationship " + relationshipType + " is not supported ").asError());
            return null;
        }

        AhqrSender sender = FhirSq.documentEntryByUidQuery(target.getId(), sqEndpoint, task.newTask());
        List<IdentifiableType> contents = sender.getContents();
        if (contents.size() != 1) {
            val.add(new ValE("Error retrieving DocumentEntry " + target.getId() + " from " + sqEndpoint + " - expected 1 entry but got " + contents.size()).asError());
            return null;
        }
        ExtrinsicObjectType eo = (ExtrinsicObjectType) contents.get(0);
        String targetId = eo.getId();

        return createAssociation("urn:ihe:iti:2007:AssociationType:RPLC",
                source.getId(),
                targetId,
                null,
                null,
                val);
    }

    public void addExternalIdentifier(RegistryObjectType ro, String scheme, String value, String id, String registryObject, String name, IdBuilder idBuilder) {
        val.add(new ValE("ExternalIdentifier " + scheme));
        //List<ExternalIdentifierType> eits = ro.getExternalIdentifier();
//        if (idBuilder != null)
//            value = idBuilder.allocate(value); // maybe override
        ExternalIdentifierType eit = new ExternalIdentifierType();
        eit.setIdentificationScheme(scheme);
        eit.setId(id);
        eit.setRegistryObject(registryObject);
        eit.setValue(value);
        eit.setObjectType("urn:oasis:names:tc:ebxml-regrep:ObjectType:RegistryObject:ExternalIdentifier");

        InternationalStringType ist = new InternationalStringType();
        LocalizedStringType lst = new LocalizedStringType();
        lst.setValue(name);
        ist.getLocalizedString().add(lst);
        eit.setName(ist);
        ro.getExternalIdentifier().add(eit);
    }

    // TODO - no profile guidance on how to convert coding.system URL to existing OIDs

    public void addClassificationFromCodeableConcept(RegistryObjectType ro, CodeableConcept cc, String scheme, String classifiedObjectId, ValE val) {
        List<Coding> coding = cc.getCoding();
        addClassificationFromCoding(ro, coding.get(0), scheme, classifiedObjectId, val);
    }

    private void addClassificationFromCoding(RegistryObjectType ro, Coding coding, String scheme, String classifiedObjectId, ValE val) {
        Objects.requireNonNull(codeTranslator);
        Objects.requireNonNull(val);
        Objects.requireNonNull(rMgr);
        Optional<Code> systemCodeOpt = codeTranslator.findCodeByClassificationAndSystem(scheme, coding.getSystem(), coding.getCode());
        if (systemCodeOpt.isPresent()) {
            Code systemCode = systemCodeOpt.get();
            String displayName = coding.getDisplay() == null ? systemCode.getDisplay() : coding.getDisplay();
            addClassification(ro, scheme, rMgr.allocateSymbolicId(), classifiedObjectId, coding.getCode(), systemCode.getCodingScheme(), displayName);
        } else {
            Optional<gov.nist.asbestos.asbestorCodesJaxb.CodeType> type = codeTranslator.findCodeTypeForScheme(scheme);
            String schemeName = (type.isPresent()) ? type.get().getName() : scheme;
            val.add(new ValE("Cannot find translation for code " + coding.getSystem() + "|" + coding.getCode() + " as part of attribute " + schemeName + " in configured codes.xml file").asError());
        }
    }

    /**
     * add external classification (see ebRIM for definition)
     * @param scheme
     * @param id
     * @param registryObject
     * @param value
     * @param codeScheme
     * @param displayName
     * @return
     */
    private void addClassification(RegistryObjectType ro, String scheme, String id, String registryObject, String value, String codeScheme, String displayName) {
        ClassificationType ct = new ClassificationType();
        ct.setClassificationScheme(scheme);
        ct.setId(id);
        ct.setObjectType("urn:oasis:names:tc:ebxml-regrep:ObjectType:RegistryObject:Classification");
        ct.setNodeRepresentation(value);
        ct.setClassifiedObject(registryObject);
        addSlot(ct, "codingScheme", codeScheme);
        addName(ct, displayName);
        ro.getClassification().add(ct);
    }

    private void addClassification(RegistryObjectType ro, String node, String id, String classifiedObject) {
        val.add(new ValE("Classification " + node));
        ClassificationType ct = new ClassificationType();
        ct.setClassifiedObject(classifiedObject);
        ct.setClassificationNode(node);
        ct.setId(id);
        ct.setObjectType("urn:oasis:names:tc:ebxml-regrep:ObjectType:RegistryObject:Classification");
        ro.getClassification().add(ct);
    }


//    private addDocument(MarkupBuilder builder, String drId, String contentId) {
//        val.add(new Val().msg("Attach Document ${drId}"))
//        builder.Document(id:drId, xmlns: 'urn:ihe:iti:xds-b:2007') {
//            Include(href: "cid:${contentId}", xmlns: 'http://www.w3.org/2004/08/xop/include')
//        }
//    }

//    static List<MhdIdentifier> getIdentifiers(IBaseResource resource) {
//        assert resource instanceof DocumentManifest || resource instanceof DocumentReference
//
//        List<Identifier> identifiers = (resource instanceof DocumentManifest) ?
//                ((DocumentManifest) resource).identifier :
//                ((DocumentReference) resource).identifier
//        identifiers.collect { Identifier ident -> new MhdIdentifier(ident)}
//    }

    private void scanBundleForAcceptability(Bundle bundle, ResourceMgr rMgr) {
        if (bundle.getMeta().getProfile().size() != 1)
            val.add(new ValE("No profile declaration present in bundle").asError()
                    .add(new ValE("3.65.4.1.2.1 Bundle Resources").asDoc()));
        try {
            CanonicalType bundleProfile = bundle.getMeta().getProfile().get(0);
            if (!profiles.contains(bundleProfile.asStringValue()))
                val.add(new ValE("Do not understand profile declared in bundle - " + bundleProfile).asError()
                        .add(new ValE("3.65.4.1.2.1 Bundle Resources").asDoc()));
            if (bundleProfile.asStringValue().equals(minimalMetadataProfile))
                isMinimalMetadata = true;
        } catch (Throwable e) {
            val.add(new ValE("Bundle.meta.profile missing").asError()
                    .add(new ValE("3.65.4.1.2.1 Bundle Resources").asDoc()));
        }

        try {
            if (!bundle.getType().toCode().equals("transaction")) {
                val.add(new ValE("Bundle.type missing").asError()
                        .add(new ValE("http://hl7.org/fhir/http.html#transaction").asDoc()));
            }
        } catch (Throwable t) {
            val.add(new ValE("Bundle.type missing").asError()
                    .add(new ValE("http://hl7.org/fhir/http.html#transaction").asDoc()));
        }

        for (ResourceWrapper res : rMgr.getBundleResources()) {
            if (!acceptableResourceTypes.contains(res.getResource().getClass()))
                val.add(new ValE("Resource type ${resource.resource.class.simpleName} is not part of MHD and will be ignored").asWarning()
                        .add(new ValE(mhdProfileRef).asDoc()));
        }

        String patientUrl = null;
        for (ResourceWrapper res : rMgr.getBundleResources()) {
            if (res.getResource() instanceof DocumentManifest) {
                DocumentManifest dm = (DocumentManifest) res.getResource();
                if (patientUrl == null) {
                    patientUrl = dm.getSubject().getReference();
                } else {
                    if (!dm.getSubject().getReference().equals(patientUrl)) {
                        val.add(new ValE("Multiple patients reference in Bundle").asError()
                                .add(new ValE("3.65.4.1.2.2 Patient Identity").asDoc()));
                        break;
                    }
                }
            }
            else if (res.getResource() instanceof DocumentReference) {
                DocumentReference dm = (DocumentReference) res.getResource();
                if (patientUrl == null) {
                    patientUrl = dm.getSubject().getReference();
                } else {
                    if (dm.hasSubject() && !dm.getSubject().getReference().equals(patientUrl)) {
                        val.add(new ValE("Multiple patients reference in Bundle").asError()
                                .add(new ValE("3.65.4.1.2.2 Patient Identity").asDoc()));
                        break;
                    }
                }
            }
        }

//        for (ResourceWrapper wrapper : rMgr.getBundleResources()) {
//            if (wrapper.getResource() instanceof DocumentReference) {
//                DocumentReference documentReference = (DocumentReference) wrapper.getResource();
//                List<DocumentReference.DocumentReferenceContentComponent> ccs = documentReference.getContent();
//                for (DocumentReference.DocumentReferenceContentComponent cc : ccs) {
//                    Attachment a = cc.getAttachment();
//                    String url = a.getUrl();
//                    if (!bundleContains(bundle, url)) {
//                        val.add(new ValE("Cannot resolve Binary reference from DocumentReference: " + url).asError()
//                                .add(new ValE("3.65.4.1.2.1 Bundle Resources").asDoc()));
//                    }
//                }
//            }
//        }
    }

    private boolean bundleContains(Bundle bundle, String fullUrl) {
        if (fullUrl == null)
            return false;
        for (Bundle.BundleEntryComponent comp : bundle.getEntry()) {
            String aFullUrl = comp.getFullUrl();
            if (fullUrl.equals(aFullUrl))
                return true;
        }
        return false;
    }

    @Override
    public void setVal(Val val) {
        this.val = val;
        if (rMgr != null)
            rMgr.setVal(val);
    }

    public BundleToRegistryObjectList setResourceMgr(ResourceMgr rMgr) {
        this.rMgr = rMgr;
        return this;
    }

    public BundleToRegistryObjectList setAssigningAuthorities(AssigningAuthorities assigningAuthorities) {
        this.assigningAuthorities = assigningAuthorities;
        return this;
    }

    public BundleToRegistryObjectList setCodeTranslator(CodeTranslator codeTranslator) {
        this.codeTranslator = codeTranslator;
        return this;
    }

    public BundleToRegistryObjectList setBundleProfile(CanonicalType bundleProfile) {
        return this;
    }

    public byte[] getDocumentContents(String id) {
        return documentContents.get(id);
    }

    public Map<String,  byte[]> getDocumentContents() {
        return documentContents;
    }

    public void setIdBuilder(IdBuilder idBuilder) {
        this.idBuilder = idBuilder;
    }

    public BundleToRegistryObjectList setErrorsOnly(boolean errorsOnly) {
        this.errorsOnly = errorsOnly;
        return this;
    }

    public Bundle getResponseBundle() {
        return responseBundle;
    }

    public boolean isResponseHasError() {
        return responseHasError;
    }

    public BundleToRegistryObjectList setTask(ITask task) {
        this.task = task;
        return this;
    }

    public BundleToRegistryObjectList setSqEndpoint(URI sqEndpoint) {
        this.sqEndpoint = sqEndpoint;
        return this;
    }
}
