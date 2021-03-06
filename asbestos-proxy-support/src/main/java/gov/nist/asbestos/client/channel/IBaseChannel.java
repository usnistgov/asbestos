package gov.nist.asbestos.client.channel;


import gov.nist.asbestos.client.channel.IChannelControl;
import gov.nist.asbestos.client.events.ITask;
import gov.nist.asbestos.http.operations.HttpBase;
import gov.nist.asbestos.http.operations.HttpDelete;
import gov.nist.asbestos.http.operations.HttpGetter;
import gov.nist.asbestos.http.operations.HttpPost;

import java.net.URI;

public interface IBaseChannel extends IChannelControl {
    void transformRequest(HttpPost requestIn, HttpPost requestOut);
    void transformRequest(HttpGetter requestIn, HttpGetter requestOut);
    void transformRequest(HttpDelete requestIn, HttpDelete requestOut);
    URI transformRequestUrl(String endpoint, HttpBase requestIn);
    void transformResponse(HttpBase responseIn, HttpBase responseOut, String proxyHostPort, String requestedType, String search);
    void setServerBase(String serverBase);
    void setProxyBase(String proxyBase);

    void setTask(ITask task);
}
