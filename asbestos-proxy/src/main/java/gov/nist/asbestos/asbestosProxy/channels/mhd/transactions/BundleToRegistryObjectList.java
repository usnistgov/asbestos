package gov.nist.asbestos.asbestosProxy.channels.mhd.transactions;


import gov.nist.asbestos.asbestorCodesJaxb.Code;
import gov.nist.asbestos.asbestosProxy.Base.IVal;
import gov.nist.asbestos.asbestosProxy.channels.mhd.resolver.Ref;
import gov.nist.asbestos.asbestosProxy.channels.mhd.resolver.ResolverConfig;
import gov.nist.asbestos.asbestosProxy.channels.mhd.resolver.ResourceCacheMgr;
import gov.nist.asbestos.asbestosProxy.channels.mhd.resolver.ResourceMgr;
import gov.nist.asbestos.asbestosProxy.channels.mhd.transactionSupport.AssigningAuthorities;
import gov.nist.asbestos.asbestosProxy.channels.mhd.transactionSupport.CodeTranslator;
import gov.nist.asbestos.asbestosProxy.channels.mhd.transactionSupport.ResourceWrapper;
import gov.nist.asbestos.asbestosProxy.channels.mhd.transactionSupport.Submission;
import gov.nist.asbestos.simapi.validation.Val;
import groovy.xml.MarkupBuilder;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.*;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.instance.model.api.IBaseResource;

