package gov.nist.asbestos.proxyWar;

import gov.nist.asbestos.asbestosProxy.servlet.ProxyServlet;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetMetadataTestxxx {

    @Mock
    HttpServletRequest channelSetupRequest;
    @Mock HttpServletResponse channelSetupResponse;

    @Mock HttpServletRequest getSetupRequest;
    @Mock HttpServletResponse getSetupResponse;

    File externalCache;

    @Test
    void getMetadataTest() throws URISyntaxException, IOException {
        String testSession = "default";
        String channelId = "proxtest";
        externalCache = Support.findExternalCache();
        // Begin Mock
        channelSetupRequest = mock(HttpServletRequest.class);
        channelSetupResponse = mock(HttpServletResponse.class);
        Support.sendChannelConfig(channelSetupRequest, "/proxy/prox/", "/proxy/prox/", testSession, channelId);

        final StubServletOutputStream servletOutputStream = new StubServletOutputStream();
        when(channelSetupResponse.getOutputStream()).thenReturn(servletOutputStream);

        // Create channel
        ProxyServlet ps = new ProxyServlet();
        ps.setExternalCache(externalCache);
        ps.doPost(channelSetupRequest, channelSetupResponse);

        getSetupRequest = mock(HttpServletRequest.class);
        getSetupResponse = mock(HttpServletResponse.class);
        Support.mockServlet(getSetupRequest, "http://localhost:8081/proxy/prox/" + testSession + "__" + channelId + "/Channel/metadata", "/proxy/prox/" + testSession + "__" + channelId + "/Channel/metadata");

        final StubServletOutputStream servletOutputStream2 = new StubServletOutputStream();
        when(getSetupResponse.getOutputStream()).thenReturn(servletOutputStream2);

        ps.doGet(getSetupRequest, getSetupResponse);
        String out = servletOutputStream2.toString();
        assertTrue(out.contains("CapabilityStatement"));
    }
}
