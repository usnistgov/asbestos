package gov.nist.asbestos.http.headers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class HeaderValue {
    private String value = null;
    private List<String> parms = new ArrayList<>();

    public HeaderValue(String headerValue) {
        String[] parts = headerValue.split(";");
        Arrays.asList(parts).forEach(part -> {
            if (value == null)
                value = part;
            else {
                parms.add(part);
            }
        });
    }

    @Override
    public String toString() {
        String val = value;

        if (!parms.isEmpty()) {
            val = val + ";" + String.join(";", parms);
        }

        return val;
    }

    public String getValue() {
        return value;
    }

    public String getValueAndParms() {
        if (parms.size() == 0)
            return value;
        return value + ";" + String.join(";", parms);
    }

    public List<String> getParms() {
        return parms;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HeaderValue that = (HeaderValue) o;
        return Objects.equals(value, that.value) &&
                Objects.equals(parms, that.parms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, parms);
    }
}
