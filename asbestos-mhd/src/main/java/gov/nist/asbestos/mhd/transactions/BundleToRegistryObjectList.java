package gov.nist.asbestos.mhd.transactions;


import gov.nist.asbestos.asbestorCodesJaxb.Code;
import gov.nist.asbestos.asbestosProxySupport.Base.IVal;
import gov.nist.asbestos.mhd.resolver.Ref;
import gov.nist.asbestos.mhd.resolver.ResolverConfig;
import gov.nist.asbestos.mhd.resolver.ResourceCacheMgr;
import gov.nist.asbestos.mhd.resolver.ResourceMgr;
import gov.nist.asbestos.mhd.transactionSupport.AssigningAuthorities;
import gov.nist.asbestos.mhd.transactionSupport.CodeTranslator;
import gov.nist.asbestos.mhd.transactionSupport.ResourceWrapper;
import gov.nist.asbestos.mhd.transactionSupport.Submission;
import gov.nist.asbestos.simapi.validation.Val;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.*;
import org.hl7.fhir.r4.model.*;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBElement;
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
    private static List<CanonicalType> profiles = Arrays.asList(new CanonicalType(comprehensiveMetadataProfile), new CanonicalType(minimalMetadataProfile));
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

    private ResourceCacheMgr resourceCacheMgr;
    private CodeTranslator codeTranslator;
    private Configuration config;
    private AssigningAuthorities assigningAuthorities;
    private Map<String, String> documents = new HashMap<>();  // symbolidId -> contentId
    private ResourceMgr rMgr;
    private CanonicalType bundleProfile;
    private Val val;


    public BundleToRegistryObjectList(ResourceCacheMgr resourceCacheMgr, CodeTranslator codeTranslator, AssigningAuthorities assigningAuthorities, Configuration config) {
        this.resourceCacheMgr = resourceCacheMgr;
        this.codeTranslator = codeTranslator;
        this.assigningAuthorities = assigningAuthorities;
        this.config = config;
    }

    public BundleToRegistryObjectList() {
        rMgr = new ResourceMgr();
    }

//    public Submission build(Bundle bundle) {
//        Objects.requireNonNull(val);
//        Objects.requireNonNull(bundle);
//        rMgr = new ResourceMgr(bundle).addResourceCacheMgr(resourceCacheMgr);
//        rMgr.setVal(val);
//        scanBundleForAcceptability(bundle, rMgr);
//        Submission submission = new Submission();
//
//        buildRegistryObjectList();
//
//        StringWriter writer = new StringWriter();
//        MarkupBuilder builder = new MarkupBuilder(writer);
//        submission.documentIdToContentId.each { id, contentId ->
//            addDocument(builder, id, contentId)
//        }
//        submission.documentDefinitions = writer.toString()
//
//        return submission;
//    }

    // TODO handle List/Folder or signal error
    private RegistryObjectListType buildRegistryObjectList() {
        RegistryObjectListType rol = new RegistryObjectListType();

        List<ResourceWrapper> docMans = rMgr.getBundleResources().stream()
                .filter(rw -> rw.getResource().getClass().equals(DocumentManifest.class))
                .collect(Collectors.toList());

        List<ResourceWrapper> docRefs = rMgr.getBundleResources().stream()
                .filter(rw -> rw.getResource().getClass().equals(DocumentReference.class))
                .collect(Collectors.toList());

        if (docMans.size() != 1)
            val.err(new Val("Found " + docMans.size() + " DocumentManifests - one required"));

        RegistryPackageType ss = null;
        if (docMans.size() > 0) {
            ResourceWrapper dm = docMans.get(0);
            ss = createSubmissionSet(dm);
            Object o = ss;
            rol.getIdentifiable().add((JAXBElement<? extends IdentifiableType>) o);
        }

        List<ExtrinsicObjectType> eos = docRefs.stream()
                .map(this::createExtrinsicObject)
                .collect(Collectors.toList());

        for (ExtrinsicObjectType eo : eos) {
            Object o = eo;
            rol.getIdentifiable().add((JAXBElement<? extends IdentifiableType>) o);
            if (ss != null) {
                AssociationType1 a = createSSDEAssociation(ss, eo);
                Object o1 = a;
                rol.getIdentifiable().add((JAXBElement<? extends IdentifiableType>) o1);
            }
        }
        return rol;
    }


