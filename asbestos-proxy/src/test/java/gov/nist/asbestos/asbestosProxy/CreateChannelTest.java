package gov.nist.asbestos.asbestosProxy;

import gov.nist.asbestos.asbestosProxy.log.SimStore;
import gov.nist.asbestos.asbestosProxy.wrapper.ProxyServlet;
import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.sharedObjects.ChannelConfigFactory;
import gov.nist.asbestos.simapi.simCommon.SimId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CreateChannelTest {

    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;

    File externalCache;

    @Test
    void createChannelTest() throws URISyntaxException {
        externalCache = findExternalCache();
        // Begin Mock
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        Map<String, List<String>> req = new HashMap<>();
        req.put("content-type", Collections.singletonList("application/json"));
        when(request.getRequestURI()).thenReturn("/proxy/prox/");
        when(request.getParameterMap()).thenReturn(req);

        ChannelConfig channelConfig = new ChannelConfig()
                .setTestSession("default")
                .setChannelId("proxtest")
                .setEnvironment("default")
                .setActorType("fhir")
                .setChannelType("passthrough")
                .setFhirBase("http://localhost:8080/fhir/fhir");
        String json = ChannelConfigFactory.convert(channelConfig);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(json.getBytes());
        ServletInputStream servletInputStream=new ServletInputStream(){
            public int read() throws IOException {
                return byteArrayInputStream.read();
            }
        };
        try {
            when(request.getInputStream()).thenReturn(servletInputStream);
        } catch (Exception e) {
            fail(e);
        }
        final StubServletOutputStream servletOutputStream = new StubServletOutputStream();
        try {
            when(response.getOutputStream()).thenReturn(servletOutputStream);
        } catch (Exception e) {
            fail(e);
        }

        // Begin Test
        ProxyServlet ps = new ProxyServlet();
        ps.setExternalCache(externalCache);
        ps.doPost(request, response);

        ChannelConfig returnedChannelConfig = ChannelConfigFactory.convert(servletOutputStream.toString());
        assertEquals(channelConfig, returnedChannelConfig);

        SimId simId = SimStore.getSimId(channelConfig);
        SimStore simStore = new SimStore(externalCache, simId).open();
        assertTrue(simStore.exists());
        assertEquals(channelConfig, simStore.getChannelConfig());
    }

    private File findExternalCache() throws URISyntaxException {
        Path ec = Paths.get(getClass().getResource("/external_cache/findme.txt").toURI()).getParent();
        return ec.toFile();
    }

    class StubServletOutputStream extends ServletOutputStream {
        public ByteArrayOutputStream baos = new ByteArrayOutputStream();
        public void write(int i) throws IOException {
            baos.write(i);
        }

        public String toString() {
            return baos.toString();
        }
    }
}
