**Setup**: Submit a Provide Document Bundle transaction [ITI-65] containing Minimal Metadata to a Document Recipient
actor. It contains a single DocumentReference, a DocumentManifest, and a Binary.

**Send Replacement**: Submit a Provide Document Bundle transaction [ITI-65] containing Minimal Metadata to a Document Recipient
actor. It contains a single DocumentReference, a DocumentManifest, and a Binary. The DocumentReference is
linked to the original DocumentReference (from setup) as its replacement.

**Read back replacement**: Read the DocumentReference submitted as a replacement and verify that its status
is current.

**Read back original**: Read the DocumentReference submitted as the original and verify that its status
has changed to superseded.

