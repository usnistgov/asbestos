package gov.nist.asbestos.http.headers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Header {
    private String name;
    private List<HeaderValue> values = new ArrayList<>();

    public Header(String header) {
        Objects.requireNonNull(header);
        String[] parts = header.split(":", 2);
        name = parts[0];
        if (parts.length == 2) {
            String[] valParts = parts[1].split(",");
            Arrays.asList(valParts).forEach(val -> {
                values.add(new HeaderValue(val));
            });
        }
    }

    public Header(String name, String value) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);
        this.name = name;
        String[] valParts = value.split(",");
        Arrays.asList(valParts).forEach(val -> {
            values.add(new HeaderValue(val));
        });
    }

    public Header(String name, List<String> theValues) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(values);
        this.name = name;
        theValues.forEach(val -> {
            values.add(new HeaderValue(val));
        });
    }

    public String getValue() {
        if (values.isEmpty())
            return null;
        return values.get(0).getValue();
    }

    public List<String> getAllValues() {
        return values.stream()
                .map(HeaderValue::getValue)
                .collect(Collectors.toList());
    }

    public String getAllValuesAsString() {
        return String.join(",", getAllValues());
    }

    public List<String> getAllValuesAndParms() {
        return values.stream()
                .map(HeaderValue::getValueAndParms)
                .collect(Collectors.toList());
    }

    public String getAllValuesAndParmsAsString() {
        return String.join(",", getAllValuesAndParms());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<HeaderValue> getValues() {
        return values;
    }

    public void setValues(List<HeaderValue> values) {
        this.values = values;
    }

    public void setValue(String val) {
        HeaderValue headerValue = new HeaderValue(val);
        List<HeaderValue> headerValues = new ArrayList<>();
        headerValues.add(headerValue);
        setValues(headerValues);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Header header = (Header) o;
        return Objects.equals(name, header.name) &&
                Objects.equals(values, header.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, values);
    }

    @Override
    public String toString() {
        return name + ":" + values.stream()
                .map(HeaderValue::toString)
                .collect(Collectors.joining(","));
    }
}