import javax.xml.bind.DatatypeConverter;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 */

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

    private ResourceCacheMgr resourceCacheMgr;
    private CodeTranslator codeTranslator;
    private Configuration config;
    private AssigningAuthorities assigningAuthorities;
    private Map<String, String> documents = new HashMap<>();  // symbolidId -> contentId
    private ResourceMgr rMgr;
    private String bundleProfile;
    private Val val;


    BundleToRegistryObjectList(ResourceCacheMgr resourceCacheMgr, CodeTranslator codeTranslator, AssigningAuthorities assigningAuthorities, Configuration config) {
        this.resourceCacheMgr = resourceCacheMgr;
        this.codeTranslator = codeTranslator;
        this.assigningAuthorities = assigningAuthorities;
        this.config = config;
    }

    public Submission build(Bundle bundle) {
        Objects.requireNonNull(val);
        Objects.requireNonNull(bundle);
        rMgr = new ResourceMgr(bundle).addResourceCacheMgr(resourceCacheMgr);
        rMgr.setVal(val);
        scanBundleForAcceptability(bundle, rMgr);

        Submission submission = buildRegistryObjectList();

        StringWriter writer = new StringWriter();
        MarkupBuilder builder = new MarkupBuilder(writer);
        submission.documentIdToContentId.each { id, contentId ->
            addDocument(builder, id, contentId)
        }
        submission.documentDefinitions = writer.toString()

        submission
    }

    // TODO handle List/Folder or signal error
    private Submission buildRegistryObjectList() {
        Submission submission = new Submission()
        submission.contentId = 'm' + baseContentId

        int index = 1

        StringWriter writer = new StringWriter()
        MarkupBuilder xml = new MarkupBuilder(writer)
        xml.RegistryObjectList(xmlns: 'urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0') {
            rMgr.resources.each { Ref url, ResourceWrapper resource ->
                if (resource.resource instanceof DocumentManifest) {
                    DocumentManifest dm = (DocumentManifest) resource.resource
                    addSubmissionSet(xml, resource)
                    addSubmissionSetAssociations(xml, resource)
                }
                else if (resource.resource instanceof DocumentReference) {
                    DocumentReference dr = (DocumentReference) resource.resource
                    ResourceWrapper loadedResource = rMgr.resolveReference(resource, new Ref(dr.content[0].attachment.url), new ResolverConfig().internalRequired())
                    if (!(loadedResource.resource instanceof Binary))
                        val.err(new Val()
                        .msg("Binary ${dr.content[0].attachment.url} is not available in Bundle."))
                    Binary b = (Binary) loadedResource.resource
                    b.id = dr.masterIdentifier.value
                    Attachment a = new Attachment()
                    a.contentId = Integer.toString(index) + baseContentId
                    a.contentType = b.contentType
                    a.content = b.content
                    submission.attachments << a
                    index++

                    addExtrinsicObject(xml, resource)
                    documents[resource.assignedId] = a.contentId
                    addRelationshipAssociations(xml, resource)
                }
            }
        }
        submission.registryObjectList = writer.toString()
        submission
    }

    private addSubmissionSetAssociations(MarkupBuilder xml, ResourceWrapper resource) {
        DocumentManifest dm = (DocumentManifest) resource.resource
        if (!dm.content) return
        dm.content.each { Reference ref ->
            ResourceWrapper loadedResource = rMgr.resolveReference(resource, new Ref(ref.reference), new ResolverConfig().internalRequired())

            if (!loadedResource.resource)
                val.err(new Val()
                .msg("DocumentManifest references ${ref.resource} - ${loadedResource.ref} is not included in the bundle"))
            addAssociation(xml, 'urn:oasis:names:tc:ebxml-regrep:AssociationType:HasMember', resource, loadedResource.url, 'SubmissionSetStatus', ['Original'])
        }
    }

    private addRelationshipAssociations(MarkupBuilder xml, ResourceWrapper resource) {
        assert resource.resource instanceof DocumentReference
        DocumentReference dr = (DocumentReference) resource.resource
        if (!dr.relatesTo || dr.relatesTo.size() == 0) return

        // GET relatesTo reference, extract entryUUID, assemble Association
        dr.relatesTo.each { DocumentReference.DocumentReferenceRelatesToComponent comp ->
            String type = comp.getCode().toCode()
            String xdsType = typeMap[type]
            if (!xdsType)
                val.err(new Val()
                .msg("RelatesTo type (${type}) cannot be translated to XDS."))

            Reference ref = comp.target

            ResourceWrapper referencedDocRef = rMgr.resolveReference(resource, new Ref(ref.reference), new ResolverConfig().externalRequired())

            if (!referencedDocRef.resource) {
                val.err(new Val()
                        .msg("Trying to load ${xdsType} Association - ${ref.reference} cannot be resolved"))
                return
            }
            addAssociation(xml, xdsType, resource, referencedDocRef.url, null, null)
        }
    }

    private addAssociation(MarkupBuilder xml, String type, ResourceWrapper source, Ref target, String slotName, List<String> slotValues) {
        val.add(new Val().msg("Association(${type}) source=${source} target=${target}"))
        def assoc = xml.Association(
                sourceObject: "${source.id}",
                targetObject: "${target.id}",
                associationType: "${type}",
                objectType: 'urn:oasis:names:tc:ebxml-regrep:ObjectType:RegistryObject:Association',
                id: "${rMgr.allocateSymbolicId()}"
        ) {
            if (slotName) {
                addSlot(xml, slotName, slotValues)
            }
        }
        return assoc
    }

    private addSubmissionSet(MarkupBuilder builder, ResourceWrapper resource) {
        DocumentManifest dm = (DocumentManifest) resource.resource

        val.add(new Val().msg("SubmissionSet(${resource.assignedId})"))
        builder.RegistryPackage(
                id: resource.assignedId,
                objectType: 'urn:oasis:names:tc:ebxml-regrep:ObjectType:RegistryObject:RegistryPackage',
                status: 'urn:oasis:names:tc:ebxml-regrep:StatusType:Approved') {

            if (dm.created)
                addSlot(builder, 'submissionTime', [translateDateTime(dm.created)])

            if (dm.description)
                addName(builder, dm.description)

            addClassification(builder, 'urn:uuid:a54d6aa5-d40d-43f9-88c5-b4633d873bdd', rMgr.allocateSymbolicId(), resource.assignedId)

            if (dm.type)
                addClassificationFromCodeableConcept(builder, dm.type, 'urn:uuid:aa543740-bdda-424e-8c96-df4873be8500', resource.assignedId)

            if (!dm.masterIdentifier?.value)
                val.err(new Val()
                .msg('DocumentManifest.masterIdentifier not present - declared by IHE to be [1..1]')
                .frameworkDoc('Table 4.5.1.2-1: FHIR DocumentManifest mapping to SubmissionSet'))

            String masterId = (dm.masterIdentifier?.value) ? dm.masterIdentifier.value : null
            if (masterId)
                addExternalIdentifier(builder, 'urn:uuid:96fdda7c-d067-4183-912e-bf5ee74998a8', unURN(masterId), rMgr.allocateSymbolicId(), resource.assignedId, 'XDSSubmissionSet.uniqueId')

            if (dm.source) {
                addExternalIdentifier(builder, 'urn:uuid:554ac39e-e3fe-47fe-b233-965d2a147832', unURN(dm.source), rMgr.allocateSymbolicId(), resource.assignedId, 'XDSSubmissionSet.sourceId')
            }

            if (dm.subject)
                addSubject(builder, resource, new Ref(dm.subject.id), 'urn:uuid:6b5aea1a-874d-4603-a4bc-96a0a7b38446', 'XDSSubmissionSet.patientId')

        }
    }

    private ExtrinsicObjectType addExtrinsicObject(ResourceWrapper resource) {
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
            addExternalIdentifier(eo, CodeTranslator.UNIQUEID, unURN(dr.getMasterIdentifier().getValue()), rMgr.allocateSymbolicId(), resource.getAssignedId(), "XDSDocumentEntry.uniqueId");
        if (dr.hasSubject() && dr.getSubject().hasReference()) {

        }


                    if (dr.subject?.hasReference())
                        addSubject(eo, resource,  new Ref(dr.getSubject()), CodeTranslator.DEPID, "XDSDocumentEntry.patientId");

                    if (dr.author) {
                        dr.author.each { Reference ref ->
                            // TODO finish
                        }
                    }


    }

    private void addName(RegistryObjectType eo, String name) {
        LocalizedStringType lst = new LocalizedStringType();
        lst.setValue(name);
        InternationalStringType ist = new InternationalStringType();
        ist.getLocalizedString().add(lst);
        eo.setName(ist);
    }

    private void addSlot(RegistryObjectType registryObject, String name, String value) {
        SlotType1 slot = new SlotType1();
        slot.setName(name);
        ValueListType valueList = new ValueListType();
        valueList.getValue().add(value);
        registryObject.getSlot().add(slot);
    }

    /**
     * Patient resources shall not be in the bundle so don't look there.  Must have fullUrl reference
     * @param builder
     * @param fullUrl
     * @param containingObjectId
     * @param scheme
     * @param subject
     * @param attName
     * @return
     */

    // TODO sourcePatientInfo is not populated
    private addSourcePatient(MarkupBuilder builder, ResourceWrapper resource, Reference sourcePatient) {
        if (!sourcePatient.reference)
            return
        val.add(new Val().msg("Resolve ${sourcePatient.reference} as SourcePatient"))
        def extra = 'DocumentReference.context.sourcePatientInfo must reference Contained Patient resource with Patient.identifier.use element set to "usual"'
        ResourceWrapper loadedPatient = rMgr.resolveReference(resource, new Ref(sourcePatient.reference), new ResolverConfig().containedRequired())
        if (!loadedPatient.resource) {
            val.err(new Val()
            .msg("Cannot load resource at ${loadedPatient.url}"))
            return
        }

        if (!(loadedPatient.resource instanceof Patient)) {
            val.err(new Val()
            .msg("Patient loaded from ${loadedPatient.url} returned a ${loadedPatient.resource.class.simpleName} instead"))
            return
        }

        Patient patient = (Patient) loadedPatient.resource

        // find identifier that aligns with required Assigning Authority
        List<Identifier> identifiers = patient.getIdentifier()

        String pid = findAcceptablePID(identifiers)
        if (pid)
            addSlot(builder, 'sourcePatientId', [pid])
    }

    private String findAcceptablePID(List<Identifier> identifiers) {
        String pid = null
        identifiers.each { Identifier identifier ->
            if (pid) return
            String value = identifier.value
            String system = identifier.system
            String oid = unURN(system)
            if (assigningAuthorities.check(oid))
                pid = "${value}^^^&${oid}&ISO"
        }
        pid
    }

    // TODO must be absolute reference
    // TODO official identifiers must be changed
    private void addSubject(RegistryObjectType ro, ResourceWrapper resource, Ref referenced, String scheme, String attName) {

        ResourceWrapper loadedResource = rMgr.resolveReference(resource, referenced, new ResolverConfig().externalRequired());
        if (!loadedResource.url) {
            val.err(new Val()
                    .msg("${resource} makes reference to ${referenced}")
                    .msg("All DocumentReference.subject and DocumentManifest.subject values shall be References to FHIR Patient Resources identified by an absolute external reference (URL).")
                    .frameworkDoc("3.65.4.1.2.2 Patient Identity"));
        }
        if (!(loadedResource.resource instanceof Patient))
            val.err(new Val()
                    .msg("${resource} points to a ${loadedResource.resource.class.simpleName} - it must be a Patient")
                    .frameworkDoc('3.65.4.1.2.2 Patient Identity'))

        Patient patient = (Patient) loadedResource.resource

        List<Identifier> identifiers = patient.getIdentifier()
        def pid = findAcceptablePID(identifiers)

        if (pid)
            addExternalIdentifier(builder, scheme, pid, rMgr.allocateSymbolicId(), resource.id, attName)
    }

    private void addExternalIdentifier(RegistryObjectType ro, String scheme, String value, String id, String registryObject, String name) {
        val.add(new Val().msg("ExternalIdentifier " + scheme));
        List<ExternalIdentifierType> eits = ro.getExternalIdentifier();
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
    }

    // TODO - no profile guidance on how to convert coding.system URL to existing OIDs

    private void addClassificationFromCodeableConcept(RegistryObjectType ro, CodeableConcept cc, String scheme, String classifiedObjectId) {
        List<Coding> coding = cc.getCoding();
        addClassificationFromCoding(ro, coding.get(0), scheme, classifiedObjectId);
    }

    private void addClassificationFromCoding(RegistryObjectType ro, Coding coding, String scheme, String classifiedObjectId) {
        Optional<Code> systemCodeOpt = codeTranslator.findCodeByClassificationAndSystem(scheme, coding.getSystem(), coding.getCode());
        if (systemCodeOpt.isPresent()) {
            Code systemCode = systemCodeOpt.get();
            addClassification(ro, scheme, rMgr.allocateSymbolicId(), classifiedObjectId, coding.getCode(), systemCode.getCodingScheme(), coding.getDisplay());
        } else
            val.err(new Val().msg("Cannot find translation for code ${coding.system}|${coding.code} (FHIR) into XDS coding scheme ${scheme} in configured codes.xml file"))
    }

    /**
     * add external classification (see ebRIM for definition)
     * @param builder
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

    private addClassification(MarkupBuilder builder, String node, String id, String classifiedObject) {
        val.add(new Val().msg("Classification ${node}"))
        builder.Classification(
                classifiedObject: "${classifiedObject}",
                classificationNode: "${node}",
                id: "${id}",
                objectType: 'urn:oasis:names:tc:ebxml-regrep:ObjectType:RegistryObject:Classification')
    }

    private addSlot(MarkupBuilder builder, String name, List<String> values) {
        val.add(new Val().msg("Slot ${name}"))
        builder.Slot(name: name) {
            ValueList {
                values.each {
                    Value "${it}"
                }
            }
        }
    }

    private addName(MarkupBuilder builder, String value) {
        val.add(new Val().msg("Name"))
        builder.Name() {
            LocalizedString(value: "${value}")
        }
    }

    private addDocument(MarkupBuilder builder, String drId, String contentId) {
        val.add(new Val().msg("Attach Document ${drId}"))
        builder.Document(id:drId, xmlns: 'urn:ihe:iti:xds-b:2007') {
            Include(href: "cid:${contentId}", xmlns: 'http://www.w3.org/2004/08/xop/include')
        }
    }

    static List<MhdIdentifier> getIdentifiers(IBaseResource resource) {
        assert resource instanceof DocumentManifest || resource instanceof DocumentReference

        List<Identifier> identifiers = (resource instanceof DocumentManifest) ?
                ((DocumentManifest) resource).identifier :
                ((DocumentReference) resource).identifier
        identifiers.collect { Identifier ident -> new MhdIdentifier(ident)}
    }

    private static String unURN(String uuid) {
        if (uuid.startsWith('urn:uuid:')) return uuid.substring(9)
        if (uuid.startsWith('urn:oid:')) return uuid.substring(8)
        return uuid
    }

    private scanBundleForAcceptability(Bundle bundle, ResourceMgr rMgr) {
        if (bundle.meta.profile.size() != 1)
            val.err(new Val()
                    .msg('No profile declaration present in bundle')
                    .frameworkDoc('3.65.4.1.2.1 Bundle Resources'))
        bundleProfile = bundle.meta.profile
        if (!profiles.contains(bundleProfile))
            val.err(new Val()
                    .msg("Do not understand profile declared in bundle - ${bundleProfile}")
                    .frameworkDoc('3.65.4.1.2.1 Bundle Resources'))

        rMgr.resources.each { Ref uri, ResourceWrapper resource ->
            if (!acceptableResourceTypes.contains(resource.resource.class)) {
                val.warn(new Val()
                        .msg("Resource type ${resource.resource.class.simpleName} is not part of MHD and will be ignored"))
                        .frameworkDoc(mhdProfileRef)
            }
        }
    }

    @Override
    public void setVal(Val val) {
        this.val = val;
    }
}
