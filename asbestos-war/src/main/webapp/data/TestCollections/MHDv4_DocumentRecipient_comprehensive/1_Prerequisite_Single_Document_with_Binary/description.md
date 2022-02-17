**Submit** a Provide Document Bundle transaction [ITI-65] containing Comprehensive Metadata to a Document Recipient
actor.

**Message contents**: Bundle resource containing a SubmissionSet type List, a DocumentReference, and a Binary. The standard
linkage between List and DocumentReference and between DocumentReference and Binary is present.

**Metadata contents**: List and DocumentReference contain the minimum required by Comprehensive
 Metadata.   List.identifier and
DocumentReference.masterIdentifier are given unique values before the transaction is sent.

**Expected Outcome**: Transaction will succeed with status 200 and the contents will be persisted to the server.

**Secondary Purpose**: This test also serves as the basis for providing the document responder base address to 
negative tests that use invalid metadata. The base address will be used to perform search queries to confirm invalid
metadata or documents were not persisted by the server accidentally.