package gov.nist.asbestos.toolkitApi.toolkitApi

import gov.nist.asbestos.simapi.toolkit.toolkitServicesCommon.DocumentContent
import gov.nist.asbestos.simapi.toolkit.toolkitServicesCommon.RefList
import groovy.transform.TypeChecked

/**
 *
 */
@TypeChecked
 interface DocumentRegRep  extends AbstractActorInterface {

    /**
     * This is the equivalent of a FindDocuments Stored Query.
     * @param patientID - full patient id with Assigning Authority
     * @return list of model references - UUIDs of the DocumentEntries
     * @throws ToolkitServiceException
     */
    RefList findDocumentsForPatientID(String patientID) throws ToolkitServiceException;

    /**
     * Get full metadata (XML) for a DocumentEntry
     * @param uuid - UUID of a DocumentEntry
     * @return full XML metadata for ExtrinsicObject representing DocumentEntry
     * @throws ToolkitServiceException
     */
    String getDocEntry(String uuid) throws ToolkitServiceException;

    /**
     * Get contents of a document
     * @param uniqueId - DocumentEntry.uniqueId
     * @return contents of the document
     * @throws ToolkitServiceException
     */
    DocumentContent getDocument(String uniqueId) throws ToolkitServiceException;
}
