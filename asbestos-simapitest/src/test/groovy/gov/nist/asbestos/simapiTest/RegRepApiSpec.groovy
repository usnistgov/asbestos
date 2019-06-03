package gov.nist.asbestos.simapiTest

import gov.nist.asbestos.simapi.toolkit.toolkitApi.RegRepApi
import spock.lang.Specification

class RegRepApiSpec extends Specification {

    def 'build reg sim' () {
        setup:
        RegRepApi tkApi = new RegRepApi()

        when:
        tkApi
                .deleteRegrep()
                .createRegRepIfNeeded()

        then:
        // more than 5 chars in date
        tkApi.regrepSimConfig.asString('Creation Time').size() > 5
    }
}
