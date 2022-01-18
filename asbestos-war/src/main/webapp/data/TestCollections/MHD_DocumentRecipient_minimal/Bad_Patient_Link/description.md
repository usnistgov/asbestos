**Setup** Get the document responder base address from a prerequisite PDB test, and use the address to verify no updates.

**Submit** a Provide Document Bundle transaction [ITI-65] containing Comprehensive Metadata to a Document Recipient
actor.

**Message contents**: Bundle resource containing a DocumentManifest, a DocumentReference, and a Binary. The standard
linkage between DocumentManifest and DocumentReference and between DocumentReference and Binary is present.

**Metadata contents**: DocumentManifest and DocumentReference contain the minimum required by Comprehensive
 Metadata except that DocumentReference.subject.reference is a bad link.   DocumentManifest.masterIdentifier and
DocumentReference.masterIdentifier are given unique values before the transaction is sent.

**Expected Outcome**: Transaction will fail with status 400 and the contents will not be persisted to the server.
