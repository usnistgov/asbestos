package gov.nist.asbestos.mhd.translation;

import java.util.Objects;

public class Id {

    private String assigningAuthority = null;   // an OID (1.*) plus ISO...
    private String aaOid = null;
    private String id = null;
    private String typeCode = null;

    public Id() {

    }

    public Id(String value) {
        Objects.requireNonNull(value);
        String[] parts = value.split("\\^");
        if (parts.length > 0)
            id = parts[0];
        if (value.contains("ISO")) {
            if (parts.length == 5)
                typeCode = parts[4];  // should be a URN
            if (parts.length >= 4) {
                String aa = parts[3];
                assigningAuthority = aa;
                if (aa.endsWith("ISO")) {
                    String[] parts2 = aa.split("&");
                    if (parts2.length == 3)
                        aaOid = parts2[1];
                }
            }
        }
    }

    public String toString() {
        if (assigningAuthority == null && aaOid != null)
            assigningAuthority = "&" + aaOid + "&ISO";
        String val = "";
        if (id != null)
            val = val + id;
        if (assigningAuthority != null)
            val = val + "^^^" + assigningAuthority;
        if (typeCode != null)
            val = val + "^" + typeCode;
        return val;
     }

    public String getAssigningAuthority() {
        return assigningAuthority;
    }

    public void setAssigningAuthority(String assigningAuthority) {
        this.assigningAuthority = assigningAuthority;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public String getAaOid() {
        return aaOid;
    }

    public void setAaOid(String aaOid) {
        this.aaOid = aaOid;
    }
}
