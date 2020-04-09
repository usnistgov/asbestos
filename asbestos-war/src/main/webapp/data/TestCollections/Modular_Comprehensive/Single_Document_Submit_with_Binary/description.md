**Submit** a Provide Document Bundle transaction [ITI-65] containing Comprehensive Metadata to a Document Recipient
actor.

**Message contents**: Bundle resource containing a DocumentManifest, a DocumentReference, and a Binary. The standard
linkage between DocumentManifest and DocumentReference and between DocumentReference and Binary is present.

**Metadata contents**: DocumentManifest contains the minimum required by Comprehensive Metadata.  DocumentReference
is missing the MasterIdentifier, a required attribute for Comprehensive Metadata.

**Expected Outcome**: Transaction will fail with status 400 and the contents will not be persisted to the server.
