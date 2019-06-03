package gov.nist.asbestos.simapiTest

import spock.lang.Specification

class AIT extends Specification {

    def 'test' () {
        when:
        def a = 1

        then:
        a == 1
    }
}
