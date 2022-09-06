**Assumptions**
Expects patient(s) to pre-exist on the FHIR server as configured by the external_patient channel.

**Purpose**
Builds a patient cache entry on the external_patient channel which is used for Connectathon purposes. 
All conformance tests now use the external patient reference instead of the patient loaded on default__default channel.

NOTE: This test operation is different from the Default Patient Tests in that, the patient 
is not created on the external server if the patient search returns no results. Connectathon Patient Manager must 
be notified if the patient does not exist on the external patient server since they have the
authority to maintain the Connectathon patient resources.
