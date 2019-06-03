package gov.nist.asbestos.simapi.simCommon;


import java.util.Objects;

public class TestSession {
    private String value;
    public transient static final TestSession DEFAULT_TEST_SESSION = new TestSession("default");
    public transient static final TestSession GAZELLE_TEST_SESSION = new TestSession("gazelle");
    public transient static final TestSession CAT_TEST_SESSION = new TestSession("cat");

    public TestSession(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    void clean() { value = value.replaceAll("\\.", "_").toLowerCase(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestSession that = (TestSession) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
