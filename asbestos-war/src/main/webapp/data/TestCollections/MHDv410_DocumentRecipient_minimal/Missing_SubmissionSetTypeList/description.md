**Setup** Get the document responder base address from a prerequisite PDB test, and use the address to verify no updates.

**Submit** a Provide Document Bundle transaction [ITI-65] containing Minimal Metadata to a Document Recipient
actor. 

**Message contents**: Bundle resource containing a DocumentReference and a Binary. There is
no List of SubmissionSet type. The DocumentReference does link to a included Binary.

**Metadata contents**: DocumentReference contains the minimum required by Minimal
 Metadata.   DocumentReference.masterIdentifier is given unique values before the transaction is sent.

**Expected Outcome**: Transaction will fail with status 400 and no contents will be persisted to the server. An OperationOutcome resource will be returned. Next, this is verified by a Recipient search operation that checks for the DocumentReference that it was not persisted by mistake.
