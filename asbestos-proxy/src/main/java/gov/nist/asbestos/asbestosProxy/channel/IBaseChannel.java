package gov.nist.asbestos.asbestosProxy.channel;


import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.http.operations.HttpGet;
import gov.nist.asbestos.http.operations.HttpPost;

import java.net.URI;

public interface IBaseChannel extends IChannelControl {
    void transformRequest(HttpPost requestIn, HttpPost requestOut);
    void transformRequest(HttpGet requestIn, HttpGet requestOut);
    URI transformRequestUrl(String endpoint, HttpBase requestIn);
    void transformResponse(HttpBase responseIn, HttpBase responseOut);
    void setServerBase(String serverBase);
    void setProxyBase(String proxyBase);
}
