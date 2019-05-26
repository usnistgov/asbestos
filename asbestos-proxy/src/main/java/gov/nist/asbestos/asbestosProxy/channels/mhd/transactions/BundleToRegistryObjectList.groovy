package gov.nist.asbestos.asbestosProxy.channels.mhd.transactions

import gov.nist.asbestos.fproxy.Base.IVal
import gov.nist.asbestos.fproxy.channels.mhd.resolver.Ref
import gov.nist.asbestos.fproxy.channels.mhd.resolver.ResolverConfig
import gov.nist.asbestos.fproxy.channels.mhd.resolver.ResourceCacheMgr
import gov.nist.asbestos.fproxy.channels.mhd.resolver.ResourceMgr
import gov.nist.asbestos.fproxy.channels.mhd.transactionSupport.*
import gov.nist.asbestos.simapi.validation.Val
import groovy.xml.MarkupBuilder
import org.hl7.fhir.r4.model.*
import org.hl7.fhir.instance.model.api.IBaseResource

import javax.xml.bind.DatatypeConverter

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

// @TypeChecked - not because of use of MarkupBuilder
class BundleToRegistryObjectList implements IVal {
//    static private final Logger logger = Logger.getLogger(BundleToRegistryObjectList.class)
    static acceptableResourceTypes = [DocumentManifest, DocumentReference, Binary, ListResource]
    static String comprehensiveMetadataProfile = 'http://ihe.net/fhir/StructureDefinition/IHE_MHD_Provide_Comprehensive_DocumentBundle'
    static String minimalMetadataProfile = 'http://ihe.net/fhir/StructureDefinition/IHE_MHD_Provide_Minimal_DocumentBundle'
    static List<String> profiles = [comprehensiveMetadataProfile, minimalMetadataProfile]
    static String baseContentId = '.de1e4efca5ccc4886c8528535d2afb251e0d5fa31d58a815@ihexds.nist.gov'
    static String mhdProfileRef = 'MHD Profile - Rev 3.1'
    static Map<String, String> typeMap = [
            replaces: 'urn:ihe:iti:2007:AssociationType:RPLC',
            transforms: 'urn:ihe:iti:2007:AssociationType:XFRM',
            signs: 'urn:ihe:iti:2007:AssociationType:signs',
            appends: 'urn:ihe:iti:2007:AssociationType:APND'
    ]

    ResourceCacheMgr resourceCacheMgr
    CodeTranslator codeTranslator
    Configuration config
    AssigningAuthorities assigningAuthorities
    Map<String, String> documents = [:]  // symbolidId -> contentId
    ResourceMgr rMgr
    String bundleProfile
    Val val


    BundleToRegistryObjectList(ResourceCacheMgr resourceCacheMgr, CodeTranslator codeTranslator, AssigningAuthorities assigningAuthorities, Configuration config) {
        this.resourceCacheMgr = resourceCacheMgr
        this.codeTranslator = codeTranslator
        this.assigningAuthorities = assigningAuthorities
        this.config = config
        this.val = val
    }

    Submission build(Bundle bundle) {
        assert val
        rMgr = new ResourceMgr(bundle).addResourceCacheMgr(resourceCacheMgr)
        rMgr.val = val
        scanBundleForAcceptability(bundle, rMgr)

        Submission submission = buildRegistryObjectList()

        def writer = new StringWriter()
        MarkupBuilder builder = new MarkupBuilder(writer)
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
                        .msg("Trying to build ${xdsType} Association - ${ref.reference} cannot be resolved"))
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

