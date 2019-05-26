package gov.nist.asbestos.sharedObjects.test;

import java.util.Objects;

class HttpTestObject2 {
    String field3;
    String field4;
    HttpTestObject1 object1;

    public String getField3() {
        return field3;
    }

    public void setField3(String field3) {
        this.field3 = field3;
    }

    public String getField4() {
        return field4;
    }

    public void setField4(String field4) {
        this.field4 = field4;
    }

    public HttpTestObject1 getObject1() {
        return object1;
    }

    public void setObject1(HttpTestObject1 object1) {
        this.object1 = object1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpTestObject2 that = (HttpTestObject2) o;
        return Objects.equals(field3, that.field3) &&
                Objects.equals(field4, that.field4) &&
                Objects.equals(object1, that.object1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field3, field4, object1);
    }
}
