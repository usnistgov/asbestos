**Setup** Get the document responder base address from a prerequisite PDB test, and use the address to verify no updates.

**Submit** a Provide Document Bundle transaction [ITI-65] containing Minimal Metadata to a Document Recipient
actor.

**Message contents**: Bundle resource containing a List of SubmissionSet type and a Binary. There is
no DocumentReference. The List does include the link to the DocumentReference missing which
is not in the Bundle.

**Metadata contents**: List contains the minimum required by Minimal
 Metadata.   List Identifier is given a unique value before the transaction is sent.

**Expected Outcome**: Transaction will fail with status 400 and no contents will be persisted to the server. An OperationOutcome resource will be returned.
