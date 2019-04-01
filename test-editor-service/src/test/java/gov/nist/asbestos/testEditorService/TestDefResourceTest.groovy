package gov.nist.asbestos.testEditorService

import spock.lang.Specification

import static gov.nist.asbestos.testEditorService.Config

class TestDefResourceTest extends Specification {

    def 'no qualifier' () {
        setup:
        def qualifier = ''
        def id = '01'
        init()

        when:
        File file = TestDefResource.getFile(qualifier, id)

        then:
        file == '/home/bill/asbestos/01.json'
    }
}
