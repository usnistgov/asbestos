**Submit** a Provide Document Bundle transaction [ITI-65] containing Minimal Metadata to a Document Recipient
actor.

**Message contents**: Bundle resource containing a DocumentManifest, a DocumentReference, but no Binary. 
A Binary was submitted by the setup page of this tool and its server url is referenced in this submission at
DocumentReference.content.attachment.url.
The standard
linkage between DocumentManifest and DocumentReference is present.

**Metadata contents**: DocumentManifest and DocumentReference contain the minimum required by Minimal
 Metadata.   DocumentManifest.masterIdentifier and
DocumentReference.masterIdentifier are given unique values before the transaction is sent.

**Expected Outcome**: Transaction will succeed with status 200 and the contents will be persisted to the server.
