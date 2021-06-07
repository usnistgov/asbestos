**Setup** Get the document responder base address from a prerequisite PDB test, and use the address to verify no updates.

**Submit** a Provide Document Bundle transaction [ITI-65] containing Minimal Metadata to a Document Recipient
actor.

**Message contents**: Bundle resource containing a DocumentManifest and a Binary. There is
no DocumentReference. The DocumentManifest does not link to a DocumentReference.

**Metadata contents**: DocumentManifest contains the minimum required by Minimal
 Metadata.   DocumentManifest.masterIdentifier is given unique values before the transaction is sent.

**Expected Outcome**: Transaction will fail with status 400 and no contents will be persisted to the server. An OperationOutcome resource will be returned.
