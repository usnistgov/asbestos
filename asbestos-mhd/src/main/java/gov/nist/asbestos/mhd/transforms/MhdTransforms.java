package gov.nist.asbestos.mhd.transforms;

import com.google.common.base.Strings;
import gov.nist.asbestos.asbestorCodesJaxb.Code;
import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.FhirClientBuilder;
import gov.nist.asbestos.client.events.ITask;
import gov.nist.asbestos.client.resolver.IdBuilder;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResolverConfig;
import gov.nist.asbestos.client.resolver.ResourceCacheMgr;
import gov.nist.asbestos.client.resolver.ResourceMgr;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.mhd.channel.MhdProfileVersionInterface;
import gov.nist.asbestos.mhd.transactionSupport.AhqrSender;
import gov.nist.asbestos.mhd.transactionSupport.AssigningAuthorities;
import gov.nist.asbestos.mhd.transactionSupport.CodeTranslator;
import gov.nist.asbestos.mhd.translation.ContainedIdAllocator;
import gov.nist.asbestos.mhd.translation.attribute.Author;
import gov.nist.asbestos.mhd.translation.attribute.AuthorRole;
import gov.nist.asbestos.mhd.translation.attribute.DateTransform;
import gov.nist.asbestos.mhd.translation.search.FhirSq;
import gov.nist.asbestos.mhd.util.Utils;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.*;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.net.URI;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MhdTransforms {
    public static String MhdListResourceName = "List";
    private ResourceMgr rMgr;
    private Val val;
    private ITask task = null;
    private static final String DRTable = "MHD: Table 4.5.1.1-1";
    public static final String URN_IETF_RFC_3986 = "urn:ietf:rfc:3986";
    public static final String URN_UUID__BDD_SUBMISSION_SET = "urn:uuid:a54d6aa5-d40d-43f9-88c5-b4633d873bdd";
    private static Map<String, String> buildTypeMap() {
        return Collections.unmodifiableMap(Stream.of(
                new AbstractMap.SimpleEntry<>("replaces", "urn:ihe:iti:2007:AssociationType:RPLC"),
                new AbstractMap.SimpleEntry<>("transforms", "urn:ihe:iti:2007:AssociationType:XFRM"),
                new AbstractMap.SimpleEntry<>("signs", "urn:ihe:iti:2007:AssociationType:signs"),
                new AbstractMap.SimpleEntry<>("appends", "urn:ihe:iti:2007:AssociationType:APND"))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue)));
    }


    public MhdTransforms(ResourceMgr rMgr, Val val, ITask task) {
        this.rMgr = rMgr;
        this.val = val;
        this.task = task;
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

    static String translateDateTime(Date date) {
        return DateTransform.fhirToDtm(date);
    }

    // TODO must be absolute reference
    // TODO official identifiers must be changed
    void addSubject(RegistryObjectType ro, ResourceWrapper resource, Ref referenced, String scheme, String attName, ValE val, AssigningAuthorities assigningAuthorities) {

        Optional<ResourceWrapper> loadedResource = rMgr.resolveReference(resource, referenced, new ResolverConfig().externalRequired());
        boolean isLoaded = loadedResource.isPresent() && loadedResource.get().isLoaded();
        if (!isLoaded) {
            val.add(new ValE(resource + " makes reference to " + referenced + " which cannot be loaded").asError()
                    .add(new ValE("   All DocumentReference.subject and DocumentManifest.subject (or List) values shall be References to FHIR Patient Resources identified by an absolute external reference (URL).").asDoc())
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
        String pid = findAcceptablePID(identifiers, assigningAuthorities);

        if (pid != null)
            addExternalIdentifier(ro, scheme, pid, rMgr.allocateSymbolicId(), resource.getAssignedId(), attName, null);
    }

    public AssociationType1 addRelationship(RegistryObjectType source, String relationshipType, Ref target, ValE val, URI sqEndpoint) {
        Objects.requireNonNull(task);
        Objects.requireNonNull(sqEndpoint);
        if (relationshipType == null ||
                !(relationshipType.equals("replaces")
                        || relationshipType.equals("appends")
                        || relationshipType.equals("transforms"))) {
            val.add(new ValE("Relationship " + relationshipType + " is not supported ").asError());
            return null;
        }

        val.add(new ValE("Once deprecated, a DocumentEntry shall not be referenced by future associations").addIheRequirement("ITI TF 3, 4.2.2"));

        AhqrSender sender = FhirSq.documentEntryByUidQuery(
                target.getId(),
                "urn:oasis:names:tc:ebxml-regrep:StatusType:Approved",
                sqEndpoint,
                task.newTask());
        List<IdentifiableType> contents = sender.getContents();
        if (contents.size() != 1) {
            val.add(new ValE("Error retrieving DocumentEntry with Approved status " + target.getId() + " from " + sqEndpoint + " - expected 1 entry but got " + contents.size()).asError());
            return null;
        }
        ExtrinsicObjectType eo = (ExtrinsicObjectType) contents.get(0);
        String targetId = eo.getId();

        String iheAssociationType = buildTypeMap().get(relationshipType);
        return createAssociation(iheAssociationType,
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

    public void addClassificationFromCodeableConcept(RegistryObjectType ro, CodeableConcept cc, String scheme, String classifiedObjectId, ValE val, CodeTranslator codeTranslator) {
        List<Coding> coding = cc.getCoding();
        addClassificationFromCoding(ro, coding.get(0), scheme, classifiedObjectId, val, codeTranslator);
    }

    private void addClassificationFromCoding(RegistryObjectType ro, Coding coding, String scheme, String classifiedObjectId, ValE val, CodeTranslator codeTranslator) {
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

    void addClassification(RegistryObjectType ro, String node, String id, String classifiedObject) {
        val.add(new ValE("Classification " + node));
        ClassificationType ct = new ClassificationType();
        ct.setClassifiedObject(classifiedObject);
        ct.setClassificationNode(node);
        ct.setId(id);
        ct.setObjectType("urn:oasis:names:tc:ebxml-regrep:ObjectType:RegistryObject:Classification");
        ro.getClassification().add(ct);
    }


    String findAcceptablePID(List<Identifier> identifiers, AssigningAuthorities assigningAuthorities) {
        Objects.requireNonNull(assigningAuthorities);
        List<String> pids = identifiers.stream()
                .filter(identifier -> assigningAuthorities.check(Utils.stripUrnPrefixes(identifier.getSystem())))
                .map(identifier -> identifier.getValue() + "^^^&" + Utils.stripUrnPrefixes(identifier.getSystem()) + "&ISO")
                .collect(Collectors.toList());

        return (pids.isEmpty()) ? null : pids.get(0);
    }

    AssociationType1 createAssociation(String type, ResourceWrapper source, Ref target, String slotName, List<String> slotValues) {
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


    public ExtrinsicObjectType createExtrinsicObject(MhdProfileVersionInterface mhdImpl, ResourceWrapper resource, ValE vale, IdBuilder idBuilder, Map<String, byte[]> documentContents, CodeTranslator codeTranslator, AssigningAuthorities assigningAuthorities) {
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

        resource.setAssignedId(mhdImpl.getExtrinsicId(vale, rMgr, dr.getIdentifier()));
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
                addSourcePatient(eo, resource, context.getSourcePatientInfo(), vale, assigningAuthorities);
            }
            if (context.hasFacilityType()) {
                vale.addTr(new ValE("facilityType"));
                addClassificationFromCodeableConcept(eo, context.getFacilityType(), CodeTranslator.HCFTCODE, resource.getAssignedId(), vale, codeTranslator);
            }
            if (context.hasPracticeSetting()) {
                vale.addTr(new ValE("practiceSetting"));
                addClassificationFromCodeableConcept(eo, context.getPracticeSetting(), CodeTranslator.PRACCODE, resource.getAssignedId(), vale, codeTranslator);
            }
            if (context.hasEvent()) {
                vale.addTr(new ValE("eventCode"));
                addClassificationFromCodeableConcept(eo, context.getEventFirstRep(), CodeTranslator.EVENTCODE, resource.getAssignedId(), vale, codeTranslator);
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
            addClassificationFromCodeableConcept(eo, dr.getType(), CodeTranslator.TYPECODE, resource.getAssignedId(), tr, codeTranslator);
        }
        if (dr.hasCategory()) {
            tr = vale.addTr(new ValE("classCode"));
            addClassificationFromCodeableConcept(eo, dr.getCategoryFirstRep(), CodeTranslator.CLASSCODE, resource.getAssignedId(), tr, codeTranslator);
        }
        if (dr.hasSecurityLabel()) {
            tr = vale.addTr(new ValE("confCode"));
            addClassificationFromCoding(eo, dr.getSecurityLabel().get(0).getCoding().get(0), CodeTranslator.CONFCODE, resource.getAssignedId(), tr, codeTranslator);
        }
        if(content.hasFormat()) {
            tr = vale.addTr(new ValE("formatCode"));
            addClassificationFromCoding(eo, dr.getContent().get(0).getFormat(), CodeTranslator.FORMATCODE, resource.getAssignedId(), tr, codeTranslator);
        }

        tr = vale.add(new ValE("DocumentReference.masterIdentifier is [1..1]").addIheRequirement(DRTable));
        if (!dr.hasMasterIdentifier())
            tr.add(new ValE("masterIdentifier not present").asError());
        else {
            tr.add(new ValE("masterIdentifier").asTranslation());
            addExternalIdentifier(eo, CodeTranslator.DE_UNIQUEID, Utils.stripUrnPrefixes(dr.getMasterIdentifier().getValue()), rMgr.allocateSymbolicId(), resource.getAssignedId(), "XDSDocumentEntry.uniqueId", idBuilder);
            resource.setAssignedUid(Utils.stripUrnPrefixes(dr.getMasterIdentifier().getValue()));
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
            addSubject(eo, resource, new Ref(sub), CodeTranslator.DE_PID, "XDSDocumentEntry.patientId", tr, assigningAuthorities);
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

    void addCreationTime(ExtrinsicObjectType eo, Date creation) {
        String creationTime = translateDateTime(creation);
        addSlot(eo, "creationTime", creationTime);
    }

    void addDescription(RegistryObjectType eo, String description) {
        LocalizedStringType lst = new LocalizedStringType();
        lst.setValue(description);
        InternationalStringType ist = new InternationalStringType();
        ist.getLocalizedString().add(lst);
        eo.setDescription(ist);
    }

    ClassificationType classificationFromAuthor(IBaseResource resource1, ResourceWrapper containing) {
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


    /**
     * Patient resources shall not be in the bundle so don't look there.  Must have fullUrl reference
     */

//    private void addSourcePatientInfo(ExtrinsicObjectType eo, ResourceWrapper resource, Reference sourcePatient, ValE val) {
//    }
    // TODO sourcePatientInfo is not populated
    void addSourcePatient(ExtrinsicObjectType eo, ResourceWrapper resource, Reference sourcePatient, ValE val, AssigningAuthorities assigningAuthorities) {
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

        String pid = findAcceptablePID(identifiers, assigningAuthorities);
        if (pid != null)
            addSlot(eo, "sourcePatientId", Collections.singletonList(pid));
    }

    public ResourceMgr getrMgr() {
        return rMgr;
    }

    public void linkDummyPatient(ResourceWrapper wrapper, ValE vale, ChannelConfig channelConfig, AssigningAuthorities assigningAuthorities, RegistryPackageType ss) {
        // Patient is optional in minimal metadata - add reference to No_Patient to make XDS Toolkit happy
        // Adds resource cache to configuration
        FhirClient fhirClient =
                channelConfig == null
                        ? FhirClientBuilder.get(null)
                        : FhirClientBuilder.get(channelConfig.asChannelId());

        Optional<ResourceWrapper> patient = fhirClient.readCachedResource(new Ref("Patient/No_Patient"));
        if (patient.isPresent()) {
            ResourceWrapper thePatient = patient.get();
            Bundle patientBundle;
            if (thePatient.getResource() instanceof Bundle) {
                patientBundle = (Bundle) thePatient.getResource();
                Ref patRef = new Ref(patientBundle.getEntry().get(0).getFullUrl());  // this must be turned into fullURL (not relative)
                addSubject(ss, wrapper, patRef , CodeTranslator.SS_PID, "XDSSubmissionSet.patientId", vale, assigningAuthorities);
            } else {
                val.add(new ValE("Internal error - Lookup of Patient/No_Patient returned " + thePatient.getResource().getClass().getSimpleName() + " instead of Bundle").asError());
            }
        } else {
            val.add(new ValE("Internal error - cannot locate Patient/No_Patient").asError());
        }
    }

    public static void withNewBase(URI base, Reference reference) {
        // base could be http or https channel base
        String newBase = //ServiceProperties.getInstance().getPropertyOrThrow(ServicePropertiesEnum.FHIR_TOOLKIT_BASE)
                 base.toString().substring(0, base.toString().indexOf("/proxy")) + "/proxy/default__default";
        Ref ref = new Ref(reference.getReference());
        ref = ref.rebase(newBase);
        reference.setReference(ref.toString());
    }

    public static OperationOutcome operationOutcomefromVal(Val val) {
        OperationOutcome oo = new OperationOutcome();
        for (ValE err : val.getErrors())
            addErrorToOperationOutcome(oo, err.getMsg());
        return oo;
    }

    public static OperationOutcome addErrorToOperationOutcome(OperationOutcome oo, String msg) {
        OperationOutcome.OperationOutcomeIssueComponent issue = oo.addIssue();
        issue.setCode(OperationOutcome.IssueType.UNKNOWN);
        issue.setSeverity(OperationOutcome.IssueSeverity.ERROR);
        issue.setDiagnostics(msg);
        return oo;
    }



    // assumes sender contains zero or one SubmissionSets
    public static BaseResource ssToDocumentManifest(CodeTranslator codeTranslator, File externalCache, AhqrSender sender, ChannelConfig channelConfig) {
        Val val = new Val();

        ResourceCacheMgr resourceCacheMgr = new ResourceCacheMgr(externalCache);
        FhirClient fhirClient = new FhirClient()
                .setResourceCacheMgr(resourceCacheMgr);
        SubmissionSetToDocumentManifest trans = new SubmissionSetToDocumentManifest();
        trans
                .setContainedIdAllocator(new ContainedIdAllocator())
                .setResourceCacheMgr(resourceCacheMgr)
                .setCodeTranslator(codeTranslator)
                .setFhirClient(fhirClient)
                .setVal(val);

        RegistryPackageType ss = null;
        List<AssociationType1> assocs = new ArrayList<>();
        List<ExtrinsicObjectType> eos = new ArrayList<>();
        for (IdentifiableType identifiableType : sender.getContents()) {
            if (identifiableType instanceof RegistryPackageType) {
                RegistryPackageType rpt = (RegistryPackageType) identifiableType;
                for (ClassificationType classificationType : rpt.getClassification()) {
                    if (URN_UUID__BDD_SUBMISSION_SET.equals(classificationType.getClassificationNode())) {
                        ss = rpt;
                    }
                }
            } else if (identifiableType instanceof AssociationType1) {
                assocs.add((AssociationType1) identifiableType);
            } else if (identifiableType instanceof ExtrinsicObjectType) {
                eos.add((ExtrinsicObjectType) identifiableType);
            }
        }
        DocumentManifest dm = null;
        if (ss != null)
            dm = trans.getDocumentManifest(ss, assocs, channelConfig);

        if (dm != null && dm.hasSubject())
            MhdTransforms.withNewBase(channelConfig.getProxyURI(), dm.getSubject());

//        if (ss == null)
//            val.add(new ValE("No SubmissionSet in query response.").asError());

        if (val.hasErrors())
            return MhdTransforms.operationOutcomefromVal(val);

        return dm;
    }

    public static BaseResource ssToListResource(CodeTranslator codeTranslator, File externalCache, AhqrSender sender, ChannelConfig channelConfig) {
        Val val = new Val();

        ResourceCacheMgr resourceCacheMgr = new ResourceCacheMgr(externalCache);
        FhirClient fhirClient = new FhirClient()
                .setResourceCacheMgr(resourceCacheMgr);
        SubmissionSetToListResource trans = new SubmissionSetToListResource(new ContainedIdAllocator(), resourceCacheMgr, codeTranslator, fhirClient, val);

        RegistryPackageType ss = null;
        List<AssociationType1> assocs = new ArrayList<>();
        List<ExtrinsicObjectType> eos = new ArrayList<>();
        for (IdentifiableType identifiableType : sender.getContents()) {
            if (identifiableType instanceof RegistryPackageType) {
                RegistryPackageType rpt = (RegistryPackageType) identifiableType;
                for (ClassificationType classificationType : rpt.getClassification()) {
                    if (URN_UUID__BDD_SUBMISSION_SET.equals(classificationType.getClassificationNode())) {
                        ss = rpt;
                    }
                }
            } else if (identifiableType instanceof AssociationType1) {
                assocs.add((AssociationType1) identifiableType);
            } else if (identifiableType instanceof ExtrinsicObjectType) {
                eos.add((ExtrinsicObjectType) identifiableType);
            }
        }
        ListResource listResource = null;
        if (ss != null)
            listResource = trans.getListResource(ss, assocs, channelConfig);

        if (listResource != null && listResource.hasSubject())
            MhdTransforms.withNewBase(channelConfig.getProxyURI(), listResource.getSubject());

//        if (ss == null)
//            val.add(new ValE("No SubmissionSet in query response.").asError());

        if (val.hasErrors())
            return MhdTransforms.operationOutcomefromVal(val);

        return listResource;

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



}
