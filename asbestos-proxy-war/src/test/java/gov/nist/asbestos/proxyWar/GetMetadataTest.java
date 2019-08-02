package gov.nist.asbestos.proxyWar;

import gov.nist.asbestos.client.events.Event;
import gov.nist.asbestos.client.events.EventStoreItem;
import gov.nist.asbestos.client.events.EventStoreSearch;
import gov.nist.asbestos.client.events.Task;
import gov.nist.asbestos.client.log.SimStore;
import gov.nist.asbestos.asbestosProxy.wrapper.ProxyServlet;
import gov.nist.asbestos.simapi.simCommon.SimId;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import javax.servlet.ServletException;
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
    void getMetadataTest() throws URISyntaxException, IOException, ServletException {
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
        Event event = new Event(new SimStore(externalCache, simId),mostRecent.getFile());
        assertNotNull(event);
        assertEquals(1, event.getTaskCount());

        Task task = event.getClientTask();
        int taski = 0;

        assertTrue(event.getRequestHeaderFile(taski).exists());

        assertTrue(event.getRequestHeaderFile(taski).exists());
        assertEquals("/proxy/prox/" + testSession + "__" + channelId + "/Channel/metadata", "/proxy/prox/" + testSession + "__" + channelId + "/Channel/metadata", task.getRequestHeader().getPathInfo().toString());
        assertEquals("GET", task.getRequestHeader().getVerb());
        assertTrue(event.getResponseHeaderFile(taski).exists());
        assertEquals(200, task.getResponseHeader().getStatus());
        assertTrue(task.getResponseBodyAsString().contains("CapabilityStatement"));
        assertTrue(event.getResponseBodyFile(taski).exists());
        assertTrue(event.getResponseBodyStringFile(taski).exists());

        assertTrue(event.getRequestHeaderFile(taski).exists());
        assertEquals("/proxy/prox/" + testSession + "__" + channelId + "/Channel/metadata", "/proxy/prox/" + testSession + "__" + channelId + "/Channel/metadata", task.getRequestHeader().getPathInfo().toString());
        assertEquals("GET", task.getRequestHeader().getVerb());
        assertTrue(event.getResponseHeaderFile(taski).exists());
        assertEquals(200, task.getResponseHeader().getStatus());
        assertTrue(event.getResponseBodyFile(taski).exists());
        assertTrue(event.getResponseBodyStringFile(taski).exists());
        assertTrue(task.getResponseBodyAsString().contains("CapabilityStatement"));

        assertTrue(out.contains("CapabilityStatement"));
    }
}
