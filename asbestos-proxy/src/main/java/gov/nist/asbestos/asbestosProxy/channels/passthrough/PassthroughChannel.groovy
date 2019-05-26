package gov.nist.asbestos.asbestosProxy.channels.passthrough

import gov.nist.asbestos.asbestosProxy.channel.BaseChannel
import gov.nist.asbestos.asbestosProxy.channel.ChannelConfig
import gov.nist.asbestos.asbestosProxy.events.EventStore
import gov.nist.asbestos.http.headers.Headers
import gov.nist.asbestos.http.operations.HttpBase
import gov.nist.asbestos.http.operations.HttpGet
import gov.nist.asbestos.http.operations.HttpPost


class PassthroughChannel implements BaseChannel {
    ChannelConfig channelConfig = null

    @Override
    void setup(ChannelConfig simConfig) {
        this.channelConfig = simConfig
    }

    @Override
    void teardown() {

    }

    @Override
    void validateConfig(ChannelConfig simConfig) {
    }

    @Override
    void handle(EventStore event) {

    }

    @Override
    void transformRequest(HttpPost requestIn, HttpPost requestOut) {
        Headers thruHeaders = HeaderBuilder.parseHeaders(requestIn.requestHeaders.getMultiple(['content', 'accept']))

        requestOut.requestHeaders = thruHeaders
        requestOut.request = requestIn.request
        requestOut.requestHeaders.verb = requestIn.requestHeaders.verb
        requestOut.requestHeaders.pathInfo = requestIn.requestHeaders.pathInfo
//        requestOut.parameterMap = requestIn.parameterMap
    }

    @Override
    void transformRequest(HttpGet requestIn, HttpGet requestOut) {
        Headers thruHeaders = HeaderBuilder.parseHeaders(requestIn.requestHeaders.getMultiple(['content', 'accept']))

        requestOut.requestHeaders = thruHeaders
        requestOut.requestHeaders.verb = requestIn.requestHeaders.verb
        requestOut.requestHeaders.pathInfo = requestIn.requestHeaders.pathInfo
//        requestOut.parameterMap = requestIn.parameterMap
    }

    @Override
    URI transformRequestUrl(String endpoint, HttpBase requestIn) {
        assert channelConfig
        channelConfig.translateEndpointToFhirBase(requestIn.requestHeaders.pathInfo)
    }

    @Override
    void transformResponse(HttpBase responseIn, HttpBase responseOut) {
        responseOut.responseHeaders = responseIn.responseHeaders
        responseOut.response = responseIn.response
    }
}
