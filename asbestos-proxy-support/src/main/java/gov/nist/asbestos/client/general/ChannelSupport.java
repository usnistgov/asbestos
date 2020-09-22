package gov.nist.asbestos.client.general;

import gov.nist.asbestos.http.headers.Headers;
import gov.nist.asbestos.http.operations.HttpBase;

import java.util.Arrays;

public class ChannelSupport {

    public static void passHeaders(HttpBase requestIn, HttpBase requestOut) {
        Headers inHeaders = requestIn.getRequestHeaders();
        Headers thruHeaders = inHeaders.select(Arrays.asList("content", "accept"));

        thruHeaders.setVerb(inHeaders.getVerb());
        thruHeaders.setPathInfo(inHeaders.getPathInfo());
        requestOut.setRequestHeaders(thruHeaders);
    }


}
