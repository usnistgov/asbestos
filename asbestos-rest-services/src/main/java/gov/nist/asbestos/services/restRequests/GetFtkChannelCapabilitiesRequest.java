package gov.nist.asbestos.services.restRequests;

import com.google.gson.Gson;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.channel.FtkChannelTypeEnum;
import gov.nist.asbestos.mhd.channel.MhdIgImplEnum;
import gov.nist.asbestos.testcollection.TestCollectionPropertiesEnum;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
// 0 - empty
// 1 - app context
// 2 - "engine"
// 3 - "channelCapabilities"
// Return list of test collection names and its test collection details

public class GetFtkChannelCapabilitiesRequest {
    private static Logger log = Logger.getLogger(GetFtkChannelCapabilitiesRequest.class.getName());

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 4 && "channelCapabilities".equalsIgnoreCase(request.uriParts.get(3));
    }

    public GetFtkChannelCapabilitiesRequest(Request request) {
        request.setType(this.getClass().getSimpleName());
        this.request = request;
    }

    class TestCollectionDetail {
        String name;
        boolean server;
        String channel;
        boolean hidden;
        String fhirIgName;
        String fhirIgVersion;
    }

    static class ChannelIgCapability {
        String igName;
        String valTcName;
        String docBase;
        ChannelIgCapability(String igName, String valTcName, String docBase)  {
            this.igName = igName;
            this.valTcName = valTcName;
            this.docBase = docBase;
        }
    }

    static class ChannelIgCapabilities {
        FtkChannelTypeEnum channelType;
        List<ChannelIgCapability> igs;
    }

    public void run() throws IOException {
        request.announce("GetFtkChannelCapabilitiesRequest");

        List<ChannelIgCapabilities>  channelIgCapabilities = new ArrayList<>();

        for (FtkChannelTypeEnum channelType : FtkChannelTypeEnum.values()) {
           switch (channelType) {
               case mhd:
                   for (MhdIgImplEnum e : MhdIgImplEnum.values()) {
                        ChannelIgCapability channelIgCapability = new ChannelIgCapability(e.getIgName(), e.
                        channelIgCapabilities.add(
                   }
           }
        }



        List<TestCollectionDetail> collections = new ArrayList<>();
        List<String> names = request.ec.getTestCollectionNames();
        for (String name : names) {
            TestCollectionDetail detail = new TestCollectionDetail();
            detail.name = name;
            Properties props = request.ec.getTestCollectionProperties(name);
            detail.server = "server".equals(props.getProperty(TestCollectionPropertiesEnum.TestType.name()));
            detail.channel = props.getProperty(TestCollectionPropertiesEnum.Channel.name());
            detail.hidden = "true".equals(props.getProperty(TestCollectionPropertiesEnum.Hidden.name()));
            detail.fhirIgName = props.getProperty(TestCollectionPropertiesEnum.FhirIgName.name());
            detail.fhirIgVersion = props.getProperty(TestCollectionPropertiesEnum.FhirIgVersion.name());
            collections.add(detail);
        }
        String json = new Gson().toJson(collections);
        request.resp.setContentType("application/json");
        request.resp.getOutputStream().print(json);
        request.ok();
    }
}
