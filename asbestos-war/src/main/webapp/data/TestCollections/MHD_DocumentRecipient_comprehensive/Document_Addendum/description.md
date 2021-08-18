**Setup**: Submit a Provide Document Bundle transaction [ITI-65] containing Comprehensive Metadata to a Document Recipient
actor. It contains a single DocumentReference, a DocumentManifest, and a Binary.

**Send Addendum**: Submit a Provide Document Bundle transaction [ITI-65] containing Comprehensive Metadata to a Document Recipient
actor. It contains a single DocumentReference, a DocumentManifest, and a Binary. The DocumentReference is
linked to the original DocumentReference (from setup) as its addendum.

**Read back addendum**: Read the DocumentReference submitted as an addendum and verify that its status
is current. Note: DocumentReference relatesTo is not yet mapped to an XDS DocumentEntry association when an XDS Toolkit based channel is used. 
The response to the GET request will not have the relatesTo element. Manually performing an XDS GetDocuments query with the DocumentEntry EntryUUID 
should be able confirm the proper association.

**Read back original**: Read the DocumentReference submitted as the original and verify that its status
has not changed (status should still be current). 

**Reference(s)**:<br> 
IHE MHD http://hl7.org/fhir/R4/valueset-document-relationship-type.html<br> 
IHE XDS ITI TF 3:4.2.2.2.1 APND.

