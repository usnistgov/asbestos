package gov.nist.asbestos.asbestosProxy;

import gov.nist.asbestos.asbestosProxy.log.SimStore;
import gov.nist.asbestos.asbestosProxy.wrapper.ProxyServlet;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.sharedObjects.ChannelConfigFactory;
import gov.nist.asbestos.simapi.simCommon.SimId;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CreateChannelTestxxx {

    @Mock HttpServletRequest channelSetupRequest;
    @Mock HttpServletResponse channelSetupResponse;

    File externalCache;

    @Test
    void createChannelTest() throws URISyntaxException, IOException {
        externalCache = Support.findExternalCache();
        // Begin Mock
        channelSetupRequest = mock(HttpServletRequest.class);
        channelSetupResponse = mock(HttpServletResponse.class);
        ChannelConfig channelConfig = Support.sendChannelConfig(channelSetupRequest, "/proxy/prox/", "/proxy/prox/", "default", "proxtest");

        final StubServletOutputStream servletOutputStream = new StubServletOutputStream();
        when(channelSetupResponse.getOutputStream()).thenReturn(servletOutputStream);

        // Begin Test
        ProxyServlet ps = new ProxyServlet();
        ps.setExternalCache(externalCache);
        ps.doPost(channelSetupRequest, channelSetupResponse);

        ChannelConfig returnedChannelConfig = ChannelConfigFactory.convert(channelSetupResponse.getOutputStream().toString());
        assertEquals(channelConfig, returnedChannelConfig);

        SimId simId = SimStore.getSimId(channelConfig);
        SimStore simStore = new SimStore(externalCache, simId).open();
        assertTrue(simStore.exists());
        assertEquals(channelConfig, simStore.getChannelConfig());
    }

}