//    private void buildSubmission() {
//        Submission submission = new Submission()
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

    private AssociationType1 createSSDEAssociation(RegistryPackageType ss, ExtrinsicObjectType de) {
        return createAssociation("urn:oasis:names:tc:ebxml-regrep:AssociationType:HasMember", ss.getId(), de.getId(), "SubmissionSetStatus", Collections.singletonList("Original"));


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
        val.add(new Val().msg("Association(" + type + ") source=" + source + " target=" + target));
        at.setSourceObject(source.getAssignedId());
        at.setTargetObject(target.getId());
        at.setAssociationType(type);
        at.setId(rMgr.allocateSymbolicId());
        addSlot(at, slotName, slotValues);
        return at;
    }

    public AssociationType1 createAssociation(String type, String sourceId, String targetId, String slotName, List<String> slotValues) {
        AssociationType1 at = new AssociationType1();
        at.setSourceObject(sourceId);
        at.setTargetObject(targetId);
        at.setAssociationType(type);
        at.setId(rMgr.allocateSymbolicId());
        if (slotName != null)
            addSlot(at, slotName, slotValues);
        return at;
    }

    private RegistryPackageType createSubmissionSet(ResourceWrapper resource) {
        DocumentManifest dm = (DocumentManifest) resource.getResource();

        RegistryPackageType ss = new RegistryPackageType();

        val.add(new Val().msg("SubmissionSet(${resource.assignedId})"));
        ss.setId(resource.getAssignedId());
        ss.setObjectType("urn:oasis:names:tc:ebxml-regrep:ObjectType:RegistryObject:RegistryPackage");

        if (dm.hasCreated())
            addSlot(ss, "submissionTime", translateDateTime(dm.getCreated()));
        if (dm.hasDescription())
            addName(ss, dm.getDescription());
        addClassification(ss, "", rMgr.allocateSymbolicId(), resource.getAssignedId());
        if (dm.hasType())
            addClassificationFromCodeableConcept(ss, dm.getType(), CodeTranslator.TYPECODE, resource.getAssignedId());
        if (!dm.hasMasterIdentifier())
            val.err(new Val("DocumentManifest.masterIdentifier not present - declared by IHE to be [1..1]"));
        else
            addExternalIdentifier(ss, CodeTranslator.SS_UNIQUEID, unURN(dm.getMasterIdentifier().getValue()), rMgr.allocateSymbolicId(), resource.getAssignedId(), "XDSSubmissionSet.uniqueId");
        if (dm.hasSource())
            addExternalIdentifier(ss, CodeTranslator.SS_SOURCEID, unURN(dm.getMasterIdentifier().getValue()), rMgr.allocateSymbolicId(), resource.getAssignedId(), "XDSSubmissionSet.uniqueId");
        if (dm.hasSubject() && dm.getSubject().hasReference())
            addSubject(ss, resource,  new Ref(dm.getSubject()), CodeTranslator.SS_PID, "XDSSubmissionSet.patientId");
        return ss;
    }

    private ExtrinsicObjectType createExtrinsicObject(ResourceWrapper resource) {
        DocumentReference dr = (DocumentReference) resource.getResource();
        if (dr.getContent() == null || dr.getContent().isEmpty()) {
            val.err(new Val("DocumentReference has no content section"));
            return null;
        }
        if (dr.getContent().size() > 1) {
            val.err(new Val("DocumentReference has multiple content sections"));
            return null;
        }
        if (dr.getContent().get(0).getAttachment() == null) {
            val.err(new Val("DocumentReference has no content/attachment"));
            return null;
        }

        ExtrinsicObjectType eo = new ExtrinsicObjectType();

        DocumentReference.DocumentReferenceContextComponent context = dr.getContext();
        DocumentReference.DocumentReferenceContentComponent content = dr.getContent().get(0);
        Attachment attachment = content.getAttachment();

        if (resource.getAssignedId() == null)
            resource.setId(rMgr.allocateSymbolicId());

        eo.setId(resource.getAssignedId());
        eo.setObjectType("urn:uuid:7edca82f-054d-47f2-a032-9b2a5b5186c1");
        eo.setMimeType(content.getAttachment().getContentType());
        if (dr.getDate() != null)
            addSlot(eo, "creationTime", translateDateTime(dr.getDate()));

        if (context != null) {
            Period period = context.getPeriod();
            if (period != null) {
                if (period.hasStart()) {
                    addSlot(eo, "serviceStartTime", translateDateTime(period.getStart()));
                }
                if (period.hasEnd()) {
                    addSlot(eo, "serviceStopTime", translateDateTime(period.getEnd()));
                }
            }
            if (context.hasSourcePatientInfo())
                addSourcePatientInfo(eo, resource, context.getSourcePatientInfo());
            if (context.hasFacilityType())
                addClassificationFromCodeableConcept(eo, context.getFacilityType(), CodeTranslator.HCFTCODE, resource.getAssignedId());
            if (context.hasPracticeSetting())
                addClassificationFromCodeableConcept(eo, context.getPracticeSetting(), CodeTranslator.PRACCODE, resource.getAssignedId());
            if (context.hasEvent())
                addClassificationFromCodeableConcept(eo, context.getEventFirstRep(), CodeTranslator.EVENTCODE, resource.getAssignedId());
        }
        if (attachment.hasLanguage())
            addSlot(eo, "languageCode", attachment.getLanguage());
        if (attachment.hasUrl())
            addSlot(eo, "repositoryUniqueId", attachment.getUrl());
        if (attachment.hasHash()) {
            Base64BinaryType hash64 = attachment.getHashElement();
            val.add("base64Binary is " + hash64.asStringValue());
            byte[] hash = hash64.getValue();
            String hashString = DatatypeConverter.printHexBinary(hash).toLowerCase();
            val.add(new Val().msg("hexBinary is " + hashString));
            addSlot(eo, "hash", hashString);
        }
        if (dr.hasDescription())
            addName(eo, dr.getDescription());
        if (dr.hasType())
            addClassificationFromCodeableConcept(eo, dr.getType(), CodeTranslator.TYPECODE, resource.getAssignedId());
        if (dr.hasCategory())
            addClassificationFromCodeableConcept(eo, dr.getCategoryFirstRep(), CodeTranslator.CLASSCODE, resource.getAssignedId());
        if (dr.hasSecurityLabel())
            addClassificationFromCoding(eo, dr.getSecurityLabel().get(0).getCoding().get(0), CodeTranslator.CONFCODE, resource.getAssignedId());
        if(content.hasFormat())
            addClassificationFromCoding(eo, dr.getContent().get(0).getFormat(), CodeTranslator.FORMATCODE, resource.getAssignedId());
        if (!dr.hasMasterIdentifier())
            val.err(new Val("DocumentReference.masterIdentifier not present - declared by IHE to be [1..1]"));
        else
            addExternalIdentifier(eo, CodeTranslator.DE_UNIQUEID, unURN(dr.getMasterIdentifier().getValue()), rMgr.allocateSymbolicId(), resource.getAssignedId(), "XDSDocumentEntry.uniqueId");
        if (dr.hasSubject() && dr.getSubject().hasReference()) {
            addSubject(eo, resource,  new Ref(dr.getSubject()), CodeTranslator.DE_PID, "XDSDocumentEntry.patientId");
        }
        if (dr.hasAuthor()) {
            for (Reference reference : dr.getAuthor()) {

            }
        }
        return eo;
    }

    private String translateDateTime(Date date) {
        return "";
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

    private void addSourcePatientInfo(ExtrinsicObjectType eo, ResourceWrapper resource, Reference sourcePatient) {
    }
        // TODO sourcePatientInfo is not populated
//    private void addSourcePatient(ExtrinsicObjectType eo, ResourceWrapper resource, Reference sourcePatient) {
//        if (!sourcePatient.reference)
//            return
//        val.add(new Val().msg("Resolve ${sourcePatient.reference} as SourcePatient"))
//        def extra = 'DocumentReference.context.sourcePatientInfo must reference Contained Patient resource with Patient.identifier.use element set to "usual"'
//        ResourceWrapper loadedPatient = rMgr.resolveReference(resource, new Ref(sourcePatient.reference), new ResolverConfig().containedRequired())
//        if (!loadedPatient.resource) {
//            val.err(new Val()
//            .msg("Cannot load resource at ${loadedPatient.url}"))
//            return
//        }
//
//        if (!(loadedPatient.resource instanceof Patient)) {
//            val.err(new Val()
//            .msg("Patient loaded from ${loadedPatient.url} returned a ${loadedPatient.resource.class.simpleName} instead"))
//            return
//        }
//
//        Patient patient = (Patient) loadedPatient.resource
//
//        // find identifier that aligns with required Assigning Authority
//        List<Identifier> identifiers = patient.getIdentifier()
//
//        String pid = findAcceptablePID(identifiers)
//        if (pid)
//            addSlot(builder, 'sourcePatientId', [pid])
//    }

    private String findAcceptablePID(List<Identifier> identifiers) {
        List<String> pids = identifiers.stream()
                .filter(identifier -> assigningAuthorities.check(unURN(identifier.getSystem())))
                .map(identifier -> identifier.getValue() + "^^^" + unURN(identifier.getSystem()) + "&ISO")
                .collect(Collectors.toList());

        return (pids.isEmpty()) ? null : pids.get(0);
    }

    // TODO must be absolute reference
    // TODO official identifiers must be changed
    private void addSubject(RegistryObjectType ro, ResourceWrapper resource, Ref referenced, String scheme, String attName) {

        ResourceWrapper loadedResource = rMgr.resolveReference(resource, referenced, new ResolverConfig().externalRequired()).get();
        if (loadedResource.getUrl() == null) {
            val.err(new Val()
                    .msg(resource + " makes reference to " + referenced)
                    .msg("All DocumentReference.subject and DocumentManifest.subject values shall be References to FHIR Patient Resources identified by an absolute external reference (URL).")
                    .frameworkDoc("3.65.4.1.2.2 Patient Identity"));
        }
        if (!(loadedResource.getResource() instanceof Patient))
            val.err(new Val()
                    .msg(resource + " points to a " + loadedResource.getResource().getClass().getSimpleName() + " - it must be a Patient")
                    .frameworkDoc("3.65.4.1.2.2 Patient Identity"));

        Patient patient = (Patient) loadedResource.getResource();

        List<Identifier> identifiers = patient.getIdentifier();
        String pid = findAcceptablePID(identifiers);

        if (pid != null)
            addExternalIdentifier(ro, scheme, pid, rMgr.allocateSymbolicId(), resource.getId(), attName);
    }

    public void addExternalIdentifier(RegistryObjectType ro, String scheme, String value, String id, String registryObject, String name) {
        val.add(new Val().msg("ExternalIdentifier " + scheme));
        //List<ExternalIdentifierType> eits = ro.getExternalIdentifier();
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

    public void addClassificationFromCodeableConcept(RegistryObjectType ro, CodeableConcept cc, String scheme, String classifiedObjectId) {
        List<Coding> coding = cc.getCoding();
        addClassificationFromCoding(ro, coding.get(0), scheme, classifiedObjectId);
    }

    private void addClassificationFromCoding(RegistryObjectType ro, Coding coding, String scheme, String classifiedObjectId) {
        Objects.requireNonNull(codeTranslator);
        Objects.requireNonNull(val);
        Objects.requireNonNull(rMgr);
        Optional<Code> systemCodeOpt = codeTranslator.findCodeByClassificationAndSystem(scheme, coding.getSystem(), coding.getCode());
        if (systemCodeOpt.isPresent()) {
            Code systemCode = systemCodeOpt.get();
            addClassification(ro, scheme, rMgr.allocateSymbolicId(), classifiedObjectId, coding.getCode(), systemCode.getCodingScheme(), coding.getDisplay());
        } else
            val.err(new Val().msg("Cannot find translation for code " + coding.getSystem() + "|" + coding.getCode() + " as part of " + scheme + " (FHIR) into XDS coding scheme " + scheme + " in configured codes.xml file"));
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
        val.add(new Val().msg("Classification " + node));
        ClassificationType ct = new ClassificationType();
        ct.setClassificationScheme(node);
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

    private static String unURN(String uuid) {
        if (uuid.startsWith("urn:uuid:")) return uuid.substring(9);
        if (uuid.startsWith("urn:oid:")) return uuid.substring(8);
        return uuid;
    }

    private void scanBundleForAcceptability(Bundle bundle, ResourceMgr rMgr) {
        if (bundle.getMeta().getProfile().size() != 1)
            val.err(new Val()
                    .msg("No profile declaration present in bundle")
                    .frameworkDoc("3.65.4.1.2.1 Bundle Resources"));
        CanonicalType bundleProfile = bundle.getMeta().getProfile().get(0);
        if (!profiles.contains(bundleProfile))
            val.err(new Val()
                    .msg("Do not understand profile declared in bundle - ${bundleProfile}")
                    .frameworkDoc("3.65.4.1.2.1 Bundle Resources"));

        for (ResourceWrapper res : rMgr.getBundleResources()) {
            if (!acceptableResourceTypes.contains(res.getResource().getClass()))
                val.warn(new Val()
                        .msg("Resource type ${resource.resource.class.simpleName} is not part of MHD and will be ignored"))
                        .frameworkDoc(mhdProfileRef);
        }

    }

    @Override
    public void setVal(Val val) {
        this.val = val;
    }

    public void setResourceMgr(ResourceMgr rMgr) {
        this.rMgr = rMgr;
    }
}
