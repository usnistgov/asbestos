**Setup** Get the document responder base address from a prerequisite PDB test, and use the address to verify no updates.

**Submit** a Provide Document Bundle transaction [ITI-65] containing Minimal Metadata to a Document Recipient
actor.

**Message contents**: Bundle resource containing a SubmissionSet Type List, a DocumentReference, and a Binary. The standard
linkage between List and DocumentReference and between DocumentReference and Binary is present.

**Metadata contents**: List and DocumentReference contain the minimum required by Comprehensive
 Metadata except that DocumentReference.content.attachment.contentType is missing.   List.identifier and
DocumentReference.masterIdentifier are given unique values before the transaction is sent.

**Expected Outcome**: Transaction will fail with status 400 and no contents will be persisted to the server. An OperationOutcome resource will be returned.
