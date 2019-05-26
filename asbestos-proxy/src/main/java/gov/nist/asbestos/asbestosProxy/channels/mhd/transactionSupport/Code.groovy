package gov.nist.asbestos.asbestosProxy.channels.mhd.transactionSupport

import groovy.xml.MarkupBuilder;

class Code {
    String code
    String codingScheme
    String display
    String system
    boolean deprecated

    Code (MarkupBuilder xml) {
        code = xml.@code
                codingScheme = xml.@codingScheme
                display = xml.@display
                system = xml.@system
                deprecated = xml.@deprecated
    }
}
