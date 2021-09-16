**Submit** a Provide Document Bundle transaction [ITI-65] containing Minimal Metadata to a Document Recipient
actor.

**Message contents**: Bundle resource containing a List, a DocumentReference, and a Binary. The standard
linkage between List and DocumentReference and between DocumentReference and Binary is present.

**Metadata contents**: List and DocumentReference contain the minimum required by Minimal
 Metadata.   List.identifier and
DocumentReference.masterIdentifier are given unique values before the transaction is sent.

**Expected Outcome**: Transaction will succeed with status 200 and the contents will be persisted to the server.
