package gov.nist.asbestos.client.channel;

import java.util.List;

public class ChannelTypeIgTestCollection {
    private FtkChannelTypeEnum channelType;
    private List<IgTestCollection> igTestCollections;

    public ChannelTypeIgTestCollection(FtkChannelTypeEnum channelType, List<IgTestCollection> igTestCollections) {
        this.channelType = channelType;
        this.igTestCollections = igTestCollections;
    }

    public FtkChannelTypeEnum getChannelType() {
        return channelType;
    }

    public List<IgTestCollection> getIgTestCollections() {
        return igTestCollections;
    }
}
