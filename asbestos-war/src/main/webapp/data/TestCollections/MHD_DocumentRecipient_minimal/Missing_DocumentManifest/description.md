**Submit** a Provide Document Bundle transaction [ITI-65] containing Minimal Metadata to a Document Recipient
actor. This is done in two sections. First, a proper PDB is sent with the purpose of discovering the Document Responder location. 
This assumes that the DocumentResponder does not vary between subsequent PDB submissions.
The proper PDB contains all necessary bundle entries in the request. Next, a bad PDB with a missing DocumentManifest is submitted to the DocumentRecipient.
The core part of the test is based on the search query on the bad PDB that was submitted. The Document Responder is searched upon for the DocumentReference master identifier. 
This should not exist.

**Message contents**: Bundle resource containing a DocumentReference and a Binary. There is
no DocumentManifest. The DocumentReference does link to a included Binary.

**Metadata contents**: DocumentReference contains the minimum required by Minimal
 Metadata.   DocumentReference.masterIdentifier is given unique values before the transaction is sent.

**Expected Outcome**: Transaction will fail with status 400 and no contents will be persisted to the server. An OperationOutcome resource will be returned. Next, this is verified by a Recipient search operation that checks for the DocumentReference that it was not persisted by mistake.
