**Setup** Get the document responder base address from a prerequisite PDB test, and use the address to verify no updates.

**Submit** a Provide Document Bundle transaction [ITI-65] containing Minimal Metadata to a Document Recipient
actor.

**Message contents**: Bundle resource containing a DocumentReference, a Binary, and a DocumentManifest. The DocumentReference links to the included Binary.

**Metadata contents**: The DocumentReference.identifier is labeled with use="usual" but the value is a UUID. DocumentReference.masterIdentifier is given unique values before the transaction is sent.

**Expected Outcome**: Transaction will fail with status 400 and no contents will be persisted to the server. An OperationOutcome resource will be returned. 

