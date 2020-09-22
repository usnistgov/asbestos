package gov.nist.abestos.client.http;

import java.util.List;
import java.util.Objects;

public class HttpTestObject1 {
    String field1;
    String field2;
    List<String> list;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpTestObject1 that = (HttpTestObject1) o;
        return Objects.equals(field1, that.field1) &&
                Objects.equals(field2, that.field2) &&
                Objects.equals(list, that.list);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field1, field2, list);
    }

    public String getField1() {
        return field1;
    }

    public void setField1(String field1) {
        this.field1 = field1;
    }

    public String getField2() {
        return field2;
    }

    public void setField2(String field2) {
        this.field2 = field2;
    }

    public List<String> getList() {
        return list;
    }

    public void setList(List<String> list) {
        this.list = list;
    }
}
