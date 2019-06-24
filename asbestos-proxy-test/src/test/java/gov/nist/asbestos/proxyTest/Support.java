package gov.nist.asbestos.proxyWarTest;

import gov.nist.asbestos.sharedObjects.ChannelConfig;
import gov.nist.asbestos.sharedObjects.ChannelConfigFactory;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

public class Support {

    static ServletInputStream channelConfigAsServletInputStream(HttpServletRequest request, ChannelConfig channelConfig) {
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
        return servletInputStream;
    }

    static ChannelConfig getChannelConfig(String testSession, String channelId) {
        return new ChannelConfig()
                .setTestSession(testSession)
                .setChannelId(channelId)
                .setEnvironment("default")
                .setActorType("fhir")
                .setChannelType("passthrough")
                .setFhirBase("http://localhost:8080/fhir/fhir");
    }

    static File findExternalCache() throws URISyntaxException {
        Path ec = Paths.get(Support.class.getResource("/external_cache/findme.txt").toURI()).getParent();
        return ec.toFile();
    }

    static ChannelConfig sendChannelConfig(HttpServletRequest request, String uri, String pathinfo, String testSession, String channelId) {
        mockServlet(request, uri, pathinfo);
        Map<String, List<String>> req = new HashMap<>();
        req.put("content-type", Collections.singletonList("application/json"));
        when(request.getParameterMap()).thenReturn(req);
        when(request.getRequestURI()).thenReturn(uri);

        ChannelConfig channelConfig = Support.getChannelConfig(testSession, channelId);
        ServletInputStream servletInputStream = Support.channelConfigAsServletInputStream(request, channelConfig);
        try {
            when(request.getInputStream()).thenReturn(servletInputStream);
        } catch (Exception e) {
            fail(e);
        }
        return channelConfig;
    }

    static void mockServlet(HttpServletRequest request, String uri, String pathInfo) {
        Map<String, List<String>> req = new HashMap<>();
        req.put("content-type", Collections.singletonList("application/json"));
        req.put("accept", Collections.singletonList("application/json"));
        when(request.getParameterMap()).thenReturn(req);
        when(request.getHeaderNames()).thenReturn(new StringTokenizer("content-type accept"));
        when(request.getHeaders("content-type")).thenReturn(new StringTokenizer("application/json"));
        when(request.getHeaders("accept")).thenReturn(new StringTokenizer("application/json"));
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getPathInfo()).thenReturn(pathInfo);
    }
}
