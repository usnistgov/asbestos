package gov.nist.asbestos.client.channel;

/**
 * Use of setters is for automatic reflection based usage only.
 */
public class IgTestCollection {
    IgNameConstants igName;
    String tcName;
    String docBase;
    IgTestCollection(IgNameConstants igName, String tcName, String docBase)  {
        this.igName = igName;
        this.tcName = tcName;
        this.docBase = docBase;
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
