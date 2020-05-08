**Submit** a Provide Document Bundle transaction [ITI-65] containing Minimal Metadata to a Document Recipient
actor.

**Message contents**: Bundle resource containing a DocumentReference and a Binary. There is
no DocumentManifest. The DocumentReference does link to a included Binary.

**Metadata contents**: DocumentReference contains the minimum required by Minimal
 Metadata.   DocumentReference.masterIdentifier is given unique values before the transaction is sent.

**Expected Outcome**: Transaction will fail with status 400 and no contents will be persisted to the server. The DocumentManifest is required by the MHD Profile.
