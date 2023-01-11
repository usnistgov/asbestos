package gov.nist.asbestos.services.restRequests;

import com.google.gson.Gson;
import gov.nist.asbestos.client.Base.Request;
import gov.nist.asbestos.client.channel.ChannelTypeIgTestCollection;
import gov.nist.asbestos.client.channel.FtkChannelTypeEnum;
import gov.nist.asbestos.mhd.channel.MhdIgImplEnum;
import gov.nist.asbestos.testcollection.TestCollectionPropertiesEnum;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;
// 0 - empty
// 1 - app context
// 2 - "engine"
// 3 - "channelTypeIgTestCollection"
// Return list of test collection names and its test collection details

public class GetFtkChannelTypeIgTestCollectionRequest {
    private static Logger log = Logger.getLogger(GetFtkChannelTypeIgTestCollectionRequest.class.getName());

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 4 && "channelTypeIgTestCollection".equalsIgnoreCase(request.uriParts.get(3));
    }

    public GetFtkChannelTypeIgTestCollectionRequest(Request request) {
        request.setType(this.getClass().getSimpleName());
        this.request = request;
    }


    public void run() throws IOException {
        request.announce("GetFtkChannelTypeIgTestCollectionRequest");

        List<ChannelTypeIgTestCollection>  channelTypeIgTestCollections =
        Arrays.stream(FtkChannelTypeEnum.values()).map(s -> s.getChannelTypeIgTestCollection()).collect(Collectors.toList());

        String json = new Gson().toJson(channelTypeIgTestCollections);
        request.resp.setContentType("application/json");
        request.resp.getOutputStream().print(json);
        request.ok();
    }
}
