**Submit** a Provide Document Bundle transaction [ITI-65] containing Minimal Metadata to a Document Recipient
actor.

**Message contents**: Bundle resource containing a DocumentReference, a Binary, and a DocumentManifest. The DocumentReference links to the included Binary.

**Metadata contents**: The DocumentReference.identifier is labeled with use="official" and the value is not a UUID. DocumentReference.masterIdentifier is given unique values before the transaction is sent.

**Expected Outcome**: Transaction will fail with status 400 and no contents will be persisted to the server. MHD Table 4.5.1.1-1 stipulates that if an identifier is specified with use="official" than the value shall
carry the entryUUID (be in UUID format).
