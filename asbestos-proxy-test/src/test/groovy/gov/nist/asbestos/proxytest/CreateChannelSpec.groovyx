package gov.nist.asbestos.proxytest

import HttpDelete
import HttpPost
import spock.lang.Specification

class CreateChannelSpec extends Specification {
  // TODO parameterize base url of proxy
    def setupSpec() {
        new HttpDelete().run("http://localhost:8081/fproxy_war/prox/default__1")
    }

    def cleanupSpec() {
        new HttpDelete().run("http://localhost:8081/fproxy_war/prox/default__1")
    }

    def 'create channel' () {
        when:
        def rc = new HttpPost().postJson(new URI('http://localhost:8081/fproxy_war/prox'),
                '''
{
  "environment": "default",
  "testSession": "default",
  "channelId": "1",
  "actorType": "balloon",
  "a": "x",
  "b": "y"
}
''').status

        then:
        rc == 201 || rc == 200
    }

    def 'delete channel' () {
        when:
        def rc = new HttpDelete().run('http://localhost:8081/fproxy_war/prox/default__1').status

        then:
        rc == 200
    }

    def 'recreate channel' () {
        when:
        def rc = new HttpPost().postJson(new URI('http://localhost:8081/fproxy_war/prox'),
                '''
{
  "environment": "default",
  "testSession": "default",
  "channelId": "1",
  "actorType": "balloon"
}
''').status

        then:
        rc == 201
    }

    def 're-recreate channel' () {
        when:
        def rc = new HttpPost().postJson(new URI('http://localhost:8081/fproxy_war/prox'),
                '''
{
  "environment": "default",
  "testSession": "default",
  "channelId": "1",
  "actorType": "balloon"}
''').status

        then:
        rc == 200
    }



    def 'post to proxy - no actor' () {
        when:
        def rc = new HttpPost().postJson(new URI('http://localhost:8081/fproxy_war/prox/default__1'), '{"message":"this is a message"}').status

        then:
        rc == 403  //
    }

    def 'post to actor balloon - no resource' () {
        when:
        def rc = new HttpPost().postJson(new URI('http://localhost:8081/fproxy_war/prox/default__1/balloon'), '{"message":"this is a message"}').status

        then:
        rc == 403  //
    }

}
