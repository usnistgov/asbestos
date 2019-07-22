package gov.nist.asbestos.proxyWar;

import gov.nist.asbestos.asbestosProxy.events.EventStore;
import gov.nist.asbestos.asbestosProxy.events.EventStoreItem;
import gov.nist.asbestos.asbestosProxy.events.EventStoreSearch;
import gov.nist.asbestos.asbestosProxy.log.SimStore;
import gov.nist.asbestos.asbestosProxy.wrapper.ProxyServlet;
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


/**
 * this test depends on hapi running on port 8080 at /fhir/fhir
 */
class GetMetadataTest {

    @Mock
    HttpServletRequest channelSetupRequest;
    @Mock HttpServletResponse channelSetupResponse;

    @Mock HttpServletRequest getSetupRequest;
    @Mock HttpServletResponse getSetupResponse;

    private File externalCache;

    @Test
    void getMetadataTest() throws URISyntaxException, IOException {
        String testSession = "default";
        String channelId = "proxtest";
        externalCache = Support.findExternalCache();
        //
        // BUILD CHANNEL
        //
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

        //
        // BUILD GETMETADATA REQUEST/MOCK
        //
        getSetupRequest = mock(HttpServletRequest.class);
        getSetupResponse = mock(HttpServletResponse.class);
        Support.mockServlet(getSetupRequest, "http://localhost:8081/proxy/prox/" + testSession + "__" + channelId + "/Channel/metadata", "/proxy/prox/" + testSession + "__" + channelId + "/Channel/metadata");

        final StubServletOutputStream servletOutputStream2 = new StubServletOutputStream();
        when(getSetupResponse.getOutputStream()).thenReturn(servletOutputStream2);
        when(getSetupRequest.getInputStream()).thenReturn(new StubServletInputStream(""));

        ps.doGet(getSetupRequest, getSetupResponse);
        String out = servletOutputStream2.toString();

        // evaluate
        SimId simId = SimId.buildFromRawId(testSession + "__" + channelId).withActorType("fhir").withEnvironment("default");
        EventStoreItem mostRecent = new EventStoreSearch(externalCache, simId).getMostRecent();
        assertNotNull(mostRecent);
        EventStore eventStore = new EventStore(new SimStore(externalCache, simId),mostRecent.getFile());
        assertNotNull(eventStore);
        assertEquals(1, eventStore.getTaskCount());

        eventStore.selectClientTask();
        assertTrue(eventStore.getRequestHeaderFile().exists());

        eventStore.selectTask(0);
        assertTrue(eventStore.getRequestHeaderFile().exists());
        assertEquals("/proxy/prox/" + testSession + "__" + channelId + "/Channel/metadata", "/proxy/prox/" + testSession + "__" + channelId + "/Channel/metadata", eventStore.getRequestHeader().getPathInfo().toString());
        assertEquals("GET", eventStore.getRequestHeader().getVerb());
        assertTrue(eventStore.getResponseHeaderFile().exists());
        assertEquals(200, eventStore.getResponseHeader().getStatus());
        assertTrue(eventStore.getResponseBodyAsString().contains("CapabilityStatement"));
        assertTrue(eventStore.getResponseBodyFile().exists());
        assertTrue(eventStore.getResponseBodyStringFile().exists());

        eventStore.selectClientTask();
        assertTrue(eventStore.getRequestHeaderFile().exists());
        assertEquals("/proxy/prox/" + testSession + "__" + channelId + "/Channel/metadata", "/proxy/prox/" + testSession + "__" + channelId + "/Channel/metadata", eventStore.getRequestHeader().getPathInfo().toString());
        assertEquals("GET", eventStore.getRequestHeader().getVerb());
        assertTrue(eventStore.getResponseHeaderFile().exists());
        assertEquals(200, eventStore.getResponseHeader().getStatus());
        assertTrue(eventStore.getResponseBodyFile().exists());
        assertTrue(eventStore.getResponseBodyStringFile().exists());
        assertTrue(eventStore.getResponseBodyAsString().contains("CapabilityStatement"));

        assertTrue(out.contains("CapabilityStatement"));
    }
}
