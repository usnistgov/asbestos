**Submit** a Provide Document Bundle transaction [ITI-65] containing Comprehensive Metadata to a Document Recipient
actor.

**Message contents**: Bundle resource containing a DocumentReference, a Binary, and a DocumentManifest. The DocumentReference links to the included Binary.

**Metadata contents**: A DocumentReference.identifier is labeled with use="official" and the value is a UUID.
This UUID will be given a new value each time the test us run so the test can be repeated without error.  DocumentReference.masterIdentifier is given unique values before the transaction is sent.

**Expected Outcome**: Transaction will succeed with status 200 and the contents will be persisted to the server. 
