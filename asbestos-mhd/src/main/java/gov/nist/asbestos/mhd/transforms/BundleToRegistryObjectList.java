package gov.nist.asbestos.mhd.transforms;


import gov.nist.asbestos.client.Base.IVal;
import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.events.ITask;
import gov.nist.asbestos.client.resolver.IdBuilder;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceMgr;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.mhd.SubmittedObject;
import gov.nist.asbestos.mhd.channel.CanonicalUriCodeEnum;
import gov.nist.asbestos.mhd.channel.MhdProfileVersionInterface;
import gov.nist.asbestos.mhd.exceptions.TransformException;
import gov.nist.asbestos.mhd.transactionSupport.AssigningAuthorities;
import gov.nist.asbestos.mhd.transactionSupport.CodeTranslator;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.AssociationType1;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectListType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Configuration;
import org.hl7.fhir.r4.model.DocumentManifest;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Reference;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


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
    private static String baseContentId = ".de1e4efca5ccc4886c8528535d2afb251e0d5fa31d58a815@ihexds.nist.gov";

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
    private Map.Entry<CanonicalUriCodeEnum,String> mhdBundleProfile;
    private MhdTransforms mhdTransforms;


    public BundleToRegistryObjectList(ChannelConfig channelConfig, Map.Entry<CanonicalUriCodeEnum, String> mhdBundleProfile) {
        this.channelConfig = channelConfig;
        this.mhdBundleProfile = mhdBundleProfile;
    }

    public SubmittedObject findSubmittedObject(BaseResource resource) {
        for (SubmittedObject submittedObject : submittedObjects) {
            if (submittedObject.getResource().equals(resource))
                return submittedObject;
        }
        return null;
    }

    public RegistryObjectListType build(MhdProfileVersionInterface mhdImpl, Bundle bundle) {
        Objects.requireNonNull(val);
        Objects.requireNonNull(bundle);
        Objects.requireNonNull(rMgr);
        Objects.requireNonNull(idBuilder);

        scanBundleForAcceptability(mhdImpl, bundle, rMgr);

        return buildRegistryObjectList(mhdImpl, val);
    }


    // TODO handle List/Folder or signal error
    public RegistryObjectListType buildRegistryObjectList(MhdProfileVersionInterface mhdVersionSpecificImpl, Val val) {
        Objects.requireNonNull(mhdVersionSpecificImpl);
        Objects.requireNonNull(val);
        Objects.requireNonNull(rMgr);

        //rMgr.setBundle(bundle);
        //scanBundleForAcceptability(bundle, rMgr);
        RegistryObjectListType rol = new RegistryObjectListType();

        responseBundle = new Bundle();

        RegistryPackageType theSs = null;
        ValE ssVale = null;
        List<ExtrinsicObjectType> eos = new ArrayList<>();

        for (ResourceWrapper wrapper : rMgr.getBundleResourceList()) {
            Bundle.BundleEntryComponent responseComponent1 = responseBundle.addEntry();
            Bundle.BundleEntryResponseComponent responseComponent = responseComponent1.getResponse();
            responseComponent.setStatus("200");
            ValE vale = new ValE(val);
            BaseResource resource = wrapper.getResource();

            RegistryPackageType ss = mhdVersionSpecificImpl.buildSubmissionSet(mhdTransforms, wrapper, val, vale, idBuilder, channelConfig, codeTranslator, assigningAuthorities, mhdBundleProfile.getKey());
            if (ss != null) {
                theSs = ss;
                ssVale = vale;
                submittedObjects.add(new SubmittedObject(wrapper.getAssignedUid(), resource));
                rol.getIdentifiable().add(new JAXBElement<>(new QName("urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0", "RegistryPackage"),
                        RegistryPackageType.class, theSs));
            }

            if (resource instanceof DocumentReference) {
                DocumentReference dr = (DocumentReference) resource;
                ExtrinsicObjectType eo = mhdTransforms.createExtrinsicObject(mhdVersionSpecificImpl, wrapper, vale, idBuilder, documentContents, codeTranslator, assigningAuthorities);
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
                            AssociationType1 hasMember = mhdTransforms.addRelationship(eo, code.toCode(), new Ref(reference.getReference()), vale, sqEndpoint);
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

        if (theSs != null) {
            for (ExtrinsicObjectType eo : eos) {
                AssociationType1 a = createSSDEAssociation(mhdTransforms, theSs, eo, ssVale);
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

    private AssociationType1 createSSDEAssociation(MhdTransforms mhdTransforms, RegistryPackageType ss, ExtrinsicObjectType de, ValE vale) {
        Objects.requireNonNull(mhdTransforms);
        return mhdTransforms.createAssociation("urn:oasis:names:tc:ebxml-regrep:AssociationType:HasMember", ss.getId(), de.getId(), "SubmissionSetStatus", Collections.singletonList("Original"), vale);


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


    private void scanBundleForAcceptability(MhdProfileVersionInterface mhdVersionSpecificImpl, Bundle bundle, ResourceMgr rMgr) {
        Objects.requireNonNull(mhdVersionSpecificImpl);



        evalBundleType(bundle);

        evalBundleResources(mhdVersionSpecificImpl, rMgr);

        evalSubjectReferences(rMgr);

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

    private void evalBundleResources(MhdProfileVersionInterface mhdVersionSpecificImpl, ResourceMgr rMgr) {
        Objects.requireNonNull(mhdVersionSpecificImpl);

        for (ResourceWrapper res : rMgr.getBundleResources()) {
            if (!mhdVersionSpecificImpl.getAcceptableResourceTypes().contains(res.getResource().getClass())) {
                String resourceClassName = "";
                try {
                    resourceClassName = res.getResource().getClass().getSimpleName();
                } catch (Exception ex) {
                    resourceClassName = "(Unknown class)";
                }
                String profileRef = mhdVersionSpecificImpl.getIheReference();
                val.add(new ValE(String.format("Resource type ${resource.resource.class.simpleName} %s is not part of %s and will be ignored", resourceClassName, profileRef))
                        .asWarning()
                        .add(new ValE(profileRef).asDoc()));
            }
        }
    }

    private void evalSubjectReferences(ResourceMgr rMgr) {
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
    }


    private void evalBundleType(Bundle bundle) {
        try {
            if (!bundle.getType().toCode().equals("transaction")) {
                val.add(new ValE("Bundle.type missing").asError()
                        .add(new ValE("http://hl7.org/fhir/http.html#transaction").asDoc()));
            }
        } catch (Throwable t) {
            val.add(new ValE("Bundle.type missing").asError()
                    .add(new ValE("http://hl7.org/fhir/http.html#transaction").asDoc()));
        }
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

    public BundleToRegistryObjectList setMhdTransforms(MhdTransforms mhdTransforms) {
        this.mhdTransforms = mhdTransforms;
        return this;
    }
}
