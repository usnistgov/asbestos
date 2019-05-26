package gov.nist.asbestos.asbestosProxy.channel;


interface BaseChannel extends ChannelControl {
    void transformRequest(HttpPost requestIn, HttpPost requestOut)
    void transformRequest(HttpGet requestIn, HttpGet requestOut)
    URI transformRequestUrl(String endpoint, HttpBase requestIn)
    void transformResponse(HttpBase responseIn, HttpBase responseOut)
}
