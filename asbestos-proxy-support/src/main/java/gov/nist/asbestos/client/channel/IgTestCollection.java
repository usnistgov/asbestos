package gov.nist.asbestos.client.channel;

import ca.uhn.fhir.validation.FhirValidator;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Use of setters is for automatic reflection based usage only.
 */
public class IgTestCollection {
    IgNameConstants igName;
    String tcName;
    String docBase;
    private transient FhirValidator fhirValidator;

    IgTestCollection(IgNameConstants igName, String tcName, String docBase, FhirValidator fhirValidator)  {
        this.igName = igName;
        this.tcName = tcName;
        this.docBase = docBase;
        this.fhirValidator = fhirValidator;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IgTestCollection that = (IgTestCollection) o;

        if (!igName.equals(that.igName)) return false;
        if (!tcName.equals(that.tcName)) return false;
        return docBase.equals(that.docBase);
    }

    @Override
    public int hashCode() {
        int result = igName.hashCode();
        result = 31 * result + tcName.hashCode();
        result = 31 * result + docBase.hashCode();
        return result;
    }

    public IgNameConstants getIgName() {
        return igName;
    }

    public String getTcName() {
        return tcName;
    }

    public String getDocBase() {
        return docBase;
    }

    @JsonIgnore
    public FhirValidator getFhirValidator() {
        return fhirValidator;
    }


    public void setIgName(IgNameConstants igName) {
        this.igName = igName;
    }

    public void setTcName(String tcName) {
        this.tcName = tcName;
    }

    public void setDocBase(String docBase) {
        this.docBase = docBase;
    }


}
