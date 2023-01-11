package gov.nist.asbestos.asbestosRestServices.mhdChannel;

import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.channel.ChannelConfigFactory;
import gov.nist.asbestos.client.channel.FtkChannelTypeEnum;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class Support {

    static ServletInputStream channelConfigAsServletInputStream(HttpServletRequest request, ChannelConfig channelConfig) {
        String json = ChannelConfigFactory.convert(channelConfig);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(json.getBytes());
        ServletInputStream servletInputStream=new ServletInputStream(){
            public int read() {
                return byteArrayInputStream.read();
            }
        };
        try {
            Mockito.when(request.getInputStream()).thenReturn(servletInputStream);
        } catch (Exception e) {
            Assertions.fail(e);
        }
        return servletInputStream;
    }

    static ChannelConfig getChannelConfig(String testSession, String channelId) {
        return new ChannelConfig()
                .setTestSession(testSession)
                .setChannelName(channelId)
                .setEnvironment("default")
                .setActorType("fhir")
                .setChannelType(FtkChannelTypeEnum.fhir)
                .setFhirBase("http://localhost:7080/fhir");
    }

    static File findExternalCache() throws URISyntaxException {
        Path ec = Paths.get(Support.class.getResource("/external_cache/findme.txt").toURI()).getParent();
        return ec.toFile();
    }

    static ChannelConfig sendChannelConfig(HttpServletRequest request, String uri, String pathinfo, String testSession, String channelId) {
        mockServlet(request, uri, pathinfo);
        Map<String, List<String>> req = new HashMap<>();
        req.put("content-type", Collections.singletonList("application/json"));
        Mockito.when(request.getParameterMap()).thenReturn(req);
        Mockito.when(request.getRequestURI()).thenReturn(uri);

        ChannelConfig channelConfig = Support.getChannelConfig(testSession, channelId);
        ServletInputStream servletInputStream = Support.channelConfigAsServletInputStream(request, channelConfig);
        try {
            Mockito.when(request.getInputStream()).thenReturn(servletInputStream);
        } catch (Exception e) {
            Assertions.fail(e);
        }
        return channelConfig;
    }

    static void mockServlet(HttpServletRequest request, String uri, String pathInfo) {
        Map<String, List<String>> req = new HashMap<>();
        req.put("content-type", Collections.singletonList("application/json"));
        req.put("accept", Collections.singletonList("application/json"));
        Mockito.when(request.getParameterMap()).thenReturn(req);
        Mockito.when(request.getHeaderNames()).thenReturn(new StringTokenizer("content-type accept"));
        Mockito.when(request.getHeaders("content-type")).thenReturn(new StringTokenizer("application/json"));
        Mockito.when(request.getHeaders("accept")).thenReturn(new StringTokenizer("application/json"));
        Mockito.when(request.getRequestURI()).thenReturn(uri);
        Mockito.when(request.getPathInfo()).thenReturn(pathInfo);
    }


}
