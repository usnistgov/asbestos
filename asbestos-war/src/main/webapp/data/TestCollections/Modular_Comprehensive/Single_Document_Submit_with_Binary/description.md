**Submit** a Provide Document Bundle transaction [ITI-65] containing Comprehensive Metadata to a Document Recipient
actor.

**Message contents**: Bundle resource containing a DocumentManifest, a DocumentReference, and a Binary. The standard
linkage between DocumentManifest and DocumentReference and between DocumentReference and Binary is present.

**Metadata contents**: DocumentManifest contains the minimum required by Comprehensive Metadata.  DocumentReference
is missing the MasterIdentifier, a required attribute for Comprehensive Metadata. DocumentManifest.masterIdentifier and
DocumentReference.masterIdentifier are are given unique values before the transaction is sent.

**Expected Outcome**: Transaction will succeed with status 200 and the contents will be persisted to the server.
