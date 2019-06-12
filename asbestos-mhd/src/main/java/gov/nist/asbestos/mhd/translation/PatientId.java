package gov.nist.asbestos.mhd.translation;

public class PatientId {
    private String patientid = "";
    private String aa = "";
    private String id = "";

    public PatientId setPatientid(String patientid) {
        this.patientid = patientid;
        this.aa = "";
        this.id = "";
        String[] parts = patientid.split("\\^\\^\\^");
        if (parts.length == 2) {
            id = parts[0].trim();
            String theAa = parts[1].trim();
            String[] aaParts = theAa.split("&");
            if (aaParts.length >= 2) {
                aa = aaParts[1].trim();
            }
        }
        return this;
    }

    public String getAa() {
        return aa;
    }

    public String getId() {
        return id;
    }
}
