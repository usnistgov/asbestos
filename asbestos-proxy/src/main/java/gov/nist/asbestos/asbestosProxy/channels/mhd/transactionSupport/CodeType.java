package gov.nist.asbestos.asbestosProxy.channels.mhd.transactionSupport;

import java.util.ArrayList;
import java.util.List;

public class CodeType {
    String name;
    String classScheme;
    List<Code> codes = new ArrayList<>();

    public CodeType(String name, String classScheme) {
        this.name = name;
        this.classScheme = classScheme;
    }
}
