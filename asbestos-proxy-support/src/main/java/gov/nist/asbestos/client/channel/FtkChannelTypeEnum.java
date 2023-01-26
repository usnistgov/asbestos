package gov.nist.asbestos.client.channel;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public enum FtkChannelTypeEnum {
    /**
     * Passthrough channel has no transformations and has all test collections implicitly added to it
     */
    fhir(true, null),
    /**
     * MHD channel has transformations because it is paired with a XDS Toolkit backend simulator
     */
    mhd(false,
            Arrays.asList(
            new IgTestCollection(IgNameConstants.MHDV_3_X, "Internal" ,"https://www.ihe.net/uploadedFiles/Documents/ITI/IHE_ITI_Suppl_MHD_Rev3-2_TI_2020-08-28.pdf"),
            new IgTestCollection(IgNameConstants.MHDV_4, "MHDv4_Internal", "https://profiles.ihe.net/ITI/MHD/4.0.1"),
            new IgTestCollection(IgNameConstants.MHDV_410, "MHDv410_Internal", "https://profiles.ihe.net/ITI/MHD/4.1.0")
            )
    );


    private boolean isPassthroughChannel = false;
    private ChannelTypeIgTestCollection channelTypeIgTestCollection;

    FtkChannelTypeEnum(boolean isPassthroughChannel, List<IgTestCollection> igTestCollections) {
        this.isPassthroughChannel = isPassthroughChannel;
        this.channelTypeIgTestCollection = new ChannelTypeIgTestCollection(this, igTestCollections);
    }

    public ChannelTypeIgTestCollection getChannelTypeIgTestCollection() {
        if (this.isPassthroughChannel) {
            /* implicitly add all IGs */
            ChannelTypeIgTestCollection all = new ChannelTypeIgTestCollection(this,
                    Arrays.stream(FtkChannelTypeEnum.values())
                            .filter(s -> ! s.equals(this) && s.getChannelTypeIgTestCollection().getIgTestCollections() != null)
                            .flatMap(s -> s.getChannelTypeIgTestCollection().getIgTestCollections().stream())
                            .collect(Collectors.toList()));

            return all;
        }

        return channelTypeIgTestCollection;
    }

    public String getMhdDocBase(IgNameConstants igNameConstant) {
        Optional<IgTestCollection> igTestCollectionOptional = getChannelTypeIgTestCollection().getIgTestCollections().stream()
                .filter(s -> s.getIgName().equals(igNameConstant))
                .findAny();
        if (igTestCollectionOptional.isPresent()) {
            return igTestCollectionOptional.get().getDocBase();
        }
        return null;
    }

}
