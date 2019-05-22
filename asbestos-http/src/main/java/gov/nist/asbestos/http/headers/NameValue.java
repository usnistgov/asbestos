package gov.nist.asbestos.http.headers;

class NameValue {
    public String name;
    public String value;

    public String toString() {
        return String.join(": ", name, value);
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