    /**
     * add ExtrinsicObject.
     * Official Identifier (entryUUID) must be set and will be used in translation.
     * @param builder
     * @param fullUrl
     * @param dr - DocumentReference to source from
     * @return
     */
    private addExtrinsicObject(MarkupBuilder builder, ResourceWrapper resource) {
        DocumentReference dr = (DocumentReference) resource.resource
        assert dr.content, 'DocumentReference has no content section'
        assert dr.content.size() == 1, 'DocumentReference has multiple content sections'
        assert dr.content[0].attachment, 'DocumentReference has no content/attachment'


        if (!resource.assignedId)
            resource.assignedId = rMgr.allocateSymbolicId()

        builder.ExtrinsicObject(
                id: resource.assignedId,
                objectType:'urn:uuid:7edca82f-054d-47f2-a032-9b2a5b5186c1',
                mimeType: dr.content[0].attachment.contentType)
                {
                    // 20130701231133
                    if (dr.indexed)
                        addSlot(builder, 'creationTime', [translateDateTime(dr.indexed)])

                    if (dr.context?.period?.start)
                        addSlot(builder, 'serviceStartTime', [translateDateTime(dr.context.period.start)])

                    if (dr.context?.period?.end)
                        addSlot(builder, 'serviceStopTime', [translateDateTime(dr.context.period.end)])

                    if (dr.content[0].attachment.language)
                        addSlot(builder, 'languageCode', dr.content.attachment.language)

                    if (dr.content?.attachment?.url)
                        addSlot(builder, 'repositoryUniqueId', dr.content.attachment.url)

                    if (dr.content[0].attachment.hashElement.value) {
                        Base64BinaryType hash64 = dr.content[0].attachment.hashElement
                        //logger.info("value is ${hash64.getValue()}")
                        val.add(new Val().msg("base64Binary is ${hash64.asStringValue()}"))
                        byte[] hash = hash64.getValue() //DatatypeConverter.parseBase64Binary(hash64.asStringValue())
                        val.add(new Val().msg("via groovy = ${hash.encodeHex().toString()}"))

                        val.add(new Val().msg("encoded is ${hash.toString()}"))

                        String hashString = DatatypeConverter.printHexBinary(hash).toLowerCase()
                        val.add(new Val().msg("hexBinary is ${hashString}"))
                        addSlot(builder, 'hash', [hashString])

//                        byte[] hash = HashTranslator.toByteArray(hash64.toString())
//                        byte[] hash = HashTranslator.toByteArrayFromBase64Binary(hash64.asStringValue())
//                        String hashString = hash.encodeHex().toString() as String
//                        addSlot(builder, 'hash', [hashString])
                    }

                    if (dr.context?.sourcePatientInfo)
                        this.addSourcePatient(builder, resource, dr.context.sourcePatientInfo)

                    if (dr.description)
                        addName(builder, dr.description)

                    if (dr.type)
                        addClassificationFromCodeableConcept(builder, dr.type, 'urn:uuid:f0306f51-975f-434e-a61c-c59651d33983', resource.assignedId)

                    if (dr.class_)
                        addClassificationFromCodeableConcept(builder, dr.class_, 'urn:uuid:41a5887f-8865-4c09-adf7-e362475b143a', resource.assignedId)

                    if (dr.securityLabel?.coding)
                        addClassificationFromCoding(builder, dr.securityLabel[0].coding[0], 'urn:uuid:f4f85eac-e6cb-4883-b524-f2705394840f', resource.assignedId)

                    if (dr.content.format.size() > 0) {
                        Coding format = dr.content.format[0]
                        if (format.system)
                            addClassificationFromCoding(builder, dr.content[0].format, 'urn:uuid:a09d5840-386c-46f2-b5ad-9c3699a4309d', resource.assignedId)
                    }

                    if (dr.context?.facilityType)
                        addClassificationFromCodeableConcept(builder, dr.context.facilityType, 'urn:uuid:f33fb8ac-18af-42cc-ae0e-ed0b0bdb91e1', resource.assignedId)

                    if (dr.context?.practiceSetting)
                        addClassificationFromCodeableConcept(builder, dr.context.practiceSetting, 'urn:uuid:cccf5598-8b07-4b77-a05e-ae952c785ead', resource.assignedId)

                    if (dr.context?.event)
                        addClassificationFromCodeableConcept(builder, dr.context.event?.first(), 'urn:uuid:2c6b8cb7-8b2a-4051-b291-b1ae6a575ef4', resource.assignedId)

                    assert dr.masterIdentifier, 'DocumentReference.masterIdentifier not present - declared by IHE to be [1..1]'
                    assert dr.masterIdentifier.value, 'DocumentReference.masterIdentifier has no value - declared by IHE to be [1..1]'
                    String masterId = unURN(dr.masterIdentifier.value)
                    addExternalIdentifier(builder, 'urn:uuid:2e82c1f6-a085-4c72-9da3-8640a32e42ab', masterId, rMgr.allocateSymbolicId(), resource.assignedId, 'XDSDocumentEntry.uniqueId')

                    if (dr.subject?.hasReference())
                        addSubject(builder, resource,  new Ref(dr.subject), 'urn:uuid:58a6f841-87b3-4a3e-92fd-a8ffeff98427', 'XDSDocumentEntry.patientId')

                    if (dr.author) {
                        dr.author.each { Reference ref ->
                            // TODO finish
                        }
                    }

                }
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
    private addSubject(MarkupBuilder builder, ResourceWrapper resource, Ref referenced, String scheme, String attName) {

        ResourceWrapper loadedResource = rMgr.resolveReference(resource, referenced, new ResolverConfig().externalRequired())
        if (!loadedResource.url) {
            val.err(new Val()
                    .msg("${resource} makes reference to ${referenced}")
                    .msg('All DocumentReference.subject and DocumentManifest.subject values shall be References to FHIR Patient Resources identified by an absolute external reference (URL).')
                    .frameworkDoc('3.65.4.1.2.2 Patient Identity'))
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

    private addExternalIdentifier(MarkupBuilder builder, String scheme, String value, String id, String registryObject, String name) {
        val.add(new Val().msg("ExternalIdentifier ${scheme}"))
        builder.ExternalIdentifier(
                identificationScheme: scheme,
                value: "${value}",
                id: "${id}",
                objectType: 'urn:oasis:names:tc:ebxml-regrep:ObjectType:RegistryObject:ExternalIdentifier',
                registryObject: "${registryObject}") {
            Name() {
                LocalizedString(value: "${name}")
            }
        }
    }

    // TODO - no profile guidance on how to convert coding.system URL to existing OIDs

    private addClassificationFromCodeableConcept(MarkupBuilder builder, CodeableConcept cc, String scheme, String classifiedObjectId) {
        Coding coding = cc.coding[0]
        if (coding)
            addClassificationFromCoding(builder, coding, scheme, classifiedObjectId)
    }

    private addClassificationFromCoding(MarkupBuilder builder, Coding coding, String scheme, String classifiedObjectId) {
        Code systemCode = codeTranslator.findCodeByClassificationAndSystem(scheme, coding.system, coding.code)
        if (!systemCode)
        val.err(new Val().msg("Cannot find translation for code ${coding.system}|${coding.code} (FHIR) into XDS coding scheme ${scheme} in configured codes.xml file"))
        addClassification(builder, scheme, rMgr.allocateSymbolicId(), classifiedObjectId, coding.code, systemCode.codingScheme, coding.display)
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
    private addClassification(MarkupBuilder builder, String scheme, String id, String registryObject, String value, String codeScheme, String displayName) {
        builder.Classification(
                classificationScheme: "${scheme}",
                id: "${id}",
                objectType: 'urn:oasis:names:tc:ebxml-regrep:ObjectType:RegistryObject:Classification',
                nodeRepresentation: "${value}",
                classifiedObject: "${registryObject}"
        ) {
            addSlot(builder, 'codingScheme', [codeScheme])
            addName(builder, displayName)
        }
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

}
