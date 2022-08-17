**Submit** a Provide Document Bundle transaction [ITI-65] containing Comprehensive Metadata to a Document Recipient
actor.

**Message contents**: Bundle resource containing a DocumentManifest, two DocumentReferences, and two Binaries. 
The standard
linkage between DocumentManifest and DocumentReferences and between DocumentReferences and Binaries is present.

**Metadata contents**: DocumentManifest contains the minimum required by Comprehensive Metadata.  DocumentReferences
contain the minimum required by Comprehensive Metadata.
DocumentManifest.masterIdentifier and
DocumentReference.masterIdentifier are given unique values before the transaction is sent.

**Expected Outcome**: Transaction will succeed with status 200 and the contents will be persisted to the server.
