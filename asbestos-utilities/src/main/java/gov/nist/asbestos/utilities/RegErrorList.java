package gov.nist.asbestos.utilities;

import java.util.ArrayList;
import java.util.List;

public class RegErrorList {
    private List<RegError> list = new ArrayList<>();

    public List<RegError> getList() {
        return list;
    }

    public boolean hasErrors() {
        for (RegError regError : list) {
            if (regError.getSeverity() == ErrorType.Error)
                return true;
        }
        return false;
    }

    public boolean hasWarnings() {
        for (RegError regError : list) {
            if (regError.getSeverity() == ErrorType.Warning)
                return true;
        }
        return false;
    }
}
