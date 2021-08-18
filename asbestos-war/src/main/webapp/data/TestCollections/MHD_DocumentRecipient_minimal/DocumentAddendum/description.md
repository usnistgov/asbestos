**Setup**: Submit a Provide Document Bundle transaction [ITI-65] containing Minimal Metadata to a Document Recipient
actor. It contains a single DocumentReference, a DocumentManifest, and a Binary.

**Send Addendum**: Submit a Provide Document Bundle transaction [ITI-65] containing Minimal Metadata to a Document Recipient
actor. It contains a single DocumentReference, a DocumentManifest, and a Binary. The DocumentReference is
linked to the original DocumentReference (from setup) as its addendum.

**Read back addendum**: Read the DocumentReference submitted as an addendum and verify that its status
is current.

**Read back original**: Read the DocumentReference submitted as the original and verify that its status
has not changed (status should still be current). 

